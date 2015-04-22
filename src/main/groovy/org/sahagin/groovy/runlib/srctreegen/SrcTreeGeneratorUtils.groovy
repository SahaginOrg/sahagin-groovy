package org.sahagin.groovy.runlib.srctreegen

import java.util.List

import org.apache.commons.lang3.tuple.Pair
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
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

    private static List<String> getArgClassQualifiedNames(MethodNode method) {
        // TODO parameterized etc

        List<String> result = new ArrayList<String>(method.getParameters().length)
        for (Parameter param : method.getParameters()) {
            result.add(param.getOriginType().getName()) // TODO getOriginType or getType?
        }
        return result
    }

    static String generateMethodKey(MethodNode method, boolean noArgClassesStr) {
        String classQualifiedName = method.getDeclaringClass().getName()
        String methodSimpleName = method.getName()
        List<String> argClassQualifiedNames = getArgClassQualifiedNames(method)
        if (noArgClassesStr) {
            return TestMethod.generateMethodKey(classQualifiedName, methodSimpleName)
        } else {
            return TestMethod.generateMethodKey(
                    classQualifiedName, methodSimpleName, argClassQualifiedNames)
        }
    }

    private static List<String> getArgClassQualifiedNames(List<ClassNode> argClasses) {
        // TODO parameterized etc

        List<String> result = new ArrayList<String>(argClasses.size())
        for (ClassNode argClass : argClasses) {
            result.add(argClass.getName())
        }
        return result
    }

    static String generateMethodKey(String classQualifiedName, String methodAsString,
        List<ClassNode> argClasses, boolean noArgClassesStr) {
        // TODO if fails to infer class type??
        List<String> argClassQualifiedNames = getArgClassQualifiedNames(argClasses)
        if (noArgClassesStr) {
            return TestMethod.generateMethodKey(classQualifiedName, methodAsString)
        } else {
            return TestMethod.generateMethodKey(
                    classQualifiedName, methodAsString, argClassQualifiedNames)
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
    private static String getTestDocFromAnnotation(MethodNode node) {
        List<AnnotationNode> annotations = node.annotations
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
    String getTestDoc(MethodNode method) {
        // TODO additional TestDoc should be prior to annotation TestDoc !?
        String annotationTestDoc = getTestDocFromAnnotation(method)
        if (annotationTestDoc != null) {
            return annotationTestDoc
        }

        List<String> argClassQualifiedNames = getArgClassQualifiedNames(method)
        AdditionalMethodTestDoc additional = additionalTestDocs.getMethodTestDoc(
                method.getDeclaringClass().getName(), method.getName(), argClassQualifiedNames)
        if (additional != null) {
            return additional.getTestDoc()
        }
        return null
    }

    // TODO
    boolean isSubMethod(MethodNode node) {
        if (isRootMethod(node)) {
            return false
        }
        return getTestDoc(node) != null
    }

}
