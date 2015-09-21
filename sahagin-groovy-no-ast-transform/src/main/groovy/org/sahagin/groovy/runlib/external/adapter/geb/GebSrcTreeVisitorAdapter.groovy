package org.sahagin.groovy.runlib.external.adapter.geb

import java.util.List;

import org.sahagin.share.srctree.code.CodeLine;
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.FieldExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCall;
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.sahagin.groovy.runlib.external.adapter.AbstractSrcTreeVisitorAdapter
import org.sahagin.groovy.runlib.external.adapter.SrcTreeVisitorAdapter.MethodType
import org.sahagin.groovy.runlib.srctreegen.CollectCodeVisitor
import org.sahagin.groovy.runlib.srctreegen.CollectSubVisitor
import org.sahagin.groovy.runlib.srctreegen.SrcTreeGeneratorUtils
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
import org.sahagin.share.srctree.code.Field
import org.sahagin.share.srctree.code.SubMethodInvoke
import org.sahagin.share.srctree.code.UnknownCode

// This visitor collects all page contents and set them to fieldTable
class GebSrcTreeVisitorAdapter extends AbstractSrcTreeVisitorAdapter {
    // pair of field key and its value type ClassNode
    private Map<String, ClassNode> fieldValueClassMap = new HashMap<String, ClassNode>(128)
    private ClassNode currentPage = null
    
    // Searches static content initialization block from the specific static initializer method node.
    // Returns null if not found
    private BlockStatement getContentClosureBlock(MethodNode method) {
        if (!GroovyASTUtils.inheritsFromClass(method.declaringClass, "geb.Page")) {
            return null
        }
        // Search static initializer
        // since content DSL closure logic has been moved to static constructor part by Groovy compiler,
        if (!method.staticConstructor) {
            return null
        }
        if (!(method.code instanceof BlockStatement)) {
            return null
        }
        List<Statement> blockStatements = (method.code as BlockStatement).statements
        for (Statement blockStatement : blockStatements) {
            if (!(blockStatement instanceof ExpressionStatement)) {
                continue
            }
            Expression exp = (blockStatement as ExpressionStatement).expression
            if (!(exp instanceof BinaryExpression)) {
                continue
            }
            Expression left = (exp as BinaryExpression).leftExpression
            if (!(left instanceof FieldExpression)) {
                continue
            }
            FieldNode field = (left as FieldExpression).field
            if (!field.isStatic() || field.name != "content") {
                continue
            }
            // found "content" property initialization part
            Expression right = (exp as BinaryExpression).rightExpression
            if (!(right instanceof ClosureExpression)) {
                continue
            }
            Statement closureCode = (right as ClosureExpression).code
            if (!(closureCode instanceof BlockStatement)) {
                continue
            }
            return (closureCode as BlockStatement)
        }
        return null
    }

    // return [testDoc string, content value Expression].
    // return [null, null] if not found
    def getTestDocAndValueFromContent(MethodCallExpression methodCall) {
        if (!(methodCall.arguments instanceof ArgumentListExpression)) {
            return [null, null]
        }
        List<Expression> arguments =
                (methodCall.arguments as ArgumentListExpression).expressions
        // first argument is option map, second argument is content definition closure
        if (arguments.size() < 2) {
            return [null, null]
        }
        if (!(arguments.get(0) instanceof MapExpression)) {
            return [null, null]
        }
        List<MapEntryExpression> mapEntries = (arguments.get(0) as MapExpression).mapEntryExpressions
        if (!(arguments.get(1) instanceof ClosureExpression)) {
            return [null, null]
        }
        Statement closureCode = (arguments.get(1) as ClosureExpression).code
        if (!(closureCode instanceof BlockStatement)) {
            return [null, null]
        }
        List<Statement> closureStatements = (closureCode as BlockStatement).statements
        if (closureStatements.size() != 1) {
            return [null, null]
        }
        Expression valueExpression
        if (closureStatements.get(0) instanceof ExpressionStatement) {
            valueExpression = (closureStatements.get(0) as ExpressionStatement).expression
        } else if (closureStatements.get(0) instanceof ReturnStatement) {
            valueExpression = (closureStatements.get(0) as ReturnStatement).expression
        } else {
            return [null, null]
        }
        for (MapEntryExpression mapEntry : mapEntries) {
            if (!(mapEntry.keyExpression instanceof ConstantExpression)) {
                continue
            }
            String mapKey = (mapEntry.keyExpression as ConstantExpression).value.toString()
            if (mapKey != "testDoc") {
                continue
            }
            if (!(mapEntry.valueExpression instanceof ConstantExpression)) {
                // TODO throw more user friendly error
                throw new RuntimeException(
                "testDoc value must be constant: " + mapEntry.valueExpression)
            }
            String testDoc = (mapEntry.valueExpression as ConstantExpression).value.toString()
            return [testDoc, valueExpression]
        }
        return [null, null]
    }

    // collect all page object content values and types,
    // and set them to fieldValueClassMap before collecting other codes
    @Override
    boolean beforeCollectCode(MethodNode method, CollectCodeVisitor visitor) {
        BlockStatement contentClosureBlock = getContentClosureBlock(method)
        if (contentClosureBlock == null) {
            return false
        }

        List<Statement> list = contentClosureBlock.statements
        // iterate page object content definition
        for (Statement statement : list) {
            Expression expression
            if (statement instanceof ExpressionStatement) {
                expression = (statement as ExpressionStatement).expression
            } else if (statement instanceof ReturnStatement) {
                expression = (statement as ReturnStatement).expression
            } else {
                continue
            }
            if (!(expression instanceof MethodCallExpression)) {
                continue
            }
            MethodCallExpression methodCall = (expression as MethodCallExpression)
            String testDoc
            Expression fieldValue
            (testDoc, fieldValue) = getTestDocAndValueFromContent(methodCall)
            if (testDoc == null) {
                continue
            }
            TestField testField = visitor.fieldTable.getByKey(
                SrcTreeGeneratorUtils.generateFieldKey(method.declaringClass, methodCall.methodAsString))
            assert testField != null

            Code fieldValueCode
            ClassNode fieldValueClass
            (fieldValueCode, fieldValueClass) =
            visitor.generateExpressionCode(fieldValue, method)
            testField.value = fieldValueCode
            fieldValueClassMap[testField.key] = fieldValueClass
        }
        return true
    }
    
    // field value is not set by this method
    @Override
    boolean collectSubMethod(MethodNode method, MethodType type, CollectSubVisitor visitor) {
        BlockStatement contentClosureBlock = getContentClosureBlock(method)
        if (contentClosureBlock == null) {
            return false
        }

        List<Statement> list = contentClosureBlock.statements
        List<TestField> testFields = new ArrayList<TestField>(list.size())
        // iterate page object content definition
        for (Statement statement : list) {
            Expression expression
            if (statement instanceof ExpressionStatement) {
                expression = (statement as ExpressionStatement).expression
            } else if (statement instanceof ReturnStatement) {
                expression = (statement as ReturnStatement).expression
            } else {
                continue
            }
            if (!(expression instanceof MethodCallExpression)) {
                continue
            }
            MethodCallExpression methodCall = (expression as MethodCallExpression)
            String testDoc
            Expression fieldValue
            (testDoc, fieldValue) = getTestDocAndValueFromContent(methodCall)
            if (testDoc == null) {
                continue
            }
            TestField testField = new TestField()
            // each method name will become page object property
            testField.simpleName = methodCall.methodAsString
            testField.testDoc = testDoc
            testFields.add(testField)
        }

        if (testFields.size() == 0) {
            return true
        }

        ClassNode classNode = method.declaringClass
        String classQName = GroovyASTUtils.getClassQualifiedName(classNode)
        TestClass testClass = visitor.rootClassTable.getByKey(classQName)
        if (testClass == null) {
            testClass = visitor.subClassTable.getByKey(classQName)
            if (testClass == null) {
                testClass = visitor.utils.generateTestClass(classNode)
                visitor.subClassTable.addTestClass(testClass)
            }
        }

        for (TestField testField : testFields) {
            testField.testClassKey = testClass.key
            testField.testClass = testClass
            testField.key =
                    SrcTreeGeneratorUtils.generateFieldKey(classNode, testField.simpleName)
            visitor.fieldTable.addTestField(testField)
            testClass.addTestFieldKey(testField.key)
            testClass.addTestField(testField)
        }

        return true
    }
    
    // get current page from at, to, via method argument
    @Override
    boolean generatedMethodInvokeCode(Code code, ClassNode classNode, CollectCodeVisitor visitor) {
        if (!(code instanceof SubMethodInvoke)) {
            return false
        }
        SubMethodInvoke invoke = (SubMethodInvoke) code
        TestMethod subMethod = invoke.subMethod
        if (subMethod.qualifiedName == "geb.Browser.at" ||
            subMethod.qualifiedName == "geb.Browser.to" ||
            subMethod.qualifiedName == "geb.Browser.via") {
            if (invoke.args.size() == 0) {
                return false
            }            
            for (Code arg : invoke.args) {
                if (!(arg instanceof ClassInstance)) {
                    continue
                }
                // if argument class is page class, it must exist in targetWholeSrcUnits 
                // or must have been loaded by class loader
                String argClassQualifiedName = ((ClassInstance) arg).testClass.qualifiedName 
                ClassNode argClassNode = SrcTreeGeneratorUtils.searchClassNode(
                        visitor.targetWholeSrcUnits, argClassQualifiedName)
                if (argClassNode == null) {
                    try {
                        argClassNode = ClassHelper.make(Class.forName(
                                argClassQualifiedName, true, visitor.srcUnit.classLoader))
                    } catch (ClassNotFoundException e) {
                        continue
                    }
                }
                if (!GroovyASTUtils.inheritsFromClass(argClassNode, "geb.Page")) {
                    continue
                }
                currentPage = argClassNode
                return false
            }
            return false
        }
    }
    
    // ignore left for page type variable assignment
    @Override
    def generateVarAssignCode(BinaryExpression binary,
            MethodNode parentMethod, CollectCodeVisitor visitor) {
        Code leftCode
        ClassNode leftClass
        (leftCode, leftClass) = visitor.generateSetterExpressionCode(binary.leftExpression, parentMethod)

        String classKey = GroovyASTUtils.getClassQualifiedName(leftClass)
        TestClass subClass = visitor.subClassTable.getByKey(classKey)
        if ((subClass != null && subClass instanceof PageClass) ||
        GroovyASTUtils.inheritsFromClass(leftClass, "geb.Page")) {
            // ignore left for page type variable assignment
            // since usually page type variable is not used in other TestDoc
            return  visitor.generateExpressionCode(binary.rightExpression, parentMethod)
        }
        return [null, null]
    }

    // search field from fieldValueClassMap
    @Override
    def generateFieldCode(String fieldName, ClassNode fieldOwnerType,
            Code receiverCode, String original, CollectCodeVisitor visitor) {
        TestField testField
        FieldNode fieldNode
        (testField, fieldNode) = visitor.getThisOrSuperTestField(fieldOwnerType, fieldName)
        // If fieldNode is found, do nothing and use it.
        // If fieldNode is not found and fieldValueClass is found, use it
        if (fieldNode == null && testField != null) {
            ClassNode fieldValueClass = fieldValueClassMap[testField.key]
            if (fieldValueClass != null) {
                Field field = new Field()
                field.fieldKey = testField.key
                field.field = testField
                field.thisInstance = receiverCode
                field.original = original
                return [field, fieldValueClass]
            }
        }
        return [null, null]
    }

    // handle current pgae delegation
    @Override
    ClassNode getDelegateToClassNode(ClassNode classNode, CollectCodeVisitor visitor) {
        if (classNode.name == "geb.Browser") {
            if (currentPage == null) {
                // must use the class loader used in srcUnit
                return ClassHelper.make(Class.forName("geb.Page", true, visitor.srcUnit.classLoader))
            } else {
                return currentPage
            }
        } else {
            return null
        }
    }

}