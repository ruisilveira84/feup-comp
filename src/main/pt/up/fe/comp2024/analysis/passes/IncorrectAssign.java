package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.Arrays;
import java.util.List;

public class IncorrectAssign extends AnalysisVisitor {
    private String currentMethod;

    private Boolean isStatic;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssign);
        addVisit("ArrayExpr",this::visitArrayExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        String estatic = method.get("isStatic");
        if(estatic.equals("true")){
            isStatic = true;
        }
        else{
            isStatic = false;
        }
        return null;
    }
    private Void visitArrayExpr(JmmNode arrayExpr, SymbolTable table){
             for(JmmNode child: arrayExpr.getChildren()){
                 //Basically, if not Integer, of if not a binaryExpr with +,*,-,/ -> We just let to check if the two elements of binaryexpr are valid to the IncompatibleTypes. (Lazy)
                 Type type;
                 if(child.getKind().equals("VarRefExpr")){
                     type = TypeUtils.getVarExprType(child,table);
                     if(type.getName().equals("int")){
                         break;
                     }
                     else{
                         var message = String.format("Incompatible cast in Assignment - Not an integer in Array.");
                         addReport(Report.newError(
                                 Stage.SEMANTIC,
                                 NodeUtils.getLine(arrayExpr),
                                 NodeUtils.getColumn(arrayExpr),
                                 message,
                                 null)
                         );
                         return null;
                     }
                 }


                 if(!child.getKind().equals("IntegerLiteral") || (child.getKind().equals("BinaryExpr") && (child.get("op").equals("&&") || child.get("op").equals("||")))){
                     var message = String.format("Incompatible cast in Assignment - Not an integer in Array.");
                     addReport(Report.newError(
                             Stage.SEMANTIC,
                             NodeUtils.getLine(arrayExpr),
                             NodeUtils.getColumn(arrayExpr),
                             message,
                             null)
                     );
                     return null;
                 }
             }
             return null;
         }
    private Void visitAssign(JmmNode assignExpr, SymbolTable table) {
        var first_member = assignExpr.getChild(0);
        var second_member = assignExpr.getChild(1);
        String firstType;
        String secondType;
        boolean first_is_array;
        boolean second_is_array;
        if (first_member.getKind().equals("VarRefExpr")) {
            var hash_attr = (first_member.get("name"));
            var type_first = table.getLocalVariables(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
            if(!type_first.isEmpty()) {
                firstType = type_first.get(0).getType().getName();
                first_is_array = type_first.get(0).getType().isArray();
            }
            else{
                type_first = table.getParameters(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
                if(!type_first.isEmpty()){
                    firstType = type_first.get(0).getType().getName();
                    first_is_array = type_first.get(0).getType().isArray();
                }
                else{
                    if(isStatic || currentMethod.equals("main")){
                        var message = String.format("Using field in static method");
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(assignExpr),
                                NodeUtils.getColumn(assignExpr),
                                message,
                                null)
                        );
                        return null;
                    }
                    type_first = table.getFields().stream().filter(child -> child.getName().equals(hash_attr)).toList();
                    firstType = type_first.get(0).getType().getName();
                    first_is_array = type_first.get(0).getType().isArray();
                }

            }
        }
        else if(first_member.getKind().equals("NewClassDeclaration")){
            firstType = first_member.get("name");
            first_is_array = false;
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
                if(!type_second.isEmpty()){

                    secondType = type_second.get(0).getType().getName();
                    second_is_array = type_second.get(0).getType().isArray();
                }
                else{
                    if(isStatic || currentMethod.equals("main")){
                        var message = String.format("Using field in static method");
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(assignExpr),
                                NodeUtils.getColumn(assignExpr),
                                message,
                                null)
                        );
                        return null;
                    }
                    type_second = table.getFields().stream().filter(child -> child.getName().equals(hash_attr)).toList();
                    secondType = type_second.get(0).getType().getName();
                    second_is_array = type_second.get(0).getType().isArray();
                }
            }
        }
        else if (second_member.getKind().equals("NewClassDeclaration")){
            secondType = second_member.get("name");
            second_is_array = false;
        }
        else{
            secondType = second_member.getKind();
            second_is_array = false;
        }

        //Check SuperClass

        var second_super_class = table.getSuper();
        if(!second_super_class.equals("") && second_super_class.equals(firstType)){
            return null;
        };
        //firstType.equals(table.getClassName()) &&
        //Check "this"
        if(!second_member.getChildren("Object").isEmpty()){
            if(isStatic || currentMethod.equals("main")){
                var message = String.format("Using 'this' in static method");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignExpr),
                        NodeUtils.getColumn(assignExpr),
                        message,
                        null)
                );
            }
            return null;
        }

        //Check if they are both imports
        var imports = table.getImports();
        String adj_first = "["+firstType+"]";
        String adj_second = "["+secondType+"]";
        boolean import_first = false;
        boolean import_second = false;
        for(String a: imports){
            if(adj_first.equals(a)){
                import_first = true;
            }
            if(adj_second.equals(a)){
                import_second = true;
            }
        }

        if(import_first && import_second){
            return null;
        };


        //If Array Declaration
        if(secondType.equals("ArrayDeclaration") && first_is_array){
            return null;
        }

        //Varargs
        if(second_member.getKind().equals("FuncCall")){
            var methodName = second_member.get("value");
            var currentNode = assignExpr;
            while(!currentNode.getKind().equals("ClassDecl")){
                currentNode = currentNode.getParent();
            }
            var funcList = currentNode.getChildren("MethodDecl").stream().filter(child->child.get("name").equals(methodName)).toList();
            if(funcList.isEmpty()){
                return null;
            }
            var func = funcList.get(0);
            var param_list = func.getChildren("Param");
            var param = func.getChild(0);
            var donothing = 1;
            var param_not_vararg = func.getChildren().size()-3; //Return + Vararg + type
            for(int i=1;i<param_list.size();i++){
                if(func.getChild(i).getChild(0).get("isVararg").equals("true")){
                    var message = String.format("Parameter other than first is varArg.");
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(assignExpr),
                            NodeUtils.getColumn(assignExpr),
                            message,
                            null)
                    );
                }
            }
            if(param.get("isVararg").equals("true")){
                var type = param.get("name");
                for(int i = param_not_vararg; i<second_member.getChildren().size();i++){
                    if(second_member.getChild(i).getKind().equals("Object") && second_member.getChild(i).get("name").equals("this")){
                        continue;
                    }
                    if(type.equals("int") && second_member.getChild(i).getKind().equals("IntegerLiteral")){
                        donothing++;
                    }
                    else if(type.equals("boolean") && !second_member.getChild(i).getKind().equals("Boolean")){
                        donothing++;
                    }
                    else if(second_member.getAttributes().contains("name")){
                            try{
                                var varName = second_member.getChild(i).get("name");
                                if(!type.equals(varName)) {
                                    donothing++;
                                }
                            }
                            catch (Exception e){
                                donothing++;
                            }
                    }
                    else{
                        var message = String.format("Incompatible type in vararg call.");
                                                addReport(Report.newError(
                                                        Stage.SEMANTIC,
                                                        NodeUtils.getLine(assignExpr),
                                                        NodeUtils.getColumn(assignExpr),
                                                        message,
                                                        null)
                                                );
                    }
                }
                return null;
            }
        }

        //Check Array Type
        /*if(secondType.equals("ArrayExpr") && first_is_array){
            for(JmmNode child: second_member.getChildren()){
                //Basically, if not Integer, of if not a binaryExpr with +,*,-,/ -> We just let to check if the two elements of binaryexpr are valid to the IncompatibleTypes. (Lazy)
                 if(!child.getKind().equals("IntegerLiteral") || (child.getKind().equals("BinaryExpr") && (child.get("op").equals("&&") || child.get("op").equals("||")))){
                     var message = String.format("Incompatible cast in Assignment.");
                     addReport(Report.newError(
                             Stage.SEMANTIC,
                             NodeUtils.getLine(assignExpr),
                             NodeUtils.getColumn(assignExpr),
                             message,
                             null)
                     );
                     return null;
                 }
            }
            return null;
        }*/

        if(firstType.equals("IntegerLiteral")){
            firstType ="int";
        }
        if(secondType.equals("IntegerLiteral")){
            secondType ="int";
        }

        //Binary Expr
        if(firstType.equals("BinaryExpr")){
            firstType = getTypeOfOp(first_member,table);
        }
        if(secondType.equals("BinaryExpr")){
            secondType = getTypeOfOp(second_member,table);
        }
        if(firstType.equals("FuncCall")){
            var methodName = first_member.get("value");
            var currentNode = first_member;
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
            first_is_array = func.getChild(0).get("isArray").equals("true");

        }
        if(secondType.equals("FuncCall")){
            var methodName = second_member.get("value");
            var currentNode = second_member;
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
            second_is_array = func.getChild(0).get("isArray").equals("true");

        }
        //We can say that only exists Array of Int
        if(secondType.equals("ArraySubscription")){
            secondType ="int";
        }

        //We can say that only exists Array of Int
        if(firstType.equals("ArraySubscription")){

            //Check if there isn't any funccall as first
            if(first_member.getChildren().size()>1 && first_member.getChild(0).getKind().equals("FuncCall")){
                var message = String.format("FuncCall on leftSide as Assignment.");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignExpr),
                        NodeUtils.getColumn(assignExpr),
                        message,
                        null)
                );
            }
            firstType ="int";
        }

        //We check the array later
        if(firstType.equals("ArrayExpr")){
            firstType = "int";
            first_is_array = true;
        }
        if(secondType.equals("ArrayExpr")){
            secondType="int";
            second_is_array = true;
        }

        //Length always returns an int
        if(secondType.equals("Length")){
            secondType = "int";
        }


        if(secondType.equals("Object")){
            secondType=table.getClassName();
        }

        //Allow array = varargs
        if(firstType.equals("int") && secondType.equals("int") && first_is_array && !second_is_array && second_member.getKind().equals("VarRefExpr")){
            int b = 1;

            //We know that varargs can only appear from parameters
            var type_second = table.getParameters(currentMethod).stream().filter(child -> child.getName().equals(second_member.get("name"))).toList();
            if(!type_second.isEmpty()){
                var currentNode = assignExpr;
                while(!currentNode.getKind().equals("MethodDecl")){
                    currentNode = currentNode.getParent();
                }
                //Get param
                var paramList = currentNode.getChildren("Param").stream().filter(child->child.get("name").equals(second_member.get("name"))).toList();
                if(!paramList.isEmpty()){
                    int c = 1;
                    String isVararg = paramList.get(0).getChild(0).get("isVararg");
                    //This  is valid
                    if(isVararg.equals("true")){
                        return null;
                    }
                }

            }

        }


        if(!firstType.toLowerCase().equals(secondType.toLowerCase()) || first_is_array!=second_is_array){
            // Create error report
            var message = String.format("Incompatible cast in Assignment.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignExpr),
                    NodeUtils.getColumn(assignExpr),
                    message,
                    null)
            );

        }
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
        String firstType;
        String secondType;
        boolean first_is_array;
        boolean second_is_array;
        if (first_member.getKind().equals("VarRefExpr")) {
            var hash_attr = (first_member.get("name"));
            var type_first = table.getLocalVariables(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
            if(!type_first.isEmpty()) {
                firstType = type_first.get(0).getType().getName();
                first_is_array = type_first.get(0).getType().isArray();
            }
            else{
                type_first = table.getParameters(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
                if(!type_first.isEmpty()){
                    firstType = type_first.get(0).getType().getName();
                    first_is_array = type_first.get(0).getType().isArray();
                }
                else{
                    if(isStatic || currentMethod.equals("main")){
                        var message = String.format("Using field in static method");
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(BinaryExpr),
                                NodeUtils.getColumn(BinaryExpr),
                                message,
                                null)
                        );
                        return null;
                    }
                    type_first = table.getFields().stream().filter(child -> child.getName().equals(hash_attr)).toList();
                    firstType = type_first.get(0).getType().getName();
                    first_is_array = type_first.get(0).getType().isArray();
                }

            }
        }
        else if(first_member.getKind().equals("NewClassDeclaration")){
            firstType = first_member.get("name");
            first_is_array = false;
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
                if(!type_second.isEmpty()){

                    secondType = type_second.get(0).getType().getName();
                    second_is_array = type_second.get(0).getType().isArray();
                }
                else{
                    if(isStatic || currentMethod.equals("main")){
                        var message = String.format("Using field in static method");
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(BinaryExpr),
                                NodeUtils.getColumn(BinaryExpr),
                                message,
                                null)
                        );
                        return null;
                    }
                    type_second = table.getFields().stream().filter(child -> child.getName().equals(hash_attr)).toList();
                    secondType = type_second.get(0).getType().getName();
                    second_is_array = type_second.get(0).getType().isArray();
                }
            }
        }
        else if (second_member.getKind().equals("NewClassDeclaration")){
            secondType = second_member.get("name");
            second_is_array = false;
        }
        else{
            secondType = second_member.getKind();
            second_is_array = false;
        }
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
        if(firstType.equals("IntegerLiteral")){
            firstType ="int";
        }
        if(secondType.equals("IntegerLiteral")){
            secondType ="int";
        }
        if(firstType.equals("BinaryExpr")){
            firstType=visitBinaryExpr(first_member,table);
        }
        if(secondType.equals("BinaryExpr")){
            secondType=visitBinaryExpr(second_member,table);
        }
        if(firstType.equals("ArraySubscription")){
            firstType ="int";
        }
        if(secondType.equals("ArraySubscription")){
            secondType ="int";
        }
        if(first_member.getKind().equals("VarRefExpr")){
            //We know that varargs can only appear from parameters
            var type_second = table.getParameters(currentMethod).stream().filter(child -> child.getName().equals(first_member.get("name"))).toList();
            if(!type_second.isEmpty()){
                var currentNode = BinaryExpr;
                while(!currentNode.getKind().equals("MethodDecl")){
                    currentNode = currentNode.getParent();
                }
                //Get param
                var paramList = currentNode.getChildren("Param").stream().filter(child->child.get("name").equals(first_member.get("name"))).toList();
                if(!paramList.isEmpty()){
                    int c = 1;
                    String isVararg = paramList.get(0).getChild(0).get("isVararg");
                    //This  is valid
                    if(isVararg.equals("true")){
                        first_is_array = true;
                    }
                }

            }
        }
        if(second_member.getKind().equals("VarRefExpr")){
            //We know that varargs can only appear from parameters
            var type_second = table.getParameters(currentMethod).stream().filter(child -> child.getName().equals(second_member.get("name"))).toList();
            if(!type_second.isEmpty()){
                var currentNode = BinaryExpr;
                while(!currentNode.getKind().equals("MethodDecl")){
                    currentNode = currentNode.getParent();
                }
                //Get param
                var paramList = currentNode.getChildren("Param").stream().filter(child->child.get("name").equals(second_member.get("name"))).toList();
                if(!paramList.isEmpty()){
                    int c = 1;
                    String isVararg = paramList.get(0).getChild(0).get("isVararg");
                    //This  is valid
                    if(isVararg.equals("true")){
                        second_is_array = true;
                    }
                }

            }
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
}
