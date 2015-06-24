package org.sahagin.groovy.runlib.external.adapter.spock

import java.util.List
import java.util.Map
import java.util.Map.Entry

import org.codehaus.groovy.ast.AnnotationNode
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
import org.sahagin.share.srctree.code.Code
import org.sahagin.share.srctree.code.CodeLine
import org.sahagin.share.srctree.code.TestStepLabel

class SpockSrcTreeVisitorAdapter extends AbstractSrcTreeVisitorAdapter {

    @Override
    List<CodeLine> collectMethodStatementCode(Statement statement,
            MethodNode method, MethodType methodType, CollectCodeVisitor visitor) {
        if (methodType != MethodType.ROOT) {
            return null
        }
        if (statement.statementLabel == null) {
            return null
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
        testStepLabel.original = statement.statementLabel + ":"
        CodeLine testStepLabelLine = new CodeLine()
        testStepLabelLine.startLine = -1
        testStepLabelLine.endLine = -1
        testStepLabelLine.code = testStepLabel
        CodeLine mainLine = visitor.generateCodeLine(statement, method)
        return [testStepLabelLine, mainLine]
    }

}