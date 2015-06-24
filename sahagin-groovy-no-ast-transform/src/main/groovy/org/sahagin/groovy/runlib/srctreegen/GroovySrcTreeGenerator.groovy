package org.sahagin.groovy.runlib.srctreegen

import java.util.Map
import java.util.concurrent.Phaser;
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
import org.sahagin.groovy.runlib.external.adapter.SrcTreeVisitorAdapter.CollectPhase
import org.sahagin.runlib.additionaltestdoc.AdditionalTestDocs
import org.sahagin.runlib.srctreegen.AdditionalTestDocsSetter
import org.sahagin.runlib.srctreegen.SrcTreeGenerator.CollectRootRequestor
import org.sahagin.runlib.srctreegen.SrcTreeGenerator.CollectSubRequestor
import org.sahagin.share.AcceptableLocales
import org.sahagin.share.srctree.SrcTree
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.Phases

class GroovySrcTreeGenerator {
    private AdditionalTestDocs additionalTestDocs
    private AcceptableLocales locales

    // additionalTestDocs can be null
    GroovySrcTreeGenerator(AdditionalTestDocs additionalTestDocs, AcceptableLocales locales) {
        if (additionalTestDocs == null) {
            // use empty additionalTestDocs
            this.additionalTestDocs = new AdditionalTestDocs()
        } else {
            this.additionalTestDocs = additionalTestDocs
        }
        this.locales = locales
    }

    SrcTree generate(String[] srcFiles, String[] classPathEntries) {
        ClassLoader parentLoader = ClassLoader.systemClassLoader
        GroovyClassLoader groovyLoader = new GroovyClassLoader(parentLoader)
        // TODO should add class path for each jar?
        for (String classPath : classPathEntries) {
            groovyLoader.addClasspath(classPath)
        }

        CompilerConfiguration compilerConf = new CompilerConfiguration()
        // disable geb, spock, sahagin-groovy itself AST transformations since they makes AST parsing difficult
        // TODO geb and spock specific logic
        compilerConf.disabledGlobalASTTransformations = new HashSet([
            "org.sahagin.groovy.runlib.runresultsgen.RunResultsGenTransformation",
            "org.spockframework.compiler.SpockTransform",
            "geb.transform.AttributeAccessingMetaClassRegisteringTransformation",
            "geb.transform.implicitassertions.ImplicitAssertionsTransformation"
        ])
        CompilationUnit compilation = new CompilationUnit(compilerConf, null, groovyLoader)
        compilation.addSources(srcFiles)
        // TODO maybe don't need to execute to CLASS_GENERATION phase,
        // maybe this works even if Phase.CANONICALIZATION is specified
        // (when Phase.CANONICALIZATION is specified, page content value closure
        // is not moved to static initializer part, so you must change CollectGebPageContentVisitor
        // logic)
        compilation.compile(Phases.CLASS_GENERATION)
        Collection<SourceUnit> sources = compilation.sources.values()
        SrcTreeGeneratorUtils utils = new SrcTreeGeneratorUtils(additionalTestDocs)

        // collect root visitor
        CollectRootVisitor beforeRootVisitor = new CollectRootVisitor(utils, CollectPhase.BEFORE)
        for (SourceUnit src : sources) {
            for (ClassNode classNode : src.AST.classes) {
                classNode.visitContents(beforeRootVisitor)
            }
        }
        CollectRootVisitor rootVisitor = new CollectRootVisitor(utils, CollectPhase.WHILE)
        for (SourceUnit src : sources) {
            for (ClassNode classNode : src.AST.classes) {
                classNode.visitContents(rootVisitor)
            }
        }
        CollectRootVisitor afterRootVisitor = new CollectRootVisitor(utils, CollectPhase.AFTER)
        for (SourceUnit src : sources) {
            for (ClassNode classNode : src.AST.classes) {
                classNode.visitContents(afterRootVisitor)
            }
        }

        // collect sub visitor
        CollectSubVisitor beforeSubVisitor = new CollectSubVisitor(
            rootVisitor.rootClassTable, utils, CollectPhase.BEFORE)
        for (SourceUnit src : sources) {
            for (ClassNode classNode : src.AST.classes) {
                classNode.visitContents(beforeSubVisitor)
            }
        }
        CollectSubVisitor subVisitor = new CollectSubVisitor(
            rootVisitor.rootClassTable, utils, CollectPhase.WHILE)
        for (SourceUnit src : sources) {
            for (ClassNode classNode : src.AST.classes) {
                classNode.visitContents(subVisitor)
            }
        }
        CollectSubVisitor afterSubVisitor = new CollectSubVisitor(
            rootVisitor.rootClassTable, utils, CollectPhase.AFTER)
        for (SourceUnit src : sources) {
            for (ClassNode classNode : src.AST.classes) {
                classNode.visitContents(afterSubVisitor)
            }
        }

        // add additional TestDoc to the table
        AdditionalTestDocsSetter setter = new AdditionalTestDocsSetter(
            subVisitor.rootClassTable, subVisitor.subClassTable,
            rootVisitor.rootMethodTable, subVisitor.subMethodTable)
        setter.set(additionalTestDocs)

        // delegation resolver
        DelegateResolver delegateResolver = new DelegateResolver(
            subVisitor.rootClassTable, subVisitor.subClassTable)
        delegateResolver.resolve()

        // collect code visitor
        for (SourceUnit src : sources) {
            CollectCodeVisitor beforeCodeVisitor = new CollectCodeVisitor(
                    src, subVisitor.rootClassTable, subVisitor.subClassTable,
                    rootVisitor.rootMethodTable, subVisitor.subMethodTable,
                    subVisitor.fieldTable, utils, CollectPhase.BEFORE)
            for (ClassNode classNode : src.AST.classes) {
                classNode.visitContents(beforeCodeVisitor)
            }
        }
        for (SourceUnit src : sources) {
            CollectCodeVisitor codeVisitor = new CollectCodeVisitor(
                    src, subVisitor.rootClassTable, subVisitor.subClassTable,
                    rootVisitor.rootMethodTable, subVisitor.subMethodTable,
                    subVisitor.fieldTable, utils, CollectPhase.WHILE)
            for (ClassNode classNode : src.AST.classes) {
                classNode.visitContents(codeVisitor)
            }
        }
        for (SourceUnit src : sources) {
            CollectCodeVisitor afterCodeVisitor = new CollectCodeVisitor(
                    src, subVisitor.rootClassTable, subVisitor.subClassTable,
                    rootVisitor.rootMethodTable, subVisitor.subMethodTable,
                    subVisitor.fieldTable, utils, CollectPhase.AFTER)
            for (ClassNode classNode : src.AST.classes) {
                classNode.visitContents(afterCodeVisitor)
            }
        }

        SrcTree result = new SrcTree()
        result.rootClassTable = subVisitor.rootClassTable
        result.subClassTable = subVisitor.subClassTable
        result.rootMethodTable = rootVisitor.rootMethodTable
        result.subMethodTable = subVisitor.subMethodTable
        result.fieldTable = subVisitor.fieldTable
        return result
    }

    SrcTree generateWithRuntimeClassPath(File srcRootDir) {
        // set up srcFilePaths
        if (!srcRootDir.exists()) {
            throw new IllegalArgumentException("directory does not exist: " + srcRootDir.absolutePath)
        }
        // TODO support Java and Groovy mixed project
        String[] extensions = ["groovy"]
        Collection<File> srcFileCollection = FileUtils.listFiles(srcRootDir, extensions, true)
        List<File> srcFileList = new ArrayList<File>(srcFileCollection)
        String[] srcFilePaths = new String[srcFileList.size()]
        for (int i = 0; i < srcFileList.size(); i++) {
            srcFilePaths[i] = srcFileList.get(i).absolutePath
        }

        // TODO check jar manifest, etc
        String classPathStr = System.getProperty("java.class.path")
        String[] classPathEntries = classPathStr.split(Pattern.quote(File.pathSeparator))
        return generate(srcFilePaths, classPathEntries)
    }
}
