package org.sahagin.groovy.runlib.external.adapter.spock

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.groovy.runlib.external.adapter.GroovyRootMethodAdapter
import org.sahagin.groovy.runlib.srctreegen.SrcTreeGeneratorUtils
import org.sahagin.groovy.share.GroovyASTUtils
import org.sahagin.runlib.external.adapter.Adapter

class SpockAdapter implements Adapter {

    @Override
    public void initialSetAdapter() {
        GroovyAdapterContainer container = GroovyAdapterContainer.globalInstance()
        container.rootMethodAdapter = new GroovyRootMethodAdapterImpl(getName())
        container.addAdditionalTestDocsAdapter(new SpockAdditionalTestDocsAdapter())
        container.addSrcTreeVisitorAdapter(new SpockSrcTreeVisitorAdapter())
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

        // TODO ignore setup, cleanup method, etc
        @Override
        public boolean isRootMethod(MethodNode node) {
            // node must not be transformed by Spock global AST transformation
            if (!GroovyASTUtils.inheritsFromClass(
                node.declaringClass, "spock.lang.Specification")) {
                return false
            }

            if (!(node.code instanceof BlockStatement)) {
                return false
            }
            List<Statement> statements =(node.code as BlockStatement).statements
            for (Statement statement : statements) {
                if (statement.statementLabel != null) {
                    // method including at least one statement label is handled as feature method
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