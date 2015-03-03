package org.sahagin.groovy.runlib.srctreegen

import java.util.List

import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.CompileUnit
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit
import org.eclipse.jdt.core.dom.ASTNode
import org.eclipse.jdt.core.dom.Block
import org.eclipse.jdt.core.dom.Expression
import org.eclipse.jdt.core.dom.ExpressionStatement
import org.eclipse.jdt.core.dom.IMethodBinding
import org.eclipse.jdt.core.dom.VariableDeclarationFragment
import org.eclipse.jdt.core.dom.VariableDeclarationStatement
import org.sahagin.runlib.external.adapter.AdapterContainer
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.srctree.TestMethodTable
import org.sahagin.share.srctree.code.Code
import org.sahagin.share.srctree.code.CodeLine
import org.sahagin.share.srctree.code.UnknownCode

class CollectCodeVisitor extends ClassCodeVisitorSupport {
    private TestMethodTable rootMethodTable
    private TestMethodTable subMethodTable
    private SourceUnit srcUnit

    CollectCodeVisitor(TestMethodTable rootMethodTable, TestMethodTable subMethodTable) {
        this.rootMethodTable = rootMethodTable
        this.subMethodTable = subMethodTable
    }
    
    void setSrcUnit(SourceUnit srcUnit) {
        this.srcUnit = srcUnit
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return null // dummy
    }
   
    @Override
    void visitMethod(MethodNode node) {
        TestMethod testMethod
        if (SrcTreeGeneratorUtils.isRootMethod(node)) {
            // TODO searching twice from table is not elegant logic..
            testMethod = rootMethodTable.getByKey(SrcTreeGeneratorUtils.generateMethodKey(node, false))
            if (testMethod == null) {
                testMethod = rootMethodTable.getByKey(SrcTreeGeneratorUtils.generateMethodKey(node, true))
            }
        } else if (SrcTreeGeneratorUtils.isSubMethod(node)) {
            // TODO searching twice from table is not elegant logic..
            testMethod = subMethodTable.getByKey(SrcTreeGeneratorUtils.generateMethodKey(node, false))
            if (testMethod == null) {
                testMethod = subMethodTable.getByKey(SrcTreeGeneratorUtils.generateMethodKey(node, true))
            }
        } else {
            super.visitMethod(node)
            return
        }

        BlockStatement body = (BlockStatement) node.getCode()
        if (body == null) {
            // no body. Maybe abstract method or interface method
            super.visitMethod(node)
            return
        }
        List<Statement> list = body.getStatements()
        for (Statement statement : list) {
            Code code
            // TODO temporal
            code = new UnknownCode()
                        
            CodeLine codeLine = new CodeLine()
            // TODO line number OK ?
            codeLine.setStartLine(statement.getLineNumber())
            codeLine.setEndLine(statement.getLastLineNumber())
            codeLine.setCode(code)
            // sometimes original value set by expressionCode method does not equal to the one of statementNode
            // TODO temp
            code.setOriginal(srcUnit.getSource().getLine(statement.getLineNumber(), null).trim())
            testMethod.addCodeBody(codeLine)
        }
        super.visitMethod(node)
        return
    }
}
