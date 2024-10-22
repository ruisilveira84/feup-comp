package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit("FuncCall",this::visitFuncCall);
        addVisit("NewClassDeclaration",this::visitNewClassDecl);

        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitFuncCall(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        var retType = "";
        String tmp = OptUtils.getTemp();

        var child = node.getChild(0);
        String value = "";
        if(child.hasAttribute("value")) {
            value = child.get("value");
        } else{
            value = child.get("name");
        }
        var child_left = value;
        var value_de_ouro = node.get("value");
        var type_name = " ";
        if(!child_left.equals("this")){
            var type = TypeUtils.getVarExprType(child,table);
            type_name = type.getName();
        }


        if(child_left.equals("this") || type_name.equals(table.getClassName())){
            code.append(tmp);

            //getParent Class
            var currentNode = node;
            while(!currentNode.getKind().equals("ClassDecl")){
                currentNode = currentNode.getParent();
            }
            var funcList = currentNode.getChildren("MethodDecl").stream().filter(child1->child1.get("name").equals(value_de_ouro)).toList();
            if(!funcList.isEmpty()){
                var func = funcList.get(0); // node of the corresponding method
                retType = OptUtils.toOllirType(func.getChild(0).get("name"));
            }
            code.append(retType).append(":=").append(retType).append(SPACE);
            code.append("invokevirtual(");

        }
        else{
            code.append("invokestatic(");
            retType = ".V";
        }

        if(child_left.equals("this")){
            code.append(child_left);
        }
        else{
            code.append(value).append(".").append(type_name);
        }

        code.append(", \"");
        code.append(value_de_ouro);

        var number_of_children = node.getNumChildren();
        var index_children = 0;
        if(number_of_children>=2){
            code.append("\"");
            for (int i = 1;i<number_of_children;i++) {
                code.append(", ");
                code.append(node.getChild(i).get("name"));
                code.append(OptUtils.toOllirType(TypeUtils.getVarExprType(node.getChild(i), table).getName()));
            }
        }
        else{
            code.append("\"");
        }

        code.append(")");
        code.append(retType);
        code.append(END_STMT);

        return new OllirExprResult(tmp+retType,code.toString());
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }
    private OllirExprResult visitNewClassDecl(JmmNode node, Void unused){
        //temp_2.Simple :=.Simple new(Simple).Simple;
        //invokespecial(temp_2.Simple,"<init>").V;
        StringBuilder code = new StringBuilder();
        var retType = "";
        String tmp = OptUtils.getTemp();
        String multi_usos = OptUtils.toOllirType(node.get("name"));
        code.append(tmp).append(multi_usos).append(" ").append(ASSIGN).append(multi_usos);
        code.append(" new(").append(node.get("name")).append(")").append(multi_usos).append(";\n");

        code.append("invokespecial(").append(tmp).append(multi_usos).append(",").append("\"<init>\"").append(").V").append(END_STMT);

        return new OllirExprResult(tmp+multi_usos,code.toString());
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
