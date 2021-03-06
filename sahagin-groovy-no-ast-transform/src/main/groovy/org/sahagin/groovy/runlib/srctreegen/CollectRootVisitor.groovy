package org.sahagin.groovy.runlib.srctreegen

import org.apache.commons.lang3.tuple.Pair
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.control.SourceUnit
import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.groovy.runlib.external.adapter.SrcTreeVisitorAdapter
import org.sahagin.groovy.runlib.external.adapter.SrcTreeVisitorAdapter.CollectPhase
import org.sahagin.groovy.runlib.external.adapter.SrcTreeVisitorAdapter.MethodType
import org.sahagin.groovy.share.GroovyASTUtils
import org.sahagin.runlib.additionaltestdoc.AdditionalTestDocs
import org.sahagin.runlib.external.CaptureStyle
import org.sahagin.share.srctree.PageClass
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.srctree.TestMethodTable

class CollectRootVisitor extends ClassCodeVisitorSupport {
    private TestClassTable rootClassTable
    private TestMethodTable rootMethodTable
    private SrcTreeGeneratorUtils utils
    private CollectPhase phase

    CollectRootVisitor(SrcTreeGeneratorUtils utils, CollectPhase phase) {
        rootClassTable = new TestClassTable()
        rootMethodTable = new TestMethodTable()
        this.utils = utils
        this.phase = phase
    }

    TestClassTable getRootClassTable() {
        return rootClassTable
    }

    TestMethodTable getRootMethodTable() {
        return rootMethodTable
    }

    SrcTreeGeneratorUtils getUtils() {
        return utils
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return null // dummy
    }

    @Override
    void visitMethod(MethodNode node) {
        List<SrcTreeVisitorAdapter> listeners =
        GroovyAdapterContainer.globalInstance().srcTreeVisitorAdapters
        if (phase == CollectPhase.BEFORE) {
            for (SrcTreeVisitorAdapter listener : listeners) {
                if (listener.beforeCollectRootMethod(node, this)) {
                    break
                }
            }
            super.visitMethod(node)
            return
        } else if (phase == CollectPhase.AFTER) {
            for (SrcTreeVisitorAdapter listener : listeners) {
                if (listener.afterCollectRootMethod(node, this)) {
                    break
                }
            }
            super.visitMethod(node)
            return
        }

        MethodType methodType
        if (SrcTreeGeneratorUtils.isRootMethod(node)) {
            methodType = MethodType.ROOT
        } else {
            methodType = MethodType.NONE
        }

        for (SrcTreeVisitorAdapter listener : listeners) {
            if (listener.collectRootMethod(node, methodType, this)) {
                super.visitMethod(node)
                return
            }
        }

        if (methodType != MethodType.ROOT) {
            super.visitMethod(node)
            return
        }

        // TODO enum etc
        ClassNode classNode = node.declaringClass
        String classQName = GroovyASTUtils.getClassQualifiedName(classNode)
        TestClass rootClass = rootClassTable.getByKey(classQName)
        if (rootClass == null) {
            rootClass = utils.generateTestClass(classNode)
            rootClassTable.addTestClass(rootClass)
        }

        TestMethod testMethod = new TestMethod()
        testMethod.key = SrcTreeGeneratorUtils.generateMethodKey(node, false)
        testMethod.simpleName = node.name
        String testDoc
        CaptureStyle captureStyle
        (testDoc, captureStyle) =utils.getMethodTestDoc(node)
        testMethod.testDoc = testDoc
        testMethod.captureStyle = captureStyle
        for (Parameter param : node.parameters) {
            testMethod.addArgVariable(param.name)
            // TODO variable argument
        }
        testMethod.testClassKey = rootClass.key
        testMethod.testClass = rootClass
        rootMethodTable.addTestMethod(testMethod)

        rootClass.addTestMethodKey(testMethod.key)
        rootClass.addTestMethod(testMethod)

        super.visitMethod(node)
    }
}
