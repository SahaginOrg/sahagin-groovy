package org.sahagin.groovy.runlib.srctreegen

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
import org.sahagin.runlib.additionaltestdoc.AdditionalMethodTestDoc
import org.sahagin.runlib.external.CaptureStyle
import org.sahagin.runlib.srctreegen.ASTUtils
import org.sahagin.share.srctree.TestMethod
import org.junit.Test
import org.sahagin.runlib.external.TestDoc

class SrcTreeGeneratorUtils {

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

    private static List<String> getArgClassQualifiedNames(ArgumentListExpression argumentList) {
        // TODO parameterized etc

        List<String> result = new ArrayList<String>(argumentList.getExpressions().size())
        for (Expression argument : argumentList.getExpressions()) {
            result.add(argument.getType().getName())
        }
        return result
    }

    static String generateMethodKey(Expression receiverExpression, String methodAsString,
        ArgumentListExpression argumentList, boolean noArgClassesStr) {
        // TODO if fails to infer class type??
        String classQualifiedName = receiverExpression.getType().getName()
        List<String> argClassQualifiedNames = getArgClassQualifiedNames(argumentList)
        if (noArgClassesStr) {
            return TestMethod.generateMethodKey(classQualifiedName, methodAsString)
        } else {
            return TestMethod.generateMethodKey(
                classQualifiedName, methodAsString, argClassQualifiedNames)
        }
    }

    // TODO
    static boolean isRootMethod(MethodNode node) {
        List<AnnotationNode> annotations = node.annotations
        if (annotations == null) {
            return false
        }
        for (AnnotationNode annotation : annotations) {
            ClassNode classNode = annotation.getClassNode()
            if (classNode.name == Test.class.getCanonicalName()) {
                return true
            }
        }
        return false
    }

    // TODO captureStyle, lang, TestDocs, etc
    static String getTestDoc(MethodNode node) {
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

    // TODO
    static boolean isSubMethod(MethodNode node) {
        if (isRootMethod(node)) {
            return false
        }
        return getTestDoc(node) != null
    }

}
