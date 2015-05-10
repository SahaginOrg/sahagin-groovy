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
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestField
import org.sahagin.share.srctree.TestFieldTable
import org.sahagin.share.srctree.TestMethodTable
import org.sahagin.share.srctree.code.UnknownCode

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

    boolean needsVisit(ClassNode classNode) {
        return SrcTreeGeneratorUtils.inheritsFromClass(classNode, "geb.Page")
    }

    // return [testDoc string, content value Expression].
    // return [null, null] if not found
    def getTestDocAndValueFromContent(MethodCallExpression methodCall) {
        if (!(methodCall.getArguments() instanceof ArgumentListExpression)) {
            return [null, null]
        }
        List<Expression> arguments = (methodCall.getArguments() as ArgumentListExpression).getExpressions()
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
                throw new RuntimeException("testDoc value must be constant: " + mapEntry.getValueExpression())
            }
            String testDoc = (mapEntry.getValueExpression() as ConstantExpression).getValue().toString()
            return [testDoc, valueExpression]
        }
        return [null, null]
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
            List<TestField> testFields = new ArrayList<TestField>(list.size())
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
                // TODO temporal code.. file value is not always Unknown code..
                UnknownCode fieldValueCode = new UnknownCode()
                fieldValueCode.setOriginal(fieldValue.getText())
                testField.setValue(fieldValueCode)
                testFields.add(testField)
            }

            if (testFields.size() == 0) {
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

            for (TestField testField : testFields) {
                testField.setTestClassKey(testClass.getKey())
                testField.setTestClass(testClass)
                testField.setKey(testClass.getKey() + "." + testField.getSimpleName())

                fieldTable.addTestField(testField)
                testClass.addTestFieldKey(testField.getKey())
                testClass.addTestField(testField)
            }
        }

        super.visitMethod(method)
    }

}


