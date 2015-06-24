package org.sahagin.groovy.share

import java.util.List;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter

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

        List<String> result = new ArrayList<String>(method.parameters.length)
        for (Parameter param : method.parameters) {
            result.add(getClassQualifiedName(param.type))
        }
        return result
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
    
    // returns null if not found
    static AnnotationNode getAnnotationNode(
        List<AnnotationNode> annotations, String annotationClassName) {
        if (annotations == null) {
            return null
        }
        for (AnnotationNode annotation : annotations) {
            ClassNode classNode = annotation.classNode
            if (classNode.name == annotationClassName) {
                return annotation
            }
        }
        return null
    }
    
}