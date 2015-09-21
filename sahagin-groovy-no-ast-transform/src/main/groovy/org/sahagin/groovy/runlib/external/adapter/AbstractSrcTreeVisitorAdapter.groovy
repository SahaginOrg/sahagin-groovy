package org.sahagin.groovy.runlib.external.adapter

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.MethodCall;
import org.codehaus.groovy.ast.stmt.Statement
import org.sahagin.groovy.runlib.external.adapter.SrcTreeVisitorAdapter.MethodType
import org.sahagin.groovy.runlib.srctreegen.CollectCodeVisitor
import org.sahagin.groovy.runlib.srctreegen.CollectRootVisitor
import org.sahagin.groovy.runlib.srctreegen.CollectSubVisitor
import org.sahagin.share.srctree.code.Code;
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
    def generateMethodInvokeCode(MethodCall methodCall, 
            MethodNode parentMethod, CollectCodeVisitor visitor) {
        return [null, null]
    }

    @Override
    boolean generatedMethodInvokeCode(Code code, ClassNode classNode, CollectCodeVisitor visitor) {
        return false
    }
    
    @Override
    def generateVarAssignCode(BinaryExpression binary,
            MethodNode parentMethod, CollectCodeVisitor visitor) {
        return [null, null]
    }

    @Override
    def generateFieldCode(String fieldName, ClassNode fieldOwnerType,
        Code receiverCode, String original, CollectCodeVisitor visitor) {
        return [null, null]
    }

    @Override
    ClassNode getDelegateToClassNode(ClassNode classNode, CollectCodeVisitor visitor) {
        return null
    }

    @Override
    boolean afterCollectCode(MethodNode method,
            CollectCodeVisitor visitor) {
        return false
    }

}