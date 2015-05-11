package org.sahagin.groovy.runlib.srctreegen

import org.codehaus.groovy.ast.MethodNode

class SpockVisitorListener extends SrcTreeVisitorListener {
    private SrcTreeGeneratorUtils utils

    SpockVisitorListener(SrcTreeGeneratorUtils utils) {
        this.utils = utils
    }

    @Override
    boolean collectCode(MethodNode node, CollectCodeVisitor visitor) {
        return false
    }

}