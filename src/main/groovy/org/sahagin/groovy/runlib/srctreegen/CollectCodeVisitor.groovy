package org.sahagin.groovy.runlib.srctreegen

import java.util.List

import net.sourceforge.htmlunit.corejs.javascript.ast.AstNode

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.CompileUnit
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.TupleExpression
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
        TestMethodTable rootMethodTable, TestMethodTable subMethodTable, SrcTreeGeneratorUtils utils) {
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

    private static MethodNode getThisOrSuperMethodNode(
        ClassNode classNode, String methodName, List<ClassNode> argClasses) {
        ClassNode parentNode = classNode
        while (parentNode != null) {
            for (MethodNode method : parentNode.getDeclaredMethods(methodName)) {
                if (method.getParameters().length != argClasses.size()) {
                    continue // go to next method
                }
                boolean matched = true
                for (int i = 0; i < argClasses.size(); i++) {
                    if (!argClasses.get(i).isDerivedFrom(method.getParameters()[i].getType())) {
                        matched = false
                        break // quit argument type checking
                    }
                }
                if (matched) {
                    return method
                }
            }
            parentNode = parentNode.getSuperClass()
        }
        return null
    }

    private static TestMethod getTestMethod(MethodNode methodNode, TestMethodTable methodTable) {
        // TODO searching twice from table is not elegant logic..
        TestMethod testMethod = methodTable.getByKey(
        SrcTreeGeneratorUtils.generateMethodKey(methodNode, false))
        if (testMethod != null) {
            return testMethod
        }
        testMethod = methodTable.getByKey(
        SrcTreeGeneratorUtils.generateMethodKey(methodNode, true))
        return testMethod
    }

    // returns [TestMethod, MethodNode]
    private def getThisOrSuperMethodSubNoDelegate(ClassNode classNode,
            String methodAsString, List<ClassNode> argClasses) {
        MethodNode methodNode = getThisOrSuperMethodNode(classNode, methodAsString, argClasses)
        if (methodNode == null) {
            return [null, null]
        }
        TestMethod testMethod = getTestMethod(methodNode, subMethodTable)
        return [testMethod, methodNode]
    }

    private ClassNode getDelegateToClassNode(ClassNode classNode) {
        TestClass testClass = SrcTreeGeneratorUtils.getTestClass(
            classNode.getName(), rootClassTable, subClassTable)
        if (testClass == null) {
            return null
        }
        if (testClass.delegateToTestClass == null) {
            return null
        }
        String delegateToClassQualifiedName = testClass.delegateToTestClass.getQualifiedName()
        Class<?> delegateToClass = null
        try {
            delegateToClass = Class.forName(delegateToClassQualifiedName)
        } catch (ClassNotFoundException e) {
            return null
        }
        return ClassHelper.make(delegateToClass)
    }

    // returns (TestMethod, MethodNode)
    // - if delegate is true, check only delegateTo class
    private def getThisOrSuperMethodSub(ClassNode classNode,
            String methodAsString, List<ClassNode> argClasses, boolean delegate) {
        if (!delegate) {
            TestMethod testMethod
            MethodNode methodNode
            (testMethod, methodNode) = getThisOrSuperMethodSubNoDelegate(
                classNode, methodAsString, argClasses)
            return [testMethod, methodNode]
        }

        ClassNode parentNode = classNode
        while (parentNode != null) {
            ClassNode delegateToClassNode = getDelegateToClassNode(parentNode)
            while (delegateToClassNode != null) {
                TestMethod testMethod
                MethodNode methodNode
                (testMethod, methodNode) = getThisOrSuperMethodSubNoDelegate(
                    delegateToClassNode, methodAsString, argClasses)
                if (methodNode != null) {
                    return [testMethod, methodNode]
                }
                delegateToClassNode = getDelegateToClassNode(delegateToClassNode)
            }
            parentNode = parentNode.getSuperClass()
        }
        return [null, null]
    }

    // returns (TestMethod, MethodNode)
    private def getThisOrSuperTestMethod(ClassNode classNode,
            String methodAsString, List<ClassNode> argClasses) {
        TestMethod invocationMethod
        MethodNode invocationMethodNode
        boolean isSuper

        // check this class and super class
        (invocationMethod, invocationMethodNode) = getThisOrSuperMethodSub(
            classNode, methodAsString, argClasses, false)
        if (invocationMethod != null) {
            return [invocationMethod, invocationMethodNode]
        }

        // check only delegateTo class for this class and super class
        (invocationMethod, invocationMethodNode) = getThisOrSuperMethodSub(
            classNode, methodAsString, argClasses, true)
        if (invocationMethod != null) {
            return [invocationMethod, invocationMethodNode]
        }

        return [null, null]
    }

    // return [Code, ClassNode]
    private def generateMethodInvokeCode(ASTNode receiver,
            String methodAsString, Expression arguments, String original, ClassNode thisClassNode) {
        // TODO receiver may be null for constructor..
        if (!(receiver instanceof Expression)) {
            return generateUnknownCode(original, ClassHelper.OBJECT_TYPE)
        }
        Expression receiverExpression = receiver as Expression
        Code receiverCode
        ClassNode receiverClassNode
        (receiverCode, receiverClassNode) = expressionCode(receiverExpression, thisClassNode)

        if (methodAsString == null || methodAsString == "") {
            return generateUnknownCode(original, ClassHelper.OBJECT_TYPE)
        }

        if (!(arguments instanceof ArgumentListExpression)) {
            return generateUnknownCode(original, ClassHelper.OBJECT_TYPE)
        }
        ArgumentListExpression argumentList = arguments as ArgumentListExpression
        List<Code> argCodes = new ArrayList<Code>(argumentList.getExpressions().size())
        List<ClassNode> argClasses = new ArrayList<ClassNode>(argumentList.getExpressions().size())
        for (Expression argExpression : argumentList.getExpressions()) {
            Code argCode
            ClassNode argClass
            (argCode, argClass) = expressionCode(argExpression, thisClassNode)
            argCodes.add(argCode)
            argClasses.add(argClass)
        }

        TestMethod invocationMethod
        MethodNode invocationMethodNode
        (invocationMethod, invocationMethodNode) =
                getThisOrSuperTestMethod(receiverClassNode, methodAsString, argClasses)
        if (invocationMethod == null || invocationMethodNode == null) {
            return generateUnknownCode(original, ClassHelper.OBJECT_TYPE)
        }

        SubMethodInvoke subMethodInvoke = new SubMethodInvoke()
        subMethodInvoke.setSubMethodKey(invocationMethod.getKey())
        subMethodInvoke.setSubMethod(invocationMethod)
        // TODO null thisInstance especially for constructor
        assert receiverExpression != null
        subMethodInvoke.setThisInstance(receiverCode)
        for (Code argCode : argCodes) {
            subMethodInvoke.addArg(argCode)
        }
        subMethodInvoke.setChildInvoke(
            invocationMethodNode.getDeclaringClass().getName() != receiverClassNode.getName())
        subMethodInvoke.setOriginal(original)
        return [subMethodInvoke, invocationMethodNode.getReturnType()]
    }

    // returns (UnknownCode, ClassNode)
    private def generateUnknownCode(String original, ClassNode classNode) {
        UnknownCode unknownCode = new UnknownCode()
        unknownCode.setOriginal(original)
        return [unknownCode, classNode]
    }

    // returns (UnknownCode, ClassNode)
    private def generateUnknownCode(Expression expression) {
        // TODO using getText is temporal logic
        return generateUnknownCode(expression.getText(), expression.getType())
    }

    // returns (Code, ClassNode)
    def expressionCode(Expression expression, ClassNode thisClassNode) {
        if (expression == null) {
            StringCode strCode = new StringCode()
            strCode.setValue(null)
            strCode.setOriginal("null")
            return [strCode, ClassHelper.OBJECT_TYPE]
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
                return [strCode, ClassHelper.STRING_TYPE]
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
        } else if ((expression instanceof VariableExpression) &&
            ((expression as VariableExpression).getName() == "this")) {
            // this keyword
            Code code = generateUnknownCode(expression).first()
            return [code, thisClassNode]
        } else {
            // TODO local var, arg ref, etc
            return generateUnknownCode(expression)
        }
    }

    @Override
    void visitMethod(MethodNode node) {
        TestMethod testMethod
        if (SrcTreeGeneratorUtils.isRootMethod(node)) {
            testMethod = getTestMethod(node, rootMethodTable)
        } else if (utils.isSubMethod(node)) {
            testMethod = getTestMethod(node, subMethodTable)
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
                code = expressionCode(expression, node.getDeclaringClass()).first()
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
