package org.sahagin.groovy.runlib.external.adapter

import org.codehaus.groovy.ast.MethodNode
import org.sahagin.groovy.runlib.srctreegen.CollectCodeVisitor
import org.sahagin.groovy.runlib.srctreegen.CollectRootVisitor
import org.sahagin.groovy.runlib.srctreegen.CollectSubVisitor

interface SrcTreeVisitorAdapter {

    enum CollectPhase {
        BEFORE,
        WHILE,
        AFTER
    }

    // Called before all CollectRootVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean beforeCollectRootMethod(MethodNode node, CollectRootVisitor visitor)

    // Called while other CollectRootVisitor method visits.
    // If returns true, the subsequent visitor or visitor listener logics are skipped
    boolean collectRootMethod(MethodNode node, CollectRootVisitor visitor)

    // Called after all CollectRootVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean afterCollectRootMethod(MethodNode node, CollectRootVisitor visitor)

    // Called before all CollectSubVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean beforeCollectSubMethod(MethodNode node, CollectSubVisitor visitor)

    // Called while other CollectSubVisitor method visits.
    // If returns true, the subsequent visitor or visitor listener logics are skipped
    boolean collectSubMethod(MethodNode node, CollectSubVisitor visitor)

    // Called after all CollectSubVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean afterCollectSubMethod(MethodNode node, CollectSubVisitor visitor)

    // Called before all CollectCodeVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean beforeCollectCode(MethodNode node, CollectCodeVisitor visitor)

    // Called while other CollectCodeVisitor method visits.
    // If returns true, the subsequent visitor or visitor listener logics are skipped
    boolean collectCode(MethodNode node, CollectCodeVisitor visitor)

    // Called after all CollectCodeVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean afterCollectCode(MethodNode node, CollectCodeVisitor visitor)

}