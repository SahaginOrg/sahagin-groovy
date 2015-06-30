package org.sahagin.groovy.runlib.srctreegen

import static org.sahagin.runlib.external.adapter.javasystem.JavaSystemAdditionalTestDocsAdapter.*

import org.eclipse.jdt.core.dom.InfixExpression
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.srctree.code.Code
import org.sahagin.share.srctree.code.SubMethodInvoke
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.DynamicVariable
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
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
import org.codehaus.groovy.ast.stmt.AssertStatement
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit
import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.groovy.runlib.external.adapter.SrcTreeVisitorAdapter
import org.sahagin.groovy.runlib.external.adapter.SrcTreeVisitorAdapter.CollectPhase
import org.sahagin.groovy.runlib.external.adapter.SrcTreeVisitorAdapter.MethodType
import org.sahagin.groovy.share.GroovyASTUtils
import org.sahagin.share.srctree.PageClass
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestField
import org.sahagin.share.srctree.TestFieldTable
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.srctree.TestMethodTable
import org.sahagin.share.srctree.code.ClassInstance
import org.sahagin.share.srctree.code.Code
import org.sahagin.share.srctree.code.CodeLine
import org.sahagin.share.srctree.code.Field
import org.sahagin.share.srctree.code.MethodArgument
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

    SourceUnit getSrcUnit() {
        return srcUnit
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
                Parameter[] params = method.parameters
                boolean isVarLengthArg = false
                int nonVarLengthMaxIndex = params.length - 1
                ClassNode varLengthArgType = null
                if (params.length != 0) {
                    ClassNode lastArgType = params[params.length - 1].type
                    if (lastArgType.isArray()) {
                        // last array argument is handled as variable length argument
                        isVarLengthArg = true
                        nonVarLengthMaxIndex = params.length - 2
                        varLengthArgType = lastArgType.componentType
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
                    if (!argClasses.get(i).isDerivedFrom(params[i].type)) {
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
            parentNode = parentNode.superClass
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
            GroovyASTUtils.getClassQualifiedName(classNode), rootClassTable, subClassTable)
        if (testClass == null) {
            return null
        }
        if (testClass.delegateToTestClass == null) {
            return null
        }
        String delegateToClassQualifiedName = testClass.delegateToTestClass.qualifiedName
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
            parentNode = parentNode.superClass
        }
        return [null, null]
    }

    // returns [TestMethod, MethodNode]
    private def getThisOrSuperTestMethod(ClassNode classNode,
            String methodAsString, List<ClassNode> argClasses) {
        // check this class and super class
        TestMethod invocationMethodNoDelegate
        MethodNode invocationMethodNodeNoDelegate
        (invocationMethodNoDelegate, invocationMethodNodeNoDelegate) = getThisOrSuperMethodSub(
            classNode, methodAsString, argClasses, false)
        if (invocationMethodNoDelegate != null) {
            return [invocationMethodNoDelegate, invocationMethodNodeNoDelegate]
        }

        // check only delegateTo class for this class and super class
        TestMethod invocationMethod
        MethodNode invocationMethodNode
        (invocationMethod, invocationMethodNode) = getThisOrSuperMethodSub(
            classNode, methodAsString, argClasses, true)
        if (invocationMethod != null) {
            return [invocationMethod, invocationMethodNode]
        }
        
        // TestMethod is not found, but at least MethodNode is found
        if (invocationMethodNodeNoDelegate != null) {
            return [invocationMethodNoDelegate, invocationMethodNodeNoDelegate]
        }
        if (invocationMethodNode != null) {
            return [invocationMethod, invocationMethodNode]
        }

        return [null, null]
    }

    // return [Code, ClassNode]
    def generateMethodInvokeCode(MethodCall methodCall, MethodNode parentMethod) {
        ASTNode receiver = methodCall.receiver
        String methodAsString = methodCall.methodAsString
        Expression arguments = methodCall.arguments
        String original = methodCall.text
        Code receiverCode
        ClassNode receiverClassNode
        if (receiver instanceof ClassNode) {
            // static method call
            receiverClassNode = receiver as ClassNode
            receiverCode = generateUnknownCode(
                receiverClassNode.nameWithoutPackage, receiverClassNode).first()
        } else if (receiver instanceof Expression) {
            (receiverCode, receiverClassNode) = generateExpressionCode(
                receiver as Expression, parentMethod)
        } else {
            // TODO receiver may be null for constructor..
            return generateUnknownCode(original, ClassHelper.OBJECT_TYPE)
        }

        if (methodAsString == null || methodAsString == "") {
            return generateUnknownCode(original, ClassHelper.OBJECT_TYPE)
        }

        if (!(arguments instanceof TupleExpression)) {
            return generateUnknownCode(original, ClassHelper.OBJECT_TYPE)
        }
        TupleExpression argumentList = arguments as TupleExpression
        List<Code> argCodes = new ArrayList<Code>(argumentList.expressions.size())
        List<ClassNode> argClasses = new ArrayList<ClassNode>(argumentList.expressions.size())
        for (Expression argExpression : argumentList.expressions) {
            Code argCode
            ClassNode argClass
            (argCode, argClass) = generateExpressionCode(argExpression, parentMethod)
            argCodes.add(argCode)
            argClasses.add(argClass)
        }

        TestMethod invocationMethod
        MethodNode invocationMethodNode
        (invocationMethod, invocationMethodNode) =
                getThisOrSuperTestMethod(receiverClassNode, methodAsString, argClasses)
        if (invocationMethod == null || invocationMethodNode == null) {
            if (invocationMethodNode == null) {
                return generateUnknownCode(original, ClassHelper.OBJECT_TYPE)
            } else {
                return generateUnknownCode(original, invocationMethodNode.returnType)
            }
        }

        SubMethodInvoke subMethodInvoke = new SubMethodInvoke()
        subMethodInvoke.subMethodKey = invocationMethod.key
        subMethodInvoke.subMethod = invocationMethod
        // TODO null thisInstance especially for constructor
        assert receiverCode != null
        subMethodInvoke.thisInstance = receiverCode
        for (Code argCode : argCodes) {
            subMethodInvoke.addArg(argCode)
        }
        subMethodInvoke.childInvoke =
                (GroovyASTUtils.getClassQualifiedName(invocationMethodNode.declaringClass) !=
                GroovyASTUtils.getClassQualifiedName(receiverClassNode))
        subMethodInvoke.original = original
        return [subMethodInvoke, invocationMethodNode.returnType]
    }

    // returns [Code, ClassNode]
    def generateVarAssignCode(BinaryExpression binary, MethodNode parentMethod) {
        Expression left = binary.leftExpression
        Expression right = binary.rightExpression
        String original = binary.text
        Code rightCode
        ClassNode rightClass
        (rightCode, rightClass) = generateExpressionCode(right, parentMethod)
        Code leftCode
        ClassNode leftClass
        (leftCode, leftClass) = generateExpressionCode(left, parentMethod)

        String classKey = GroovyASTUtils.getClassQualifiedName(leftClass)
        TestClass subClass = subClassTable.getByKey(classKey)
        // TODO geb specific logic
        if ((subClass != null && subClass instanceof PageClass) ||
            GroovyASTUtils.inheritsFromClass(leftClass, "geb.Page")) {
            // ignore left for page type variable assignment
            // since usually page type variable is not used in other TestDoc
            return [rightCode, rightClass]
        }

        VarAssign assign = new VarAssign()
        assign.original = original
        assign.variable = leftCode
        assign.value = rightCode
        return [assign, ClassHelper.VOID_TYPE]
    }

    // returns [Code, ClassNode]
    private def generateFieldCodeSub(String fieldName,
        ClassNode fieldOwnerType, Code receiverCode, String original, ClassNode fieldVarType) {
        String fieldKey = SrcTreeGeneratorUtils.generateFieldKey(fieldOwnerType, fieldName)
        TestField testField = fieldTable.getByKey(fieldKey)
        if (testField == null) {
            return generateUnknownCode(original, fieldVarType)
        }
        Field field = new Field()
        field.fieldKey = testField.key
        field.field = testField
        field.thisInstance = receiverCode
        field.original = original
        ClassNode fieldType
        if (testField.value != null && testField.value.rawASTTypeMemo != null) {
            // TODO maybe memo concept can be used in many place
            fieldType = testField.value.rawASTTypeMemo as ClassNode
        } else {
            fieldType = fieldVarType
        }
        return [field, fieldType]
    }

    // returns [Code, ClassNode]
    def generateFieldCode(PropertyExpression property, MethodNode parentMethod) {
        Expression receiver = property.objectExpression
        Code receiverCode
        ClassNode receiverClass
        (receiverCode, receiverClass) = generateExpressionCode(receiver, parentMethod)
        return generateFieldCodeSub(property.propertyAsString,
            receiverClass, receiverCode, property.text, property.type)
    }

    // returns [Code, ClassNode]
    def generateClassInstanceCode(ClassExpression classExp) {
        TestClass testClass = SrcTreeGeneratorUtils.getTestClass(
                GroovyASTUtils.getClassQualifiedName(classExp.type),
                rootClassTable, subClassTable)
        if (testClass != null) {
            ClassInstance classInstance = new ClassInstance()
            classInstance.testClassKey = testClass.key
            classInstance.testClass = testClass
            classInstance.original = classExp.text
            return [classInstance, ClassHelper.CLASS_Type]
        } else {
            return generateUnknownCode(
                    classExp.type.nameWithoutPackage, ClassHelper.CLASS_Type)
        }
    }
    
    // returns [Code, ClassNode]
    private def generateAssertMethodInvokeCode(
        Expression expression, String original, MethodNode parentMethod) {
        String assertMethodKey = TestMethod.generateMethodKey(CLASS_QUALIFIED_NAME, METHOD_ASSERT)
        TestMethod assertMethod = subMethodTable.getByKey(assertMethodKey)
        assert assertMethod != null
        SubMethodInvoke assertMethodInvoke = new SubMethodInvoke()
        assertMethodInvoke.setSubMethodKey(assertMethodKey)
        assertMethodInvoke.setSubMethod(assertMethod)
        assertMethodInvoke.addArg(generateExpressionCode(expression, parentMethod).first())
        assertMethodInvoke.setOriginal(original)
        return [assertMethodInvoke, ClassHelper.VOID_TYPE]
    }
        
    private def generateBinaryExpMethodInvokeCode(
        BinaryExpression binaryExp, MethodNode parentMethod) {
        String operationMethodKey
        ClassNode classNode;
        if (binaryExp.operation.text == '==') {
            operationMethodKey = TestMethod.generateMethodKey(CLASS_QUALIFIED_NAME, METHOD_EQUALS)
            classNode = ClassHelper.boolean_TYPE
        } else if (binaryExp.operation.text == '!=') {
            operationMethodKey = TestMethod.generateMethodKey(CLASS_QUALIFIED_NAME, METHOD_NOT_EQUALS)
            classNode = ClassHelper.boolean_TYPE
        } else {
            return generateUnknownCode(binaryExp)
        }

        TestMethod operationMethod = subMethodTable.getByKey(operationMethodKey)
        assert operationMethod != null
        SubMethodInvoke operationMethodInvoke = new SubMethodInvoke()
        operationMethodInvoke.setSubMethodKey(operationMethodKey)
        operationMethodInvoke.setSubMethod(operationMethod)
        Code leftCode = generateExpressionCode(binaryExp.leftExpression, parentMethod).first()
        Code rightcode = generateExpressionCode(binaryExp.rightExpression, parentMethod).first()
        operationMethodInvoke.addArg(leftCode)
        operationMethodInvoke.addArg(rightcode)
        operationMethodInvoke.setOriginal(binaryExp.text)
        return [operationMethodInvoke, classNode]
    }
    
    // returns [UnknownCode, ClassNode]
    def generateUnknownCode(String original, ClassNode classNode) {
        UnknownCode unknownCode = new UnknownCode()
        unknownCode.original = original
        return [unknownCode, classNode]
    }

    // returns [UnknownCode, ClassNode]
    def generateUnknownCode(Expression expression) {
        // TODO using text property is temporal logic
        return generateUnknownCode(expression.text, expression.type)
    }

    // returns [Code, ClassNode]
    def generateExpressionCode(Expression expression, MethodNode parentMethod) {
        if (expression == null) {
            StringCode strCode = new StringCode()
            strCode.value = null
            strCode.original = "null"
            return [strCode, ClassHelper.OBJECT_TYPE]
        // TODO handle local variable assignment
        } else if (expression instanceof BinaryExpression) {
            BinaryExpression binary = expression as BinaryExpression
            if (binary instanceof DeclarationExpression
                || binary.operation.text == "=") {
                // variable declaration or assignment
                if (binary.rightExpression == null
                    || binary.rightExpression instanceof EmptyExpression) {
                    return generateUnknownCode(expression)
                } else {
                    return generateVarAssignCode(binary, parentMethod)
                }
            } else {
                return generateBinaryExpMethodInvokeCode(binary, parentMethod)
            }
        } else if (expression instanceof ConstantExpression) {
            ConstantExpression constant = expression as ConstantExpression
            Object value = constant.value
            if (value instanceof String) {
                StringCode strCode = new StringCode()
                strCode.value = value as String
                strCode.original = expression.text
                return [strCode, ClassHelper.STRING_TYPE]
            } else {
                return generateUnknownCode(expression)
            }
        } else if (expression instanceof PropertyExpression) {
            PropertyExpression property = expression as PropertyExpression
            return generateFieldCode(property, parentMethod)
        } else if (expression instanceof MethodCallExpression) {
            MethodCallExpression methodCall = expression as MethodCallExpression
            return generateMethodInvokeCode(methodCall, parentMethod)
        } else if (expression instanceof StaticMethodCallExpression) {
            StaticMethodCallExpression methodCall = expression as StaticMethodCallExpression
            return generateMethodInvokeCode(methodCall, parentMethod)
        } else if (expression instanceof ConstructorCallExpression) {
            ConstructorCallExpression constructorCall = expression as ConstructorCallExpression
            return generateMethodInvokeCode(constructorCall, parentMethod)
        } else if (expression instanceof VariableExpression) {
            VariableExpression variable = expression as VariableExpression
            if (variable.name == "this") {
                // this keyword
                Code code = generateUnknownCode(expression).first()
                return [code, parentMethod.declaringClass]
            } else if (variable.accessedVariable instanceof Parameter) {
                Parameter param = variable.accessedVariable as Parameter
                int index = -1
                for (int i = 0; i < parentMethod.parameters.length; i++) {
                    if (parentMethod.parameters[i].name == param.name) {
                        index = i
                        break
                    }
                }
                if (index == -1) {
                    return generateUnknownCode(expression)
                }
                MethodArgument methodArg = new MethodArgument()
                methodArg.argIndex = index
                methodArg.original = expression.text
                return [methodArg, parentMethod.parameters[index].type]
            } else if (variable.accessedVariable instanceof DynamicVariable) {
                // this may be dynamically defined property (such as Geb page object contents),
                // so try to find testField for this class
                DynamicVariable dynamicVar = variable.accessedVariable as DynamicVariable
                return generateFieldCodeSub(dynamicVar.name,
                    parentMethod.declaringClass, null, expression.text, dynamicVar.type)
            } else {
                return generateUnknownCode(expression)
            }
        } else if (expression instanceof ClassExpression) {
            ClassExpression classExp = expression as ClassExpression
            return generateClassInstanceCode(classExp)
        } else {
            return generateUnknownCode(expression)
        }
    }

    // returns [CodeLine, ClassNode]  
    // returns [null, null] if there is no corresponding codeLine
    def generateCodeLine(Statement statement, Code code, ClassNode classNode) {
        String lineText = srcUnit.source.getLine(statement.lineNumber, null)
        if (lineText == null) {
            // Maybe this statement is automatically generated by compiler.
            // Just ignore such statements
            return [null, null]
        }
        
        CodeLine codeLine = new CodeLine()
        // TODO line number OK ?
        codeLine.startLine = statement.lineNumber
        codeLine.endLine = statement.lastLineNumber
        codeLine.code = code
        // sometimes original value set by expressionCode method does not equal to
        // the one of statementNode
        // TODO temp
        code.original = lineText.trim()
        return [codeLine, classNode]
    }
    
    // returns [CodeLine, ClassNode]
    // returns [null, null] if there is no corresponding codeLine
    def generateCodeLine(Statement statement, MethodNode method) {
        String lineText = srcUnit.source.getLine(statement.lineNumber, null)
        if (lineText == null) {
            // Maybe this statement is automatically generated by compiler.
            // Just ignore such statements
            return [null, null]
        }

        Code code
        ClassNode classNode
        if (statement instanceof ExpressionStatement) {
            Expression expression = (statement as ExpressionStatement).expression
            (code, classNode) = generateExpressionCode(expression, method)
        } else if (statement instanceof ReturnStatement) {
            Expression expression = (statement as ReturnStatement).expression
            (code, classNode) = generateExpressionCode(expression, method)
        } else if (statement instanceof AssertStatement) {
            (code, classNode) = generateAssertMethodInvokeCode(
                (statement as AssertStatement).booleanExpression.expression, lineText.trim(), method)
        } else {
            code = new UnknownCode()
            classNode = ClassHelper.VOID_TYPE // dummy
        }
        
        return generateCodeLine(statement, code, classNode)
    }

    @Override
    void visitMethod(MethodNode node) {
        List<SrcTreeVisitorAdapter> listeners =
        GroovyAdapterContainer.globalInstance().srcTreeVisitorAdapters
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
        }

        MethodType methodType
        if (SrcTreeGeneratorUtils.isRootMethod(node)) {
            methodType = MethodType.ROOT
        } else if (utils.isSubMethod(node)) {
            methodType = MethodType.SUB
        } else {
            methodType = MethodType.NONE
        }

        for (SrcTreeVisitorAdapter listener : listeners) {
            if (listener.collectCode(node, methodType, this)) {
                super.visitMethod(node)
                return
            }
        }

        TestMethod testMethod
        if (methodType == MethodType.ROOT) {
            testMethod = SrcTreeGeneratorUtils.getTestMethod(node, rootMethodTable)
        } else if (methodType == MethodType.SUB) {
            testMethod = SrcTreeGeneratorUtils.getTestMethod(node, subMethodTable)
        } else {
            super.visitMethod(node)
            return
        }

        if (node.code == null || !(node.code instanceof BlockStatement)) {
            // no body means maybe abstract method or interface method
            super.visitMethod(node)
            return
        }

        for (Statement statement : (node.code as BlockStatement).statements) {
            List<CodeLine> codeLines = null
            for (SrcTreeVisitorAdapter listener : listeners) {
                codeLines = listener.collectMethodStatementCode(statement, node, methodType, this)
                if (codeLines != null && codeLines.size() > 0) {
                    break
                }
            }
            if (codeLines == null || codeLines.size() == 0) {
                CodeLine codeLine =  generateCodeLine(statement, node).first()
                if (codeLine == null) {
                    continue
                }
                codeLines = [codeLine]
            }
            testMethod.addAllCodeBodies(codeLines)
        }
        super.visitMethod(node)
        return
    }

}
