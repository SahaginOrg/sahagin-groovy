package org.sahagin.groovy.runlib.srctreegen

import java.io.File
import java.util.Map.Entry
import java.util.regex.Pattern

import groovyjarjarantlr.collections.AST

import org.codehaus.groovy.antlr.AntlrASTProcessSnippets
import org.codehaus.groovy.antlr.AntlrParserPlugin
import org.codehaus.groovy.antlr.SourceBuffer
import org.codehaus.groovy.antlr.UnicodeEscapingReader
import org.codehaus.groovy.antlr.parser.GroovyLexer
import org.codehaus.groovy.antlr.parser.GroovyRecognizer
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.CompileUnit
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.ErrorCollector
import org.codehaus.groovy.control.SourceUnit
import org.sahagin.runlib.srctreegen.SrcTreeGenerator.CollectRootRequestor
import org.sahagin.runlib.srctreegen.SrcTreeGenerator.CollectSubRequestor
import org.sahagin.share.srctree.SrcTree
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.ast.ClassNode

class SrcTreeGenerator {

    SrcTree generate(String[] srcPaths) {        
        ClassLoader parentLoader = ClassLoader.getSystemClassLoader()
        GroovyClassLoader groovyLoader = new GroovyClassLoader(parentLoader)

        // TODO check jar manifest, etc
        String classPathStr = System.getProperty("java.class.path")
        String[] classPathArray = classPathStr.split(Pattern.quote(File.pathSeparator))
        for (String classPath : classPathArray) {
            groovyLoader.addClasspath(classPath)
        }
        
        CompilationUnit compilation = new CompilationUnit(groovyLoader)
        compilation.addSources(srcPaths)
        compilation.compile()
        Collection<SourceUnit> sources = compilation.sources.values()
                
        CollectRootVisitor rootVisitor = new CollectRootVisitor()
        for (SourceUnit src : sources) {
            for (ClassNode classNode : src.getAST().getClasses()) {
                classNode.visitContents(rootVisitor)
            }
        }
        CollectSubVisitor subVisitor = new CollectSubVisitor(rootVisitor.getRootClassTable())
        for (SourceUnit src : sources) {
            for (ClassNode classNode : src.getAST().getClasses()) {
                classNode.visitContents(subVisitor)
            }
        }
        // TODO additional
        CollectCodeVisitor codeVisitor = new CollectCodeVisitor(
            rootVisitor.getRootMethodTable(), subVisitor.getSubMethodTable())
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
        return result
    }
}
