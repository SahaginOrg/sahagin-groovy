package org.sahagin.groovy.runlib.srctreegen

import org.apache.commons.lang3.tuple.Pair
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.control.SourceUnit
import org.sahagin.groovy.runlib.srctreegen.SrcTreeVisitorListener.CollectPhase
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

    @Override
    protected SourceUnit getSourceUnit() {
        return null // dummy
    }

    // TODO visit all field with TestDoc and collect fieldTable data

    @Override
    void visitMethod(MethodNode node) {
        if (phase == CollectPhase.BEFORE) {
            for (SrcTreeVisitorListener listener : utils.getListeners()) {
                if (listener.beforeCollectSubMethod(node, this)) {
                    break
                }
            }
            super.visitMethod(node)
            return
        } else if (phase == CollectPhase.AFTER) {
            for (SrcTreeVisitorListener listener : utils.getListeners()) {
                if (listener.afterCollectSubMethod(node, this)) {
                    break
                }
            }
            super.visitMethod(node)
            return
        } else {
            for (SrcTreeVisitorListener listener : utils.getListeners()) {
                if (listener.collectSubMethod(node, this)) {
                    super.visitMethod(node)
                    return
                }
            }
        }

        if (!utils.isSubMethod(node)) {
            super.visitMethod(node)
            return
        }

        // TODO enum etc
        ClassNode classNode = node.getDeclaringClass()
        String classQName = SrcTreeGeneratorUtils.getClassQualifiedName(classNode)
        TestClass testClass = rootClassTable.getByKey(classQName)
        if (testClass == null) {
            testClass = subClassTable.getByKey(classQName)
            if (testClass == null) {
                testClass = utils.generateTestClass(classNode)
                subClassTable.addTestClass(testClass)
            }
        }

        TestMethod testMethod = new TestMethod()
        testMethod.setKey(SrcTreeGeneratorUtils.generateMethodKey(node, false))
        testMethod.setSimpleName(node.getName())
        // TODO captureStyle, TestDocs, etc
        testMethod.setTestDoc(utils.getMethodTestDoc(node))
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


