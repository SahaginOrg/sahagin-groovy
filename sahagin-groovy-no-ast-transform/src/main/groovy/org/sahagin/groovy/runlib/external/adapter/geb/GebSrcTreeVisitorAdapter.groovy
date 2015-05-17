package org.sahagin.groovy.runlib.external.adapter.geb

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
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestField
import org.sahagin.share.srctree.TestFieldTable
import org.sahagin.share.srctree.TestMethodTable
import org.sahagin.share.srctree.code.Code
import org.sahagin.share.srctree.code.UnknownCode

// This visitor collects all page contents and set them to fieldTable
class GebSrcTreeVisitorAdapter extends AbstractSrcTreeVisitorAdapter {

    // Searches static content initialization block from the specific static initializer method node.
    // Returns null if not found
    private BlockStatement getContentClosureBlock(MethodNode node) {
        if (!GroovyASTUtils.inheritsFromClass(node.getDeclaringClass(), "geb.Page")) {
            return null
        }
        // Search static initializer
        // since content DSL closure logic has been moved to static constructor part by Groovy compiler,
        if (!node.staticConstructor) {
            return null
        }
        if (!(node.getCode() instanceof BlockStatement)) {
            return null
        }
        List<Statement> blockStatements = (node.getCode() as BlockStatement).getStatements()
        for (Statement blockStatement : blockStatements) {
            if (!(blockStatement instanceof ExpressionStatement)) {
                continue
            }
            Expression exp = (blockStatement as ExpressionStatement).getExpression()
            if (!(exp instanceof BinaryExpression)) {
                continue
            }
            Expression left = (exp as BinaryExpression).getLeftExpression()
            if (!(left instanceof FieldExpression)) {
                continue
            }
            FieldNode field = (left as FieldExpression).getField()
            if (!field.isStatic() || field.getName() != "content") {
                continue
            }
            // found "content" property initialization part
            Expression right = (exp as BinaryExpression).getRightExpression()
            if (!(right instanceof ClosureExpression)) {
                continue
            }
            Statement closureCode = (right as ClosureExpression).getCode()
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
        if (!(methodCall.getArguments() instanceof ArgumentListExpression)) {
            return [null, null]
        }
        List<Expression> arguments =
                (methodCall.getArguments() as ArgumentListExpression).getExpressions()
        // first argument is option map, second argument is content definition closure
        if (arguments.size() < 2) {
            return [null, null]
        }
        if (!(arguments.get(0) instanceof MapExpression)) {
            return [null, null]
        }
        List<MapEntryExpression> mapEntries = (arguments.get(0) as MapExpression).getMapEntryExpressions()
        if (!(arguments.get(1) instanceof ClosureExpression)) {
            return [null, null]
        }
        Statement closureCode = (arguments.get(1) as ClosureExpression).getCode()
        if (!(closureCode instanceof BlockStatement)) {
            return [null, null]
        }
        List<Statement> closureStatements = (closureCode as BlockStatement).getStatements()
        if (closureStatements.size() != 1) {
            return [null, null]
        }
        Expression valueExpression
        if (closureStatements.get(0) instanceof ExpressionStatement) {
            valueExpression = (closureStatements.get(0) as ExpressionStatement).getExpression()
        } else if (closureStatements.get(0) instanceof ReturnStatement) {
            valueExpression = (closureStatements.get(0) as ReturnStatement).getExpression()
        } else {
            return [null, null]
        }
        for (MapEntryExpression mapEntry : mapEntries) {
            if (!(mapEntry.getKeyExpression() instanceof ConstantExpression)) {
                continue
            }
            String mapKey = (mapEntry.getKeyExpression() as ConstantExpression).getValue().toString()
            if (mapKey != "testDoc") {
                continue
            }
            if (!(mapEntry.getValueExpression() instanceof ConstantExpression)) {
                // TODO throw more user friendly error
                throw new RuntimeException(
                "testDoc value must be constant: " + mapEntry.getValueExpression())
            }
            String testDoc = (mapEntry.getValueExpression() as ConstantExpression).getValue().toString()
            return [testDoc, valueExpression]
        }
        return [null, null]
    }

    // collect all page object content values and types before collecting other codes
    @Override
    boolean beforeCollectCode(MethodNode node, CollectCodeVisitor visitor) {
        BlockStatement contentClosureBlock = getContentClosureBlock(node)
        if (contentClosureBlock == null) {
            return false
        }

        List<Statement> list = contentClosureBlock.getStatements()
        // iterate page object content definition
        for (Statement statement : list) {
            Expression expression
            if (statement instanceof ExpressionStatement) {
                expression = (statement as ExpressionStatement).getExpression()
            } else if (statement instanceof ReturnStatement) {
                expression = (statement as ReturnStatement).getExpression()
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
            TestField testField = visitor.getFieldTable().getByKey(SrcTreeGeneratorUtils.generateFieldKey(
                    node.getDeclaringClass(), methodCall.getMethodAsString()))
            assert testField != null

            Code fieldValueCode
            ClassNode fieldValueClass
            (fieldValueCode, fieldValueClass) =
            visitor.generateExpressionCode(fieldValue, node)
            // TODO maybe memo concept can be used in many place
            fieldValueCode.setRawASTTypeMemo(fieldValueClass)
            testField.setValue(fieldValueCode)
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

        List<Statement> list = contentClosureBlock.getStatements()
        List<TestField> testFields = new ArrayList<TestField>(list.size())
        // iterate page object content definition
        for (Statement statement : list) {
            Expression expression
            if (statement instanceof ExpressionStatement) {
                expression = (statement as ExpressionStatement).getExpression()
            } else if (statement instanceof ReturnStatement) {
                expression = (statement as ReturnStatement).getExpression()
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
            testField.setSimpleName(methodCall.getMethodAsString())
            testField.setTestDoc(testDoc)
            testFields.add(testField)
        }

        if (testFields.size() == 0) {
            return true
        }

        ClassNode classNode = method.getDeclaringClass()
        String classQName = GroovyASTUtils.getClassQualifiedName(classNode)
        TestClass testClass = visitor.getRootClassTable().getByKey(classQName)
        if (testClass == null) {
            testClass = visitor.getSubClassTable().getByKey(classQName)
            if (testClass == null) {
                testClass = visitor.getUtils().generateTestClass(classNode)
                visitor.getSubClassTable().addTestClass(testClass)
            }
        }

        for (TestField testField : testFields) {
            testField.setTestClassKey(testClass.getKey())
            testField.setTestClass(testClass)
            testField.setKey(
                    SrcTreeGeneratorUtils.generateFieldKey(classNode, testField.getSimpleName()))
            visitor.getFieldTable().addTestField(testField)
            testClass.addTestFieldKey(testField.getKey())
            testClass.addTestField(testField)
        }

        return true
    }

}