package org.sahagin.groovy.runlib.external.adapter.spock

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.groovy.runlib.external.adapter.GroovyRootMethodAdapter
import org.sahagin.groovy.runlib.srctreegen.SrcTreeGeneratorUtils
import org.sahagin.runlib.external.adapter.Adapter

class SpockAdapter implements Adapter {

    @Override
    public void initialSetAdapter() {
        GroovyAdapterContainer container = GroovyAdapterContainer.globalInstance()
        container.setRootMethodAdapter(new GroovyRootMethodAdapterImpl(getName()))
    }

    @Override
    public String getName() {
        return "spock"
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
            if (!SrcTreeGeneratorUtils.inheritsFromClass(
                node.getDeclaringClass(), "spock.lang.Specification")) {
                return false
            }
            for (AnnotationNode annotation : annotations) {
                ClassNode classNode = annotation.getClassNode()
                if (classNode.name == "org.spockframework.runtime.model.FeatureMetadata") {
                    // FeatureMetadata is automatically added to the spock feature method
                    return true
                }
            }
        }

        @Override
        public String getName() {
            return name
        }

    }

}