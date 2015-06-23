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
        String classQualifiedName = GroovyASTUtils.getClassQualifiedName(method.getDeclaringClass())
        String methodSimpleName = method.getName()
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

    // returns null if not found
    static AnnotationNode getAnnotationNode(
        List<AnnotationNode> annotations, String annotationClassName) {
        if (annotations == null) {
            return null
        }
        for (AnnotationNode annotation : annotations) {
            ClassNode classNode = annotation.getClassNode()
            if (classNode.name == annotationClassName) {
                return annotation
            }
        }
        return null
    }
    
    // TODO captureStyle, lang, TestDocs, etc
    private static Object getAnnotationValue(
        List<AnnotationNode> annotations, Class<?> annotationClass, String varName) {
        AnnotationNode annotation = 
        GroovyASTUtils.getAnnotationNode(annotations, annotationClass.getCanonicalName())
        if (annotation == null) {
            return null
        }
        Expression valueNode = annotation.getMember(varName)
        assert valueNode != null
        assert valueNode instanceof ConstantExpression
        return (valueNode as ConstantExpression).getValue()
    }
    
    // returns [testDoc, isPage (boolean value)]
    def getClassTestDoc(ClassNode classNode) {
        // TODO additional TestDoc should be prior to annotation TestDoc !?
        // TODO Pages, TestDocs
        String pageTestDoc = 
        getAnnotationValue(classNode.getAnnotations(), Page.class, 'value') as String
        if (pageTestDoc != null) {
            return [pageTestDoc, true]
        }
        String testDoc = 
        getAnnotationValue(classNode.getAnnotations(), TestDoc.class, 'value') as String
        if (testDoc != null) {
            return testDoc
        }
        AdditionalMethodTestDoc additional =
        additionalTestDocs.getClassTestDoc(GroovyASTUtils.getClassQualifiedName(classNode))
        if (additional != null) {
            return [additional.getTestDoc(), additional instanceof AdditionalPage]
        }
        return [null, false]
    }

    // TODO captureStyle, lang, TestDocs, etc
    String getMethodTestDoc(MethodNode method) {
        // TODO additional TestDoc should be prior to annotation TestDoc !?
        String annotationTestDoc = 
        getAnnotationValue(method.getAnnotations(), TestDoc.class, 'value') as String
        if (annotationTestDoc != null) {
            return annotationTestDoc
        }

        List<String> argClassQualifiedNames = GroovyASTUtils.getArgClassQualifiedNames(method)
        AdditionalMethodTestDoc additional = additionalTestDocs.getMethodTestDoc(
            GroovyASTUtils.getClassQualifiedName(method.getDeclaringClass()),
            method.getName(), argClassQualifiedNames)
        if (additional != null) {
            return additional.getTestDoc()
        }
        return null
    }

    String getFieldTestDoc(FieldNode field) {
        // TODO consider additional testDoc
        return getAnnotationValue(field.getAnnotations(), TestDoc.class, 'value') as String
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
        testClass.setKey(classQName)
        testClass.setQualifiedName(classQName)
        // TODO captureStyle, TestDocs, etc
        testClass.setTestDoc(testDoc)
        return testClass
    }

}
