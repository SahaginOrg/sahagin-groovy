package org.sahagin.groovy.runlib.runresultsgen

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.ThrowStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.control.CompilePhase
import org.sahagin.groovy.share.GroovyASTUtils;
import org.sahagin.share.srctree.TestMethod

// must be called after spock AST transformation
// TODO how to disable sahagin without removing sahagin-groovy from the dependency
// TODO throw error if javaagent option is also specified
// TODO later phase than CANONICALIZATION maybe better
@GroovyASTTransformation(phase=CompilePhase.CANONICALIZATION)
class RunResultsGenTransformation implements ASTTransformation {
    
    private Statement hookStatement(String methodName, List<Expression> args) {
        ClassNode classExpType = new ClassNode(GroovyHookMethodDef.class)
        ClassExpression classExp = new ClassExpression(classExpType)
        ArgumentListExpression argList
        if (args == null) {
            argList = ArgumentListExpression.EMPTY_ARGUMENTS
        } else {
            argList = new ArgumentListExpression(args)
        }
        MethodCallExpression methodCall = new MethodCallExpression(classExp, methodName, argList)
        return new ExpressionStatement(methodCall)
    }
    
    private ConstantExpression classQualifiedNameExp(MethodNode methodNode) {
        String classQualifiedName = GroovyASTUtils.getClassQualifiedName(methodNode.declaringClass)
        return new ConstantExpression(classQualifiedName)
    }
    
    private ConstantExpression methodSimpleNameExp(MethodNode methodNode) {
        // TODO spock spefic logic
        AnnotationNode featureMetaData = GroovyASTUtils.getAnnotationNode(
            methodNode.annotations, "org.spockframework.runtime.model.FeatureMetadata")
        if (featureMetaData == null) {
            // not spock feature method
            return new ConstantExpression(methodNode.name)
        }
        Expression nameExpression = featureMetaData.getMember("name")
        if (!(nameExpression instanceof ConstantExpression)) {
            // not valid spock feature method
            return new ConstantExpression(methodNode.name)
        }
        Object originalMethodName = (nameExpression as ConstantExpression).value
        return new ConstantExpression(originalMethodName)
    }
    
    private ConstantExpression actualMethodSimpleNameExp(MethodNode methodNode) {
        return new ConstantExpression(methodNode.name)
    }
    
    private ConstantExpression argClassesStrExp(MethodNode methodNode) {
        String argClassesStr = TestMethod.argClassQualifiedNamesToArgClassesStr(
            GroovyASTUtils.getArgClassQualifiedNames(methodNode))
        return new ConstantExpression(argClassesStr)   
    }
    
    private ConstantExpression lineExp(Statement statement) {
        return new ConstantExpression(statement.lineNumber)
    }
    
    private ConstantExpression actualLineExp(Statement statement) {
        return new ConstantExpression(statement.lastLineNumber)
    }
    
    private VariableExpression throwableVarExp() {
        return new VariableExpression("e", new ClassNode(Throwable.class))
    }
    
    private Statement initializeStatement() {
        return hookStatement("initialize", null)
    }

    private Statement beforeMethodHookStatement(
        ConstantExpression classExp, ConstantExpression methodExp, ConstantExpression actualMethodExp) {
        return hookStatement("beforeMethodHook", [classExp, methodExp, actualMethodExp])
    }
        
    private Statement methodErrorHookStatement(
        ConstantExpression classExp, ConstantExpression methodExp, VariableExpression throwableExp) {
        return hookStatement("methodErrorHook", [classExp, methodExp, throwableExp])
    }
    
    private Statement afterMethodHookStatement(
        ConstantExpression classExp, ConstantExpression methodExp) {
        return hookStatement("afterMethodHook", [classExp, methodExp])
    }
    
    private Statement beforeCodeLineHookStatement(
        ConstantExpression classExp, ConstantExpression methodExp, ConstantExpression actualMethodExp, 
        ConstantExpression argExp, ConstantExpression lineExp, ConstantExpression actualLineExp) {
        return hookStatement("beforeCodeLineHook", 
            [classExp, methodExp, actualMethodExp, argExp, lineExp, actualLineExp]);
    }
    
    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        VariableExpression throwableExp = throwableVarExp()
        for (ASTNode astNode : astNodes) {
            if (astNode instanceof ModuleNode) {
                for (ClassNode classNode : (astNode as ModuleNode).classes) {
                    for (MethodNode methodNode : classNode.methods) {
                        if (methodNode.code instanceof BlockStatement) {
                            BlockStatement block = methodNode.code as BlockStatement
                            ConstantExpression classExp = classQualifiedNameExp(methodNode)
                            ConstantExpression methodExp = methodSimpleNameExp(methodNode)
                            ConstantExpression actualMethodExp = actualMethodSimpleNameExp(methodNode)
                            ConstantExpression argExp = argClassesStrExp(methodNode)
                                                    
                            BlockStatement tryStatement = new BlockStatement()
                            tryStatement.setVariableScope(block.variableScope)
                            tryStatement.addStatement(initializeStatement())
                            tryStatement.addStatement(beforeMethodHookStatement(classExp, methodExp, actualMethodExp))
                            for (Statement line : block.statements) {
                                tryStatement.addStatement(line)
                                if (line.lineNumber == -1 || line.lastLineNumber == -1) {
                                    // skip no line statement (maybe this line has been generated by the compiler)
                                    continue
                                }
                                ConstantExpression lineExp = lineExp(line)
                                ConstantExpression actualLineExp = actualLineExp(line)
                                tryStatement.addStatement(beforeCodeLineHookStatement(
                                    classExp, methodExp, actualMethodExp, argExp, lineExp, actualLineExp))
                            }
                            
                            BlockStatement catchBlockStatement = new BlockStatement()
                            catchBlockStatement.setVariableScope(block.variableScope)
                            catchBlockStatement.addStatements(
                                    [initializeStatement(),
                                        methodErrorHookStatement(classExp, methodExp, throwableExp),
                                        new ThrowStatement(throwableExp)])
                            Parameter expceptionParam = new Parameter(new ClassNode(Throwable.class), "e")
                            CatchStatement catchStatement = new CatchStatement(expceptionParam, catchBlockStatement)

                            BlockStatement finallyStatement = new BlockStatement()
                            finallyStatement.setVariableScope(block.variableScope)
                            finallyStatement.addStatements(
                                    [initializeStatement(), afterMethodHookStatement(classExp, methodExp)])

                            TryCatchStatement tryCatchStatement =
                            new TryCatchStatement(tryStatement, finallyStatement)
                            tryCatchStatement.addCatch(catchStatement)
                            
                            BlockStatement newBlock = new BlockStatement()
                            newBlock.setVariableScope(block.variableScope)
                            newBlock.addStatement(tryCatchStatement)
                            methodNode.setCode(newBlock)
                        }
                    }
                }
            }
        }
    }

}
