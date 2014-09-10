/**
 * <h2>What you'll read about and proposed order of study</h2>
 * 
 * This file offer a therorical run-down of Java SE JavaBeans and Java EE
 * managed beans and how everything relates to each other. This package in
 * itself does not contain concept-proving tests. However, for a better picture
 * of what will happen in subpackages, this file is a must read.<p>
 * 
 * <i>Then</i>, proposed order of study:
 * 
 * <ol>
 *   <li>{@linkplain com.martinandersson.javaee.cdi.packaging}</li>
 *   <li>{@linkplain com.martinandersson.javaee.cdi.scope}</li>
 *   <li>{@linkplain com.martinandersson.javaee.cdi.resolution}</li>
 *   <li>TODO: More..</li>
 * </ol>
 * 
 * 
 * 
 * <h2>JavaBeans and Java EE</h2>
 * 
 * The age-old definition of a Java Bean from the JavaBeans
 * specification<sup>1</sup> is:
 * <pre>{@code
 * 
 *     "A Java Bean is a reusable software component that can be manipulated
 *     visually in a builder tool."
 * 
 * }</pre>
 * 
 * The most wide-spread feature of the JavaBeans specification is actually a
 * non-mandatory design pattern, a convention on how to provide read- and write
 * access to a field of a class (section 8.3), which in itself is called a
 * "property" if convention is followed suite. The convention is basically only
 * a naming scheme. Property {@code name} has read access if method {@code
 * public String getName()} is provided.<p>
 * 
 * Other languages such as ECMAScript 5 (= newish standard of JavaScript) and C#
 * has native language support for properties, turning basic assignment
 * operations into method calls. Java doesn't. With a tight convention though,
 * Java tooling has a uniformed way of property access and won't miss out.<p>
 * 
 * The JavaBeans specification is high on "visual manipulation" of Java beans,
 * as exemplified by the previous quote. But the tooling has increased over the
 * years and is represented today by everything from JDK provided {@linkplain
 * java.beans.PropertyDescriptor} to the persistence provider behind JPA
 * entities in Java EE<sup>2</sup>.<p>
 * 
 * For the most part, Java EE "beans" has nothing to do with JavaBeans. We
 * provide Java EE tooling the information it needs by using annotations.<p>
 * 
 * Java is probably the winner of all languages when it comes to abundance of
 * tooling and frameworks. Some require and use JavaBeans conventions, some
 * don't. With this background in mind, it isn't hard to understand why modern
 * street language has popularised the word "bean" to mean an object that
 * has "something more to it", a little something that goes beyond what the
 * source code of the class offer. Life will be easier if one does not try to
 * find an exact definition of the word "bean".<p>
 * 
 * 
 * 
 * <h2>Java EE Managed Beans</h2>
 * 
 * The "Managed Beans" specification<sup>3</sup> define a "basic model" which
 * other specifications can relax or extend (section MB.1.1):
 * <pre>{@code
 * 
 *     "Managed Beans are container-managed objects with minimal requirements,
 *      otherwise known under the acronym “POJOs” (Plain Old Java Objects). They 
 *      support a small set of basic services, such as resource injection,
 *      lifecycle callbacks and interceptors. Other, more advanced, aspects will
 *      be introduced in companion specifications, so as to keep the basic model
 *      as simple and as universally useful as possible."
 * 
 * }</pre>
 * 
 * So a managed bean may use services provided by the server, awesome. How is a
 * managed bean defined? The managed beans specification says (section MB.2):
 * <pre>{@code
 * 
 *     "[..] a Managed Bean component must be declared using the
 *      ManagedBean annotation, but other specifications are allowed to alter
 *      this requirement, e.g. to provide a purely XML-based way to turn a class
 *      into a Managed Bean."
 * 
 * }</pre>
 * 
 * The annotation in question is
 * {@linkplain javax.annotation.ManagedBean @javax.annotation.ManagedBean}.<p>
 * 
 * Some components are considered a "managed bean" by sheer existence. For
 * example {@linkplain javax.servlet.annotation.WebServlet @WebServlet}
 * (servlets) and {@linkplain
 * javax.websocket.server.ServerEndpoint @ServerEndpoint} (websockets)
 * <sup>4</sup>. Other types that are not [necessarily] "auto-deployed" are:
 * 
 * <ul>
 *   <li>
 *       JSF Managed Beans<br>
 *       <i>Component-defining annotation:</i>
 *       {@linkplain javax.faces.bean.ManagedBean @javax.faces.bean.ManagedBean}
 *   </li>
 *   <li>
 *        CDI Managed Beans<br>
 *        <i>Component-defining annotation:</i>
 *        basically none, see next section
 *   </li>
 *   <li>
 *        Enterprise JavaBeans<br>
 *        <i>Component-defining annotations:</i>
 *        {@linkplain javax.ejb.Stateless @javax.ejb.Stateless},
 *        {@linkplain javax.ejb.Stateful @javax.ejb.Stateful},
 *        {@linkplain javax.ejb.Singleton @javax.ejb.Singleton},
 *        {@linkplain javax.ejb.MessageDriven @javax.ejb.MessageDriven}
 *   </li>
 * </ul>
 * 
 * Inevitably, some confusion should arise between JSF- and "regular" Java EE
 * managed beans since they both use an <strong>annotation with the same simple
 * name</strong> (<i>@javax.annotation.ManagedBean</i> versus
 * <i>@javax.faces.bean.ManagedBean</i>).<p>
 * 
 * JSF managed beans are used as backing beans for JSF pages only. It is an old
 * artifact, soon to be deprecated<sup>5</sup> and should be avoided because JSF
 * pages today can use much more feature rich CDI managed beans (just add
 * {@linkplain javax.inject.Named @javax.inject.Named} to the bean
 * class<sup>6</sup>). Besides, everything move in favor of CDI managed beans
 * anyways and some things even speak for a future deprecation of EJB:s as
 * well<sup>7</sup>.<p>
 * 
 * It is important to note that the {@code @ManagedBean} annotation is not the
 * only annotation that has been duplicated across two distinct packages.
 * JavaServer Faces (JSR-344) also define bean scopes in package {@code
 * javax.faces.bean}, for example
 * {@linkplain javax.faces.bean.RequestScoped @javax.faces.bean.RequestScoped}.
 * However, just like JSF managed beans should be replaced with CDI managed
 * beans, so too should scope annotations be (<i>everything</i> in package
 * "javax.faces.bean" are, according to note 4, soon to be deprecated). CDI
 * declare his scopes in package {@code javax.enterprise.context}. For example,
 * {@linkplain javax.enterprise.context.RequestScoped @javax.enterprise.context.RequestScoped}.<p>
 * 
 * More on CDI scopes and EJB:s somewhere else.<p>
 * 
 * 
 * 
 * <h2>How to define a CDI managed bean</h2>
 * 
 * In the most simplest case, any class that is concrete (non-abstract) and has
 * a no-arg constructor fit the bill and can be turned into a managed
 * bean<sup>8</sup> (with default scope
 * {@linkplain javax.enterprise.context.Dependent @javax.enterprise.context.Dependent}).
 * However, whether or not the bean is actually discoverable depends on how you
 * package the application. Next stop, read:
 * <pre>{@code
 * 
 *     ./com/martinandersson/javaee/cdi/packaging/package-info.java
 * 
 * }</pre>
 * 
 * A good resource to have at hands when developing CDI beans is the following
 * page:
 * <pre>{@code
 *     http://docs.oracle.com/javaee/7/api/javax/enterprise/inject/package-summary.html
 * }</pre>
 * 
 * 
 * 
 * 
 * <h3>Note 1</h3>
 * http://download.oracle.com/otndocs/jcp/7224-javabeans-1.01-fr-spec-oth-JSpec/<p>
 * 
 * 
 * <h3>Note 2</h3>
 * JPA 2.1 mandates JavaBeans property convention only when property access is
 * used. The normal use case is field access which does not require the
 * convention.<p>
 * 
 * 
 * <h3>Note 3</h3>
 * The managed beans specification is part of the Java EE 6 umbrella
 * specification (JSR-316) and therefore not listed where the Java EE 7 umbrella
 * specification (JSR-342) has been put:
 * <pre>{@code
 *     https://jcp.org/aboutJava/communityprocess/final/jsr342/index.html
 * 
 * }</pre>
 * 
 * The Java EE 7 specification (that is, JSR-342, which sometimes is called
 * "umbrella specification) do refer to the managed beans specification from
 * Java EE 6 and require full support of it. However, one must go to the old
 * page of the Java EE 6 specification to actually find it:
 * <pre>{@code
 *     https://jcp.org/aboutJava/communityprocess/final/jsr316/index.html
 * 
 * }</pre>
 * 
 * 
 * <h3>Note 4</h3>
 * I cannot list all managed components that possibly exist in a Java EE server,
 * nor am I sure exactly how they extend the "basic model" or what restrictions
 * might apply. As far as I can tell though, no managed component exist that are
 * restricted from using dependency injection, which is the bridge to other
 * server-side resources one might use; bean-managed transactions for example.
 * Early implementations of Tyrus, the WebSocket (JSR-356) reference
 * implementation struggled long and hard to get DI to work, but latest versions
 * have no problems with DI and the specification itself mandate full CDI
 * support. "Entity beans" is an old type of EJB beans that used to be managed
 * but they have been deprecated in favor or thinner JPA entity classes. JPA
 * entity classes are not "managed beans", they are "managed instances". The JPA
 * specification does not specify whether or not entity classes support CDI,
 * which I think that most application servers won't do.<p>
 * 
 * 
 * <h3>Note 5</h3>
 * JavaServer Faces 2.2 specification (JSR-344), section 5.4:
 * <pre>{@code
 * 
 *     "The annotations in the package javax.faces.bean will be deprecated in a
 *      version of the JSF specification after 2.2. Therefore, developers are
 *      strongly recommended avoid using those annotations and instead use the
 *      ones from Java EE 6."
 * 
 * }</pre>
 * 
 * 
 * <h3>Note 6</h3>
 * See the CDI specification (Context and Dependency Injection for Java 1.1
 * JSR-346), section "2.6 Bean Names" and "5.3 EL name resolution".<p>
 * 
 * 
 * <h3>Note 7</h3>
 * Java EE 7 introduced {@code @Transactional}, making it possible for CDI beans
 * to demarcate transaction boundaries with annotation-driven meta programming,
 * something only EJB:s could do earlier. Java EE 8 market questionnaires and
 * related documents has hinted that the EJB specification will be closer
 * "aligned" with the CDI specification.<p>
 * 
 * 
 * <h3>Note 8</h3>
 * See the CDI specification (Context and Dependency Injection for Java 1.1
 * JSR-346), section "3.1.1 Which Java classes are managed beans?". 
 */
package com.martinandersson.javaee.cdi;