package org.sahagin.groovy.runlib.srctreegen

import org.apache.commons.lang3.tuple.Pair
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.Expression
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
import org.sahagin.share.srctree.TestFieldTable
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.srctree.TestMethodTable

class CollectSubVisitor extends ClassCodeVisitorSupport {
    private TestClassTable subClassTable
    private TestMethodTable subMethodTable
    private TestClassTable rootClassTable
    private TestFieldTable fieldTable
    private SrcTreeGeneratorUtils utils
    private CollectPhase phase

    CollectSubVisitor(TestClassTable rootClassTable,
        SrcTreeGeneratorUtils utils, CollectPhase phase) {
        this.rootClassTable = rootClassTable
        this.subClassTable = new TestClassTable()
        this.subMethodTable = new TestMethodTable()
        this.fieldTable = new TestFieldTable()
        this.utils = utils
        this.phase = phase
    }

    TestClassTable getRootClassTable() {
        return rootClassTable
    }

    TestClassTable getSubClassTable() {
        return subClassTable
    }

    TestMethodTable getSubMethodTable() {
        return subMethodTable
    }

    TestFieldTable getFieldTable() {
        return fieldTable
    }

    SrcTreeGeneratorUtils getUtils() {
        return utils
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return null // dummy
    }

    // TODO visit all field with TestDoc and collect fieldTable data

    @Override
    void visitMethod(MethodNode node) {
        List<SrcTreeVisitorAdapter> listeners =
        GroovyAdapterContainer.globalInstance().srcTreeVisitorAdapters
        if (phase == CollectPhase.BEFORE) {
            for (SrcTreeVisitorAdapter listener : listeners) {
                if (listener.beforeCollectSubMethod(node, this)) {
                    break
                }
            }
            super.visitMethod(node)
            return
        } else if (phase == CollectPhase.AFTER) {
            for (SrcTreeVisitorAdapter listener : listeners) {
                if (listener.afterCollectSubMethod(node, this)) {
                    break
                }
            }
            super.visitMethod(node)
            return
        }

        MethodType methodType
        if (utils.isSubMethod(node)) {
            methodType = MethodType.SUB
        } else {
            methodType = MethodType.NONE
        }

        for (SrcTreeVisitorAdapter listener : listeners) {
            if (listener.collectSubMethod(node, methodType, this)) {
                super.visitMethod(node)
                return
            }
        }

        if (methodType != MethodType.SUB) {
            super.visitMethod(node)
            return
        }

        // TODO enum etc
        ClassNode classNode = node.declaringClass
        String classQName = GroovyASTUtils.getClassQualifiedName(classNode)
        TestClass testClass = rootClassTable.getByKey(classQName)
        if (testClass == null) {
            testClass = subClassTable.getByKey(classQName)
            if (testClass == null) {
                testClass = utils.generateTestClass(classNode)
                subClassTable.addTestClass(testClass)
            }
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
        testMethod.testClassKey = testClass.key
        testMethod.testClass = testClass
        subMethodTable.addTestMethod(testMethod)

        testClass.addTestMethodKey(testMethod.key)
        testClass.addTestMethod(testMethod)

        super.visitMethod(node)
    }
}