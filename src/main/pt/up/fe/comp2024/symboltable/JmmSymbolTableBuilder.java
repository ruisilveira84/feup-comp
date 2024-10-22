package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.VAR_DECL;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        var classDecl = root.getChildren(Kind.CLASS_DECL).get(0);
        System.out.println(classDecl.toTree());
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        System.out.println(className);

        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        var imports = buildImports(root);
        var superClass = buildSuper(classDecl);
        var fields = buildFields(classDecl);

        return new JmmSymbolTable(className, methods, returnTypes, params, locals, imports, superClass, fields);
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, Type> map = new HashMap<>();

        for(var child: classDecl.getChildren(METHOD_DECL)){
            var a = child;
            var c = child.getObject("returnType");
            var b = 2;
            map.put(child.get("name"),new Type(child.getChildren("Type").get(0).get("name"),isArray(child.getChildren("Type").get(0))));
        };
            var a = 1;

        //classDecl.getChildren(METHOD_DECL).stream()
                //.forEach(method -> map.put(method.get("name"), method.getChildren("Type").get(0).get("name")));

        return map;
    }


    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();

        //var intType = new Type(TypeUtils.getIntTypeName(), false);

        // Arrays.asList(new Symbol(intType, method.getJmmChild(1).get("name")))
        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), auxParams(method)));

        return map;
    }

    // Auxiliary function to obtain Parameters
    private static List<Symbol> auxParams(JmmNode classDecl) {
        List<Symbol> finalList = new ArrayList<>();

        classDecl.getChildren(Kind.PARAM).stream()
                .forEach(method -> finalList.add(new Symbol(new Type(method.getJmmChild(0).get("name"), isArray(method.getJmmChild(0))), method.get("name"))));

        return finalList;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // Implementation already given. getLocalsList expanded.

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }

    private static List<String> buildImports(JmmNode classDecl) {
        //System.out.println(Arrays.toString(classDecl.getChildrenStream().filter(child->child.getKind().equals("ImportStmt")).map(imprt -> imprt.get("name")).toList().toArray())); //nome ou objeto em si (?)
        return classDecl.getChildrenStream().filter(child->child.getKind().equals("ImportStmt"))
                .map(imprt -> imprt.get("name")) //nome ou objeto em si (?)
                .toList();
    }

    private static String buildSuper(JmmNode classDecl){
        String superClass;
        try{
            superClass = classDecl.get("extendname");
        }
        catch (NullPointerException e){
            return "";
        }
        return superClass;
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        /*
        Receives a node and returns a list of Symbols corresponding to its field variables
         */
        return classDecl.getChildren(VAR_DECL).stream().map(varDecl -> new Symbol(getVarType(varDecl), varDecl.get("name"))).toList();
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        /*
        Receives a node and returns a list of Symbols corresponding to its local variables.
        Symbol(type, name):
        - The type is given by our auxiliar function getVarType.
        - The name is given by the use of .get("name")
         */
        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(getVarType(varDecl), varDecl.get("name"))).toList();
    }

    private static boolean isArray(JmmNode varDecl){
        if(varDecl.get("isArray")=="true"){
            return true;
        }
        else{
            return false;
        }
    }
    private static Type getVarType(JmmNode varDecl){
        /*
        Receives a node and returns the type of the variable declared on the node.
         */
        var name = varDecl.getChild(0).get("name"); // Getting the Type of the kind by using the getChild(0).
        var varType = new Type(name,isArray(varDecl.getChild(0)));

        /*
        The switch under this comment may be useful when dealing with VarArgs OR ID class identification.
         */
        /**switch(name){
            case "int":
                return new Type(TypeUtils.getIntName(), false);
            case "string":
                return new Type(TypeUtils.getStringName(), false);
            case "boolean":
                return new Type(TypeUtils.getBoolName(), false);
            case ARRAY_TYPE_NAME:
                Type array =new Type(TypeUtils.getIntName(), true);
                // The atribute isVarArgs tells us if we are dealing with arrays or varArgs since both are similar.
                array.putObject("isVarArgs",false); // putObject() allows us to add one more attribute to the array.
                return array;
            case VARARG_TYPE_NAME:
                Type varArg = new Type(TypeUtils.getIntName(), true);
                varArg.putObject("isVarArgs", true);
                return varArg;
            case ID_TYPE_NAME:
                JmmNode typeNode = varDecl.getJmmChild(0); //Gives us the type of the node. - 0 is the type
                return new Type(typeNode.get("name"),false);
            default:
                throw new RuntimeException("Type kind not recognized." + name);
        }**/
        return varType;
    }


}
