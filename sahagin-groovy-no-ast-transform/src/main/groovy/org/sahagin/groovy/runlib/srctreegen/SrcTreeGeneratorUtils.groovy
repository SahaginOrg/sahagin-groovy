package org.sahagin.groovy.runlib.srctreegen

import java.util.List

import org.apache.bcel.generic.RETURN
import org.apache.commons.lang3.tuple.Pair
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.groovy.share.GroovyASTUtils
import org.sahagin.runlib.additionaltestdoc.AdditionalMethodTestDoc
import org.sahagin.runlib.additionaltestdoc.AdditionalPage
import org.sahagin.runlib.additionaltestdoc.AdditionalTestDocs
import org.sahagin.runlib.external.CaptureStyle
import org.sahagin.runlib.external.Locale
import org.sahagin.runlib.srctreegen.ASTUtils
import org.sahagin.share.srctree.PageClass
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.srctree.TestMethodTable
import org.sahagin.runlib.external.TestDoc
import org.sahagin.runlib.external.Page
import org.sahagin.runlib.external.adapter.JavaAdapterContainer

class SrcTreeGeneratorUtils {
    private AdditionalTestDocs additionalTestDocs

    SrcTreeGeneratorUtils(AdditionalTestDocs additionalTestDocs) {
        this.additionalTestDocs = additionalTestDocs
    }

    static TestClass getTestClass(String classQualifiedName,
            TestClassTable rootClassTable, TestClassTable subClassTable) {
        if (classQualifiedName == null) {
            return null
        }

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

    static TestMethod getTestMethod(MethodNode methodNode, TestMethodTable methodTable) {
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

    static String generateMethodKey(MethodNode method, boolean noArgClassesStr) {
        String classQualifiedName = GroovyASTUtils.getClassQualifiedName(method.declaringClass)
        String methodSimpleName = method.name
        List<String> argClassQualifiedNames = GroovyASTUtils.getArgClassQualifiedNames(method)
        if (noArgClassesStr) {
            return TestMethod.generateMethodKey(classQualifiedName, methodSimpleName)
        } else {
            return TestMethod.generateMethodKey(
                    classQualifiedName, methodSimpleName, argClassQualifiedNames)
        }
    }

    static String generateFieldKey(ClassNode propClassNode, String propName) {
        return GroovyASTUtils.getClassQualifiedName(propClassNode) + "." + propName
    }

    // get annotation value assuming it is Capture valueS
    // returns null if not found
    private static Object getAnnotationValue(
            List<AnnotationNode> annotations, Class<?> annotationClass, String varName) {
        Expression valueNode = GroovyASTUtils.getAnnotationValueExpression(
            annotations, annotationClass, varName)
        if (valueNode == null) {
            return null
        }
        assert valueNode instanceof ConstantExpression
        return (valueNode as ConstantExpression).value
    }

    // get annotation value assuming it is CaptureStyle value
    // returns default value if not found
    private static CaptureStyle getAnnotationCaptureStyleValue(
            List<AnnotationNode> annotations, Class<?> annotationClass, String varName) {
        Expression valueNode = GroovyASTUtils.getAnnotationValueExpression(
            annotations, annotationClass, varName)
        if (valueNode == null) {
            return CaptureStyle.default
        }
        assert valueNode instanceof PropertyExpression
        String enumValue = (valueNode as PropertyExpression).propertyAsString
        CaptureStyle result = CaptureStyle.getEnum(enumValue)
        assert result != null
        return result
    }

    // get annotation value assuming it is Locale value
    // returns default value if not found
    private static Locale getAnnotationLocaleValue(
            List<AnnotationNode> annotations, Class<?> annotationClass, String varName) {
        Expression valueNode = GroovyASTUtils.getAnnotationValueExpression(
                annotations, annotationClass, varName)
        if (valueNode == null) {
            return Locale.default
        }
        assert valueNode instanceof PropertyExpression
        String enumValue = (valueNode as PropertyExpression).propertyAsString
        Locale result = Locale.getEnum(enumValue)
        assert result != null
        return result
    }

    // returns [testDoc, isPage (boolean value)]
    def getClassTestDoc(ClassNode classNode) {
        // TODO additional TestDoc should be prior to annotation TestDoc !?
        // TODO Pages, TestDocs
        String pageTestDoc =
                getAnnotationValue(classNode.annotations, Page.class, 'value') as String
        if (pageTestDoc != null) {
            return [pageTestDoc, true]
        }
        String testDoc =
                getAnnotationValue(classNode.annotations, TestDoc.class, 'value') as String
        if (testDoc != null) {
            return testDoc
        }
        AdditionalMethodTestDoc additional =
                additionalTestDocs.getClassTestDoc(GroovyASTUtils.getClassQualifiedName(classNode))
        if (additional != null) {
            return [additional.testDoc, additional instanceof AdditionalPage]
        }
        return [null, false]
    }

    // TODO captureStyle, lang, TestDocs, etc
    String getMethodTestDoc(MethodNode method) {
        // TODO additional TestDoc should be prior to annotation TestDoc !?
        String annotationTestDoc =
                getAnnotationValue(method.annotations, TestDoc.class, 'value') as String
        if (annotationTestDoc != null) {
            return annotationTestDoc
        }

        List<String> argClassQualifiedNames = GroovyASTUtils.getArgClassQualifiedNames(method)
        AdditionalMethodTestDoc additional = additionalTestDocs.getMethodTestDoc(
                GroovyASTUtils.getClassQualifiedName(method.declaringClass),
                method.name, argClassQualifiedNames)
        if (additional != null) {
            return additional.testDoc
        }
        return null
    }

    String getFieldTestDoc(FieldNode field) {
        // TODO consider additional testDoc
        return getAnnotationValue(field.annotations, TestDoc.class, 'value') as String
    }

    static boolean isRootMethod(MethodNode node) {
        return GroovyAdapterContainer.globalInstance().isRootMethod(node)
    }

    boolean isSubMethod(MethodNode node) {
        if (isRootMethod(node)) {
            return false
        }
        return getMethodTestDoc(node) != null
    }

    TestClass generateTestClass(ClassNode classNode) {
        TestClass testClass
        String testDoc
        boolean isPage
        (testDoc, isPage) = getClassTestDoc(classNode)
        if (isPage) {
            testClass = new PageClass()
        } else {
            testClass = new TestClass()
        }
        String classQName = GroovyASTUtils.getClassQualifiedName(classNode)
        testClass.key = classQName
        testClass.qualifiedName = classQName
        // TODO captureStyle, TestDocs, etc
        testClass.testDoc = testDoc
        return testClass
    }

}
