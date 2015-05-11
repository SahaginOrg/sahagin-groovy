package org.sahagin.groovy.runlib.srctreegen

import java.util.Map
import java.util.regex.Pattern

import org.apache.commons.io.FileUtils
import org.codehaus.groovy.antlr.AntlrASTProcessSnippets
import org.codehaus.groovy.antlr.AntlrParserPlugin
import org.codehaus.groovy.antlr.SourceBuffer
import org.codehaus.groovy.antlr.UnicodeEscapingReader
import org.codehaus.groovy.antlr.parser.GroovyLexer
import org.codehaus.groovy.antlr.parser.GroovyRecognizer
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.CompileUnit
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.ErrorCollector
import org.codehaus.groovy.control.SourceUnit
import org.sahagin.runlib.additionaltestdoc.AdditionalTestDocs
import org.sahagin.runlib.srctreegen.AdditionalTestDocsSetter
import org.sahagin.runlib.srctreegen.SrcTreeGenerator.CollectRootRequestor
import org.sahagin.runlib.srctreegen.SrcTreeGenerator.CollectSubRequestor
import org.sahagin.share.AcceptableLocales
import org.sahagin.share.srctree.SrcTree
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.ast.ClassNode

class SrcTreeGenerator {
    private AdditionalTestDocs additionalTestDocs
    private AcceptableLocales locales

    // additionalTestDocs can be null
    SrcTreeGenerator(AdditionalTestDocs additionalTestDocs, AcceptableLocales locales) {
        if (additionalTestDocs == null) {
            // use empty additionalTestDocs
            this.additionalTestDocs = new AdditionalTestDocs()
        } else {
            this.additionalTestDocs = additionalTestDocs
        }
        this.locales = locales
    }

    SrcTree generate(String[] srcFiles, String[] classPathEntries) {
        ClassLoader parentLoader = ClassLoader.getSystemClassLoader()
        GroovyClassLoader groovyLoader = new GroovyClassLoader(parentLoader)
        // TODO should add class path for each jar?
        for (String classPath : classPathEntries) {
            groovyLoader.addClasspath(classPath)
        }

        CompilationUnit compilation = new CompilationUnit(groovyLoader)
        compilation.addSources(srcFiles)
        compilation.compile()
        Collection<SourceUnit> sources = compilation.sources.values()
        SrcTreeGeneratorUtils utils = new SrcTreeGeneratorUtils(additionalTestDocs)

        CollectRootVisitor rootVisitor = new CollectRootVisitor(utils)
        for (SourceUnit src : sources) {
            for (ClassNode classNode : src.getAST().getClasses()) {
                classNode.visitContents(rootVisitor)
            }
        }

        CollectSubVisitor subVisitor = new CollectSubVisitor(
            rootVisitor.getRootClassTable(), utils)
        for (SourceUnit src : sources) {
            for (ClassNode classNode : src.getAST().getClasses()) {
                classNode.visitContents(subVisitor)
            }
        }

        CollectGebPageContentVisitor gebVisitor = new CollectGebPageContentVisitor(
            rootVisitor.getRootClassTable(), subVisitor.getSubClassTable(),
            subVisitor.getFieldTable(), utils)
        for (SourceUnit src : sources) {
            for (ClassNode classNode : src.getAST().getClasses()) {
                if (gebVisitor.needsVisit(classNode)) {
                    classNode.visitContents(gebVisitor)
                }
            }
        }

        // add additional TestDoc to the table
        AdditionalTestDocsSetter setter = new AdditionalTestDocsSetter(
            rootVisitor.getRootClassTable(), subVisitor.getSubClassTable(),
            rootVisitor.getRootMethodTable(), subVisitor.getSubMethodTable())
        setter.set(additionalTestDocs)

        DelegateResolver delegateResolver = new DelegateResolver(
            rootVisitor.getRootClassTable(), subVisitor.getSubClassTable())
        delegateResolver.resolve()

        CollectCodeVisitor codeVisitor = new CollectCodeVisitor(
            rootVisitor.getRootClassTable(), subVisitor.getSubClassTable(),
            rootVisitor.getRootMethodTable(), subVisitor.getSubMethodTable(),
            subVisitor.getFieldTable(), utils)
        for (SourceUnit src : sources) {
            codeVisitor.setSrcUnit(src)
            for (ClassNode classNode : src.getAST().getClasses()) {
                classNode.visitContents(codeVisitor)
            }
        }

        SrcTree result = new SrcTree()
        result.setRootClassTable(rootVisitor.getRootClassTable())
        result.setSubClassTable(subVisitor.getSubClassTable())
        result.setRootMethodTable(rootVisitor.getRootMethodTable())
        result.setSubMethodTable(subVisitor.getSubMethodTable())
        result.setFieldTable(subVisitor.getFieldTable())
        return result
    }

    SrcTree generateWithRuntimeClassPath(File srcRootDir) {
        // set up srcFilePaths
        if (!srcRootDir.exists()) {
            throw new IllegalArgumentException("directory does not exist: " + srcRootDir.getAbsolutePath())
        }
        // TODO support Java and Groovy mixed project
        String[] extensions = ["groovy"]
        Collection<File> srcFileCollection = FileUtils.listFiles(srcRootDir, extensions, true)
        List<File> srcFileList = new ArrayList<File>(srcFileCollection)
        String[] srcFilePaths = new String[srcFileList.size()]
        for (int i = 0; i < srcFileList.size(); i++) {
            srcFilePaths[i] = srcFileList.get(i).getAbsolutePath()
        }

        // TODO check jar manifest, etc
        String classPathStr = System.getProperty("java.class.path")
        String[] classPathEntries = classPathStr.split(Pattern.quote(File.pathSeparator))
        return generate(srcFilePaths, classPathEntries)
    }
}
