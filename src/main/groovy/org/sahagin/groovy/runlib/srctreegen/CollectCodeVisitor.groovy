package org.sahagin.groovy.runlib.srctreegen



import java.util.List
import net.sourceforge.htmlunit.corejs.javascript.ast.AstNode
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.CompileUnit
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit
import org.sahagin.runlib.external.adapter.AdapterContainer
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.srctree.TestMethodTable
import org.sahagin.share.srctree.code.Code
import org.sahagin.share.srctree.code.CodeLine
import org.sahagin.share.srctree.code.StringCode
import org.sahagin.share.srctree.code.SubMethodInvoke
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

    private TestMethod getTestMethod(Expression receiverExpression,
            String methodAsString, ArgumentListExpression argumentList) {
        TestMethod testMethod = subMethodTable.getByKey(
                SrcTreeGeneratorUtils.generateMethodKey(receiverExpression, methodAsString, argumentList, false))
        if (testMethod != null) {
            return testMethod
        }
        testMethod = subMethodTable.getByKey(
                SrcTreeGeneratorUtils.generateMethodKey(receiverExpression, methodAsString, argumentList, true))
        if (testMethod != null) {
            return testMethod
        }
        return null
    }

    private Code generateMethodInvokeCode(ASTNode receiver,
        String methodAsString, Expression arguments, String original) {
        // TODO receiver may be null for constructor..
        if (!(receiver instanceof Expression)) {
            return generateUnknownCode(original)
        }
        Expression receiverExpression = receiver as Expression

        if (methodAsString == null || methodAsString == "") {
            return generateUnknownCode(original)
        }

        if (!(arguments instanceof ArgumentListExpression)) {
            return generateUnknownCode(original)
        }
        ArgumentListExpression argumentList = arguments as ArgumentListExpression

        // TODO childInvoke support

        TestMethod invocationMethod = getTestMethod(receiverExpression, methodAsString, argumentList)
        if (invocationMethod == null) {
            return generateUnknownCode(original)
        }

        SubMethodInvoke subMethodInvoke = new SubMethodInvoke()
        subMethodInvoke.setSubMethodKey(invocationMethod.getKey())
        subMethodInvoke.setSubMethod(invocationMethod)
        // TODO null thisInstance especially for constructor
        assert receiverExpression != null
        subMethodInvoke.setThisInstance(expressionCode(receiverExpression))
        for (Expression argExpression : argumentList.getExpressions()) {
            subMethodInvoke.addArg(expressionCode(argExpression))
        }
        // TODO set childInvoke
        subMethodInvoke.setOriginal(original)
        return subMethodInvoke
    }

    private UnknownCode generateUnknownCode(String original) {
        UnknownCode unknownCode = new UnknownCode()
        unknownCode.setOriginal(original)
        return unknownCode
    }

    private UnknownCode generateUnknownCode(Expression expression) {
        return generateUnknownCode(expression.getText()) // TODO temporal logic
    }

    private Code expressionCode(Expression expression) {
        if (expression == null) {
            StringCode strCode = new StringCode()
            strCode.setValue(null)
            strCode.setOriginal("null")
            return strCode
        // TODO handle local variable assignment
        } else if (expression instanceof BinaryExpression) {
            BinaryExpression binary = expression as BinaryExpression
            if (binary instanceof DeclarationExpression
                || binary.getOperation().getText() == "=") {
                // variable declaration or assignment
                if (binary.getRightExpression() == null
                    || binary.getRightExpression() instanceof EmptyExpression) {
                    return generateUnknownCode(expression)
                } else {
                    return expressionCode(binary.getRightExpression())
                }
            } else {
                return generateUnknownCode(expression)
            }
        } else if (expression instanceof ConstantExpression) {
            ConstantExpression constant = expression as ConstantExpression
            Object value = constant.getValue()
            if (value instanceof String) {
                StringCode strCode = new StringCode()
                strCode.setValue(value as String)
                strCode.setOriginal(expression.getText())
                return strCode
            } else {
                return generateUnknownCode(expression)
            }
        } else if (expression instanceof MethodCallExpression) {
            // TODO TestStepLabel handling
            MethodCallExpression methodCall = expression as MethodCallExpression
            return generateMethodInvokeCode(methodCall.getReceiver(),
                methodCall.getMethodAsString(), methodCall.getArguments(), methodCall.getText())
        } else if (expression instanceof ConstructorCallExpression) {
            ConstructorCallExpression constructorCall = expression as ConstructorCallExpression
            return generateMethodInvokeCode(constructorCall.getReceiver(),
                constructorCall.getMethodAsString(), constructorCall.getArguments(), constructorCall.getText())
        } else {
            // TODO local var , arg ref, etc
            return generateUnknownCode(expression)
        }
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
            if (statement instanceof ExpressionStatement) {
                Expression expression = (statement as ExpressionStatement).getExpression()
                code = expressionCode(expression)
            } else {
                code = new UnknownCode()
            }
            CodeLine codeLine = new CodeLine()
            // TODO line number OK ?
            codeLine.setStartLine(statement.getLineNumber())
            codeLine.setEndLine(statement.getLastLineNumber())
            codeLine.setCode(code)
            // sometimes original value set by expressionCode method does not equal to the one of statementNode
            // TODO temp
            //code.setOriginal(statement.getText())
            code.setOriginal(srcUnit.getSource().getLine(statement.getLineNumber(), null).trim())
            testMethod.addCodeBody(codeLine)
        }
        super.visitMethod(node)
        return
    }
}
