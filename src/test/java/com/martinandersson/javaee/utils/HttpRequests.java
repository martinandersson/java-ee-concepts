package com.martinandersson.javaee.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Procedures for making {@code HTTP/1.1} request to the test server.<p>
 * 
 * All requests made with this class are non-persistent, meaning that the header
 * {@code Connection: close} is included in the request sent to the server.
 * After receiving this header, HTTP compliant web servers must close the
 * connection after the response.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class HttpRequests
{
    private HttpRequests() {
        // Empty
    }
    
    /**
     * Will make a GET-request to the provided Servlet test driver and return an
     * expected Java object as response.
     * 
     * @param <T> type of returned object
     * @param url deployed application URL ("application context root"),
     *            provided by Arquillian
     * @param testDriverType the test driver class
     * 
     * @return object returned by the test driver
     */
    public static <T> T getObject(URL url, Class<?> testDriverType) {
        final URL testDriver;
        final URLConnection conn;
        
        try {
            testDriver = new URL(url, testDriverType.getSimpleName());
            conn = testDriver.openConnection();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        
        conn.setRequestProperty("Connection", "close");
        
        try (ObjectInputStream in = new ObjectInputStream(conn.getInputStream());) {
            return (T) in.readObject();
        }
        catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}