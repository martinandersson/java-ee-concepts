/**
 * Demonstrates using Arquillian to manage two JVM:s, one running as client and
 * one running as server. This is as "real" as a test can become.<p>
 * 
 * Highlights:
 * 
 * <ul>
 *   <li>Arquillian dual client/server JVM:s with {@code @InSequence} and {@code @RunAsClient}.</li>
 *   <li>Concurrent high-performance HTTP request counter.</li>
 *   <li>A Java Servlet parsing parameters from a HTTP GET request.</li>
 *   <li>Client code illustrating high- and low level API:s for making a HTTP/1.1 GET request.</li>
 * </ul>
 */
package com.martinandersson.javaee.arquillian.clientserver;