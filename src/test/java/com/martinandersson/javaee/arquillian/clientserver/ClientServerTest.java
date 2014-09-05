package com.martinandersson.javaee.arquillian.clientserver;

import com.martinandersson.javaee.arquillian.helloworld.HelloWorldEJB;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
 * This test will make a HTTP GET request from client to server.<p>
 * 
 * This class is a skeleton and demonstration of just how real a test can be.
 * The client- and server code is executed in two different JVM:s.<p>
 * 
 * If you haven't already, read and study {@code HelloWorldTest} before
 * reading through this file.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@RunWith(Arquillian.class)
public class ClientServerTest
{
    private static final Logger LOGGER = Logger.getLogger(ClientServerTest.class.getName());
    
    
    
    /*
     *  --------------
     * | DEPENDENCIES |
     *  --------------
     */
    
    @Rule
    public TestName name = new TestName();
    
    @Inject
    HelloWorldEJB helloWorldEJB;
    
    @Deployment
    private static WebArchive buildDeployment()
    {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ClientServerTest.class.getSimpleName() + ".war");
        war.addClasses(HelloWorldEJB.class, ServerAPI.class);
        
        LOGGER.info(() -> war.toString(true).replace("\n", "\n\t"));
        
        return war;
    }
    
    
    
    /*
     *  --------------------
     * | LIFE CYCLE LOGGING |
     *  --------------------
     */
    
    @BeforeClass
    public static void __afterDeploy() {
        // Goes to the client's console:
        LOGGER.info(() -> "DEPLOYED: " + ClientServerTest.class.getSimpleName());
    }
    
    @AfterClass
    public static void __afterUndeploy() {
        // Goes to the client's console:
        LOGGER.info(() -> "UNDEPLOYED: " + ClientServerTest.class.getSimpleName());
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
     * Server..<p>
     * 
     * will assert that so far, not one single request has been made to the
     * {@code ServerAPI} servlet.
     */
    @Test
    @InSequence(1)
    public void server_assertZeroRequests()
    {
        // Is executed on the Server in another JVM:
        assertEquals("Number of HTTP requests", 0, ServerAPI.getRequestCount());
    }
    
    /**
     * Client..<p>
     * 
     * will make a request to the server and assert the expected response.
     * 
     * @param url the "context root" of the application, provided by Arquillian
     * 
     * @throws java.io.IOException on all kinds of errors
     */
    @Test
    @RunAsClient // <-- Have you noticed?
    @InSequence(2)
    public void client_makeRequest(@ArquillianResource URL url) throws IOException
    {
        // Is executed in the original Arquillian JVM, acting like a client.
        
        LOGGER.info(() -> "Deployed application URL: " + url);
        
        URL helloServlet = new URL(url, ServerAPI.class.getSimpleName() + "?hello=world"); // <-- possible MalformedURLException
        
        /*
         * The next try-catch statement is using a high-level API for making the
         * HTTP request and high-level API:s are most often the preferred choice.
         * However, we cannot get the "raw" server response from an
         * URLConnection. If you want to play around with the HTTP protocol,
         * a method for doing so has been provided a bit further down. For
         * example, the next try-catch could be replaced with:
         * 
         *     makeGETRequest(helloServlet, "hello=world");
         */
        
        // Possible IOException:
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(helloServlet.openStream()))) {
            assertEquals("WORLD", reader.readLine());
        }
    }
    
    /**
     * Server..<p>
     * 
     *  will assert that by now, one single request has been made.
     */
    @Test
    @InSequence(3)
    public void server_assertOneRequest()
    {
        // Is executed on the Server in another JVM:
        
        assertEquals("Number of HTTP requests", 1, ServerAPI.getRequestCount());
    }
    
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    /**
     * Will make a HTTP/1.1 GET request.<p>
     * 
     * Keep-alive (persistent connections) are default in HTTP/1.1. Given that
     * the implementation of this method is a basic socket that does not parse
     * or act upon returned "Content-Length" and "Connection" headers, the GET
     * request that this method send will include the header "Connection: close".
     * After receiving this header, HTTP compliant web servers must drop the
     * connection after they have put all their response bytes on the wire. If a
     * server misbehave, this method will hang until either end timeout (which
     * we do after 3 seconds).<p>
     * 
     * Note that {@linkplain URL} does not percent-encode illegal ASCII or
     * non-ASCII characters in the <i>path</i>*; replacing ' ' with '+' or "%20"
     * for example. Nor does this method do so and that includes the provided
     * parameters.<p>
     * 
     * Also note that for convenience, the query part of the provided URL is
     * ignored. The parameters are supplied separately. The fragment part is
     * also ignored (not for convenience though, because I'm sloppy).<p>
     * 
     * *the <i>host</i> part of URL must not contain unsupported characters to
     * begin with.
     * 
     * @param url the URL
     * @param log if {@code true}, will log request/response on {@code Level.INFO}
     * @param parameters parameters for the query string ("name=value")
     * 
     * @return one String for each line Server responded
     * 
     * @see escape(String, String)
     */
    private String[] makeGETRequest(URL url, String... parameters) throws IOException
    {
        final int    port  = url.getPort();
        final String host  = url.getHost(),
                     path  = url.getPath(),
                     query = Stream.of(parameters).collect(Collectors.joining("&", "?", "")),
                     CRLF  = "\r\n";
        
        // Header's reference: http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
        
        StringBuilder sb = new StringBuilder()
                .append("GET ").append(path).append(query).append(" HTTP/1.1").append(CRLF)
                .append("Host: ").append(host);
        
        if (port > 0) {
            sb.append(':').append(port);
        }
        
        final String request = sb.append(CRLF)
               .append("Connection: close")                  .append(CRLF)
               .append("Accept: text/*")                     .append(CRLF)
               .append("User-Agent: An awesome Java Socket") .append(CRLF)
               .append(CRLF).toString();
        
        try (Socket socket = new Socket(host, port > 0 ? port : 80);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), false);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)))
        {
            socket.setSoTimeout(3_000); // 3 seconds
            
            out.write(request);
            out.flush();
            
            return in.lines().toArray(String[]::new);
        }
    }
    
    /**
     * Will percent-encode, or "escape", a GET request parameter and associated
     * value.<p>
     * 
     * For example: <pre>{@code
     * 
     *     escape("donald duck", "fictional"); // produces "donald%20duck=fictional" or "donald+duck=fictional"
     * }</pre>
     * 
     * @param name goes by many names such as field, key or parameter
     * @param value value!
     * 
     * @return mashed and escaped parameters
     */
    private String escape(String name, String value)
    {
        final String charset = StandardCharsets.UTF_8.name();
        
        try {
            return URLEncoder.encode(name, charset) +
                    "=" +
                   URLEncoder.encode(value, charset);
        }
        catch (UnsupportedEncodingException e) {
            throw new AssertionError("No way dude!", e);
        }
    }
}