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
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit
import org.sahagin.runlib.additionaltestdoc.AdditionalTestDocs
import org.sahagin.runlib.external.adapter.AdapterContainer
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.srctree.TestMethodTable
import org.sahagin.share.srctree.code.Code
import org.sahagin.share.srctree.code.CodeLine
import org.sahagin.share.srctree.code.StringCode
import org.sahagin.share.srctree.code.SubMethodInvoke
import org.sahagin.share.srctree.code.UnknownCode

class CollectCodeVisitor extends ClassCodeVisitorSupport {
    private TestClassTable rootClassTable
    private TestClassTable subClassTable
    private TestMethodTable rootMethodTable
    private TestMethodTable subMethodTable
    private SrcTreeGeneratorUtils utils
    private SourceUnit srcUnit

    CollectCodeVisitor(TestClassTable rootClassTable, TestClassTable subClassTable,
        TestMethodTable rootMethodTable, TestMethodTable subMethodTable,
        SrcTreeGeneratorUtils utils) {
        this.rootClassTable = rootClassTable
        this.subClassTable = subClassTable
        this.rootMethodTable = rootMethodTable
        this.subMethodTable = subMethodTable
        this.utils = utils
    }

    void setSrcUnit(SourceUnit srcUnit) {
        this.srcUnit = srcUnit
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return null // dummy
    }

    private TestClass getTestClass(String classQualifiedName) {
        TestClass testClass = subClassTable.getByKey(classQualifiedName)
        if (testClass != null) {
            return testClass
        }
        testClass = rootClassTable.getByKey(classQualifiedName)
        if (testClass != null) {
            return testClass
        }
        return null
    }

    private TestMethod getTestMethod(String classQualifiedName,
            String methodAsString, ArgumentListExpression argumentList) {
        // TODO argumentList type maybe super type of actual method argument type..
        TestMethod testMethod = subMethodTable.getByKey(utils.generateMethodKey(
                classQualifiedName, methodAsString, argumentList, false))
        if (testMethod != null) {
            return testMethod
        }
        // TODO argumentList type maybe super type of actual method argument type..
        testMethod = subMethodTable.getByKey(utils.generateMethodKey(
                classQualifiedName, methodAsString, argumentList, true))
        if (testMethod != null) {
            return testMethod
        }
        return null
    }

    // returns (TestMethod, isSuper)
    // - if delegate is true, check only delegateTo class
    def getThisOrSuperMethodSub(ClassNode classNode,
            String methodAsString, ArgumentListExpression argumentList, boolean delegate) {
        ClassNode parent = classNode
        while (parent != null) {
            String classQualifiedName
            if (delegate) {
                TestClass parentTestClass = getTestClass(parent.getName())
                if (parentTestClass == null) {
                    parent = parent.getSuperClass()
                    continue
                }
                TestClass delegateToTestClass = parentTestClass.delegateToTestClass
                if (delegateToTestClass == null) {
                    parent = parent.getSuperClass()
                    continue
                }
                classQualifiedName = delegateToTestClass.getQualifiedName()
            } else {
                classQualifiedName = parent.getName()
            }

            TestMethod parentTestMethod = getTestMethod(
                classQualifiedName, methodAsString, argumentList)
            if (parentTestMethod != null) {
                return [parentTestMethod, parent != classNode]
            }
            parent = parent.getSuperClass()
        }
        return [null, false]
    }

    // returns (TestMethod, isSuper)
    private def getThisOrSuperTestMethod(ClassNode classNode,
            String methodAsString, ArgumentListExpression argumentList) {
        TestMethod invocationMethod
        boolean isSuper

        // check this class and super class
        (invocationMethod, isSuper) = getThisOrSuperMethodSub(
            classNode, methodAsString, argumentList, false)
        if (invocationMethod != null) {
            return [invocationMethod, isSuper]
        }

        // check only delegateTo class for this class and super class
        (invocationMethod, isSuper) = getThisOrSuperMethodSub(
            classNode, methodAsString, argumentList, true)
        if (invocationMethod != null) {
            return [invocationMethod, isSuper]
        }

        return [null, false]
    }

    private Code generateMethodInvokeCode(ASTNode receiver,
        String methodAsString, Expression arguments, String original, ClassNode thisClassNode) {
        // TODO receiver may be null for constructor..
        if (!(receiver instanceof Expression)) {
            return generateUnknownCode(original)
        }
        Expression receiverExpression = receiver as Expression
        ClassNode classNode
        if ((receiverExpression instanceof VariableExpression) &&
            ((receiverExpression as VariableExpression).getName() == "this")) {
            // type of receiverExpression is Object in this case,
            // but the type should be the type for thisClassNode
            classNode = thisClassNode
        } else {
            classNode = receiverExpression.getType()
        }

        if (methodAsString == null || methodAsString == "") {
            return generateUnknownCode(original)
        }

        if (!(arguments instanceof ArgumentListExpression)) {
            return generateUnknownCode(original)
        }
        ArgumentListExpression argumentList = arguments as ArgumentListExpression

        TestMethod invocationMethod
        boolean isSuper
        (invocationMethod, isSuper) =
        getThisOrSuperTestMethod(classNode, methodAsString, argumentList)
        if (invocationMethod == null) {
            return generateUnknownCode(original)
        }

        SubMethodInvoke subMethodInvoke = new SubMethodInvoke()
        subMethodInvoke.setSubMethodKey(invocationMethod.getKey())
        subMethodInvoke.setSubMethod(invocationMethod)
        // TODO null thisInstance especially for constructor
        assert receiverExpression != null
        subMethodInvoke.setThisInstance(expressionCode(receiverExpression, thisClassNode))
        for (Expression argExpression : argumentList.getExpressions()) {
            subMethodInvoke.addArg(expressionCode(argExpression, thisClassNode))
        }
        subMethodInvoke.setChildInvoke(isSuper)
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

    private Code expressionCode(Expression expression, ClassNode thisClassNode) {
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
                    return expressionCode(binary.getRightExpression(), thisClassNode)
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
                methodCall.getMethodAsString(), methodCall.getArguments(),
                methodCall.getText(), thisClassNode)
        } else if (expression instanceof ConstructorCallExpression) {
            ConstructorCallExpression constructorCall = expression as ConstructorCallExpression
            return generateMethodInvokeCode(constructorCall.getReceiver(),
                constructorCall.getMethodAsString(), constructorCall.getArguments(),
                constructorCall.getText(), thisClassNode)
        } else {
            // TODO local var, arg ref, etc
            return generateUnknownCode(expression)
        }
    }

    @Override
    void visitMethod(MethodNode node) {
        TestMethod testMethod
        if (utils.isRootMethod(node)) {
            // TODO searching twice from table is not elegant logic..
            testMethod = rootMethodTable.getByKey(utils.generateMethodKey(node, false))
            if (testMethod == null) {
                testMethod = rootMethodTable.getByKey(utils.generateMethodKey(node, true))
            }
        } else if (utils.isSubMethod(node)) {
            // TODO searching twice from table is not elegant logic..
            testMethod = subMethodTable.getByKey(utils.generateMethodKey(node, false))
            if (testMethod == null) {
                testMethod = subMethodTable.getByKey(utils.generateMethodKey(node, true))
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
                code = expressionCode(expression, node.getDeclaringClass())
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
