package org.sahagin.groovy.runlib.srctreegen

import org.codehaus.groovy.ast.MethodNode
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestFieldTable
import org.sahagin.share.srctree.TestMethodTable

abstract class SrcTreeVisitorListener {

    enum MethodType {
        NONE,
        ROOT,
        SUB
    }

    // if returns true, the subsequent visitor or visitor listener logics are skipped
    boolean beforeCollectRootMethod(MethodNode node,
        MethodType type, CollectRootVisitor visitor) {
        return false
    }

    // if returns true, the subsequent visitor or visitor listener logics are skipped
    boolean beforeCollectSubMethod(MethodNode node,
        MethodType type, CollectSubVisitor visitor) {
        return false
    }

    // if returns true, the subsequent visitor or visitor listener logics are skipped
    boolean beforeCollectCode(MethodNode node,
        MethodType type, CollectCodeVisitor visitor) {
        return false
    }

}
