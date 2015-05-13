package org.sahagin.groovy.runlib.external.adapter

import org.codehaus.groovy.ast.MethodNode

public interface GroovyRootMethodAdapter {

    boolean isRootMethod(MethodNode node)

    String getName()

}