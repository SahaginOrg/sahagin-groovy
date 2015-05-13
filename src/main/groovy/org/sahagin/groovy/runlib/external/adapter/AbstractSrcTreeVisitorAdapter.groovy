package org.sahagin.groovy.runlib.external.adapter

import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.stmt.Statement
import org.sahagin.groovy.runlib.external.adapter.SrcTreeVisitorAdapter.MethodType
import org.sahagin.groovy.runlib.srctreegen.CollectCodeVisitor
import org.sahagin.groovy.runlib.srctreegen.CollectRootVisitor
import org.sahagin.groovy.runlib.srctreegen.CollectSubVisitor
import org.sahagin.share.srctree.code.CodeLine

class AbstractSrcTreeVisitorAdapter implements SrcTreeVisitorAdapter {

    @Override
    boolean beforeCollectRootMethod(MethodNode method,
            CollectRootVisitor visitor) {
        return false
    }

    @Override
    boolean collectRootMethod(MethodNode method,
            MethodType methodType, CollectRootVisitor visitor) {
        return false
    }

    @Override
    boolean afterCollectRootMethod(MethodNode method,
            CollectRootVisitor visitor) {
        return false
    }

    @Override
    boolean beforeCollectSubMethod(MethodNode method,
            CollectSubVisitor visitor) {
        return false
    }

    @Override
    boolean collectSubMethod(MethodNode method,
            MethodType methodType, CollectSubVisitor visitor) {
        return false
    }

    @Override
    boolean afterCollectSubMethod(MethodNode method,
            CollectSubVisitor visitor) {
        return false
    }

    @Override
    boolean beforeCollectCode(MethodNode method,
            CollectCodeVisitor visitor) {
        return false
    }

    @Override
    boolean collectCode(MethodNode method,
            MethodType methodType, CollectCodeVisitor visitor) {
        return false
    }

    @Override
    List<CodeLine> collectMethodStatementCode(Statement statement,
            MethodNode method, MethodType methodType, CollectCodeVisitor visitor) {
        return new ArrayList<CodeLine>(0)
    }

    @Override
    boolean afterCollectCode(MethodNode method,
            CollectCodeVisitor visitor) {
        return false
    }

}