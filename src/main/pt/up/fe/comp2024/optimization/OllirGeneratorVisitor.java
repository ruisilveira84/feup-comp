package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        //addVisit(VAR_DECL, this::visitFields);
        addVisit(IMPORT_DECL, this::visitImports);
        addVisit("ExprStmt", this::visitExprStmt);
        //addVisit("FuncCall",this::visitFuncCall);

        setDefaultVisit(this::defaultVisit);
    }

    /*private String visitFuncCall(JmmNode jmmNode, Void unused) {

    }*/


    private String visitAssignStmt(JmmNode node, Void unused) {

        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);


        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }

    private String visitFields(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder(".field public ");

        // name
        var name = node.get("name");
        code.append(name);

        // type
        var retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append(retType);
        code.append(";\n");

        return code.toString();
    }

    private String visitImports(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder("import ");

        // name
        var name = node.get("name");
        var importName_withoutList =name.substring(1, name.length() - 1);
        var importName_split = importName_withoutList.split(", ");
        var importName = String.join(".", importName_split);
        code.append(importName);
        code.append(";\n");

        return code.toString();
    }

    private String visitExprStmt(JmmNode node, Void unused){
        StringBuilder code = new StringBuilder();

        var retType = "";
        var invokeType = "";

        var nodeChild = node.getChild(0);
        var nodeChildValue = nodeChild.get("value");

        var importList = table.getImports();
        var methodList = table.getMethods();
        List<String> codeList = new ArrayList<>();
        //var parameters = node.getChildren();
        //var firstParam = parameters.get(0);
        codeList.add("(");

        boolean isStatic = false;

        var varRefs = nodeChild.getChildren();
        var target = varRefs.get(0);
        var inside = "";
        if(varRefs.size()>1){inside = varRefs.get(1).get("name");}

        for(int i = 0; i < importList.size(); i++){
            var importName = importList.get(i).substring(1, importList.get(i).length() - 1);
            if(importName.equals(target.get("name"))){
                codeList.add(target.get("name"));
                codeList.add(", \"");
                codeList.add(nodeChildValue);
                codeList.add("\"");
                if(varRefs.size()>1){ codeList.add(", ");
                codeList.add(inside);
                codeList.add(OptUtils.toOllirType(TypeUtils.getVarExprType(varRefs.get(1), table).getName()));}
                isStatic = true;
            }
        }
        if(!isStatic) {
            for(int i = 0; i < methodList.size(); i++){
                if(methodList.get(i).equals(target.get("name"))){
                    retType = OptUtils.toOllirType(table.getReturnType(methodList.get(i)).getName());
                    codeList.add(nodeChild.get("value"));
                    codeList.add(".");
                    codeList.add(table.getClassName());
                    codeList.add(", ");
                    codeList.add("nodeChildValue");
                }
            }
        }
        codeList.add(")");

        if(isStatic){
            invokeType = "invokestatic";
            code.append(invokeType);
            for (var element: codeList){
                code.append(element);
            }
            code.append(".V");
        }
        else{
            invokeType= "invokevirtual";
            code.append(invokeType);
            for (var element: codeList){
                code.append(element);
            }
            code.append(retType);
        }

        code.append(";");
        return code.toString();

        /*
        var child = node.getChild(0);
        String value = "";
        if(child.hasAttribute("value")) {
            value = child.get("value");
        } else{
            value = child.get("name");
        }
        var child_left = child.getChild(0).get("name");

        if(child_left.equals("this")){
            code.append("invokevirtual(");
            //getParent Class
            var currentNode = node;
            while(!currentNode.getKind().equals("ClassDecl")){
                currentNode = currentNode.getParent();
            }
            var funcList = currentNode.getChildren("MethodDecl").stream().filter(child1->child1.get("name").equals(child_left)).toList();
            if(!funcList.isEmpty()){
                var func = funcList.get(0); // node of the corresponding method
                retType = OptUtils.toOllirType(func.getChild(0).get("name"));
            }

        }
        else{
            code.append("invokestatic(");
            retType = ".V";
        }

        code.append(child_left);
        code.append(", \"");
        code.append(value);

        var number_of_children = child.getNumChildren();
        var index_children = 0;
        if(number_of_children>=2){
            code.append("\"");
            for (int i = 1;i<number_of_children;i++) {
                code.append(", ");
                code.append(child.getChild(i).get("name"));
                code.append(OptUtils.toOllirType(TypeUtils.getVarExprType(child.getChild(i), table).getName()));
            }
        }
        else{
            code.append("\"");
        }

        code.append(")");
        code.append(retType);
        code.append(";\n");

        return code.toString();
         */
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        boolean isStatic = NodeUtils.getBooleanAttribute(node, "isStatic", "false");

        if(isStatic) {
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        int count_param = 0;
        // Iterate over the children of the node
        for (var child : node.getChildren()) {
            // Check if the child has the PARAM kind
            if (child.getKind().equals("Param")) {
                count_param++;
            }
        }


        //param
        code.append("(");
        for (int i = 1; i <= count_param; i++) {
            var paramCode = visit(node.getJmmChild(i));
            code.append(paramCode);
            if(i != count_param){
                code.append(", ");
            }
        }
        code.append(")");

        // type
        var retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append(retType);
        code.append(L_BRACKET);

        // rest of its children stmts
        var afterParam = count_param + 1;
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            code.append(childCode);
        }

        if(retType.equals(".V")){
            code.append("ret.V ;\n");
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());

        if(!table.getSuper().isEmpty()){
            code.append(" extends ");
            code.append(table.getSuper());
        }
        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            if(child.getKind().equals("VarDeclaration")){
                code.append(".field public ");

                // name
                var name = child.get("name");
                code.append(name);

                // type
                var retType = OptUtils.toOllirType(child.getJmmChild(0));
                code.append(retType);
                code.append(";\n");
            }

            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
