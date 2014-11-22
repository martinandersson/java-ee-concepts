package com.martinandersson.javaee.utils;

import static java.lang.System.out;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A high-level thread scheduler, running provided "threads" by insertion-order
 * and serially.<p>
 * 
 * A client-provided "thread" is more like a green thread; encapsulated in a
 * {@code Consumer<Yielder>}. Each consumer provided to the
 * {@linkplain #enqueue(Consumer...) enqueue(Consumer...)} method is executed
 * using a dedicated {@code Thread} instance not shared by other consumers.<p>
 * 
 * The consumer is referred to as a "stage". The client add statements to his
 * stage and schedule the stage for execution using a {@code ThreadScheduler}.
 * The stage is provided a {@linkplain Yielder} which the stage may use to
 * cooperate with other stages by yielding the CPU to them.<p>
 * 
 * Only one stage may run at at any given time, and their order of execution is
 * the same order as they were added to the scheduler.<p>
 * 
 * Unlike {@linkplain Thread#yield()}, {@linkplain Yielder#yield()} is not a
 * hint to the scheduler. If a stage invoke this method, then his executing
 * thread shall be made dormant, kindly waiting for his turn. It is his turn
 * again when all other stages (not yet completed) has been scheduled exactly
 * once according to their enqueued order.<p>
 * 
 * For my part, I needed a tool that made it easy to schedule jobs run in
 * different threads to ease testing/demonstration of overlapping transactions
 * and their inter-transactional behavior. For example, I wanted T1 to lookup a
 * JPA entity, then let T2 mutate state of the same JPA entity, only to provoke
 * an {@code OptimisticLockException} when T1 was scheduled again to persist his
 * version of the entity. In this case, mutating the version attribute can be
 * done in other ways, but I've needed a more lenient and unobtrusive way to
 * "program" sequential threads before and I believe that Java EE test code
 * benefit substantially from this class. If code is supposed to execute in well
 * defined steps across different threads, then only the semantics of a
 * scheduler can make that test code reliable and trustworthy.<p>
 * 
 * This class is executable and provide code samples in
 * {@linkplain #main(java.lang.String...) public void static main()}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class ThreadScheduler
{
    private final ThreadFactory threads;
    private final List<Consumer<Yielder>> stages;
    private final List<StageRunner> runners;
    
    
    
    {
        stages = new ArrayList<>();
        runners = new ArrayList<>();
    }
    
    /**
     * Initializes a newly created {@code ThreadScheduler} with a thread factory
     * provided by {@linkplain Executors#defaultThreadFactory()}.
     */
    public ThreadScheduler() {
        threads = Executors.defaultThreadFactory();
    }
    
    /**
     * Initializes a newly created {@code ThreadScheduler} with a thread factory
     * provided by {@linkplain Executors#defaultThreadFactory()} and specified
     * stages.
     * 
     * @param stages will be enqueued with the scheduler at once
     */
    public ThreadScheduler(Consumer<Yielder>... stages) {
        this();
        enqueue(stages);
    }
    
    /**
     * Initializes a newly created {@code ThreadScheduler} with a specified
     * thread factory.
     * 
     * @param factory must not be {@code null}
     */
    public ThreadScheduler(ThreadFactory factory) {
        threads = Objects.requireNonNull(factory);
    }
    
    /**
     * Initializes a newly created {@code ThreadScheduler} with a specified
     * thread factory and stages.
     * 
     * @param factory must not be {@code null}
     * @param stages will be enqueued with the scheduler at once
     */
    public ThreadScheduler(ThreadFactory factory, Consumer<Yielder>... stages) {
        this(factory);
        enqueue(stages);
    }
    
    
    
    /**
     * Enqueues a stage so that it may be executed by the scheduler.<p>
     * 
     * Same stage may be enqueued many times. One time or many, every provided
     * stage will always be executed by his own unique worker thread.
     * 
     * @param stages the stages
     * 
     * @return self for chaining
     */
    public synchronized ThreadScheduler enqueue(Consumer<Yielder>... stages) {
        Stream.of(stages)
                .map(Objects::requireNonNull) // or ".filter(Objects::nonNull)" but we prefer NPE.
                .forEach(this.stages::add);
        
        return this;
    }
    
    /**
     * Enqueues stages so that it may be executed by the scheduler.<p>
     * 
     * Same stage may be enqueued many times. One time or many, every provided
     * stage will always be executed by his own unique worker thread.
     * 
     * @param stages the stages
     * 
     * @return self for chaining
     */
    public synchronized ThreadScheduler enqueue(Collection<Consumer<Yielder>> stages) {
        stages.stream()
                .map(Objects::requireNonNull)
                .forEach(this.stages::add);
        
        return this;
    }
    
    
    /**
     * Start executing all previously provided stages serially and in order.<p>
     * 
     * This method block until all stages has been executed whether they
     * completed normally or one stage threw an exception. If a stage do throw
     * an exception, then the exception is propagated to the caller of this
     * method.<p>
     * 
     * As is the case with runtime exceptions during stage execution, if this
     * thread is interrupted while waiting, then on best-effort basis, the
     * remaining stages will not be scheduled again.
     * 
     * @return self for chaining
     * 
     * @throws IllegalStateException if fewer than two stages has been provided
     * @throws InterruptedException if client's thread is interrupted while waiting
     */
    public synchronized ThreadScheduler run() throws InterruptedException {
        if (stages.size() < 2) {
            throw new IllegalStateException("To few stages enqueued. Gotta have at least two or there's no purpose using this class!");
        }
        
        if (!runners.isEmpty()) {
            throw new IllegalStateException("You must call reset() first.");
        }
        
        List<Thread> workers = new ArrayList<>();
        
        try {
            // Create and start all runners
            stages.stream().forEach(stage -> {
                StageRunner r = new StageRunner(stage);
                runners.add(r);
                
                Thread t = threads.newThread(r);
                workers.add(t);
                
                t.start();
            });
            
            // Busy-wait for sluggish runners to arrive at their semaphore before opening the party
            for (StageRunner r : runners) {
                while (!r.permit.hasQueuedThreads()) {
                    translateThrowable(r.throwable);
                    Thread.yield();
                }
            }
            
            // Release first permit
            runners.get(0).permit.release();
            
            for (int i = 0; i < workers.size(); ++i) {
                workers.get(i).join(); // <-- happens-before: JLS 8 section 17.4.5 (so throwable field need not be marked volatile)
                translateThrowable(runners.get(i).throwable);
            }
        }
        finally {
            workers.stream().forEach(Thread::interrupt);
        }
        
        return this;
    }
    
    /**
     * Will clear this scheduler of all stages, making him ready to accept new
     * ones.<p>
     * 
     * Note that this method must be invoked between two calls to
     * {@linkplain #run() run()}.
     * 
     * @return self for chaining
     */
    public synchronized ThreadScheduler reset() {
        stages.clear();
        runners.clear();
        return this;
    }
    
    private void translateThrowable(Throwable t) throws InterruptedException {
        if (t == null) {
            return;
        }
        
        if (t instanceof InterruptedException) {
            throw (InterruptedException) t;
        }
        else if (t instanceof UncheckedInterruptedException) {
            throw (InterruptedException) t.getCause();
        }
        else if (t instanceof Error) {
            throw (Error) t;
        }
        else {
            // A ClassCastException here would mean that a stage is not a Consumer<Yielder> anymore!
            throw (RuntimeException) t;
        }
    }
    
    
    
    private class StageRunner implements Runnable {
        final Consumer<Yielder> stage;
        final Thread client;
        
        /**
         * Does not Thread.isAlive() suffice, why this flag?
         * 
         * Think about what could happen had we used Thread.isAlive().
         * 
         * When a stage ends (runner thread about to die), a permit for the next
         * runner will be released. But things may move so fast that the next
         * runner released finish a set of work instantly and call back to the
         * callee's semaphore who is just about to die. The newly awoken runner
         * think all is well and will go back to eternal sleep again; waiting
         * for his next permit that will never be released.
         * 
         * The current design solve that problem by making the runner that has
         * finished all his work mark himself as dead before releasing the next
         * permit.
         */
        final AtomicBoolean isAlive;
        final Semaphore permit;
        
        int myIndex;
        
        Throwable throwable;
        
        
        
        StageRunner(Consumer<Yielder> stage) {
            this.stage = stage;
            this.client = Thread.currentThread();
            
            this.isAlive = new AtomicBoolean(false);
            this.permit = new Semaphore(0);
        }
        
        
        
        @Override
        public void run() {
            // Deferred initialization logic:
            myIndex = runners.indexOf(this);
            assert myIndex > -1 : "StageRunner started before being added to List.";
            
            isAlive.set(true);
            
            try {
                /*
                 * The permit for the first scheduled thread is provided by the
                 * admin (client) thread after he has created all runners:
                 */
                permit.acquire();

                stage.accept(() -> {
                    if (client.isInterrupted()) {
                        throw new UncheckedInterruptedException(new InterruptedException("Client interrupted."));
                    }
                    
                    if (releaseNextPermit()) {
                        try {
                            this.permit.acquire();
                        }
                        catch (InterruptedException e) {
                            throw new UncheckedInterruptedException(e);
                        }
                    }
                    // else don't block, let client code continue
                });
            }
            catch (Throwable e) {
                throwable = e;
            }
            finally {
                isAlive.set(false);
                
                if (!client.isInterrupted() && throwable == null) {
                    // Stage completed successfully, so release next permit:
                    releaseNextPermit();
                }
            }
        }
        
        private boolean releaseNextPermit() {
            StageRunner next = nextRunnerAlive();
            if (next != null) {
                assert next.permit.availablePermits() == 0 : "Two stages must not run simultaneously.";
                next.permit.release();
                return true;
            }
            return false;
        }
        
        private StageRunner nextRunnerAlive() { // ..which is waiting for a permit we assume..
            // Look at all indices after our position
            for (int i = myIndex + 1; i < runners.size(); ++i) {
                StageRunner r = runners.get(i);
                
                if (r.isAlive.get()) {
                    return r;
                }
            }
            
            // Look at all indices before our position
            for (int i = 0; i < myIndex; ++i) {
                StageRunner r = runners.get(i);
                
                if (r.isAlive.get()) {
                    return r;
                }
            }
            
            return null;
        }
    }
    
    private static class UncheckedInterruptedException extends RuntimeException {
        UncheckedInterruptedException(InterruptedException e) {
            super(e);
        }
    }
    
    
    
    /**
     * A yielder provide method {@linkplain #yield() yield()} that a stage may
     * invoke to "yield" and let another stage begin or resume his execution
     * path until that thread yield or die.
     */
    @FunctionalInterface
    public interface Yielder {
        /**
         * Yield the calling thread's use of CPU to the next stage ready to
         * execute.<p>
         * 
         * This method has no effect if there is no other stages left to
         * execute.<p>
         * 
         * It is imperative that client code must not try-catch exceptions
         * caused by this method. Doing so may have a detrimental effect on the
         * runtime behavior of {@code ThreadScheduler}.
         * 
         * @see ThreadScheduler
         * @see Yielder
         */
        void yield();
    }
    
    
    
    /**
     * Demonstrates how to use the {@code ThreadScheduler} class.
     * 
     * @param ignored ignored
     * 
     * @throws InterruptedException for simplicity only, should not happen
     */
    public static void main(String... ignored) throws InterruptedException {
        final ThreadScheduler scheduler = new ThreadScheduler();
        
        // BASIC USAGE
        // -----------
        
        Consumer<Yielder> T1 = yielder -> {
            out.println("T1 begin.");
            
            yielder.yield(); // <-- will yield to T2 and then block-wait
            
            out.println("T1 continued.");
            
            // Thread die and T2 continues.
        };
        
        Consumer<Yielder> T2 = yielder -> {
            out.println("T2 begin.");
            
            yielder.yield(); // <-- will yield back to T1 and then block-wait
            
            out.println("T2 continued.");
            
            // No more stages for anyone: client's call to scheduler.run() return.
        };
        
        /*
         * First block, or scene, of the T1 stage will run until yielder.yield().
         * T1's thread goes to sleep and T2 stage is executed using another
         * thread, until T2 call yielder.yield(). Then, T1's thread wakeup and
         * do one print to console before thread death. Then, T2's thread is
         * awakened and print one line to the console before he die. Finally,
         * control is returned to the client's thread.
         * 
         * No stage is ever executed at the same time another stage is, and each
         * stage has his own dedicated worker thread assigned to him.
         */
        scheduler.enqueue(T1, T2)
                 .run()
                 .reset();
        
        out.println("Client code is alive again!");
        
        
        
        // CAN ONLY YIELD WITH EFFECT IF THERE'S OTHER STAGES TO RUN
        // ---------------------------------------------------------
        out.println();
        
        T1 = yielder -> {
            out.println("T1 will yield to T2");
            yielder.yield();
            
            out.println("T1 about to yield many times over to no one.");
            yielder.yield();
            yielder.yield();
            
            out.println("T1 did not block =)");
        };
        
        T2 = yielder -> {
            out.println("T2 over and out!");
        };
        
        scheduler.enqueue(T1, T2)
                 .run()
                 .reset();
        
        
        
        // 1) SAFE TO ENQUE SAME STAGE MANY TIMES, 2) EXCEPTIONS IMMIDATELY PROPAGATE AND REMAINING STAGES NOT EXECUTED
        // ------------------------------------------------------------------------------------------------------------
        out.println();
        
        /*
         * "Atomic" is not needed here as stages execute serially, value need
         * only be effectively final. However, I prefer a clean API over
         * nighthacks such as "int[] value = {0}".
         */
        AtomicInteger value = new AtomicInteger(2);
        
        Consumer<Yielder> divider = yielder -> {
            int divisor = value.getAndDecrement();
            
            out.print(Thread.currentThread().getId() + ": 1 / " + divisor + " = ");
            out.println(1 / divisor);
        };
        
        scheduler.enqueue(divider, divider, divider, divider);
        
        try {
            scheduler.run();
        }
        catch (ArithmeticException e) {
            out.println("Oops! Cannot divide by zero. Fourth divider was not executed.");
        }
        
        scheduler.reset();
        
        
        
        // INTERRUPTING CLIENT MAKE REMAINING STAGES NOT EXECUTE (BEST-EFFORTS BASIS)
        // --------------------------------------------------------------------------
        out.println();
        
        Thread client = Thread.currentThread();
        
        Consumer<Yielder> helloWorld = yielder -> {
            out.println("Hello World!");
            
            // Client's thread is having a coffee at row 524, let's wake him up!
            client.interrupt();
        };
        
        scheduler.enqueue(helloWorld, helloWorld);
        
        try {
            scheduler.run();
        }
        catch (InterruptedException e) {
            out.println("Client was interrupted and only one \"Hello World\" was printed.");
        }
    }
}