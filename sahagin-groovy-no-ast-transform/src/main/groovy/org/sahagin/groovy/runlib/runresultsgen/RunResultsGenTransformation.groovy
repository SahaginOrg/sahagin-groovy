package org.sahagin.groovy.runlib.runresultsgen

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ModuleNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.control.CompilePhase

// TODO how to disable sahagin without removing sahagin-groovy from the dependency
// TODO throw error if javaagent option is also specified
@GroovyASTTransformation(phase=CompilePhase.CONVERSION)
class RunResultsGenTransformation implements ASTTransformation {
    
    Statement createPrintlnStatement(str) {
      VariableExpression thisExp = new VariableExpression("this")
      ConstantExpression argConstant = new ConstantExpression(str)
      ArgumentListExpression args = new ArgumentListExpression()
      args.addExpression(argConstant)        
      MethodCallExpression methodCall = new MethodCallExpression(thisExp, "println", args)
      return new ExpressionStatement(methodCall)
    }

    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        println "call visit: " + sourceUnit.name
        for (ASTNode astNode : astNodes) {
            if (astNode instanceof ModuleNode) {
                for (ClassNode classNode : (astNode as ModuleNode).getClasses()) {
                    for (MethodNode methodNode : classNode.getMethods()) {
                        if (methodNode.getCode() instanceof BlockStatement) {
                            BlockStatement block = methodNode.getCode() as BlockStatement
                            List<Statement> statements = (methodNode.getCode() as BlockStatement).getStatements()
                            statements.add(statements.size() - 2, createPrintlnStatement("before last"));
                        }
                    }
                }
            }
        }
    }

}
