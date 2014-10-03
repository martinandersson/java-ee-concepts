package com.martinandersson.javaee.ejb.sessionbeans.testdriver;

import com.martinandersson.javaee.ejb.sessionbeans.AbstractSessionBean;
import com.martinandersson.javaee.ejb.sessionbeans.SingletonBean;
import com.martinandersson.javaee.ejb.sessionbeans.StatefulBean;
import com.martinandersson.javaee.ejb.sessionbeans.StatelessBean;
import com.martinandersson.javaee.utils.PhasedExecutorService;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Executes a particular {@code Operation} against session bean references of
 * {@code EJBType}.
 * 
 * The settings (Operation and EJBType), is passed to the Servlet in the POST
 * body as an {@code ExecutionSettings} object.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@WebServlet("/TestDriver")
public class TestDriver extends HttpServlet
{
    private static final Logger LOGGER = Logger.getLogger(TestDriver.class.getName());
    
    @EJB SingletonBean singleton1;
    @EJB SingletonBean singleton2;
    
    @EJB StatelessBean stateless1;
    @EJB StatelessBean stateless2;
    
    // Removed in __removeStatefulBeans():
    @EJB StatefulBean stateful1;
    @EJB StatefulBean stateful2;
    
    @Resource
    ManagedThreadFactory threadFactory;
    

    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        ObjectInputStream in = new ObjectInputStream(req.getInputStream());
        
        final ExecutionSettings settings;
        
        try {
            settings = (ExecutionSettings) in.readObject();
            LOGGER.info(() -> "Will run session bean test using: " + settings);
        }
        catch (ClassNotFoundException e) {
            final String msg = "I do not have the class file of whatever it is you're trying to send me. Message: " + e.getMessage();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");
            resp.setContentLength(msg.length());
            resp.getWriter().write(msg);
            return;
        }
        
        final Beans<?> beanRefs = getBeanReferences(settings.getEJBType());
        
        final Report report;
        
        switch (settings.getOperation()) {
            case CALL_TWO_SERIALLY:
                report = doCallTwoSerially(beanRefs);
                break;
                
            case CALL_ONE_SERIALLY:
                report = doCallOneSerially(beanRefs);
                break;
                
            case CALL_ONE_CONCURRENTLY:
                report = doCallOneConcurrently(beanRefs);
                break;
                
            case SELF_INVOKING_PROXY:
                // TODO: Wrap all other operations too??
                report = possibleException(this::doSelfInvokingProxy, beanRefs);
                break;
                
            default:
                throw new UnsupportedOperationException("Have no implementation for: " + settings.getOperation());
        }
        
        ObjectOutputStream out = new ObjectOutputStream(resp.getOutputStream());
        out.writeObject(report);
    }
    
    /**
     * Apply {@code beanReferences} to the provided {@code method} and return
     * whatever the method returns.<p>
     * 
     * All exceptions thrown by {@code method} is caught and a new report based
     * on the exception is returned instead.
     * 
     * @param method the consumer of {@code beanReferences}
     * @param beanReferences argument for the {@code method}
     * 
     * @return method's report on success, otherwise a report that wraps the
     *         exception
     */
    private Report possibleException(Function<Beans<?>, Report> method, Beans<?> beanReferences) {
        try {
            return method.apply(beanReferences);
        }
        catch (Exception e) {
            return new Report(e);
        }
    }
    
    /**
     * Returns a bean reference holder with references to this driver's injected
     * session beans depending on the provided type.
     * 
     * @param type session bean type
     * @return a bean reference holder with references to this driver's injected
     *         session beans depending on the provided type
     */
    private Beans<? extends AbstractSessionBean> getBeanReferences(EJBType type) {
        
        final AbstractSessionBean bean1, bean2;
        
        switch (type) {
            case SINGLETON:
                bean1 = singleton1; bean2 = singleton2;
                break;
                
            case STATELESS:
                bean1 = stateless1; bean2 = stateless2;
                break;
                
            case STATEFUL:
                bean1 = stateful1; bean2 = stateful2;
                break;
                
            default:
                throw new UnsupportedOperationException("Have no implementation for: " + type);
        }
        
        return new Beans<AbstractSessionBean>(){
            @Override public AbstractSessionBean getBean1() {
                return bean1; }
            @Override public AbstractSessionBean getBean2() {
                return bean2; }
        };
    }
    
    /**
     * See {@link Operation#CALL_TWO_SERIALLY}.
     * @param beans holder of the references
     * @return report ready to send to client
     * @see Operation#CALL_TWO_SERIALLY
     */
    private Report doCallTwoSerially(Beans<?> beans) {
        int bean1Id = beans.getBean1().getId(),
            bean2Id = beans.getBean2().getId();
        
        return new Report(bean1Id, bean2Id);
    }
    
    /**
     * See {@link Operation#CALL_ONE_SERIALLY}.
     * @param beans holder of the references
     * @return report ready to send to client
     * @see Operation#CALL_ONE_SERIALLY
     */
    private Report doCallOneSerially(Beans<?> beans) {
        int bean1Id = beans.getBean1().getId(),
            bean2Id = beans.getBean1().getId();
        
        return new Report(bean1Id, bean2Id);
    }
    
    /**
     * See {@link Operation#CALL_ONE_CONCURRENTLY}.
     * @param beans holder of the references
     * @return report ready to send to client
     * @see Operation#CALL_ONE_CONCURRENTLY
     */
    private Report doCallOneConcurrently(Beans<?> beans) {
        try (PhasedExecutorService executor = new PhasedExecutorService(2, threadFactory)) {
            AbstractSessionBean bean = beans.getBean1();
            
            List<Future<Integer>> ids = executor.invokeManyTimes(bean::sleepAndGetId, 2);
            
            return new Report(ids.get(0).get(), ids.get(1).get());
        }
        catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.WARNING, "Failed to execute the beans simultaneously.", e);
            return null;
        }
    }
    
    /**
     * See {@link Operation#SELF_INVOKING_PROXY}.
     * @param beans holder of the references
     * @return report ready to send to client
     * @see Operation#SELF_INVOKING_PROXY
     */
    private Report doSelfInvokingProxy(Beans<?> beans) {
        AbstractSessionBean proxyRef = beans.getBean1();
        int[] ids = proxyRef.getIdAndThatOf(proxyRef);
        return new Report(ids[0], ids[1]);
    }
    
    /**
     * Holder of session bean references.
     * 
     * @param <T> type of session bean
     */
    private interface Beans<T extends AbstractSessionBean> {
        T getBean1();
        T getBean2();
    }
    
    @PreDestroy
    private void __removeStatefulBeans()
    {
        /*
         * The Servlet is the client of our two stateful beans, no one else. If
         * we want to tie the stateful beans to a particular user, then we must
         * do an explicit JNDI lookup and put the reference in the HttpSession
         * object.
         * 
         * Each time we use @EJB to inject the beans, which in this case is just
         * once because only one instance of this servlet exist, then we
         * receive two new instances. It is the client, this servlet, that
         * manage the life cycle of our stateful beans (idle beans may also
         * timeout and be removed by the server). We have created them using
         * @EJB, now we must destroy them. Which is what we do in this method
         * just before the servlet goes down =)
         * 
         * If we don't want to bother with the @Remove method ourselves, then we
         * can let CDI do our job. Switch @EJB to @Inject and we get a
         * "contextual" instance with a scope. The scope is by default
         * @Dependent which is exactly what we want here. A "contextual"
         * stateful bean doesn't even need to have a @Remove method declared.
         * 
         * Using @Inject with stateful beans isn't a dumb idea. For the life
         * time of this servlet instance, the bean references will always point
         * to the same instances. After having called the @Remove method,
         * invoking the old and deprecated references will throw a
         * NoSuchEJBException. So what would this servlet do if we wanted a new
         * stateful bean for each request?
         * 
         * The servlet would need to manually lookup the bean references at the
         * start of the request using JNDI, pass these references around and
         * finally make sure that the beans are properly removed when the
         * request is done (perhaps try-finally on service()?).
         * 
         * And what if we wanted new beans for each user/session? Same
         * procedure, only put the references in the HttpSession object and then
         * use a HttpSesssionListener to discard them when the session is
         * invalidated.
         * 
         * All of this can be arranged for us by CDI (make sure to annotate the
         * bean class with @RequestScoped or @SessionScoped or have a producer
         * method/field that do so). Furthermore, this arrangement free us from
         * the burden of passing around the bean references since that is kind
         * of just what CDI do for us if other injection points also use the
         * same @Inject construct in favor of @EJB.
         *
         * Note that stateful beans are the only session beans that make sense
         * to @Inject as contextual instances. There's no value added using
         * @Inject instead of @EJB when injecting other session bean types. See
         * the CDI 1.1 specification, section "3.2 Session Beans" for more info.
         * In fact, @Inject cannot lookup a @Remote interface and if used on
         * other session bean types than stateful, might prove to be a
         * limitation for the application developer (for this particular issue,
         * a CDI producer method/field can solve the problem).
         */
        
        Stream.of(stateful1, stateful2).forEach(bean -> {
            try {
                bean.remove();
            }
            catch (Exception e) {
                /*
                 * Given GlassFish, we expect this to happen. "Illegal" loopback
                 * call make the bean invalid, making the proxy throw a
                 * NoSuchEJBException. See StatefulTest.loopbackIsNonPortable().
                 */
                LOGGER.log(Level.WARNING, "Caught exception while doing @Remove on stateful bean.", e);
            }
        });
    }
}