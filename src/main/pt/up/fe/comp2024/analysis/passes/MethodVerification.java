package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

public class MethodVerification extends AnalysisVisitor {
    private String currentMethod;
    private boolean isStatic;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.METHOD_DECL,this::visitMethodVerify);
        addVisit("FuncCall",this::visitFuncCall);
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
    private Void visitMethodVerify(JmmNode method, SymbolTable table) {
        int b = 1;
        currentMethod = method.get("name");
        String estatic = method.get("isStatic");
        if(estatic.equals("true")){
            isStatic = true;
        }
        else{
            isStatic = false;
        }

        //Check if there are two methods with same name
        if(table.getMethods().stream().filter(child->child.equals(currentMethod)).toList().size()>1){
            var message = String.format("Method already defined!");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }

        //Check if two or more parameters with same name
        var param_list_repeat = table.getParameters(currentMethod);
        int g = 1;
        for (Symbol param_repeat: param_list_repeat){
            if(param_list_repeat.stream().filter(child->child.getName().equals(param_repeat.getName())).toList().size()>1){
                var message = String.format("Param repeated!");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }
        }

        var params_for_var_args = method.getChildren("Param");

        //Check If There are two or more arguments varargs
        if(params_for_var_args.stream().filter(child->child.getChild(0).get("isVararg").equals("true")).toList().size()>1){
            var message = String.format("More than two varargs!");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }

        //Check if there isn't any vararg at any position that isn't the last
        for(int i = 0;i<(params_for_var_args.size() - 1);i++){
            if(params_for_var_args.get(i).getChild(0).get("isVararg").equals("true")){
                var message = String.format("Vararg at an invalid position at parameter!");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }
        }

        //if(param_list_repeat.stream().filter(child->child.getName().equals(param_repeat.getName())).toList().size()>1)


        //Check Main
        if(currentMethod.equals("main")){

            //More than One parameter is invalid
            var param_list = method.getChildren("Param");
            if(param_list.size()>1){
                var message = String.format("More than one parameter in main");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }

            //Not static is invalid
            if(!isStatic){
                var message = String.format("Method main isn't static!");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }

            //Param needs to be string
            var param = param_list.get(0);
            Type type_class = TypeUtils.getVarExprType(param,table);
            var type = type_class.getName();
            if(!type.equals("String")){
                var message = String.format("Param in method main is not string!");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }


            //Param needs to be array too
            String array = param.getChild(0).get("isArray");
            if(array.equals("false")){
                var message = String.format("Param in method main isn't Array.");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }

            //TODO: SE NAO PASSAR EM TUDO DA MAIN, verificar se tem q se chamar args


        }

        //Only main can be static
        if(!currentMethod.equals("main") && isStatic){
            var message = String.format("Method not main is Static!");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }

        //More than one return isn't valid
        if(method.getChildren("ReturnStmt").size()>1){
            var message = String.format("More than one return!");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }


        //Return in Void isn't acceptable too;
        var a = method.getChildren("ReturnStmt");
        if(method.getChild(0).get("name").equals("void") && (!method.getChildren("ReturnStmt").isEmpty())){
            var message = String.format("Return on void method!");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }

        //Just skip if method is void
        if(method.getChild(0).get("name").equals("void")){
            return null;
        }

        //Check if return is last!
        /*if(!method.getChildren().getLast().getKind().equals("ReturnStmt") && !method.getChild(0).get("name").equals("void")){
            var message = String.format("Return isn't the last statement!");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }*/

        //Check if return is there! (Ofc if not void!)
        var retList = method.getChildren("ReturnStmt");
        if(retList.isEmpty() && !method.getChild(0).get("name").equals("void")){
            var message = String.format("No Return Detected!");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }
        //Check if returns are compatible
        var ret = retList.get(0);
        var arg = ret.getChild(0);
        if(arg.getKind().equals("Object")){

        }
        boolean is_array =false;
        String type_arg = "";
        if(arg.getKind().equals("IntegerLiteral")){
            type_arg = "int";
        }else if(arg.getKind().equals("Boolean")){
            type_arg = "boolean";
        }
        else if(arg.getKind().equals("BinaryExpr")) {
            String op = arg.get("op");
            if (op.equals("&&") || op.equals("||")) {
                type_arg = "boolean";
            } else {
                type_arg = "int";
            }
        }
        else if(arg.getKind().equals("FuncCall")){
            var methodName = arg.get("value");
            var currentNode = arg;
            while(!currentNode.getKind().equals("ClassDecl")){
                currentNode = currentNode.getParent();
            }
            var funcList = currentNode.getChildren("MethodDecl").stream().filter(child->child.get("name").equals(methodName)).toList();
            if(funcList.isEmpty()){
                return null;
            }
            var func = funcList.get(0);
            var type_ret = func.getChild(0).get("name");
            type_arg = type_ret;
        }
        else{
            var tipe = TypeUtils.getVarExprType(arg,table);
            type_arg = tipe.getName();
            is_array = tipe.isArray();

        }
        //Check if we are returning an array to something that ISNT an array
        if(is_array && method.getChild(0).get("isArray").equals("false")){
            var message = String.format("Incompatible Return and Type - Array Return on non array");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }
        if(!is_array && method.getChild(0).get("isArray").equals("true")){
            var message = String.format("Incompatible Return and Type - Non Array Return on array");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }

        //General verification
        if(!type_arg.equals(method.getChild(0).get("name"))){
            var message = String.format("Incompatible Return and Type");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }





        return null;
    }
    private Void visitFuncCall(JmmNode call, SymbolTable table) {
        String type = null;
        boolean type_is_array;
        /*if(call.getParent().getParent().getChild(0).getKind().equals("ArraySubscription")){
            var message = String.format("Func on left side!");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(call),
                    NodeUtils.getColumn(call),
                    message,
                    null)
            );
            return null;
        }*/

        //Test IF IMPORT
        if(call.getChild(0).hasAttribute("name")) {
            var test_if_import = (call.getChild(0).get("name"));
            for (String child : table.getImports()) {
                String child_sub = child.substring(1, child.length() - 1);
                if (child_sub.equals(test_if_import)) {
                    return null;
                }
            }
            ;
        }
        if(!call.getChildren("Object").isEmpty()){
            if((currentMethod.equals("main")&&isStatic) || isStatic ){
                var message = String.format("Using this in main or static method");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(call),
                        NodeUtils.getColumn(call),
                        message,
                        null)
                );
                return null;
            }
            //Se o metodo nao é main, nem estatico, temos de verificar se o método existe;

        }

        if(call.getChild(0).getKind().equals("VarRefExpr")){
            var hash_attr = (call.getChild(0).get("name"));
            var type_second = table.getLocalVariables(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
            if(!type_second.isEmpty()) {
                type = type_second.get(0).getType().getName();
                type_is_array = type_second.get(0).getType().isArray();
            }
            else{
                type_second = table.getParameters(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
                type = type_second.get(0).getType().getName();
                type_is_array = type_second.get(0).getType().isArray();
            }
        }

        //If name coincides with class decl, just check if there is a method with the same name
        var currentNode = call;
        while(!currentNode.getKind().equals("ClassDecl")){
            currentNode = currentNode.getParent();
        }
        var funcList = currentNode.getChildren("MethodDecl").stream().filter(child->child.get("name").equals(call.get("value"))).toList();
        //If name coincides with class decl, just check if there is a method with the same name
        //Second condition is to cover the this case!
        if(currentNode.get("name").equals(type) || !call.getChildren("Object").isEmpty()){
            funcList = currentNode.getChildren("MethodDecl").stream().filter(child->child.get("name").equals(call.get("value"))).toList();
            //Although we didn't found any function, it can be part of the extend. And there is something we can't check.
            //No Extend, It is undeclared.
            if(funcList.isEmpty() && !currentNode.hasAttribute("extendname")){
                var message = String.format("Undeclared method!");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(call),
                        NodeUtils.getColumn(call),
                        message,
                        null)
                );
                return null;
            } else if (currentNode.hasAttribute("extendname")) {
                //Check imports
                String extend = currentNode.get("extendname");
                for (String child: table.getImports()){
                    String child_sub = child.substring( 1, child.length() - 1 );
                    if(child_sub.equals(extend)){
                        return null;
                    }
                };
                var message = String.format("Undeclared method!");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(call),
                        NodeUtils.getColumn(call),
                        message,
                        null)
                );
                return null;
            }else if(!funcList.isEmpty()) {
                         var func = funcList.get(0);

                         // Verify if parameters are well defined
                         for(var child : func.getChildren("Param")) {
                             String param_type = child.getChild(0).get("name");

                             // Int, Boolean and String are okay
                             if(param_type.equals("int")||param_type.equals("boolean")||param_type.equals("String")){
                                 break;
                             }

                             // Void can't be parameter
                             if(param_type.equals("void")){
                                 var message = String.format("Void as a parameter isn't accepted!");
                                 addReport(Report.newError(
                                         Stage.SEMANTIC,
                                         NodeUtils.getLine(call),
                                         NodeUtils.getColumn(call),
                                         message,
                                         null)
                                 );
                                 return null;
                             }

                             //Ok now we have the customs, it can be the proper class or extendclass
                             if(param_type.equals(currentNode.get("name")) || param_type.equals(table.getSuper())){
                                 break;
                             }

                             //Now, verify the imports
                             String extend = param_type;
                             int breakzao = 0;
                             for (String child_import: table.getImports()){
                                 String child_sub;
                                 if (child_import.contains(",")){
                                     child_sub = child_import.substring( child_import.lastIndexOf(",")+2, child_import.length() - 1 );
                                 }
                                 else{
                                     child_sub = child_import.substring( 1, child_import.length() - 1 );
                                 }

                                 if(child_sub.equals(extend)){
                                     breakzao = 1;
                                     break;
                                 }

                             };
                             if (breakzao == 1){
                                 break;
                             }
                             //We just verified everything, so, if it doesn't match anything, it isn't possible.
                             var message = String.format("Not recognizable type as a parameter isn't accepted!");
                             addReport(Report.newError(
                                     Stage.SEMANTIC,
                                     NodeUtils.getLine(call),
                                     NodeUtils.getColumn(call),
                                     message,
                                     null)
                             );
                             return null;

                         }

                         // We let varargs to another time
                         if(func.getChild(0).get("isVararg").equals("false")){
                             //If has point, has one more child, and we need to know to check if the call is ok
                             int remove = 0;
                             if(call.get("hasPoint").equals("true")){
                                 remove = 1;
                             }

                             //Varargs can change the idea of the param num check
                             if(func.getChildren("Param").stream().filter(child->child.getChild(0).get("isVararg").equals("true")).toList().size()>0){
                                 //Check number of arguments is less to parameters
                                 if ((func.getChildren("Param").size() > (call.getChildren().size() - remove))) {
                                     var message = String.format("Num of arguments to a call doesn't match!");
                                     addReport(Report.newError(
                                             Stage.SEMANTIC,
                                             NodeUtils.getLine(call),
                                             NodeUtils.getColumn(call),
                                             message,
                                             null)
                                     );
                                     return null;
                                 }
                             }
                             else {

                                 //Check number of arguments is equal to parameters
                                 if ((func.getChildren("Param").size() != (call.getChildren().size() - remove))) {
                                     var message = String.format("Num of arguments to a call doesn't match!");
                                     addReport(Report.newError(
                                             Stage.SEMANTIC,
                                             NodeUtils.getLine(call),
                                             NodeUtils.getColumn(call),
                                             message,
                                             null)
                                     );
                                     return null;
                                 }
                             }

                             //Now, actually check the Params
                             int i = 0;
                             for(var child : func.getChildren("Param")){

                                 //First, treat each argument
                                 if(i>=call.getChildren().size()){
                                     break;
                                 }
                                 var arg = call.getChild(i);
                                 if(call.get("hasPoint").equals("true") && i == 0){
                                     i++;
                                     if(i>=call.getChildren().size()){
                                         break;
                                     }
                                     arg = call.getChild(i);
                                 }
                                 if(arg.getKind().equals("Object")){
                                     i++;
                                     if(i>=call.getChildren().size()){
                                         break;
                                     }
                                     arg = call.getChild(i);

                                 }
                                 boolean isArray = false;
                                 String type_arg = "";
                                 if(arg.getKind().equals("IntegerLiteral")){
                                     type_arg = "int";
                                 }else if(arg.getKind().equals("Boolean")){
                                     type_arg = "boolean";
                                 }
                                 else if(arg.getKind().equals("BinaryExpr")) {
                                     String op = arg.get("op");
                                     if (op.equals("&&") || op.equals("||")) {
                                         type_arg = "boolean";
                                     } else {
                                         type_arg = "int";
                                     }
                                 }
                                 else{
                                     var tipe = TypeUtils.getVarExprType(arg,table);
                                     type_arg = tipe.getName();
                                     isArray = tipe.isArray();
                                     }
                                 var b = child.getChild(0).get("isVararg");
                                 if(isArray && child.getChild(0).get("isVararg").equals("true") ){
                                     i++;
                                     break;
                                 }
                                 if((isArray && child.getChild(0).get("isArray").equals("false"))){
                                     var message = String.format("Incompatible Param and Type - Array Param on non array");
                                     addReport(Report.newError(
                                             Stage.SEMANTIC,
                                             NodeUtils.getLine(child),
                                             NodeUtils.getColumn(child),
                                             message,
                                             null)
                                     );
                                     return null;
                                 }
                                 if(!isArray && child.getChild(0).get("isArray").equals("true")) {
                                     var message = String.format("Incompatible Param and Type - Non Array Param on array");
                                     addReport(Report.newError(
                                             Stage.SEMANTIC,
                                             NodeUtils.getLine(child),
                                             NodeUtils.getColumn(child),
                                             message,
                                             null)
                                     );
                                     return null;
                                 }
                                 if(!type_arg.equals(child.getChild(0).get("name"))){
                                     var message = String.format("Incompatible argument to a method parameter!");
                                     addReport(Report.newError(
                                             Stage.SEMANTIC,
                                             NodeUtils.getLine(call),
                                             NodeUtils.getColumn(call),
                                             message,
                                             null)
                                     );
                                     return null;
                                 }
                                 i++;
                             }
                         }
        }


        }
        else{

            //Verify if it is an import
            String extend = type;
            for (String child: table.getImports()){
                String child_sub = child.substring( 1, child.length() - 1 );
                if(child_sub.equals(extend)){
                    return null;
                }
            };
        }

        int b = 1;
        return null;
    }

}
