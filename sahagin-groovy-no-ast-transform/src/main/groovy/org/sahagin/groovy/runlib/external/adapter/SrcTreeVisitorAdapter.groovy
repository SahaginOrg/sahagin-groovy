package org.sahagin.groovy.runlib.external.adapter

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.MethodCall;
import org.codehaus.groovy.ast.stmt.Statement
import org.sahagin.groovy.runlib.srctreegen.CollectCodeVisitor
import org.sahagin.groovy.runlib.srctreegen.CollectRootVisitor
import org.sahagin.groovy.runlib.srctreegen.CollectSubVisitor
import org.sahagin.share.srctree.code.Code;
import org.sahagin.share.srctree.code.CodeLine

interface SrcTreeVisitorAdapter {

    enum CollectPhase {
        BEFORE,
        WHILE,
        AFTER
    }

    enum MethodType {
        ROOT,
        SUB,
        NONE
    }

    // TODO for now, before and after event does not have methodType argument for efficiency

    // Called before all CollectRootVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean beforeCollectRootMethod(MethodNode method, CollectRootVisitor visitor)
   
    // Called while CollectRootVisitor visits a method.
    // If returns true, the subsequent visitor or visitor listener logics for this method are skipped
    boolean collectRootMethod(MethodNode method, MethodType methodType, CollectRootVisitor visitor)

    // Called after all CollectRootVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean afterCollectRootMethod(MethodNode method, CollectRootVisitor visitor)

    // Called before all CollectSubVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean beforeCollectSubMethod(MethodNode method, CollectSubVisitor visitor)

    // Called while CollectSubVisitor visits a method.
    // If returns true, the subsequent visitor or visitor listener logics for this method are skipped 
    boolean collectSubMethod(MethodNode method, MethodType methodType, CollectSubVisitor visitor)

    // Called after all CollectSubVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean afterCollectSubMethod(MethodNode method, CollectSubVisitor visitor)

    // Called before all CollectCodeVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean beforeCollectCode(MethodNode method, CollectCodeVisitor visitor)

    // Called while CollectCodeVisitor visits a method.
    // If returns true, the subsequent visitor or visitor listener logics for this method are skipped
    boolean collectCode(MethodNode method, MethodType methodType, CollectCodeVisitor visitor)

    // Called while CollectCodeVisitor visits each statement for a method body.
    // If not empty list is returned, the subsequent visitor or visitor listener logics 
    // for this statement are skipped
    List<CodeLine> collectMethodStatementCode(Statement statement, MethodNode method,
        MethodType methodType, CollectCodeVisitor visitor)
    
    // Called while CollectCodeVisitor generates SubMethodInvoke code.
    // This method returns the pair of [Code, ClassNode], and if Code is not null,
    // the subsequent visitor or visitor listener logics for this expression are skipped
    def generateMethodInvokeCode(MethodCall methodCall, 
        MethodNode parentMethod, CollectCodeVisitor visitor)
    
    // Called after CollectCodeVisitor has generated MethodInvoke code.
    // If returns true, the subsequent visitor or visitor listener logics 
    // for this MethodInvoke are skipped
    boolean generatedMethodInvokeCode(Code code, ClassNode classNode, CollectCodeVisitor visitor)
    
    // Called while CollectCodeVisitor generates VarAssign code.
    // This method returns the pair of [Code, ClassNode], and if Code is not null,
    // the subsequent visitor or visitor listener logics for this expression are skipped
    def generateVarAssignCode(BinaryExpression binary, 
        MethodNode parentMethod, CollectCodeVisitor visitor)
    
    // Called while CollectCodeVisitor generates Field code.
    // This method returns the pair of [Code, ClassNode], and if Code is not null,
    // the subsequent visitor or visitor listener logics for this field are skipped
    def generateFieldCode(String fieldName, ClassNode fieldOwnerType, 
        Code receiverCode, String original, CollectCodeVisitor visitor)
    
    // Called while CollectCodeVisitor searches ClassNode to which the specified classNode delegates.
    // If returned ClassNode is not null, the subsequent visitor or visitor listener logics 
    // for this classNode are skipped
    ClassNode getDelegateToClassNode(ClassNode classNode, CollectCodeVisitor visitor)

    // Called after all CollectCodeVisitor method visits.
    // If returns true, the subsequent visitor listener logics are skipped
    boolean afterCollectCode(MethodNode method, CollectCodeVisitor visitor)

}