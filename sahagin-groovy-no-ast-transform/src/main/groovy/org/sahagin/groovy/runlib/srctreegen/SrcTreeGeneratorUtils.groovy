package org.sahagin.groovy.runlib.srctreegen

import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.control.SourceUnit
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.sahagin.groovy.runlib.external.adapter.GroovyAdapterContainer
import org.sahagin.groovy.share.GroovyASTUtils
import org.sahagin.runlib.additionaltestdoc.AdditionalMethodTestDoc
import org.sahagin.runlib.additionaltestdoc.AdditionalPage
import org.sahagin.runlib.additionaltestdoc.AdditionalTestDocs
import org.sahagin.runlib.external.CaptureStyle
import org.sahagin.runlib.external.Locale
import org.sahagin.runlib.external.Page
import org.sahagin.runlib.external.PageDoc;
import org.sahagin.runlib.external.PageDocs;
import org.sahagin.runlib.external.Pages
import org.sahagin.runlib.external.TestDoc
import org.sahagin.runlib.external.TestDocs
import org.sahagin.share.AcceptableLocales
import org.sahagin.share.srctree.PageClass
import org.sahagin.share.srctree.TestClass
import org.sahagin.share.srctree.TestClassTable
import org.sahagin.share.srctree.TestMethod
import org.sahagin.share.srctree.TestMethodTable

class SrcTreeGeneratorUtils {
    private AdditionalTestDocs additionalTestDocs
    private AcceptableLocales locales

    SrcTreeGeneratorUtils(AdditionalTestDocs additionalTestDocs, AcceptableLocales locales) {
        this.additionalTestDocs = additionalTestDocs
        this.locales = locales
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

    // get annotation value assuming it is Object value.
    // returns null if not found
    private static Object getAnnotationValue(AnnotationNode annotation, String varName) {
        if (annotation == null) {
            throw new NullPointerException()
        }
        if (varName == null) {
            throw new NullPointerException()
        }
        Expression valueExp = annotation.getMember(varName)
        if (valueExp == null) {
            return null
        }
        assert valueExp instanceof ConstantExpression
        return (valueExp as ConstantExpression).value
    }

    // get annotation value assuming it is CaptureStyle value.
    // returns default value if not found
    private static CaptureStyle getAnnotationCaptureStyleValue(
        AnnotationNode annotation, String varName) {
        if (annotation == null) {
            throw new NullPointerException()
        }
        if (varName == null) {
            throw new NullPointerException()
        }
        Expression valueExp = annotation.getMember(varName)
        if (valueExp == null) {
            return CaptureStyle.default
        }
        assert valueExp instanceof PropertyExpression
        String enumValue = (valueExp as PropertyExpression).propertyAsString
        CaptureStyle result = CaptureStyle.valueOf(enumValue)
        if (result == null) {
            return CaptureStyle.default
        }
        return result
    }

    // get annotation value assuming it is Locale value.
    // returns default value if not found
    private static Locale getAnnotationLocaleValue(
        AnnotationNode annotation, String varName) {
        if (annotation == null) {
            throw new NullPointerException()
        }
        if (varName == null) {
            throw new NullPointerException()
        }
        Expression valueExp = annotation.getMember(varName)
        if (valueExp == null) {
            return Locale.default
        }
        assert valueExp instanceof PropertyExpression
        String enumValue = (valueExp as PropertyExpression).propertyAsString
        CaptureStyle result = Locale.valueOf(enumValue)
        if (result == null) {
            return Locale.default
        }
        return result
    }

    // return [Map<Locale, String>, CaptureStyle]
    // return empty list and null pair if no TestDoc is found
    private static def getAllTestDocs(List<AnnotationNode> annotations) {
        AnnotationNode testDocAnnotation = GroovyASTUtils.getAnnotationNode(annotations, TestDoc.class)
        AnnotationNode testDocsAnnotation = GroovyASTUtils.getAnnotationNode(annotations, TestDocs.class)
        if (testDocAnnotation != null && testDocsAnnotation != null) {
            // TODO throw IllegalTestScriptException
            throw new RuntimeException('do not use @TestDoc and @TestDocs at the same place')
        }

        // all @testDoc annotations including annotations contained in @TestDocs
        List<AnnotationNode> allTestDocAnnotations = new ArrayList<AnnotationNode>(2)
        CaptureStyle resultCaptureStyle = null

        if (testDocAnnotation != null) {
            // get @TestDoc
            allTestDocAnnotations.add(testDocAnnotation)
            resultCaptureStyle = getAnnotationCaptureStyleValue(testDocAnnotation, 'capture')
        } else if (testDocsAnnotation != null) {
            throw new RuntimeException('TODO implement')
        }

        // get resultTestDocMap
        Map<Locale, String> resultTestDocMap = 
        new HashMap<Locale, String>(allTestDocAnnotations.size())
        for (AnnotationNode eachTestDocAnnotation : allTestDocAnnotations) {
            Object value = getAnnotationValue(eachTestDocAnnotation, 'value')
            Locale locale = getAnnotationLocaleValue(eachTestDocAnnotation, 'locale')
            resultTestDocMap.put(locale, (String) value)
        }

        return [resultTestDocMap, resultCaptureStyle]
    }

    // return empty list if no Page is found
    private static Map<Locale, String> getAllPageDocs(
            List<AnnotationNode> annotations) {

        // all @PageDoc or @Page annotations including annotations contained in @PageDocs or @Page
        List<AnnotationNode> allPageAnnotations = new ArrayList<AnnotationNode>(2)

        List<Class<?>> singlePageAnnotationClasses = new ArrayList<Class<?>>(2);
        singlePageAnnotationClasses.add(PageDoc.class);
        singlePageAnnotationClasses.add(Page.class);
        for (Class<?> annotationClass : singlePageAnnotationClasses) {
            AnnotationNode annotation = GroovyASTUtils.getAnnotationNode(annotations, annotationClass);
            if (annotation == null) {
                continue; // annotation is not found
            }
            if (allPageAnnotations.size() > 0) {
                // TODO throw IllegalTestScriptException
                throw new RuntimeException("don't use multiple page annoations at the same place");
            }
            allPageAnnotations.add(annotation);
        }
        
        List<Class<?>> multiplePageAnnotationClasses = new ArrayList<Class<?>>(2);
        multiplePageAnnotationClasses.add(PageDocs.class);
        multiplePageAnnotationClasses.add(Pages.class);
        for (Class<?> annotationClass : multiplePageAnnotationClasses) {
            AnnotationNode annotation = GroovyASTUtils.getAnnotationNode(annotations, annotationClass);
            if (annotation == null) {
                continue; // annotation is not found
            }
            if (allPageAnnotations.size() > 0) {
                // TODO throw IllegalTestScriptException
                throw new RuntimeException("don't use multiple page annoations at the same place");
            }
            // get @PageDoc or @Page from @PageDocs or @Pages
            Object value = getAnnotationValue(annotation, "value");
            Object[] values = (Object[]) value;
            for (Object element : values) {
                allPageAnnotations.add((IAnnotationBinding) element);
            }
        }
        
        // get resultPageMap
        Map<Locale, String> resultPageMap = 
        new HashMap<Locale, String>(allPageAnnotations.size())
        for (AnnotationNode eachPageAnnotation : allPageAnnotations) {
            Object value = getAnnotationValue(eachPageAnnotation, 'value')
            Locale locale = getAnnotationLocaleValue(eachPageAnnotation, 'locale')
            resultPageMap.put(locale, (String) value)
        }

        return resultPageMap
    }

    // returns [value(String), CaptureStyle]
    // return null pair if no TestDoc is found
    private static def getTestDoc(List<AnnotationNode> annotations, AcceptableLocales locales) {
        Map<Locale, String> testDocMap
        CaptureStyle captureStyle
        (testDocMap, captureStyle) = getAllTestDocs(annotations)
        if (testDocMap.isEmpty()) {
            return [null, captureStyle] // no @TestDoc found
        }

        String testDoc = null
        for (Locale locale : locales.locales) {
            String value = testDocMap.get(locale)
            if (value != null) {
                testDoc = value
                break
            }
        }
        if (testDoc == null) {
            // set empty string if no locale matched data is found
            return ['', captureStyle]
        } else {
            return [testDoc, captureStyle]
        }
    }
    
    // return null if no Page found
    private static String getPageDoc(
        List<AnnotationNode> annotations, AcceptableLocales locales) {
        Map<Locale, String> allPages = getAllPageDocs(annotations)
        if (allPages.isEmpty()) {
            return null // no @Page found
        }

        for (Locale locale : locales.locales) {
            String value = allPages.get(locale)
            if (value != null) {
                return value
            }
        }
        // set empty string if no locale matched data is found
        return ''
    }

    // returns [testDoc(String), isPage (boolean)]
    def getClassTestDoc(ClassNode classNode) {
        // TODO additional TestDoc should be prior to annotation TestDoc !?
        String pageDoc = getPageDoc(classNode.annotations, locales)
        if (pageDoc != null) {
            return [pageDoc, true]
        }
        String testDoc = getTestDoc(classNode.annotations, locales).first()
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

    // returns [testDoc(String), CaptureStyle]
    def getMethodTestDoc(MethodNode method) {
        // TODO additional TestDoc should be prior to annotation TestDoc !?
        CaptureStyle captureStyle
        String testDoc
        (testDoc, captureStyle) = getTestDoc(method.annotations, locales)
        if (testDoc != null) {
            return [testDoc, captureStyle]
        }

        List<String> argClassQualifiedNames = GroovyASTUtils.getArgClassQualifiedNames(method)
        AdditionalMethodTestDoc additional = additionalTestDocs.getMethodTestDoc(
                GroovyASTUtils.getClassQualifiedName(method.declaringClass),
                method.name, argClassQualifiedNames)
        if (additional != null) {
            return [additional.testDoc, additional.captureStyle]
        }
        return [null, CaptureStyle.default]
    }

    String getFieldTestDoc(FieldNode field) {
        // TODO consider additional testDoc
        return getTestDoc(field.annotations, locales).first()
    }

    static boolean isRootMethod(MethodNode node) {
        return GroovyAdapterContainer.globalInstance().isRootMethod(node)
    }

    boolean isSubMethod(MethodNode node) {
        if (isRootMethod(node)) {
            return false
        }
        String testDoc = getMethodTestDoc(node).first()
        return testDoc != null
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
        testClass.testDoc = testDoc
        return testClass
    }
    
    // returns null if not found
    static ClassNode searchClassNode(Collection<SourceUnit> sources, String classQualifiedName) {
        for (SourceUnit src : sources) {
            for (ClassNode classNode : src.AST.classes) {
               if (classNode.name == classQualifiedName) {
                   return classNode
               }
            }
        }
        return null
    }
}
