package org.sahagin.groovy.runlib.external.adapter.spock

import static org.sahagin.groovy.runlib.external.adapter.spock.SpockAdditionalTestDocsAdapter.*

import java.util.List
import java.util.Map
import java.util.Map.Entry

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.sahagin.groovy.runlib.external.adapter.AbstractSrcTreeVisitorAdapter
import org.sahagin.groovy.runlib.external.adapter.SrcTreeVisitorAdapter.MethodType
import org.sahagin.groovy.runlib.srctreegen.CollectCodeVisitor
import org.sahagin.groovy.runlib.srctreegen.SrcTreeGeneratorUtils
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.srctree.code.Code
import org.sahagin.share.srctree.code.CodeLine
import org.sahagin.share.srctree.code.SubMethodInvoke
import org.sahagin.share.srctree.code.TestStepLabel

class SpockSrcTreeVisitorAdapter extends AbstractSrcTreeVisitorAdapter {
    private String currentSpockLabel = null
    
    @Override
    List<CodeLine> collectMethodStatementCode(Statement statement,
            MethodNode method, MethodType methodType, CollectCodeVisitor visitor) {
        if (methodType != MethodType.ROOT) {
            return null
        }
        if (statement.statementLabel == null) {
            return [generateImplicitAssertCodeLine(statement, method, visitor)]
        }

        String blockText = null
        if (statement instanceof ExpressionStatement) {
            Expression expression = (statement as ExpressionStatement).expression
            if (expression instanceof ConstantExpression) {
                // ConstantExpression statement with label corresponds to block label text
                blockText = (String) (expression as ConstantExpression).value
                assert blockText != null && blockText != ""
            }
        } else if (statement instanceof ReturnStatement) {
            // last line block label can be ReturnStatement
            Expression expression = (statement as ReturnStatement).expression
            if (expression instanceof ConstantExpression) {
                // ConstantExpression statement with label corresponds to block label text
                blockText = (String) (expression as ConstantExpression).value
                assert blockText != null && blockText != ""
            }
        }

        if (blockText != null) {
            TestStepLabel testStepLabel = new TestStepLabel()
            testStepLabel.label = statement.statementLabel
            // last found label must be current label,
            // assuming nested feature method call does not exist
            currentSpockLabel = testStepLabel.label
            testStepLabel.text = blockText
            testStepLabel.original = statement.statementLabel + ": " + blockText

            CodeLine testStepLabelLine = new CodeLine()
            testStepLabelLine.startLine = -1
            testStepLabelLine.endLine = -1
            testStepLabelLine.code = testStepLabel
            return [testStepLabelLine]
        }

        // no text block label and block first statement
        TestStepLabel testStepLabel = new TestStepLabel()
        testStepLabel.label = statement.statementLabel
        // last found label must be current label,
        // assuming nested feature method call does not exist
        currentSpockLabel = testStepLabel.label
        testStepLabel.original = statement.statementLabel + ":"
        CodeLine testStepLabelLine = new CodeLine()
        testStepLabelLine.startLine = -1
        testStepLabelLine.endLine = -1
        testStepLabelLine.code = testStepLabel
        CodeLine mainLine = generateImplicitAssertCodeLine(statement, method, visitor)
        return [testStepLabelLine, mainLine]
    }

    @Override
    boolean afterCollectCode(MethodNode method, CollectCodeVisitor visitor) {
        currentSpockLabel = null // reset current label
        return false
    }
    
    // generate implicit assert for boolean statement inside then/expect block,
    // otherwise normal CodeLine for the specified statement
    private CodeLine generateImplicitAssertCodeLine(
        Statement statement, MethodNode method, CollectCodeVisitor visitor) {
        CodeLine codeLine
        ClassNode classNode
        (codeLine, classNode) = visitor.generateCodeLine(statement, method)
        if (classNode != ClassHelper.boolean_TYPE && classNode != ClassHelper.Boolean_TYPE) {
            return codeLine
        }
        if (currentSpockLabel != "then" && currentSpockLabel != "expect") {
            return codeLine
        }
        
        String implicitAssertMethodKey = TestMethod.generateMethodKey(
            CLASS_QUALIFIED_NAME, METHOD_IMPLICIT_ASSERT)
        TestMethod implicitAssertMethod = visitor.subMethodTable.getByKey(implicitAssertMethodKey)
        assert implicitAssertMethod != null
        SubMethodInvoke implicitAssertMethodInvoke = new SubMethodInvoke()
        implicitAssertMethodInvoke.subMethodKey = implicitAssertMethodKey
        implicitAssertMethodInvoke.subMethod = implicitAssertMethod
        implicitAssertMethodInvoke.addArg(codeLine.code)
        implicitAssertMethodInvoke.original = codeLine.code.original
        return visitor.generateCodeLine(
            statement, implicitAssertMethodInvoke, ClassHelper.VOID_TYPE).first()
    }

}