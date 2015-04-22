package org.sahagin.groovy.runlib.srctreegen

import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.control.SourceUnit
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestMethodTable

// usage
// 1. call initializeVisitor
// 2. call visitClass for all ClassNode
// 3. call finalizeVisitor and get delegation map
class CollectDelegateVisitor {
    private boolean initializeCalled = false
    private List<ClassNode> classNodesRelatedToDelegation = new ArrayList<ClassNode>(64)
    private List<TestClass> testClassesHavingDelegationTo = new ArrayList<TestClass>(32)
    private TestClassTable rootClassTable
    private TestClassTable subClassTable
    private SrcTreeGeneratorUtils utils

    CollectDelegateVisitor(TestClassTable rootClassTable, TestClassTable subClassTable) {
        this.rootClassTable = rootClassTable
        this.subClassTable = subClassTable
    }

    private void initializeVisitorSub(TestClassTable classTable) {
        for (TestClass testClass : classTable.getTestClasses()) {
            if (testClass.delegateToTestClassKey != null) {
                testClassesHavingDelegationTo.add(testClass)
                if (testClass.delegateToTestClass == null) {
                    TestClass delegateToTestClass = SrcTreeGeneratorUtils.getTestClass(
                            testClass.delegateToTestClassKey, rootClassTable, subClassTable)
                    if (delegateToTestClass == null) {
                        // TODO treat as sub class
                        // (AdditionalTestDocsSetter also treat such class as sub class)
                        throw new RuntimeException(
                        "delegation to non-sub non-root method is not supported yet")
                    }
                    testClass.setDelegateToTestClass(delegateToTestClass)
                }
            }
        }
    }

    // resolve all delegationToTestClass in class tables
    // and set up testClassesHavingDelegationTo
    void initializeVisitor() {
        classNodesRelatedToDelegation.clear()
        testClassesHavingDelegationTo.clear()
        initializeVisitorSub(rootClassTable)
        initializeVisitorSub(subClassTable)
        initializeCalled = true
    }

    void visitClass(ClassNode node) {
        if (!initializeCalled) {
            throw new IllegalStateException("not initialized")
        }
        for (TestClass testClass : testClassesHavingDelegationTo) {
            if (testClass.getQualifiedName() == node.getName()
            || testClass.getDelegateToTestClassKey() == node.getName()) {
                classNodesRelatedToDelegation.add(node)
            }
        }
    }

    private ClassNode getClassNode(String classQualifiedName, List<ClassNode> classNodes) {
        for (ClassNode classNode : classNodes) {
            if (classNode.getName() == classQualifiedName) {
                return classNode
            }
        }
        throw new IllegalArgumentException("classNode not found: " + classQualifiedName)
    }

    Map<ClassNode, ClassNode> finalizeVisitor() {
        Map<ClassNode, ClassNode> delegationMap =
        new HashMap<ClassNode, ClassNode>(testClassesHavingDelegationTo.size())
        for (TestClass testClass : testClassesHavingDelegationTo) {
            ClassNode keyNode = getClassNode(
                testClass.getQualifiedName(), classNodesRelatedToDelegation)
            ClassNode valueNode = getClassNode(
                testClass.getDelegateToTestClassKey(), classNodesRelatedToDelegation)
            delegationMap.put(testClass, testClass)
        }
        return delegationMap
    }
}