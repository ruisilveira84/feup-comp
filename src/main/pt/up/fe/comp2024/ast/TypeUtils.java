package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String STRING_TYPE_NAME = "string";
    private static final String BOOL_TYPE_NAME = "bool";

    public static String getIntName() { return INT_TYPE_NAME; }
    public static String getStringName() {
        return STRING_TYPE_NAME;
    }
    public static String getBoolName() {
        return BOOL_TYPE_NAME;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*","/","-" -> new Type(INT_TYPE_NAME, false);
            case "&&","||" -> new Type(BOOL_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    public static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        String typeName = varRefExpr.getKind();
        boolean isArray =false;
        if(typeName.equals("Int")) return new Type(TypeUtils.getIntName(), false);
        if(typeName.equals("String")) return new Type(TypeUtils.getStringName(), false);
        if(typeName.equals("Bool")) return new Type(TypeUtils.getBoolName(), false);
        //Nao tenho a certeza disto sinceramente
        if(typeName.equals("ArraySubscription")) return new Type(TypeUtils.getIntName(),false);
        if(typeName.equals("ArrayExpr")) return new Type(TypeUtils.getIntName(),true);
        var currentNode = varRefExpr;
        while(!currentNode.getKind().equals("MethodDecl")){
            currentNode = currentNode.getParent();
        }
        var currentMethod = currentNode.get("name");
        var hash_attr = varRefExpr.get("name");
        String type;
        var type_second = table.getLocalVariables(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
        if(!type_second.isEmpty()) { //Is Local
            type = type_second.get(0).getType().getName();
            isArray = type_second.get(0).getType().isArray();
        }
        else{ //Is parameter
            type_second = table.getParameters(currentMethod).stream().filter(child -> child.getName().equals(hash_attr)).toList();
            if(!type_second.isEmpty()) { //Is Field
                type = type_second.get(0).getType().getName();
                isArray = type_second.get(0).getType().isArray();
            }
            else{
                type_second = table.getFields().stream().filter(child -> child.getName().equals(hash_attr)).toList();
                    type = type_second.get(0).getType().getName();
                isArray = type_second.get(0).getType().isArray();

            }
        }
        return new Type(type, isArray);
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
