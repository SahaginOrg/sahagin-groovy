package org.sahagin.groovy.runlib.srctreegen

import org.codehaus.groovy.ast.MethodNode
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestFieldTable
import org.sahagin.share.srctree.TestMethodTable

abstract class SrcTreeVisitorListener {

    enum CollectPhase {
        BEFORE,
        WHILE,
        AFTER
    }

    // Called before all CollectRootVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean beforeCollectRootMethod(MethodNode node, CollectRootVisitor visitor) {
        return false
    }

    // Called while other CollectRootVisitor method visits.
    // If returns true, the subsequent visitor or visitor listener logics are skipped
    boolean collectRootMethod(MethodNode node, CollectRootVisitor visitor) {
        return false
    }

    // Called after all CollectRootVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean afterCollectRootMethod(MethodNode node, CollectRootVisitor visitor) {
        return false
    }

    // Called before all CollectSubVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean beforeCollectSubMethod(MethodNode node, CollectSubVisitor visitor) {
        return false
    }

    // Called while other CollectSubVisitor method visits.
    // If returns true, the subsequent visitor or visitor listener logics are skipped
    boolean collectSubMethod(MethodNode node, CollectSubVisitor visitor) {
        return false
    }

    // Called after all CollectSubVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean afterCollectSubMethod(MethodNode node, CollectSubVisitor visitor) {
        return false
    }

    // Called before all CollectCodeVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean beforeCollectCode(MethodNode node, CollectCodeVisitor visitor) {
        return false
    }

    // Called while other CollectCodeVisitor method visits.
    // If returns true, the subsequent visitor or visitor listener logics are skipped
    boolean collectCode(MethodNode node, CollectCodeVisitor visitor) {
        return false
    }

    // Called after all CollectCodeVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean afterCollectCode(MethodNode node, CollectCodeVisitor visitor) {
        return false
    }

}
