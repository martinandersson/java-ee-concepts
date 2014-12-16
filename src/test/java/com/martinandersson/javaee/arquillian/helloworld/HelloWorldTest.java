package com.martinandersson.javaee.arquillian.helloworld;


import java.util.logging.Logger;
import javax.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

/**
 * A hello world program for Arquillian. Arquillian is a testing framework
 * for running tests on the <i>inside</i> of a Java EE application server. We
 * use ShrinkWrap to build the archive that is deployed to the server.<p>
 * 
 * 
 * 
 * <h2>How to execute</h2>
 * 
 * Using NetBeans: Start GlassFish, then execute this test. Continue reading for
 * more detailed information and some comments related to Eclipse.<p>
 * 
 * 
 * 
 * <h2>Server adapters and Maven profiles</h2>
 * 
 * Arquillian connects to the server using an adapter. For Arquillian to
 * discover the adapter, we need to put the adapter on the classpath. If there
 * is no adapter on the classpath, or multiple adapters exist on the classpath,
 * then Arquillian will break and the test will fail. One could manually
 * nighthack the POM file's dependencies before each run, alas this wear and
 * tear on the programmer's fingers. Step into the picture: Maven build
 * profiles.<p>
 * 
 * A POM file may include any number of Maven build profiles. A profile makes it
 * easy to specify settings and dependencies that should be applied only when a
 * particular profile is chosen/activated. In our POM file, there exists two
 * profiles ready for use: "glassfish-remote" and "wildfly-remote". Basically,
 * what these profiles do is to put a GlassFish server adapter or a WildFly
 * server adapter on the classpath. Which adapter goes into the classpath and
 * therefore which server to execute the test on, is determined by activating
 * one or the other profile.<p>
 * 
 * "glassfish-remote" is the default profile. Therefore, as long as you have
 * the GlassFish server running, execute this test file as you normally would.
 * In NETBEANS, right-click anywhere in this file and select "Test File". In
 * ECLIPSE, right-click in the file and select "Run As" -> "JUnit Test".<p>
 * 
 * If you want to switch to "wildfly-remote", in NETBEANS, chose the profile
 * in the only drop-down menu you see in the toolbar. Simple as that. ECLIPSE is
 * always more troublesome. You might want to begin your googleling using
 * keywords "run configurations", and good luck.<p>
 * 
 * While reading the rest of this text, be sure to examine the JavaDoc of
 * Arquillian as well: <pre>{@code
 * 
 *     http://docs.jboss.org/arquillian/api/latest/
 * }</pre>
 * 
 * Note that both servers default to using port 8080 so you cannot have them
 * running at the same time unless you configure them differently. The
 * administrator Web GUI defaults to port 4848 for GlassFish and port 9990 for
 * WildFly.<p>
 * 
 * 
 * 
 * <h2>How Arquillian (and the source code of this file) works</h2>
 * 
 * JUnit will launch the test but Arquillian will be the one running it. For
 * that we use the JUnit provided {@code @RunWith} annotation to specify
 * Arquillian as the test runner.<p>
 * 
 * First thing Arquillian do is to look for a public static method annotated
 * with {@code @Deployment}. If this method cannot be found, the test case will
 * continue as a regular JUnit test case and nothing will be deployed to the
 * server. Arquillian will still be the test runner and using Arquillian as a
 * pure client side testing facility can be useful too. For example, Arquillian
 * provides the {@code @InSequence} annotation that allow the developer to more
 * easily and intuitively specify a test order as compared with JUnit's
 * {@code @FixMethodOrder(MethodSorters.NAME_ASCENDING)} which require ascending
 * test method names (see file "ClientServerTest.java" in the "clientserver"
 * package for an example).<p>
 * 
 * If the {@code @Deployment} annotated method is found, it will be invoked and
 * asked for an {@code Archive} or a {@code Descriptor}. In this test class, we
 * will build a {@code JavaArchive}. The built archive will in turn be wrapped
 * in another archive which is the archive that Arquillian actually deploys.
 * Huh?<p>
 * 
 * Our deployment is like a "mini program", leaving us to decide exactly what is
 * put on the server's classpath and therefore what is accessible from our
 * program/test from within the server. Arquillian need to add a few things in
 * order to make the test work. For example, the class file that our test belong
 * to ({@code HelloWorldTest.class}, excluding class files of super types). Some
 * of this stuff depends on the target environment, and how the archive is built
 * may also depend on the target environment. Therefore, Arquillian will
 * decorate our archive and wrap it with another archive in which Arquillian
 * also put all these other runtime dependencies.<p>
 * 
 * 
 * 
 * <h2>Arquillian configuration</h2>
 * 
 * Arquillian is configured using "./src/test/resources/arquillian.xml". Open
 * this file and you'll see that we make use of only one custom property:
 * "deploymentExportPath". Current configuration will make the real deployed
 * archive be stored in the project's target directory ("./target/deployments").
 * The archive we gave Arquillian through our {@code @Deployment} annotated
 * method can be found in the deployed archive. Keeping the deployed archive
 * offer a convenient way to inspect exactly what things went to the server.<p>
 * 
 * 
 * 
 * <h2>Target containers and test environments</h2>
 * 
 * Arquillian can run the test in many different containers. Often, the
 * container is a real Java EE server (JBoss, WildFly [= JBoss rebranded],
 * GlassFish). The container may be a "lightweight" Java EE server (Tomcat,
 * Jetty). The container may also be like a subset of a server: a standalone
 * module of the server, the CDI/EJB provider (Weld, OpenEJB).<p>
 * 
 * In this project, we will only target real application servers (see below) and
 * when we do, the test runner will most likely be executed as a {@code
 * Servlet} - on the inside of the server.<p>
 * 
 * All servlets reside in the web tier of an application server and is a
 * "managed bean" or an "application component" (feel free to pick your flavor).
 * What that means is that our test class have access to services from the
 * server such as the famous {@code @Inject} annotation. But, the servlet is not
 * a Java Enterprise Bean (EJB). Therefore, the test method will not
 * "automatically execute" in a transaction context and you cannot use
 * annotations that only apply to EJB:s (a servlet that want a transaction must
 * {@code @Inject UserTransaction} and call {@code UserTransaction.begin()}).<p>
 * 
 * There exist Arquillian extensions that do all kinds to "enrichment" of
 * your test, for example one that manage transactions. However, Arquillian
 * hacks does not represent the environment that our application will run in
 * once it is deployed to a production server. Also, such practice will severely
 * hinder the learning progression for the aspiring Java EE student as different
 * technologies and component environments is mixed together into one huge
 * poisoned cocktail. As I will shortly return to, Arquillian was meant to write
 * "real tests" - let's keep it that way!<p>
 * 
 * See more details here and feel free to make up your own mind:
 * <pre>{@code
 * 
 *     https://docs.jboss.org/author/display/ARQ/Test+enrichers
 *     https://docs.jboss.org/author/display/ARQ/Extensions
 * 
 * }</pre>
 * 
 * 
 * 
 * <h2>What is an embedded, managed and remote container?</h2>
 * 
 * An <strong>embedded container</strong> is run within the same JVM as the
 * Arquillian test do. The embedded container is fully managed by Arquillian.
 * Arquillian start and stop the embedded container. A <strong>managed
 * container</strong> is also managed by Arquillian, but the container will be
 * launched in a separate JVM. Given that both embedded and managed containers
 * are.. well managed, you might find it a bit strange that only one of them is
 * called managed. The <i>managed container</i> was actually called <i>local
 * container</i> once upon time.<p>
 * 
 * Arquillian does <u>not</u> manage the <strong>remote container</strong>.
 * Today, the remote container can only be a real application server and not a
 * subset thereof. The server has to be manually started by the developer before
 * the test executes.<p>
 * 
 * Arquillian prides itself with the slogan "write real tests". As has already
 * been commented upon, using extensions and features that go beyond what the
 * application server provides makes a real test go unreal. If the test is also
 * deployed to an embedded or managed container, then we're quite far away from
 * the reality that Arquillian was meant to provide. My personal advise is to
 * run your test in real remote application servers only. There are three
 * reasons:<p>
 * 
 * 1) Mocking or substitution is the work of the Devil. We want the real
 * deal - period.<p>
 * 
 * 2) Doing the managing of non-remote servers take a lot of time. For each
 * single test run, of which you execute in the hundreds on a single day,
 * Arquillian has to start and stop the server. If you strive for "cohesion" or
 * locality in your project, you might also feel obliged to let Maven or any
 * other build tool actually unpack the server dependencies for each test run
 * and not store the container files outside the target directory. Contrast this
 * time-consuming burden with the test workflow of using a remote server. You
 * start the server only once. Also, you are in full control of the server which
 * makes debugging of your test case so much easier. The deployment phase to the
 * server may in theory take just a little bit longer than deployment to a JVM
 * local embedded one, but me personally, I haven't even noticed (note 1).<p>
 * 
 * 3) This is the most important reason that will save you the most time:
 * <strong>Point 1 is true</strong>. Yes, embedded and managed containers have a
 * habit of not working and meet the requirements of Java EE specifications. Of
 * course, you will only discover that on the third day of researching your
 * self-acclaimed fault and reading through a vast amount of specifications,
 * until you gave up and deployed your test to a real, that is remote, server.
 * Then you discovered that everything was working just fine.<p>
 * 
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class HelloWorldTest
{
    private static final Logger LOGGER = Logger.getLogger(HelloWorldTest.class.getName());
    
    @Deployment
    private static JavaArchive buildDeployment() {
        /*
         * If you don't provide a name in the next statement and only provide
         * the archive class file, then a random name will be generated. Given
         * that the name will be random, old deployment files won't be
         * overwritten and the target directory will continune to grow until
         * the project is cleaned.
         * 
         * Also note that ShrinkWrap has a "fluent interface", so we wouldn't
         * need to use three statements here, we could have used just one.
         */
        
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, HelloWorldTest.class.getSimpleName() + ".jar");
        jar.addClass(HelloWorldEJB.class);
        
        LOGGER.info(() -> jar.toString(true).replace("\n", "\n\t"));
        
        return jar;
    }
    
    
    /*
     *  --------------
     * | DEPENDENCIES |
     *  --------------
     */
    
    @Rule
    public TestName name = new TestName();
    
    @EJB
    HelloWorldEJB helloWorldEJB;
    
    

    
    
    
    /*
     *  --------------------
     * | LIFE CYCLE LOGGING |
     *  --------------------
     */
    
    @BeforeClass
    public static void __afterDeploy() {
        // Goes to the client's console:
        LOGGER.info(() -> "DEPLOYED: " + HelloWorldTest.class.getSimpleName());
    }
    
    @AfterClass
    public static void __afterUndeploy() {
        // Goes to the client's console:
        LOGGER.info(() -> "UNDEPLOYED: " + HelloWorldTest.class.getSimpleName());
    }
    
    @Before
    public void __beforeTest() {
        // Goes to the server log:
        LOGGER.info(() -> "******** EXECUTING: " + name.getMethodName() + " ********");
    }
    
    @After
    public void __afterTest() {
        // Goes to the server log:
        LOGGER.info("******** FINISHED ********");
    }
    
    
    
    /*
     *  -------
     * | TESTS |
     *  -------
     */
    
    /**
     * This test is executed on the Server.
     */
    @Test
    public void helloWorldEJB_toUpperCase() throws Exception {
        LOGGER.info("Where is this printed? In the server log!");
        
        /*
         * Q1: Will the EJB instance we refer to next already have been created
         *     (during "injection") or will the container defer the creation
         *     until the first time the bean is used?
         */
        
        final String reply = helloWorldEJB.toUpperCase("Hello World");
        
        assertEquals("HELLO WORLD", reply);
    }
}



/*
 * ANSWERS
 * -------
 * 
 * Q1: Look in the server log. The instance is created lazily when it is used
 *     for the first time. This behavior is not specified in the EJB 3.1
 *     specification (it is for @Singleton, see 4.8.1). The server log also
 *     prove that the object pool of stateless EJB:s is gradually filled over
 *     time. Not all pooled beans are created in one single shot.
 */



/*
 * NOTES
 * -----
 * 
 * Note 1: If the "remote" server is accessed using the loopback interface
 *         (localhost), then the deployment file and the communication between
 *         the JVM:s won't even go through the network hardware as it is
 *         bypassed.
 */