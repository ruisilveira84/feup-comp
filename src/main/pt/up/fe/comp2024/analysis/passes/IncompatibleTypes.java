package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.HashMap;

public class IncompatibleTypes extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit("ArraySubscription",this::visitArraySubscription);
    }
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }
    private String getTypeofIndex(JmmNode BinaryExpr, SymbolTable table) {
        int a = 1;
        var first_member = BinaryExpr.getChild(0);
        var second_member = BinaryExpr.getChild(1);
        String firstType ="";
        String secondType = "";
        boolean first_is_array = false;
        boolean second_is_array = false;
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
    private Void visitArraySubscription(JmmNode arraySubscription, SymbolTable table){
        var index =arraySubscription.getChild(1);
        var indexType ="";
        if (index.getKind().equals("VarRefExpr")) {
            var hash_attr = (index.get("name"));
            var type_first = table.getLocalVariables(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
            indexType = type_first.get(0).getType().getName();
        }
        else if(index.getKind().equals("BinaryExpr")){
            indexType = getTypeofIndex(index,table);
        }
        else{
            indexType = index.getKind();
        }

        //check if first is array
        var array = arraySubscription.getChild(0);
        if (array.getKind().equals("VarRefExpr")) {
            var hash_attr = (array.get("name"));
            var type_second = table.getLocalVariables(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();

            if(!type_second.isEmpty()) {
                Boolean isArray = type_second.get(0).getType().isArray();
                if(!isArray){
                    var message = String.format("Trying to do Array Access in a not-array.");

                    var rep = Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(index),
                            NodeUtils.getColumn(index),
                            message,
                            null);
                    addReport(rep);
                    return null;
                }
            }
            else{
                type_second = table.getParameters(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
                var currentNode = arraySubscription;
                while(!currentNode.getKind().equals("ClassDecl")){
                    currentNode = currentNode.getParent();
                }
                var funcList = currentNode.getChildren("MethodDecl").stream().filter(child->child.get("name").equals(currentMethod)).toList();
                var func = funcList.get(0);
                boolean isMethodVarArg = func.getChild(func.getChildren().size()-2).getChild(0).hasAttribute("isVararg");
                if(isMethodVarArg){
                    return null;
                }
                Boolean isArray = type_second.get(0).getType().isArray();

                //We know that varargs can only appear from parameters
                //lets assume vararg is false
                String isVararg = "false";
                if(!type_second.isEmpty()){
                    currentNode = arraySubscription;
                    while(!currentNode.getKind().equals("MethodDecl")){
                        currentNode = currentNode.getParent();
                    }
                    //Get param
                    var paramList = currentNode.getChildren("Param").stream().filter(child->child.get("name").equals(hash_attr)).toList();
                    if(!paramList.isEmpty()){
                        int c = 1;
                        isVararg = paramList.get(0).getChild(0).get("isVararg");

                    }

                }
                //Special case if second is vararg
                if(!isArray && isVararg.equals("true")){
                    return null;
                }
                if(!isArray){
                    var message = String.format("Trying to do Array Access in a not-array.");

                    var rep = Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(index),
                            NodeUtils.getColumn(index),
                            message,
                            null);
                    addReport(rep);
                    return null;
                }
            }
        }


        if(indexType.equals("IntegerLiteral") || indexType.equals("int")) {
            return null;
        }
        else{
            var message = String.format("Index is not INT.");

            var rep = Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(index),
                    NodeUtils.getColumn(index),
                    message,
                    null);
            addReport(rep);
        }
        return null;

    }

    public Void visitBinaryExpr(JmmNode BinaryExpr, SymbolTable table) {
        int a = 1;
        var first_member = BinaryExpr.getChild(0);
        var second_member = BinaryExpr.getChild(1);
        String firstType = "";
        String secondType ="";
        boolean first_is_array = false;
        boolean second_is_array = false;
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
        String op = BinaryExpr.get("op");
        if(firstType.equals("FuncCall")){
            var methodName = first_member.get("value");
            var currentNode = BinaryExpr;
            while(!currentNode.getKind().equals("ClassDecl")){
                currentNode = currentNode.getParent();
            }
            var funcList = currentNode.getChildren("MethodDecl").stream().filter(child->child.get("name").equals(methodName)).toList();
            if(funcList.isEmpty()){
                return null;
            }
            var func = funcList.get(0);
            var type_ret = func.getChild(0).get("name");
            firstType = type_ret;

        }
        if(secondType.equals("FuncCall")){
            var methodName = second_member.get("value");
            var currentNode = BinaryExpr;
            while(!currentNode.getKind().equals("ClassDecl")){
                currentNode = currentNode.getParent();
            }
            var funcList = currentNode.getChildren("MethodDecl").stream().filter(child->child.get("name").equals(methodName)).toList();
            if(funcList.isEmpty()){
                return null;
            }
            var func = funcList.get(0);
            var type_ret = func.getChild(0).get("name");
            secondType = type_ret;

        }
        if((op.equals("&&")||op.equals("||")) && (firstType.equals("int")||firstType.equals("IntegerLiteral"))){
            var message = String.format("Incompatible assign.Int expr to bool expr");

            var rep = Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(BinaryExpr),
                    NodeUtils.getColumn(BinaryExpr),
                    message,
                    null);
            addReport(rep);
            return null;
        }
        else if((op.equals("*")||op.equals("+")||op.equals("/")||op.equals("-")||op.equals("<")||op.equals(">")) && firstType.equals("boolean")){
            var message = String.format("Incompatible assign. Bool Expr in Int expr");

            var rep = Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(BinaryExpr),
                    NodeUtils.getColumn(BinaryExpr),
                    message,
                    null);
            addReport(rep);
            return null;
        }
        if(firstType.equals("BinaryExpr")||secondType.equals("BinaryExpr")){
            return null;
        }
        if(firstType.equals("IntegerLiteral")){
            firstType ="int";
        }
        if(secondType.equals("IntegerLiteral")){
            secondType ="int";
        }
        if(secondType.equals("ArraySubscription")){
            secondType ="int";
        }
        if(firstType.equals("ArraySubscription")){
            firstType ="int";
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
        return null;
    }
}
