package org.sahagin.groovy.runlib.external.adapter.junit4

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.eclipse.jdt.core.dom.IMethodBinding
import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.groovy.runlib.external.adapter.GroovyRootMethodAdapter
import org.sahagin.runlib.external.adapter.Adapter
import org.sahagin.runlib.external.adapter.junit4.JUnit4AdditionalTestDocsAdapter
import org.sahagin.runlib.srctreegen.ASTUtils

class GroovyJUnit4Adapter implements Adapter {

    @Override
    public void initialSetAdapter() {
        GroovyAdapterContainer container = GroovyAdapterContainer.globalInstance()
        container.setRootMethodAdapter(new GroovyRootMethodAdapterImpl(getName()))
        container.addAdditionalTestDocsAdapter(new JUnit4AdditionalTestDocsAdapter())
    }

    @Override
    public String getName() {
        return "jUnit4"
    }

    private static class GroovyRootMethodAdapterImpl implements GroovyRootMethodAdapter {
        private String name

        private GroovyRootMethodAdapterImpl(String name) {
            this.name = name
        }

        @Override
        public boolean isRootMethod(MethodNode node) {
            List<AnnotationNode> annotations = node.annotations
            if (annotations == null) {
                return false
            }
            for (AnnotationNode annotation : annotations) {
                ClassNode classNode = annotation.classNode
                if (classNode.name == "org.junit.Test") {
                    return true
                }
            }
            return false
        }

        @Override
        public String getName() {
            return name
        }

    }

}