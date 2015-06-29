/**
 * Currently, this package offer two tests related to EJB:s and exceptions
 * thrown by them.<p>
 * 
 * {@linkplain com.martinandersson.javaee.ejb.exceptions.EJBTransactionRolledbackExceptionTest}
 * examine when the client can expect to see an
 * {@code EJBTransactionRolledbackException}.<p>
 * 
 * {@linkplain com.martinandersson.javaee.ejb.exceptions.SystemAndApplicationExceptionTest}
 * examine the difference between EJB system-level exceptions and EJB
 * application applications, as well as look into whether or not using a
 * colocated {@code @Remote} EJB makes any difference.
 */
package com.martinandersson.javaee.ejb.exceptions;
