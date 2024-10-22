package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit("VarDecl",this::visitVarDeclExpr);
        addVisit("ImportStmt",this::visitImportStmt);
    }

    private Void visitImportStmt(JmmNode imprt, SymbolTable table) {
        var imports = table.getImports();

        //Check if there is two equal imports
        if(table.getImports().stream().filter(child->child.equals(imprt.get("name").toString())).toList().size()>1){
            var message = String.format("Import duplicated.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(imprt),
                    NodeUtils.getColumn(imprt),
                    message,
                    null)
            );
            return null;
        }//Check if there is two equal Import Classes
        if(table.getImports().stream().filter(child->child.substring(child.lastIndexOf(",")+2,child.length() - 1).equals(imprt.get("ID"))).toList().size()>1){
            var message = String.format("Import Class Duplicated.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(imprt),
                    NodeUtils.getColumn(imprt),
                    message,
                    null)
            );
            return null;
        }

        return null;

    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }
    private Void visitVarDeclExpr(JmmNode varDecl, SymbolTable table) {
        int d = 4;
        var name = varDecl.get("name");

        // Fields
        if(varDecl.getParent().getKind().equals("ClassDecl")){
            //Check field with same name FOR FIELDS
            if (table.getFields().stream().filter(child -> child.getName().equals(name)).toList().size() > 1) {
                var message = String.format("Variable '%s' already defined in field.", name);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varDecl),
                        NodeUtils.getColumn(varDecl),
                        message,
                        null)
                );
                return null;
            }

            //Check if field who is varargs
            if(varDecl.getChild(0).get("isVararg").equals("true")){
                var message = String.format("VarArg Field");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varDecl),
                        NodeUtils.getColumn(varDecl),
                        message,
                        null)
                );
                return null;
            }

        }
        else {
            //Check if local is vararg
            if(varDecl.getChild(0).get("isVararg").equals("true")){
                var message = String.format("VarArg Local");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varDecl),
                        NodeUtils.getColumn(varDecl),
                        message,
                        null)
                );
                return null;
            }

            //Check if there is a local in same method;
            if (table.getLocalVariables(currentMethod).stream().filter(child -> child.getName().equals(name)).toList().size() > 1) {
                var message = String.format("Variable '%s' already exists in local.", name);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varDecl),
                        NodeUtils.getColumn(varDecl),
                        message,
                        null)
                );
                return null;
            }

            //Check if there is already a parameter in the same method
            if (!table.getParameters(currentMethod).stream().filter(child -> child.getName().equals(name)).toList().isEmpty()) {
                var message = String.format("Variable '%s' already defined in parameters.", name);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varDecl),
                        NodeUtils.getColumn(varDecl),
                        message,
                        null)
                );
                return null;
            }
        }
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("name");

        // Var is a field, return
        if (table.getFields().stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        //Var is a import (ig?),return
        for(String importe: table.getImports()){
            var importName_withoutList = importe.substring(1, importe.length() - 1);
            if(importName_withoutList.equals(varRefName)){
                return null;
            }
        }

        // Var is a parameter, return
        if (table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a declared variable, return
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
            return null;
        }

        // Create error report
        var message = String.format("Variable '%s' does not exist.", varRefName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr),
                message,
                null)
        );

        return null;
    }


}
