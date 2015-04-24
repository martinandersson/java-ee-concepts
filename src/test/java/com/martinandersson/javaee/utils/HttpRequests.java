package com.martinandersson.javaee.utils;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Procedures for making {@code HTTP/1.1} request to the test server.<p>
 * 
 * All requests made with this class are non-persistent, meaning that the header
 * {@code Connection: close} is included in the request sent to the server.
 * After receiving this header, HTTP compliant web servers must close the
 * connection after the response.<p>
 * 
 * Note that the underlying Java entity used to make HTTP requests by this
 * class is {@code HttpURLConnection} which most likely uses pooled connections.
 * Therefore, invoking these methods in a concurrent test has limited effects.
 * Consider using a Socket instead (which we will add in this class in the
 * future, or change the implementation of this method).<p>
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class HttpRequests
{
    private HttpRequests() {
        // Empty
    }
    
    
    
    /*
     *  --------------
     * | EXTERNAL API |
     *  --------------
     */
    
    /**
     * Shortcut for
     * {@linkplain #getBytes(URL, String, RequestParameter...) getBytes(contextRoot, null)}.
     * 
     * @param contextRoot
     *            deployed application URL ("application context root"),
     *            provided by Arquillian
     */
    public static void ping(URL contextRoot) {
        getBytes(contextRoot, null);
    }
    
    /**
     * Will make a GET-request to the provided Servlet test driver and return
     * the raw response body as a {@code byte[]}.<p>
     * 
     * Servlets that do not respond with anything will make the client see a
     * a byte array of zero length.<p>
     * 
     * A specified path is optional and may be {@code null} or the empty String.
     * In this case, the deployed test servlet is mapped to be listening on the
     * application context root <sup>1</sup> or mapped to be the default servlet
     * of the application <sup>2</sup>. In both cases, it doesn't matter which
     * path is added to the application context root as the target Servlet will
     * become the target of the request.<p>
     * 
     * <sup>1</sup> url pattern = "" <br>
     * <sup>2</sup> url pattern = "/"
     * 
     * @param contextRoot
     *            deployed application URL ("application context root"),
     *            provided by Arquillian
     * @param path
     *            path to servlet (may be {@code null} or empty)
     * @param parameters
     *            each parameter will be added to the GET-request
     * 
     * @return object returned by the test driver
     */
    public static byte[] getBytes(URL contextRoot, String path, RequestParameter... parameters) {
        final URLConnection conn = openNonPersistentConnection(contextRoot, path, parameters);
        
        try (InputStream in = conn.getInputStream()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            
            int b;
            
            while ((b = in.read()) != -1) { // <-- read unsigned byte
                buffer.write(b);            // <-- write unsigned byte
            }
            
            return buffer.toByteArray();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    /**
     * Shortcut for:
     * {@linkplain #getObject(URL, String, RequestParameter...) getObject(contextRoot, null)}.
     * 
     * @param <T>
     *            type of returned object
     * 
     * @param contextRoot
     *            deployed application URL ("application context root"),
     *            provided by Arquillian
     * 
     * @return object returned by the test driver
     */
    public static <T> T getObject(URL contextRoot) {
        return getObject(contextRoot, null);
    }
    
    /**
     * Will make a GET-request to the provided Servlet test driver and return an
     * expected Java object as response.<p>
     * 
     * If Servlet return nothing, {@code null} is returned.<p>
     * 
     * A specified path is optional and may be {@code null} or the empty String.
     * In this case, the deployed test servlet is mapped to be listening on the
     * application context root <sup>1</sup> or mapped to be the default servlet
     * of the application <sup>2</sup>. In both cases, it doesn't matter which
     * path is added to the application context root as the target Servlet will
     * become the target of the request.<p>
     * 
     * <sup>1</sup> url pattern = "" <br>
     * <sup>2</sup> url pattern = "/"
     * 
     * @param <T>
     *            type of returned object
     * @param contextRoot
     *            deployed application URL ("application context root"),
     *            provided by Arquillian
     * @param path
     *            path to servlet (may be {@code null} or empty)
     * @param parameters
     *            each parameter will be added to the GET-request
     * 
     * @return object returned by the test driver
     */
    public static <T> T getObject(URL contextRoot, String path, RequestParameter... parameters) { 
        final URLConnection conn = openNonPersistentConnection(contextRoot, path, parameters);
        
        try (ObjectInputStream in = new ObjectInputStream(conn.getInputStream());) {
            return (T) in.readObject();
        }
        catch (EOFException e) {
            return null;
        }
        catch (IOException e) {
            // Might be that you haven't packaged all dependent class files with the @Deployment?
            // Servlet or endpoint your trying to call isn't properly implemented?
            throw new UncheckedIOException(e);
        }
        catch (ClassNotFoundException e) {
            throw new AssertionError("Got object of unknown type.", e);
        }
    }
    
    /**
     * Will make a POST-request to the provided Servlet test driver and return an
     * expected Java object as response.<p>
     * 
     * The POST body will contained the provided {@code toSend} object in its
     * binary form.<p>
     * 
     * This method is largely equivalent with {@linkplain
     * #getObject(URL, String, RequestParameter...)}.
     * 
     * @param <T>
     *            type of returned object
     * @param contextRoot
     *            deployed application URL ("application context root"),
     *            provided by Arquillian
     * @param path
     *            path to servlet (may be {@code null} or empty)
     * @param toSend
     *            serialized and put in body of the POST request
     * 
     * @return object returned by the test driver
     */
    public static <T> T sendGetObject(URL contextRoot, String path, Serializable toSend) {
        Objects.requireNonNull(toSend);
        
        final HttpURLConnection conn = openNonPersistentConnection(contextRoot, path);
        
        try {
            conn.setRequestMethod("POST");
        }
        catch (ProtocolException e) {
            throw new AssertionError("POST is supported and yes, URLConnection is known to have a bad API design.", e);
        }
        
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        
        try (OutputStream raw = conn.getOutputStream();
             ObjectOutputStream writer = new ObjectOutputStream(raw);) {
             
             writer.writeObject(toSend);
             
             raw.write('\r'); raw.write('\n');
             raw.write('\r'); raw.write('\n');
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        
        try (InputStream raw = conn.getInputStream();
             ObjectInputStream reader = new ObjectInputStream(raw);) {
             return (T) reader.readObject();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        catch (ClassNotFoundException e) {
            throw new AssertionError("Got object of unknown type.", e);
        }
    }
    
    
    
    public static class RequestParameter
    {
        private static String buildQuery(RequestParameter... requestParameters) {
            return Stream.of(requestParameters)
                    .map(RequestParameter::asKeyValue)
                    .collect(Collectors.joining("&", "?", ""));
        }
        
        private static final Pattern WS_CHAR = Pattern.compile("\\s");
        
        private final String key;
        private final String value;
        
        public RequestParameter(String key, String value) {
            if (WS_CHAR.matcher(key).find())
                throw new IllegalArgumentException("Whitespace found in key \"" + key + "\". Please percent-encode the key.");
            
            if (WS_CHAR.matcher(value).find())
                throw new IllegalArgumentException("Whitespace found in value: \"" + value + "\". Please percent-encode the value.");
            
            this.key = key;
            this.value = value;
        }
        
        public final String asKeyValue() {
            return key + "=" + value;
        }
    }
    
    
    
    /*
     *  --------------
     * | INTERNAL API |
     *  --------------
     */
    
    /**
     * Will transform provided arguments into a {@code HttpURLConnection}
     * against the test Servlet.
     * 
     * @param contextRoot application context root, as provided by Arquillian
     * @param path path to servlet (may be {@code null} or empty)
     * @param parameters optional request parameters
     * 
     * @return a connection against the test Servlet
     * 
     * @throws IllegalArgumentException if provided URL is not a HTTP URI
     */
    private static HttpURLConnection openNonPersistentConnection(URL contextRoot, String path, RequestParameter... parameters) {
        final String query = RequestParameter.buildQuery(parameters);
        
        try {
            URL url = new URL(contextRoot, (path == null ? "" : path) + query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Connection", "close");
            return conn;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        catch (ClassCastException e) {
            throw new IllegalArgumentException("Provided URL is not a HTTP URI", e);
        }
    }
}