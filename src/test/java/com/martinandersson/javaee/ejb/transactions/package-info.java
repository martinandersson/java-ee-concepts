/**
 * EJB 3.2, section "8.4 Application Assembler's Responsibilities":
 * <pre>{@code
 *     [..] the behavior specified by the transaction attributes is typically an
 *     intrinsic part of the semantics of an application.
 * }</pre>
 * 
 * TODO: "Inheritance" of bean managed transactions into beans who use container
 * managed transaction demarcation. Beans that use bean managed transaction
 * demarcation does not inherit CMT and will most likely fail if trying to start
 * a nested transaction.
 */
package com.martinandersson.javaee.ejb.transactions;
