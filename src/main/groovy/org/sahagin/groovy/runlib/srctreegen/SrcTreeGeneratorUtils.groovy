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
import org.sahagin.runlib.additionaltestdoc.AdditionalMethodTestDoc
import org.sahagin.runlib.additionaltestdoc.AdditionalPage
import org.sahagin.runlib.additionaltestdoc.AdditionalTestDocs
import org.sahagin.runlib.external.CaptureStyle
import org.sahagin.runlib.srctreegen.ASTUtils
import org.sahagin.share.srctree.PageClass
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestMethod
import org.sahagin.runlib.external.TestDoc
import org.sahagin.runlib.external.Page

class SrcTreeGeneratorUtils {
    private AdditionalTestDocs additionalTestDocs
    private List<SrcTreeVisitorListener> listeners

    SrcTreeGeneratorUtils(AdditionalTestDocs additionalTestDocs) {
        this.additionalTestDocs = additionalTestDocs
        this.listeners = new ArrayList<SrcTreeVisitorListener>(4)
    }

    void addListener(SrcTreeVisitorListener listener) {
        listeners.add(listener)
    }

    List<SrcTreeVisitorListener> getListeners() {
        return listeners
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

    static String getClassQualifiedName(ClassNode classNode) {
        if (classNode.isArray()) {
            return getClassQualifiedName(classNode.getComponentType()) + "[]"
        } else {
            return classNode.getName()
        }
    }

    private static List<String> getArgClassQualifiedNames(MethodNode method) {
        // TODO parameterized etc

        List<String> result = new ArrayList<String>(method.getParameters().length)
        for (Parameter param : method.getParameters()) {
            result.add(getClassQualifiedName(param.getType()))
        }
        return result
    }

    static String generateMethodKey(MethodNode method, boolean noArgClassesStr) {
        String classQualifiedName = getClassQualifiedName(method.getDeclaringClass())
        String methodSimpleName = method.getName()
        List<String> argClassQualifiedNames = getArgClassQualifiedNames(method)
        if (noArgClassesStr) {
            return TestMethod.generateMethodKey(classQualifiedName, methodSimpleName)
        } else {
            return TestMethod.generateMethodKey(
                    classQualifiedName, methodSimpleName, argClassQualifiedNames)
        }
    }

    static String generateFieldKey(ClassNode propClassNode, String propName) {
        return getClassQualifiedName(propClassNode) + "." + propName
    }

    // TODO move this logic to adapter
    static boolean isRootMethod(MethodNode node) {
        List<AnnotationNode> annotations = node.annotations
        if (annotations == null) {
            return false
        }
        if (inheritsFromClass(node.getDeclaringClass(), "spock.lang.Specification")) {
            for (AnnotationNode annotation : annotations) {
                ClassNode classNode = annotation.getClassNode()
                if (classNode.name == "org.spockframework.runtime.model.FeatureMetadata") {
                    // FeatureMetadata is automatically added to the spock feature method
                    return true
                }
            }
        } else {
            for (AnnotationNode annotation : annotations) {
                ClassNode classNode = annotation.getClassNode()
                if (classNode.name == "org.junit.Test") {
                    return true
                }
            }
        }
        return false
    }

    // TODO captureStyle, lang, TestDocs, etc
    private static String getAnnotationValue(
        List<AnnotationNode> annotations, Class<?> annotationClass) {
        if (annotations == null) {
            return null
        }
        for (AnnotationNode annotation : annotations) {
            ClassNode classNode = annotation.getClassNode()
            if (classNode.name == annotationClass.getCanonicalName()) {
                Expression valueNode = annotation.getMember("value")
                assert valueNode != null
                assert valueNode instanceof ConstantExpression
                return (valueNode as ConstantExpression).getValue().toString()
            }
        }
        return null
    }

    // returns [testDoc, isPage (boolean value)]
    def getClassTestDoc(ClassNode classNode) {
        // TODO additional TestDoc should be prior to annotation TestDoc !?
        // TODO Pages, TestDocs
        String pageTestDoc = getAnnotationValue(
            classNode.getAnnotations(), Page.class)
        if (pageTestDoc != null) {
            return [pageTestDoc, true]
        }
        String testDoc = getAnnotationValue(
            classNode.getAnnotations(), TestDoc.class)
        if (testDoc != null) {
            return testDoc
        }
        AdditionalMethodTestDoc additional =
        additionalTestDocs.getClassTestDoc(getClassQualifiedName(classNode))
        if (additional != null) {
            return [additional.getTestDoc(), additional instanceof AdditionalPage]
        }
        return [null, false]
    }

    // TODO captureStyle, lang, TestDocs, etc
    String getMethodTestDoc(MethodNode method) {
        // TODO additional TestDoc should be prior to annotation TestDoc !?
        String annotationTestDoc = getAnnotationValue(
            method.getAnnotations(), TestDoc.class)
        if (annotationTestDoc != null) {
            return annotationTestDoc
        }

        List<String> argClassQualifiedNames = getArgClassQualifiedNames(method)
        AdditionalMethodTestDoc additional = additionalTestDocs.getMethodTestDoc(
            getClassQualifiedName(method.getDeclaringClass()),
            method.getName(), argClassQualifiedNames)
        if (additional != null) {
            return additional.getTestDoc()
        }
        return null
    }

    String getFieldTestDoc(FieldNode field) {
        // TODO consider additional testDoc
        return getAnnotationValue(field.getAnnotations(), TestDoc.class)
    }

    // TODO
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
        String classQName = getClassQualifiedName(classNode)
        testClass.setKey(classQName)
        testClass.setQualifiedName(classQName)
        // TODO captureStyle, TestDocs, etc
        testClass.setTestDoc(testDoc)
        return testClass
    }

    // check whether classNode is the class for className
    // or inherits from the class for className
    static boolean inheritsFromClass(ClassNode classNode, String className) {
        ClassNode parentNode = classNode
        while (parentNode != null) {
            if (parentNode.getName() == className) {
                return true
            }
            parentNode = parentNode.getSuperClass()
        }
        return false
    }

}
