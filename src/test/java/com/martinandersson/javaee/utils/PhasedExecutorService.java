package com.martinandersson.javaee.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Worker threads used by this executor service wait for each other and try to
 * start tasks at the same time.<p>
 * 
 * Thus, a batch of tasks is executed in "pulses" or "phases", maximizing the
 * probability of parallelism. Otherwise, phase shifting and non-synchronized
 * starts of task execution may have a detrimental effect on the degree of
 * effective parallelism if worker threads execute their logic at different
 * times.<p>
 * 
 * A pulse is executed only if enough tasks has been supplied to feed all worker
 * threads. A residue is executed as-is by free worker threads. For example,
 * configure this service to use 5 threads, but feeding him only with 14 tasks
 * will account for two synchronized pulses, the last 4 tasks will be executed
 * as soon as worker threads become available.<p>
 * 
 * Supplying fewer tasks than worker threads make this executor service unable
 * to execute a pulse. Given that pulses or phases of execution is the entire
 * purpose of this executor service, then that is considered an error and the
 * batch of tasks will be rejected with an {@code IllegalArgumentException}.<p>
 * 
 * This executor service should be <strong>used sparingly by test code
 * only</strong> and has two constraints enforcing this contract:
 * 
 * <ol>
 *   <li>All methods declared by {@linkplain Executor} and {@linkplain
 *       ExecutorService} that accept one single task will throw a {@code
 *       UnsupportedOperationException}.</li><br />
 * 
 *   <li>No batches of tasks are allowed to be executed in parallel. If one
 *       batch is already executing when another batch is supplied, an {@code
 *       IllegalStateException} will be thrown.</li><br />
 * </ol>
 * 
 * The constraints leave only two task-accepting methods left to use:
 * {@linkplain #invokeAll(java.util.Collection) invokeAll(Collection)} and
 * {@linkplain #invokeAll(java.util.Collection, BeforeEachPhase)
 * invokeAll(Collection, BeforeEachPhase)}. The latter being a specialized
 * method only for {@code PhasedExecutorService} that makes client code able
 * to intercept each phase for debugging or early termination.<p>
 * 
 * Furthermore, the thread pool is constructed and "destroyed" for each batch
 * of tasks executed. This makes overall memory consumption low and does not
 * require an explicit shutdown by the client as is otherwise the case with
 * executor services.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class PhasedExecutorService implements ExecutorService
{
    private final AtomicBoolean isExecutingTasks = new AtomicBoolean();
    
    private final ThreadFactory threadFactory;
    
    private final int threadCount;
    
    private volatile Phaser phaser;
    
    private volatile boolean shutdown = false;
    
    
    
    public PhasedExecutorService() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    public PhasedExecutorService(int threadCount) {
        this(threadCount, runnable -> {
            Thread t = new Thread(runnable);
            t.setDaemon(true);
            return t;
        });
    }
    
    public PhasedExecutorService(int threadCount, ThreadFactory threadFactory) {
        if (Runtime.getRuntime().availableProcessors() < 2)
            throw new IllegalStateException("Impossible to execute provided tasks in parallel. You have to few CPU:s lol.");
        
        if (threadCount < 2)
            throw new UnsupportedOperationException("Use a real executor service.");
        
        this.threadCount = threadCount;
        
        this.threadFactory = Objects.requireNonNull(threadFactory, "threadFactory is null");
    }
    
    
    
    @Override
    public void shutdown() {
        shutdown = true;
    }

    /**
     * <strong><i>This method will always return an empty list as this
     * feature has not been implemented yet.</i></strong><p>
     * 
     * {@inheritDoc}
     * 
     * @return an empty {@code List<Runnable>}.
     */
    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        
        if (isExecutingTasks.get() && phaser != null) {
            try {
                phaser.forceTermination();
            }
            catch (NullPointerException e) {
                ; // Too late :'(
            }
        }
        
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean isShutdown() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isTerminated() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * {@inheritDoc}<p>
     * 
     * Note that the task-start synchronization happens only if there are at
     * least as many tasks as there are threads. The residual lot is executed
     * "unphased".<p>
     * 
     * @param <T> type of result
     * @param tasks tasks to execute
     * 
     * @throws RejectedExecutionException if any task cannot be scheduled for
     *         execution
     * @throws IllegalStateException if service is already doing a batch
     * @throws IllegalArgumentException if fewer tasks than worker threads is
     *         supplied
     * 
     * @return all futures for management of task and retrieval of result
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        return invokeAll(tasks, (ignored1, ignored2) -> false);
    }
    
    /**
     * See {@linkplain #invokeAll(Collection)}.
     * 
     * @param <T>
     * @param tasks
     * @param beforeEachPhase
     * 
     * @return 
     */
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, BeforeEachPhase beforeEachPhase) {
        if (shutdown) {
            throw new RejectedExecutionException("Executor service has been shutdown.");
        }
        
        if (isExecutingTasks.getAndSet(true)) {
            throw new IllegalStateException("Already doing a batch.");
        }
        
        final int size = tasks.size();
        
        if (size < threadCount) {
            throw new IllegalArgumentException("To few tasks for a proper synchronization across all threads. Consider using a normal executor service.");
        }
        
        // All worker threads poll tasks from this Queue:
        Queue<FutureTask<T>> __tasks = new ConcurrentLinkedQueue<>(
                tasks.stream().map(FutureTask::new).collect(Collectors.toList()));
        
        // Amount of tasks that has been picked from Queue (ConcurrentLinkedQueue has an expensive .size()):
        AtomicInteger pickedTasks = new AtomicInteger();
        
        // Returned to caller:
        List<Future<T>> asFutures = new ArrayList<>(__tasks);
        
        phaser = new Phaser(threadCount) {
            @Override protected boolean onAdvance(int phase, int registeredParties) {
                return beforeEachPhase.onAdvance(phase, registeredParties); }
        };
        
        CyclicBarrier finished = new CyclicBarrier(threadCount + 1, () -> {
            // Inverted order:
            phaser = null;
            isExecutingTasks.set(false);
        });
        
        Runnable waitForCompletion = () -> {
            try { finished.await(); }
            catch (InterruptedException | BrokenBarrierException e) { ; }
        };
        
        IntStream.range(0, threadCount).forEach(x -> threadFactory.newThread(() -> {
            FutureTask<T> task;
            while (shutdown == false && (task = __tasks.poll()) != null)
            {
                final int hasExecuted = pickedTasks.getAndIncrement();
                
                if (task.isCancelled())
                    continue;
                
                /*
                 * Use phaser if buddies are already waiting, otherwise
                 * calculate wether or not we have enough tasks left for a
                 * new phase, in which case we accept to be the first thread
                 * waiting for other workers.
                 */

                if (phaser.getArrivedParties() > 0 || (size - hasExecuted) >= threadCount)
                    phaser.arriveAndAwaitAdvance();
                
                if (phaser.isTerminated()) {
                    break;
                }

                // Go!
                task.run(); // <-- FutureTask.run() deals properly with cancelled state and Throwable
            }
            
            waitForCompletion.run();
        }).start());
        
        // According to ExecutorService, we must wait for all to complete:
        waitForCompletion.run();
        
        return asFutures;
    }
    
    
    
    /*
     *  ---------------
     * | NOT SUPPORTED |
     *  ---------------
     */
    
    /** 
     * Not supported.
     * 
     * @param tasks ignored
     * @param timeout ignored
     * @param unit ignored
     * 
     * @return nothing
     * 
     * @throws UnsupportedOperationException always
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Use a real executor service.");
    }
    
    /** 
     * Not supported.
     * 
     * @param task ignored
     * 
     * @return nothing
     * 
     * @throws UnsupportedOperationException always
     */
    @Override
    public <T> Future<T> submit(Callable<T> task) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Use a real executor service.");
    }

    /** 
     * Not supported.
     * 
     * @param task ignored
     * 
     * @return nothing
     * 
     * @throws UnsupportedOperationException always
     */
    @Override
    public <T> Future<T> submit(Runnable task, T result) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Use a real executor service.");
    }
    
    /** 
     * Not supported.
     * 
     * @param task ignored
     * 
     * @return nothing
     * 
     * @throws UnsupportedOperationException always
     */
    @Override
    public Future<?> submit(Runnable task) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Use a real executor service.");
    }
    
    /** 
     * Not supported.
     * 
     * @param tasks ignored
     * 
     * @return nothing
     * 
     * @throws UnsupportedOperationException always
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Use a real executor service.");
    }
    
    /** 
     * Not supported.
     * 
     * @param tasks ignored
     * @param timeout ignored
     * @param unit ignored
     * 
     * @return nothing
     * 
     * @throws UnsupportedOperationException always
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Use a real executor service.");
    }
    
    /** 
     * Not supported.
     * 
     * @param command ignored
     * 
     * @throws UnsupportedOperationException always
     */
    @Override
    public void execute(Runnable command) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Use a real executor service.");
    }
    
    
    
    
    
    
    /*
     *  ----------------------------
     * | BEFORE-EACH-PHASE CALLBACK |
     *  ----------------------------
     */
    
    /**
     * Callback invoked before each new phase or pulse is executed. 
     */
    @FunctionalInterface
    public interface BeforeEachPhase {
        /**
         * Callback invoked before each new phase or pulse is executed.
         * 
         * @param phase the next phase, starts at {@code 0}
         * @param parties amount of threads waiting to execute a task, will
         *        always be the thread count
         * 
         * @return {@code true} if the executor service should abort the phase
         *         and terminate, otherwise {@code false}
         */
        boolean onAdvance(int phase, int parties);
    }
}