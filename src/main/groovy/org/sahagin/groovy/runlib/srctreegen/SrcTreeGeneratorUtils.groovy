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
import org.sahagin.runlib.additionaltestdoc.AdditionalMethodTestDoc
import org.sahagin.runlib.additionaltestdoc.AdditionalTestDocs
import org.sahagin.runlib.external.CaptureStyle
import org.sahagin.runlib.srctreegen.ASTUtils
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestMethod
import org.sahagin.runlib.external.TestDoc

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

    private static String getClassQualifiedName(ClassNode classNode) {
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

    // TODO move this logic to adapter
    static boolean isRootMethod(MethodNode node) {
        List<AnnotationNode> annotations = node.annotations
        if (annotations == null) {
            return false
        }
        for (AnnotationNode annotation : annotations) {
            ClassNode classNode = annotation.getClassNode()
            if (classNode.name == "org.junit.Test") {
                return true
            }
        }
        return false
    }

    // TODO captureStyle, lang, TestDocs, etc
    private static String getTestDocFromAnnotation(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return null
        }
        for (AnnotationNode annotation : annotations) {
            ClassNode classNode = annotation.getClassNode()
            if (classNode.name == TestDoc.class.getCanonicalName()) {
                Expression valueNode = annotation.getMember("value")
                assert valueNode != null
                assert valueNode instanceof ConstantExpression
                return (valueNode as ConstantExpression).getValue().toString()
            }
        }
        return null
    }

    // TODO captureStyle, lang, TestDocs, etc
    String getMethodTestDoc(MethodNode method) {
        // TODO additional TestDoc should be prior to annotation TestDoc !?
        String annotationTestDoc = getTestDocFromAnnotation(method.getAnnotations())
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
        return getTestDocFromAnnotation(field.getAnnotations())
    }

    // TODO
    boolean isSubMethod(MethodNode node) {
        if (isRootMethod(node)) {
            return false
        }
        return getMethodTestDoc(node) != null
    }

}
