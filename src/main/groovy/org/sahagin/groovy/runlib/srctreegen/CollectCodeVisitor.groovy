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
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCall
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit
import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.groovy.runlib.external.adapter.SrcTreeVisitorAdapter
import org.sahagin.groovy.runlib.external.adapter.SrcTreeVisitorAdapter.CollectPhase
import org.sahagin.runlib.additionaltestdoc.AdditionalTestDocs
import org.sahagin.runlib.external.adapter.AdapterContainer
import org.sahagin.share.srctree.PageClass
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestField
import org.sahagin.share.srctree.TestFieldTable
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.srctree.TestMethodTable
import org.sahagin.share.srctree.code.Code
import org.sahagin.share.srctree.code.CodeLine
import org.sahagin.share.srctree.code.Field
import org.sahagin.share.srctree.code.StringCode
import org.sahagin.share.srctree.code.SubMethodInvoke
import org.sahagin.share.srctree.code.UnknownCode
import org.sahagin.share.srctree.code.VarAssign

class CollectCodeVisitor extends ClassCodeVisitorSupport {
    private TestClassTable rootClassTable
    private TestClassTable subClassTable
    private TestMethodTable rootMethodTable
    private TestMethodTable subMethodTable
    private TestFieldTable fieldTable
    private SrcTreeGeneratorUtils utils
    private SourceUnit srcUnit
    private CollectPhase phase

    CollectCodeVisitor(SourceUnit srcUnit,
        TestClassTable rootClassTable, TestClassTable subClassTable,
        TestMethodTable rootMethodTable, TestMethodTable subMethodTable,
        TestFieldTable fieldTable, SrcTreeGeneratorUtils utils, CollectPhase phase) {
        this.srcUnit = srcUnit
        this.rootClassTable = rootClassTable
        this.subClassTable = subClassTable
        this.rootMethodTable = rootMethodTable
        this.subMethodTable = subMethodTable
        this.fieldTable = fieldTable
        this.utils = utils
        this.phase = phase
    }

    TestClassTable getRootClassTable() {
        return rootClassTable
    }

    TestMethodTable getRootMethodTable() {
        return rootMethodTable
    }

    TestClassTable getSubClassTable() {
        return subClassTable
    }

    TestMethodTable getSubMethodTable() {
        return subMethodTable
    }

    TestFieldTable getFieldTable() {
        return fieldTable
    }

    SrcTreeGeneratorUtils getUtils() {
        return utils
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
                Parameter[] params = method.getParameters()
                boolean isVarLengthArg = false
                int nonVarLengthMaxIndex = params.length - 1
                ClassNode varLengthArgType = null
                if (params.length != 0) {
                    ClassNode lastArgType = params[params.length - 1].getType()
                    if (lastArgType.isArray()) {
                        // last array argument is handled as variable length argument
                        isVarLengthArg = true
                        nonVarLengthMaxIndex = params.length - 2
                        varLengthArgType = lastArgType.getComponentType()
                    }
                }

                if (nonVarLengthMaxIndex > argClasses.size() - 1) {
                    continue // go to next method
                }
                if (!isVarLengthArg && (params.length != argClasses.size())) {
                    // argument length must be the same
                    continue // go to next method
                }

                boolean matched = true

                for (int i = 0; i <= nonVarLengthMaxIndex; i++) {
                    if (!argClasses.get(i).isDerivedFrom(params[i].getType())) {
                        matched = false
                        break // quit argument type checking
                    }
                }
                if (!matched) {
                    continue // non variable length argument does not match
                }
                if (isVarLengthArg) {
                    for (int i = nonVarLengthMaxIndex + 1; i < argClasses.size(); i++) {
                        if (!argClasses.get(i).isDerivedFrom(varLengthArgType)) {
                            matched = false
                            break // quit argument type checking
                        }
                        if (!matched) {
                            continue // non variable length argument does not match
                        }
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

    // returns [TestMethod, MethodNode]
    private def getThisOrSuperMethodSubNoDelegate(ClassNode classNode,
            String methodAsString, List<ClassNode> argClasses) {
        MethodNode methodNode = getThisOrSuperMethodNode(classNode, methodAsString, argClasses)
        if (methodNode == null) {
            return [null, null]
        }
        TestMethod testMethod = SrcTreeGeneratorUtils.getTestMethod(methodNode, subMethodTable)
        return [testMethod, methodNode]
    }

    private ClassNode getDelegateToClassNode(ClassNode classNode) {
        TestClass testClass = SrcTreeGeneratorUtils.getTestClass(
            SrcTreeGeneratorUtils.getClassQualifiedName(classNode), rootClassTable, subClassTable)
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

    // returns [TestMethod, MethodNode]
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

    // returns [TestMethod, MethodNode]
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
    def generateMethodInvokeCode(MethodCall methodCall, ClassNode thisClassNode) {
        ASTNode receiver = methodCall.getReceiver()
        String methodAsString = methodCall.getMethodAsString()
        Expression arguments = methodCall.getArguments()
        String original = methodCall.getText()
        Code receiverCode
        ClassNode receiverClassNode
        if (receiver instanceof ClassNode) {
            // static method call
            receiverClassNode = receiver as ClassNode
            receiverCode = generateUnknownCode(
                receiverClassNode.getNameWithoutPackage(), receiverClassNode).first()
        } else if (receiver instanceof Expression) {
            (receiverCode, receiverClassNode) = generateExpressionCode(
                receiver as Expression, thisClassNode)
        } else {
            // TODO receiver may be null for constructor..
            return generateUnknownCode(original, ClassHelper.OBJECT_TYPE)
        }

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
            (argCode, argClass) = generateExpressionCode(argExpression, thisClassNode)
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
        assert receiverCode != null
        subMethodInvoke.setThisInstance(receiverCode)
        for (Code argCode : argCodes) {
            subMethodInvoke.addArg(argCode)
        }
        subMethodInvoke.setChildInvoke(
            SrcTreeGeneratorUtils.getClassQualifiedName(invocationMethodNode.getDeclaringClass()) !=
            SrcTreeGeneratorUtils.getClassQualifiedName(receiverClassNode))
        subMethodInvoke.setOriginal(original)
        return [subMethodInvoke, invocationMethodNode.getReturnType()]
    }

    // returns [VarAssing, ClassNode]
    def generateVarAssignCode(BinaryExpression binary, ClassNode thisClassNode) {
        Expression left = binary.getLeftExpression()
        Expression right = binary.getRightExpression()
        String original = binary.getText()
        Code rightCode
        ClassNode rightClass
        (rightCode, rightClass) = generateExpressionCode(right, thisClassNode)
        Code leftCode
        ClassNode leftClass
        (leftCode, leftClass) = generateExpressionCode(left, thisClassNode)

        String classKey = SrcTreeGeneratorUtils.getClassQualifiedName(leftClass)
        TestClass subClass = subClassTable.getByKey(classKey)
        // TODO geb specific logic
        if ((subClass != null && subClass instanceof PageClass) ||
            SrcTreeGeneratorUtils.inheritsFromClass(leftClass, "geb.Page")) {
            // ignore left for page type variable assignment
            // since usually page type variable is not used in other TestDoc
            return [rightCode, rightClass]
        }

        VarAssign assign = new VarAssign()
        assign.setOriginal(original)
        assign.setVariable(leftCode)
        assign.setValue(rightCode)
        return [assign, ClassHelper.VOID_TYPE]
    }

    def generateFieldCode(PropertyExpression property, ClassNode thisClassNode) {
        Expression receiver = property.getObjectExpression()
        Code receiverCode
        ClassNode receiverClass
        (receiverCode, receiverClass) = generateExpressionCode(receiver, thisClassNode)
        String fieldKey = SrcTreeGeneratorUtils.generateFieldKey(
            receiverClass, property.getPropertyAsString())
        TestField testField = fieldTable.getByKey(fieldKey)
        if (testField == null) {
            return generateUnknownCode(property)
        }
        Field field = new Field()
        field.setFieldKey(testField.getKey())
        field.setField(testField)
        field.setThisInstance(receiverCode)
        field.setOriginal(property.getText())
        return [field, property.getType()] // TODO maybe getType always returns Object type
    }

    // returns [UnknownCode, ClassNode]
    def generateUnknownCode(String original, ClassNode classNode) {
        UnknownCode unknownCode = new UnknownCode()
        unknownCode.setOriginal(original)
        return [unknownCode, classNode]
    }

    // returns [UnknownCode, ClassNode]
    def generateUnknownCode(Expression expression) {
        // TODO using getText is temporal logic
        return generateUnknownCode(expression.getText(), expression.getType())
    }

    // returns [Code, ClassNode]
    def generateExpressionCode(Expression expression, ClassNode thisClassNode) {
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
                    return generateVarAssignCode(binary, thisClassNode)
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
        } else if (expression instanceof PropertyExpression) {
            PropertyExpression property = expression as PropertyExpression
            return generateFieldCode(property, thisClassNode)
        } else if (expression instanceof MethodCallExpression) {
            // TODO TestStepLabel handling
            MethodCallExpression methodCall = expression as MethodCallExpression
            return generateMethodInvokeCode(methodCall, thisClassNode)
        } else if (expression instanceof StaticMethodCallExpression) {
            StaticMethodCallExpression methodCall = expression as StaticMethodCallExpression
            return generateMethodInvokeCode(methodCall, thisClassNode)
        } else if (expression instanceof ConstructorCallExpression) {
            ConstructorCallExpression constructorCall = expression as ConstructorCallExpression
            return generateMethodInvokeCode(constructorCall, thisClassNode)
        } else if ((expression instanceof VariableExpression) &&
            ((expression as VariableExpression).getName() == "this")) {
            // this keyword
            Code code = generateUnknownCode(expression).first()
            return [code, thisClassNode]
        } else if (expression instanceof ClassExpression) {
            ClassExpression classExp = expression as ClassExpression
            // TODO getting original text. this logic is not elegant logic..
            return generateUnknownCode(
                expression.getType().getNameWithoutPackage(),  ClassHelper.CLASS_Type)
        } else {
            // TODO local var, arg ref, etc
            return generateUnknownCode(expression)
        }
    }

    @Override
    void visitMethod(MethodNode node) {
        List<SrcTreeVisitorAdapter> listeners =
        GroovyAdapterContainer.globalInstance().getSrcTreeVisitorAdapters()
        if (phase == CollectPhase.BEFORE) {
            for (SrcTreeVisitorAdapter listener : listeners) {
                if (listener.beforeCollectCode(node, this)) {
                    break
                }
            }
            super.visitMethod(node)
            return
        } else if (phase == CollectPhase.AFTER) {
            for (SrcTreeVisitorAdapter listener : listeners) {
                if (listener.afterCollectCode(node, this)) {
                    break
                }
            }
            super.visitMethod(node)
            return
        } else {
            for (SrcTreeVisitorAdapter listener : listeners) {
                if (listener.collectCode(node, this)) {
                    super.visitMethod(node)
                    return
                }
            }
        }

        TestMethod testMethod
        if (SrcTreeGeneratorUtils.isRootMethod(node)) {
            testMethod = SrcTreeGeneratorUtils.getTestMethod(node, rootMethodTable)
        } else if (utils.isSubMethod(node)) {
            testMethod = SrcTreeGeneratorUtils.getTestMethod(node, subMethodTable)
        } else {
            super.visitMethod(node)
            return
        }

        if (!(node.getCode() instanceof BlockStatement)) {
            super.visitMethod(node)
            return
        }
        BlockStatement body = node.getCode() as BlockStatement
        if (body == null) {
            // no body. Maybe abstract method or interface method
            super.visitMethod(node)
            return
        }
        List<Statement> list = body.getStatements()
        for (Statement statement : list) {
            String lineText = srcUnit.getSource().getLine(statement.getLineNumber(), null)
            if (lineText == null) {
                // Maybe this statement is automatically generated by compiler.
                // Just ignore such statements
                continue
            }

            Code code
            if (statement instanceof ExpressionStatement) {
                Expression expression = (statement as ExpressionStatement).getExpression()
                code = generateExpressionCode(expression, node.getDeclaringClass()).first()
            } else {
                code = new UnknownCode()
            }
            CodeLine codeLine = new CodeLine()
            // TODO line number OK ?
            codeLine.setStartLine(statement.getLineNumber())
            codeLine.setEndLine(statement.getLastLineNumber())
            codeLine.setCode(code)
            // sometimes original value set by expressionCode method does not equal to
            // the one of statementNode
            // TODO temp
            //code.setOriginal(statement.getText())
            code.setOriginal(lineText.trim())
            testMethod.addCodeBody(codeLine)
        }
        super.visitMethod(node)
        return
    }
}
