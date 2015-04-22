package org.sahagin.groovy.runlib.srctreegen

import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.control.SourceUnit
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestMethodTable

class DelegateResolver {
    private TestClassTable rootClassTable
    private TestClassTable subClassTable

    DelegateResolver(TestClassTable rootClassTable, TestClassTable subClassTable) {
        this.rootClassTable = rootClassTable
        this.subClassTable = subClassTable
    }

    private void resolveSub(TestClassTable classTable) {
        for (TestClass testClass : classTable.getTestClasses()) {
            if (testClass.delegateToTestClassKey != null) {
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
    void resolve() {
        resolveSub(rootClassTable)
        resolveSub(subClassTable)
    }

}