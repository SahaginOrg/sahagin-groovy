package org.sahagin.groovy.runlib.external.adapter

import org.codehaus.groovy.ast.MethodNode
import org.sahagin.groovy.runlib.srctreegen.CollectCodeVisitor
import org.sahagin.groovy.runlib.srctreegen.CollectRootVisitor
import org.sahagin.groovy.runlib.srctreegen.CollectSubVisitor

class AbstractSrcTreeVisitorAdapter implements SrcTreeVisitorAdapter {

    @Override
    public boolean beforeCollectRootMethod(MethodNode node, CollectRootVisitor visitor) {
        return false
    }

    @Override
    public boolean collectRootMethod(MethodNode node, CollectRootVisitor visitor) {
        return false
    }

    @Override
    public boolean afterCollectRootMethod(MethodNode node, CollectRootVisitor visitor) {
        return false
    }

    @Override
    public boolean beforeCollectSubMethod(MethodNode node, CollectSubVisitor visitor) {
        return false
    }

    @Override
    public boolean collectSubMethod(MethodNode node, CollectSubVisitor visitor) {
        return false
    }

    @Override
    public boolean afterCollectSubMethod(MethodNode node, CollectSubVisitor visitor) {
        return false
    }

    @Override
    public boolean beforeCollectCode(MethodNode node, CollectCodeVisitor visitor) {
        return false
    }

    @Override
    public boolean collectCode(MethodNode node, CollectCodeVisitor visitor) {
        return false
    }

    @Override
    public boolean afterCollectCode(MethodNode node, CollectCodeVisitor visitor) {
        return false
    }

}