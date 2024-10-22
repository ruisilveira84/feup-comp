package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

public class IncorrectStatements extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IF_ELSE_STMT, this::visitIfElse);
        addVisit("WhileStmt",this::visitIfElse);
    }
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }
    private String getTypeOfOp(JmmNode binaryOp, SymbolTable table){
        String type = visitBinaryExpr(binaryOp,table);
        return type;
    }
    private String visitBinaryExpr(JmmNode BinaryExpr, SymbolTable table) {
        int a = 1;
        var first_member = BinaryExpr.getChild(0);
        var second_member = BinaryExpr.getChild(1);
        String firstType="";
        String secondType="";
        boolean first_is_array =false;
        boolean second_is_array =false;
        if (first_member.getKind().equals("VarRefExpr")) {
            var hash_attr = (first_member.get("name"));
            var type_first = table.getLocalVariables(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
            if(!type_first.isEmpty()) {
                firstType = type_first.get(0).getType().getName();
                first_is_array = type_first.get(0).getType().isArray();
            }
            else{ //Is Parameter
                type_first = table.getParameters(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
                if(!type_first.isEmpty()) { //Is Field
                    firstType = type_first.get(0).getType().getName();
                    first_is_array = type_first.get(0).getType().isArray();
                }
                else{
                    type_first = table.getFields().stream().filter(child -> child.getName().equals(hash_attr)).toList();
                    if(!type_first.isEmpty()){
                        firstType = type_first.get(0).getType().getName();
                        first_is_array = type_first.get(0).getType().isArray();
                    }
                }
            }
        }
        else{
            firstType = first_member.getKind();
            first_is_array = false;
        }
        if (second_member.getKind().equals("VarRefExpr")) {
            var hash_attr = (second_member.get("name"));
            var type_second = table.getLocalVariables(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
            if(!type_second.isEmpty()) {
                secondType = type_second.get(0).getType().getName();
                second_is_array = type_second.get(0).getType().isArray();
            }
            else{
                type_second = table.getParameters(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
                if(!type_second.isEmpty()) { //Is Field
                    secondType = type_second.get(0).getType().getName();
                    second_is_array = type_second.get(0).getType().isArray();
                }
                else{
                    type_second = table.getFields().stream().filter(child -> child.getName().equals(hash_attr)).toList();
                    if(!type_second.isEmpty()){
                        secondType = type_second.get(0).getType().getName();
                        second_is_array = type_second.get(0).getType().isArray();
                    }
                }
            }
        }
        else{
            secondType = second_member.getKind();
            second_is_array = false;
        }
        if(firstType.equals("IntegerLiteral")){
            firstType ="int";
        }
        if(secondType.equals("IntegerLiteral")){
            secondType ="int";
        }
        //boolean firstArray = first_member.get()
        if(!firstType.equals(secondType) || first_is_array!=second_is_array){
            // Create error report
            var message = String.format("Incompatible assign.");

            var rep = Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(BinaryExpr),
                    NodeUtils.getColumn(BinaryExpr),
                    message,
                    null);
            addReport(rep);

        }
        return firstType;
    }

    private Void visitIfElse(JmmNode IfElse, SymbolTable table) {
        var i = 1;
        var test = IfElse.getChild(0).getKind();
        if(IfElse.getChild(0).getKind().equals("IntegerLiteral")){
            var message = String.format("Condition is INT!.");

            var rep = Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(IfElse),
                    NodeUtils.getColumn(IfElse),
                    message,
                    null);
            addReport(rep);
        }
        else if(IfElse.getChild(0).getKind().equals("Boolean")){
            return null;
        }
        else if(IfElse.getChild(0).getKind().equals("VarRefExpr")){
            var tipe = TypeUtils.getVarExprType(IfElse.getChild(0),table);
            var tipo = tipe.getName();
            if(tipo.equals("IntegerLiteral")){
                var message = String.format("Condition is INT!.");

                var rep = Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(IfElse),
                        NodeUtils.getColumn(IfElse),
                        message,
                        null);
                addReport(rep);
            }
            else if(tipo.equals("boolean")){
                return null;
            }
            else{
                var message = String.format("Condition is INT!.");

                var rep = Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(IfElse),
                        NodeUtils.getColumn(IfElse),
                        message,
                        null);
                addReport(rep);
            }
        }
        else if(IfElse.getChild(0).getKind().equals("BinaryExpr")) {
            String getCond = getTypeOfOp(IfElse.getChild(0),table);
            if(getCond.equals("IntegerLiteral") && !(IfElse.getChild(0).get("op").equals("<")||IfElse.getChild(0).get("op").equals(">"))){
                var message = String.format("Condition is INT!.");

                var rep = Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(IfElse),
                        NodeUtils.getColumn(IfElse),
                        message,
                        null);
                addReport(rep);
            }
            else if(getCond.equals("Boolean")){
                return null;
            }
            else if(getCond.equals("int") && !(IfElse.getChild(0).get("op").equals("<")||IfElse.getChild(0).get("op").equals(">"))){
                var message = String.format("Condition is INT!.");

                var rep = Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(IfElse),
                        NodeUtils.getColumn(IfElse),
                        message,
                        null);
                addReport(rep);
            }
        }
        else{
            var message = String.format("Condition is INT!.");

            var rep = Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(IfElse),
                    NodeUtils.getColumn(IfElse),
                    message,
                    null);
            addReport(rep);
        }
        return null;
    }
}
