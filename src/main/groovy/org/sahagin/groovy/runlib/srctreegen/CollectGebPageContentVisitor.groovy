package org.sahagin.groovy.runlib.srctreegen

import org.apache.commons.lang3.tuple.Pair
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.FieldExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestField
import org.sahagin.share.srctree.TestFieldTable
import org.sahagin.share.srctree.TestMethodTable

// Geb specific visitor. This visitor collects all page contents and set them to fieldTable
// TODO move this class to test framework specific logic directory
class CollectGebPageContentVisitor extends ClassCodeVisitorSupport {
    private TestClassTable subClassTable
    private TestClassTable rootClassTable
    private TestFieldTable fieldTable
    private SrcTreeGeneratorUtils utils

    CollectGebPageContentVisitor(TestClassTable rootClassTable,
        TestClassTable subClassTable, TestFieldTable fieldTable,
        SrcTreeGeneratorUtils utils) {
        this.rootClassTable = rootClassTable
        this.subClassTable = subClassTable
        this.fieldTable = fieldTable
        this.utils = utils
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return null // dummy
    }

    private boolean inheritsFromGebPage(ClassNode classNode) {
        ClassNode parentNode = classNode
        while (parentNode != null) {
            if (parentNode.getName() == "geb.Page") {
                return true
            }
            parentNode = parentNode.getSuperClass()
        }
        return false
    }

    boolean needsVisit(ClassNode classNode) {
        return inheritsFromGebPage(classNode)
    }

    // return null if not found
    private String getTestDocFromContentMethodCall(MethodCallExpression methodCall) {
        if (!(methodCall.getArguments() instanceof ArgumentListExpression)) {
            return null
        }
        List<Expression> arguments = (methodCall.getArguments() as ArgumentListExpression).getExpressions()
        // first argument is option map, second argument is content definition closure
        if (arguments.size() < 2) {
            return null
        }
        if (!(arguments.get(0) instanceof MapExpression)) {
            return null
        }
        List<MapEntryExpression> mapEntries = (arguments.get(0) as MapExpression).getMapEntryExpressions()
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
                throw new RuntimeException("testDoc value must be constant: " + mapEntry.getValueExpression())
            }
            return (mapEntry.getValueExpression() as ConstantExpression).getValue().toString()
        }
        return null
    }

    @Override
    void visitMethod(MethodNode method) {
        // Search static initializer
        // since content DSL closure logic has been moved to static constructor part by Groovy compiler,
        if (!method.staticConstructor) {
            super.visitMethod(method)
            return
        }
        if (!(method.getCode() instanceof BlockStatement)) {
            super.visitMethod(method)
            return
        }
        List<Statement> blockStatements = (method.getCode() as BlockStatement).getStatements()
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
            List<Statement> list = (closureCode as BlockStatement).getStatements()
            List<Pair<String, String>> fieldNameAndTestDocs = new ArrayList<Pair<String, String>>(list.size())
            // iterate page object content definition
            for (Statement statement : list) {
                if (!(statement instanceof ExpressionStatement)) {
                    continue
                }
                Expression expression = (statement as ExpressionStatement).getExpression()
                if (!(expression instanceof MethodCallExpression)) {
                    continue
                }
                MethodCallExpression methodCall = (expression as MethodCallExpression)
                String testDoc = getTestDocFromContentMethodCall(methodCall)
                if (testDoc == null) {
                    continue
                }
                // the property for each method name will be defined on the page object
                String fieldName = methodCall.getMethodAsString()
                fieldNameAndTestDocs.add(Pair.of(fieldName, testDoc))
            }

            if (fieldNameAndTestDocs.size() == 0) {
                continue
            }

            ClassNode classNode = method.getDeclaringClass()
            String classQName = SrcTreeGeneratorUtils.getClassQualifiedName(classNode)
            TestClass testClass = rootClassTable.getByKey(classQName)
            if (testClass == null) {
                testClass = subClassTable.getByKey(classQName)
                if (testClass == null) {
                    testClass = utils.generateTestClass(classNode)
                    subClassTable.addTestClass(testClass)
                }
            }

            for (Pair<String, String> pair : fieldNameAndTestDocs) {
                TestField testField = new TestField()
                testField.setTestClassKey(testClass.getKey())
                testField.setTestClass(testClass)
                testField.setKey(testClass.getKey() + "." + pair.getLeft())
                testField.setSimpleName(pair.getLeft())
                testField.setTestDoc(pair.getRight())
                testField.setValue(null) // TODO not supported now

                fieldTable.addTestField(testField)
                testClass.addTestFieldKey(testField.getKey())
                testClass.addTestField(testField)
            }
        }

        super.visitMethod(method)
    }

}


