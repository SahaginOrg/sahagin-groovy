package org.sahagin.groovy.share

import java.util.List;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.Expression;

class GroovyASTUtils {
    
    static String getClassQualifiedName(ClassNode classNode) {
        if (classNode.isArray()) {
            return getClassQualifiedName(classNode.componentType) + "[]"
        } else {
            return classNode.name
        }
    }
    
    static List<String> getArgClassQualifiedNames(MethodNode method) {
        // TODO parameterized etc

        List<String> result = new ArrayList<>(method.parameters.length)
        for (Parameter param : method.parameters) {
            result.add(getClassQualifiedName(param.type))
        }
        return result
    }
    
    static String getterName(String fieldName) {
        if (fieldName == null || fieldName == "") {
            throw new IllegalArgumentException(fieldName)
        }
        return "get" + fieldName.substring(0, 1).toUpperCase(Locale.ROOT) + fieldName.substring(1)
    }

    static String setterName(String fieldName) {
        if (fieldName == null || fieldName == "") {
            throw new IllegalArgumentException(fieldName)
        }
        return "set" + fieldName.substring(0, 1).toUpperCase(Locale.ROOT) + fieldName.substring(1)
    }
    
    // check whether classNode is the class for className
    // or inherits from the class for className
    static boolean inheritsFromClass(ClassNode classNode, String className) {
        ClassNode parentNode = classNode
        while (parentNode != null) {
            if (parentNode.name == className) {
                return true
            }
            parentNode = parentNode.superClass
        }
        return false
    }

    // Search AnnotationNode list by class name
    // - returns null if not found
    static AnnotationNode getAnnotationNode(
            List<AnnotationNode> annotations, String annotationClassName) {
        if (annotations == null) {
            return null
        }
        for (AnnotationNode annotation : annotations) {
            ClassNode classNode = annotation.classNode
            // TODO if multiple annotations for annotationClassName exists
            if (classNode.name == annotationClassName) {
                return annotation
            }
        }
        return null
    }

    // Search AnnotationNode list by class
    // - returns null if not found
    static AnnotationNode getAnnotationNode(
            List<AnnotationNode> annotations, Class<?> annotationClass) {
        return getAnnotationNode(annotations, annotationClass.canonicalName)
    }
}