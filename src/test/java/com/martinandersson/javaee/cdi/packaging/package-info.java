/**
 * <h2>Summary</h2>
 * 
 * You may package an empty {@code beans.xml} for possible discovery of all
 * bean classes including unannotated ones, or you may include contents in the
 * file but if so, you must specify a {@code bean-discovery-mode} of {@code
 * none}, {@code annotated} or {@code all}. If you don't package the file with
 * your application, then the archive will be processed but only annotated beans
 * will be discovered.<p>
 * 
 * 
 * 
 * <h2>Order of study</h2>
 * 
 * <ol>
 *   <li>Read through this file, then..</li>
 *   <li>{@linkplain com.martinandersson.javaee.cdi.packaging.ExplicitPackageTest ExplicitPackageTest.java}</li>
 *   <li>{@linkplain com.martinandersson.javaee.cdi.packaging.ImplicitPackageValidTest ImplicitPackageValidTest.java}</li>
 *   <li>{@linkplain com.martinandersson.javaee.cdi.packaging.ImplicitPackageInvalidTest ImplicitPackageInvalidTest.java}</li>
 * </ol><p>
 * 
 * 
 * 
 * <h2>Packaging modern CDI applications (Java EE 7)</h2>
 * 
 * There are three types of bean archives:
 * 
 * <ol>
 *   <li>Not a bean archive</li>
 *   <li>Explicit bean archive</li>
 *   <li>Implicit bean archive</li>
 * </ol>
 * 
 * If an archive is <strong>not a bean archive</strong>, then the CDI container
 * cannot, or rather, will not manage stuff put in the archive. For
 * <strong>explicit bean</strong> archives, everything in the archive is
 * eligible for management and may be used as an injectable component unless the
 * bean/class itself has been annotated
 * {@linkplain javax.enterprise.inject.Vetoed @javax.enterprise.inject.Vetoed}.
 * For <strong>implicit archives</strong>, CDI only manage beans that has an
 * explicit "bean defining annotation". This annotation is a scope type such as
 * {@linkplain javax.enterprise.context.RequestScoped @javax.enterprise.context.RequestScoped}.
 * WildFly 8.1.0 and my own understanding of all related Java EE
 * specifications<sup>1</sup> also define the "base model" annotation
 * {@linkplain javax.annotation.ManagedBean @javax.annotation.ManagedBean} as
 * a "bean defining annotation". GlassFish however don't and will crash if one
 * try to inject a {@code @ManagedBean} from an implicit bean archive.<p>
 * 
 * Do note that explicit archives do not require explicit bean annotations,
 * implicit archives do. That is like totally inverted so one might think that
 * the expert group (many times referred to using the acronym "EG") behind the
 * specification were all high when they made up the archive definitions.
 * However, I think that the chosen terminology has to do with a possible
 * requirement of a {@code beans.xml} descriptor file. Explicit archives require
 * the file, implicit archives don't. That whole annotation-thing is probably
 * just an unfortunate mismatch.<p>
 * 
 * You know by now the effects of the different archive types; whether or not
 * beans in these archives are discovered. Next, we'll dig deeper into what
 * constitutes the different archive types. Inevitably, the {@code beans.xml}
 * file will be referred to many times over. A {@code beans.xml} skeleton and a
 * good reference to have open during the rest of today's reading can be found
 * here:
 * <pre>{@code
 *     http://docs.oracle.com/javaee/7/tutorial/doc/cdi-adv001.htm
 * }</pre>
 * 
 * 
 * 
 * <h3>The not a bean archive archive</h3>
 * 
 * A EAR package is not a "module", and can therefore not be a bean
 * archive<sup>2</sup>. A bean archive must be a JAR-, WAR- or RAR package.
 * These files in turn may be packaged as modules within an EAR file which is
 * another thing.<p>
 * 
 * Technically speaking, an archive with no classes in it or an archive that
 * contain only classes CDI cannot use (abstract classes for example) cannot be
 * a "bean archive".<p>
 * 
 * An archive is not a bean archive if the {@code beans.xml} descriptor file has
 * been provided and attribute {@code bean-discovery-mode} of the root element
 * {@code <beans>} has been set to {@code none}. Value "none" mean that the
 * container will not look for beans in the archive as opposed to the other two
 * possible values: "annotated" and "all".<p>
 * 
 * An archive that does not bundle the {@code beans.xml} descriptor file is an
 * implicit bean archive, unless the archive also contains a CDI extension. If
 * the archive contains an extension, the archive will become a "not a bean
 * archive archive" that CDI ignore.<p>
 * 
 * 
 * 
 * <h3>Explicit bean archives</h3>
 * 
 * If the archive has a {@code beans.xml} descriptor file bundled with it, and
 * this file is either 1) empty or 2) {@code bean-discovery-mode} has been set
 * to {@code all}, then the archive is an explicit bean archive. As described
 * previously, all classes in such an archive is a possible candidate for CDI
 * management. This is the most used packaging strategy - for better or worse.<p>
 * 
 * 
 * 
 * <h3>Implicit bean archives</h3>
 * 
 * Any other archive that has not previously been described is said to be an
 * implicit bean archive. This basically only leave us with two cases: either
 * the archive has no {@code beans.xml} file or one is present but {@code
 * bean-discovery-mode} has been set to {@code annotated}.<p>
 * 
 * By definition therefore, pritty much all archives used in modern web
 * applications, which lack the the {@code beans.xml} file, are implicit bean
 * archives; processed by the CDI container in a hunt for annotated beans he can
 * manage and make use of. If you're a good sport that make use of annotations,
 * then you don't have to bother about the {@code beans.xml} file either.<p>
 * 
 * The CDI 1.1 specification has a twist in store when it comes to defining an
 * "implicit bean archive". Section 12.1 says:
 * <pre>{@code
 *     "An implicit bean archive is any other archive which contains one or more
 *      [..] session beans."
 * }</pre>
 * 
 * What implication this has for the reality, apart from what already define an
 * implicit bean archive as outlined above, is beyond what I can comprehend.<p>
 * 
 * 
 * <h3>In which path should we put beans.xml?</h3>
 * 
 * The answer is straight forward. CDI 1.1 specification, section 12.1:
 * <pre>{@code 
 * 
 *     "The beans.xml file must be named:
 * 
 *      • META-INF/beans.xml , or,
 *      • in a war, WEB-INF/beans.xml or WEB-INF/classes/META-INF/beans.xml.
 * 
 *      If a war has a file named beans.xml in both the WEB-INF directory and in
 *      the WEB-INF/classes/META-INF directory, then non-portable behavior
 *      results. Portable applications must have a beans.xml file in only one of
 *      the WEB-INF or the WEBINF/classes/META-INF directories."
 * 
 * }</pre>
 * 
 * It is important to note the first paragraph of the previous "not a bean
 * archive archive" section: EAR files are not modules and cannot be bean
 * archives. Putting "META-INF/beans.xml" directly in an EAR file make no sense.
 * Therefore, the first bullet point in the previous quote applies only to JAR-
 * and RAR files.<p>
 * 
 * 
 * 
 * <h3>The version and discovery mode dilemma</h3>
 * 
 * Some sources, of which the CDI 1.1 specification itself is one, trouble
 * themselves by adding complexity to the previously described archive types.
 * The Java EE 7 tutorial linked earlier says:
 * <pre>{@code
 *     "An explicit bean archive is an archive that contains a beans.xml
 *      deployment descriptor, which can [..] contain no version number, or
 *      contain the version number 1.1 with the bean-discovery-mode attribute
 *      set to all."
 * }</pre>
 * 
 * This quote probably originate from the CDI 1.1 specification (section 12.1):
 * <pre>{@code
 *     "An explicit bean archive is an archive which contains a beans.xml file
 *      with a version number of 1.1 (or later), with the bean-discovery-mode of
 *      all [and so forth..]"
 * }</pre>
 * 
 * These two quotes indicate that the version number is of utmost importance and
 * that the {@code bean-discovery-mode} attribute has to be particularly
 * customized for different versions.<p>
 * 
 * However, the {@code beans.xml} file cannot have "no version number" because
 * if the version attribute is left out, it defaults to version
 * "1.1"<sup>3</sup>. Furthermore, attribute {@code bean-discovery-mode} must be
 * specified because it is required by the XML schema definition<sup>3</sup>
 * (the specification says this attribute has a default value "annotated" which
 * is false). Version 1.0 of {@code beans.xml} do not define the attribute
 * {@code bean-discovery-mode}<sup>4</sup>. Adding that would be an error, at
 * least if you stick to the old schema file which is exactly what you'll most
 * likely be doing if you use version 1.0.<p>
 * 
 * 
 * 
 * 
 * 
 * <h2>Packaging legacy archives (Java EE 6)</h2>
 * 
 * "Implicit", "explicit" and "discovery mode" are are all new concepts
 * introduced in CDI 1.1 (Java EE 7). CDI 1.0 (Java EE 6) has no clue about
 * them.<p>
 * 
 * Here's the deal about that one. Once upon time, all developers wrote EJB:s
 * and POJO:s they thought suited well as CDI managed beans. Then came
 * deployment time and only EJB:s gave up a sign of life. Nothing at all
 * happened with the precious CDI beans that was packaged with the archive too.
 * Many developers wasted many hours trying to debug the situation until they
 * discovered that CDI 1.0 require "a file named beans.xml" (section 12.1).
 * Hence, the archive is required to be an "explicit archive" using modern
 * terminology.<p>
 * 
 * The expert group behind CDI 1.0 had been a bit afraid that the server would
 * have to do to much work, scanning and process too many classes for nothing -
 * had the requirement of the file not been there. It turned out though that the
 * absolute majority of all developers used CDI which today is somewhat of a
 * core service in all back-end software. Moreover, most deployments are quite
 * small and processing classes in them take no time at all. So the only effect
 * of this "premature optimization" was a waste of time for a shitload of
 * developers.<p>
 * 
 * CDI 1.0 doesn't comment whether or not the file may be empty, only that a
 * file had to be there. But servers kind of picked up on that and began
 * processing the archives as long as a file literally was packaged with the
 * archive even though it might be completely empty. CDI 1.1 has continued the
 * tradition and declared the "empty file" as having marked an "explicit
 * archive" in which all classes are processed.<p>
 * 
 * So, packaging a legacy archive doesn't differ to much from modern packaging
 * strategies. Just make sure an empty {@code beans.xml} file is present and the
 * end result will be the same no matter CDI version. However, modern
 * deployments using CDI 1.1 can optionally leave out the file altogether and
 * still have annotated beans processed. But be vary that trying to deploy a
 * modern archive with no {@code beans.xml} file to an old server might not
 * cause any deployment issues
 * ({@linkplain javax.enterprise.inject.spi.DeploymentException DeploymentException}),
 * but it will cause the injection of fields in your application to not work
 * properly (NullPointerException).<p>
 * 
 * 
 * 
 * <h3>Note 1</h3>
 * CDI 1.1 (JSR-346) only define {@code @ManagedBean} as a "bean defining
 * annotation" implicitly. For more information:
 * <pre>{@code 
 *     http://stackoverflow.com/questions/25327057
 * }</pre>
 * 
 * If you haven't already, read through the concept of "managed beans" explained
 * in file:
 * <pre>{@code 
 *     ./com/martinandersson/javaee/cdi/package-info.java<p>
 * }</pre><p>
 * 
 * 
 * <h3>Note 2</h3>
 * See Java EE 7 specification (JSR-342) chapter EE.8 and CDI 1.1 specification
 * (JSR-346) section 5.1.
 * 
 * 
 * <h3>Note 3</h3>
 * See the XML schema definition file here:
 * <pre>{@code
 *     http://xmlns.jcp.org/xml/ns/javaee/beans_1_1.xsd
 * }</pre>
 * 
 * 
 * <h3>Note 4</h3>
 * See the XML schema definition file here:
 * <pre>{@code
 *     http://java.sun.com/xml/ns/javaee/beans_1_0.xsd
 * }</pre>
 */
package com.martinandersson.javaee.cdi.packaging;