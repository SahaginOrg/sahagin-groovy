package org.sahagin.groovy.runlib.srctreegen

import org.apache.commons.lang3.tuple.Pair
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.control.SourceUnit
import org.sahagin.runlib.external.CaptureStyle
import org.sahagin.share.srctree.PageClass
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.srctree.TestMethodTable
import org.junit.Test

class CollectRootVisitor extends ClassCodeVisitorSupport {
    private TestClassTable rootClassTable
    private TestMethodTable rootMethodTable

    CollectRootVisitor() {
        rootClassTable = new TestClassTable()
        rootMethodTable = new TestMethodTable()
    }

    TestClassTable getRootClassTable() {
        return rootClassTable
    }

    TestMethodTable getRootMethodTable() {
        return rootMethodTable
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return null // dummy
    }

    @Override
    void visitMethod(MethodNode node) {
        if (!SrcTreeGeneratorUtils.isRootMethod(node)) {
            super.visitMethod(node)
            return
        }

        // TODO enum etc
        ClassNode classNode = node.getDeclaringClass()

        TestClass rootClass = rootClassTable.getByKey(classNode.getName())
        if (rootClass == null) {
            rootClass = new TestClass()
            rootClass.setKey(classNode.getName())
            rootClass.setQualifiedName(classNode.getName())
            // TODO testDoc, captureStyle, TestDocs, etc
            rootClass.setTestDoc("")
            rootClassTable.addTestClass(rootClass)
        }

        TestMethod testMethod = new TestMethod()
        testMethod.setKey(SrcTreeGeneratorUtils.generateMethodKey(node, false))
        testMethod.setSimpleName(node.getName())
        // TODO captureStyle, TestDocs, etc
        testMethod.setTestDoc(SrcTreeGeneratorUtils.getTestDoc(node))
        for (Parameter param : node.getParameters()) {
            testMethod.addArgVariable(param.getName())
            // TODO variable argument
        }
        testMethod.setTestClassKey(rootClass.getKey())
        testMethod.setTestClass(rootClass)
        rootMethodTable.addTestMethod(testMethod)

        rootClass.addTestMethodKey(testMethod.getKey())
        rootClass.addTestMethod(testMethod)

        super.visitMethod(node)
    }

}
