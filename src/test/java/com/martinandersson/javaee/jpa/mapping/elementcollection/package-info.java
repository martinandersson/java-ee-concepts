/**
 * JPA 2.1, section "2.6 Collections of Embeddable Classes and Basic Types":
 * <pre>{@code
 * 
 *     A persistent field or property of an entity or embeddable class may
 *     correspond to a collection of a basic type or embeddable class ("element
 *     collection"). Such a collection, when specified as such by the
 *     ElementCollection annotation, is mapped by means of a collection table,
 *     as defined in Section 11.1.8. If the ElementCollection annotation (or XML
 *     equivalent) is not specified for the collection-valued field or property,
 *     the rules of Section 2.8 apply.
 * 
 * }</pre>
 * 
 * 
 * Section 2.8 doesn't list the field type {@code Set}. Section "11.1.14
 * ElementCollection Annotation" says:
 * <pre>{@code
 * 
 *     The ElementCollection annotation (or equivalent XML element) must be
 *     specified if the collection is to be mapped by means of a collection
 *     table.
 * 
 * }</pre>
 * 
 * 
 * My understanding of section 2.6 and 11.1.14 is that {@code @ElementCollection}
 * is optional and may be left out. Only if added shall the collection be mapped
 * to his own table. GlassFish and EclipseLink work in this way, WildFly and
 * Hibernate crash upon deployment.<p>
 * 
 * In order to make at least one of the provided two tests work on WildFly (the
 * one that uses {@code @ElementCollection}; {@code
 * ElementCollectionSeparateTableTest}), they have been split into separate
 * files and separate deployments.<p>
 * 
 * Using Arquillian's techniques for multiple deployments (i.e.
 * {@code @Deployment(name = "name")} and {@code @OperateOnDeployment("name")})
 * won't bypass this issue.<p>
 * 
 * Bug filed here:
 * <pre>{@code
 * 
 *     https://hibernate.atlassian.net/browse/HHH-9402
 * 
 * }</pre>
 */
package com.martinandersson.javaee.jpa.mapping.elementcollection;