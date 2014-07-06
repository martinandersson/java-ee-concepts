package com.martinandersson.javaee.resources;

import javax.annotation.ManagedBean;
import javax.annotation.sql.DataSourceDefinition;

/**
 * A POJO written only for the purpose of defining a data source, easily
 * attachable to a deployment archive.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@DataSourceDefinition( // note 1
        name         = "java:app/env/ArquillianDS",
        className    = "org.apache.derby.jdbc.ClientXADataSource",
        serverName   = "localhost",
        portNumber   = 1527,
        databaseName = "arquillian-test-db",
        user         = "app",
        password     = "app",
        properties   = {"connectionAttributes=;create=true"})
@ManagedBean // <-- note 2
public class ArquillianDS {}

/*
 * Note 1: Instead of explicitly providing all the properties, I would like to
 *         provide one single connection url like so:
 *             url = "jdbc:derby://localhost:1527/arquillian-test-db;create=true;user=app;password=app"
 * 
 *         However, GlassFish cannot parse the url property correctly. See:
 *             https://java.net/jira/browse/GLASSFISH-20773
 * 
 * Note 2: @DataSourceDefinition must be put on a managed bean. Here, the
 *         ArquillianDS class has explicitly been marked as a managed bean. A
 *         @Stateless bean or any other server side component such as a Servlet
 *         is implicitly a "managed bean" too.
 */