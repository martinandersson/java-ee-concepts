Java EE Concepts
---

A Java Maven project using Arqullian to explore Java EE concepts.

The goal is to provide a smallish framework for exploration and testing of Java EE code without having to dig through piles of specifications.

This project, as was the creator's original intent, suits well for Java EE education making "live coding" a plausible alternative for the teacher.

Each package illustrate a Java EE concept and provide elaborative kickass comments that explain everything of relevance with quotes and references to Java EE specifications and other litterature. The test classes will "prove" what really happens on the inside of a Java EE application server. These tests execute without any hassle right in your IDE.

Next stop, read:   
[./test/java/com/martinandersson/javaee/arquillian/package-info.java](https://github.com/MartinanderssonDotcom/java-ee-concepts/blob/master/src/test/java/com/martinandersson/javaee/arquillian/package-info.java)

Required of you
---
* JDK 8
* Latest and greatest versions of GlassFish and/or WildFly running (profiles included for both)

Failing tests?
---
Not all tests are passed by GlassFish and WildFly. One could argue that is by design. The test code in this project work hard to demonstrate and explore technologies outlined in related Java EE specifications. The test code does not cater to limitations of Java EE product providers. If a test fail, go to the source code file. Read JavaDoc and other source code comments. Chances are high that the issue you ran into has been thoroughly described and reported as a bug somewhere.

Contributors
---
Martin Andersson (webmaster@martinandersson.com)
