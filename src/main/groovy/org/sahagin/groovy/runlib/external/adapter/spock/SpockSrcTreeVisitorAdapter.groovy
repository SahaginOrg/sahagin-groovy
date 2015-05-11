package org.sahagin.groovy.runlib.external.adapter.spock

import org.codehaus.groovy.ast.MethodNode
import org.sahagin.groovy.runlib.external.adapter.AbstractSrcTreeVisitorAdapter
import org.sahagin.groovy.runlib.srctreegen.CollectCodeVisitor
import org.sahagin.groovy.runlib.srctreegen.SrcTreeGeneratorUtils

class SpockSrcTreeVisitorAdapter extends AbstractSrcTreeVisitorAdapter {

    @Override
    boolean collectCode(MethodNode node, CollectCodeVisitor visitor) {
        if (!SrcTreeGeneratorUtils.isRootMethod(node)) {
            return false
        }


        return false
    }

}