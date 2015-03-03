package org.sahagin.groovy.runlib.srctreegen

import org.apache.commons.lang3.tuple.Pair
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.control.SourceUnit
import org.eclipse.jdt.core.dom.ITypeBinding
import org.eclipse.jdt.core.dom.SingleVariableDeclaration
import org.sahagin.runlib.external.CaptureStyle
import org.sahagin.share.srctree.PageClass
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.srctree.TestMethodTable

class CollectSubVisitor extends ClassCodeVisitorSupport {
    private TestClassTable subClassTable
    private TestMethodTable subMethodTable
    private TestClassTable rootClassTable

    CollectSubVisitor(TestClassTable rootClassTable) {
        this.rootClassTable = rootClassTable
        this.subClassTable = new TestClassTable();
        this.subMethodTable = new TestMethodTable();
    }

    TestClassTable getSubClassTable() {
        return subClassTable
    }

    TestMethodTable getSubMethodTable() {
        return subMethodTable
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return null // dummy
    }
    
    @Override
    void visitMethod(MethodNode node) {
        if (!SrcTreeGeneratorUtils.isSubMethod(node)) {
            super.visitMethod(node)
            return
        }

        // TODO enum etc
        ClassNode classNode = node.getDeclaringClass()

        TestClass testClass = rootClassTable.getByKey(classNode.getName())
        if (testClass == null) {
            testClass = subClassTable.getByKey(classNode.getName())
            if (testClass == null) {
                // TODO testDoc handling
                testClass = new TestClass()
                testClass.setKey(classNode.getName())
                testClass.setQualifiedName(classNode.getName())
                testClass.setTestDoc("")
                subClassTable.addTestClass(testClass)
            }
        }

        TestMethod testMethod = new TestMethod()
        testMethod.setKey(SrcTreeGeneratorUtils.generateMethodKey(node, false))
        testMethod.setSimpleName(node.getName())
        // TODO testDocc and captureStyle
        testMethod.setTestDoc("")
        for (Parameter param : node.getParameters()) {
            testMethod.addArgVariable(param.getName())
            // TODO variable argument
        }
        testMethod.setTestClassKey(testClass.getKey())
        testMethod.setTestClass(testClass)
        subMethodTable.addTestMethod(testMethod)

        testClass.addTestMethodKey(testMethod.getKey())
        testClass.addTestMethod(testMethod)

        super.visitMethod(node)
    }

}

