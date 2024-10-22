package pt.up.fe.comp2024.backend;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import javax.naming.NotContextException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.*;


/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private static final String SPACE = " ";

    private static boolean isAssignment = false;

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    int label_size = 0;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class, this::generateCallInstruction);
        generators.put(PutFieldInstruction.class, this::PutgenerateFieldInstruction);
        generators.put(Field.class, this::generateField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(OpCondInstruction.class,this::generateOpCondInstruction);
        generators.put(GotoInstruction.class,this::generateGoToInstruction);
        generators.put(SingleOpCondInstruction.class,this::generateSimpleOpCondInstruction);
        generators.put(SingleOpInstruction.class,this::generateSingleOpInstruction);
        generators.put(UnaryOpInstruction.class,this::generateUnaryOpCondInstruction);

    }




    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class public ").append(className).append(NL).append(NL);

        var superClasName = ollirResult.getOllirClass().getSuperClass();
        var superclass = "";
        if (superClasName == null){
            code.append(".super java/lang/Object").append(NL);
            superclass = "java/lang/Object";
        } else {
            code.append(".super ").append(superClasName).append(NL).append(NL);
            superclass = superClasName;
        }
        for(var field: ollirResult.getOllirClass().getFields()){
            code.append(generators.apply(field));
        }

        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial %s/<init>()V
                    return
                .end method
                """;
        String finale = String.format(defaultConstructor,superclass);
        code.append(finale);



        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }



        return code.toString();
    }


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        var methodStatic = method.isStaticMethod();
        var statico = "";
        if(methodStatic){
            statico = "static ";
        }
        var importClasses = ollirResult.getOllirClass().getImportedClasseNames();

        code.append("\n.method ").append(modifier).append(statico).append(methodName).append("(");
        for(var param: method.getParams()){
            var x = param.getType().getTypeOfElement().name();
            switch (x){
                case "ARRAYREF":
                    ArrayType b = (ArrayType) param.getType();
                    code.append("[").append(getTypeofString(b));
                    /*if(b.getElementType().getTypeOfElement().name().equals("INT32")){
                        code.append(";");
                    }*/

                    /*var name = c.getTypeOfElement().name();
                    if(method.getOllirClass().getClassName().equals(name)){
                        code.append("L").append(name).append(";");
                    }else{
                        code.append("Ljava/lang/").append(name).append(";");
                    }*/
                    break;
                case "STRING":
                    code.append("[Ljava/lang/String;");
                    break;
                case "INT32":
                    code.append("I");
                    break;
                case "BOOLEAN":
                    code.append("Z");
                    break;
                case "OBJECTREF":
                    ClassType a = (ClassType)param.getType();
                    if(method.getOllirClass().getClassName().equals(a.getName())){
                        code.append("L").append(a.getName()).append(";");
                    }
                    else if(importClasses.contains(a.getName())){
                        String full_name = " ";
                        for(String g: currentMethod.getOllirClass().getImports()){
                            String verify = g.substring(g.lastIndexOf(".") + 1);
                            if(verify.equals(a.getName())){
                                full_name = g;
                                break;
                            }
                        }
                        full_name = full_name.replace('.','/');
                        code.append("L"+full_name+";");
                    }
                    else{
                        code.append("Ljava/lang/").append(a.getName()).append(";");
                    }
                    break;
                default:
                    throw new NotImplementedException(x);
            }

        }
        code.append(")");
        switch (method.getReturnType().getTypeOfElement().name()){
            case "ARRAYREF":
                var i = 4;
                ArrayType b = (ArrayType) method.getReturnType();
                code.append("[").append(getTypeofString(b));


                    /*var name = c.getTypeOfElement().name();
                    if(method.getOllirClass().getClassName().equals(name)){
                        code.append("L").append(name).append(";");
                    }else{
                        code.append("Ljava/lang/").append(name).append(";");
                    }*/
                break;
            case "INT32":
                code.append("I");
                break;
            case "STRING":
                code.append("[Ljava/lang/String;");
                break;
            case "BOOLEAN":
                code.append("Z");
                break;
            case "VOID":
                code.append("V");
                break;
            case "OBJECTREF":
                ClassType a = (ClassType)method.getReturnType();
                if(method.getOllirClass().getClassName().equals(a.getName())){
                    code.append("L").append(a.getName()).append(";");
                }else if(importClasses.contains(a.getName())){
                    String full_name = " ";
                    for(String g: currentMethod.getOllirClass().getImports()){
                        String verify = g.substring(g.lastIndexOf(".") + 1);
                        if(verify.equals(a.getName())){
                            full_name = g;
                            break;
                        }
                    }
                    full_name = full_name.replace('.','/');
                    code.append("L"+full_name+";");
                }else{
                    code.append("Ljava/lang/").append(a.getName()).append(";");
                }

                break;
            default:
                throw new NotImplementedException(method.getReturnType().getTypeOfElement().name());
        }
        code.append(NL);


        var method_code = new StringBuilder();

        var labels = method.getLabels();

        for (var inst : method.getInstructions()) {
            if(labels.containsValue(inst)){
                for(var entry : labels.entrySet()){
                    if(entry.getValue().equals(inst)){
                        method_code.append(NL).append(entry.getKey()).append(":\n");

                    }
                }
            }
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            method_code.append(instCode);
        }
        var a = currentMethod.getVarTable().size();
        var b = currentMethod.getVarTable();
        var max = 0;
        for(var child: b.entrySet()){
            if(child.getValue().getVirtualReg()>max){
                max = child.getValue().getVirtualReg();
            }

        }
        List<String> items = Arrays.asList(method_code.toString().split("\n",-1));
        int stack_value = 0;
        int max_stack = 0;
        for(var child: items){
            if(child.equals("")){
                continue;
            }
            String sub = child;
            if(sub.trim().isEmpty()){
                continue;
            }
            var h = sub.charAt(0);
            while(sub.charAt(0)==32){
                sub = sub.substring(1);
            }
            int stack_diff = getStackModOfInstr(sub);
            stack_value += stack_diff;
            if(stack_value>=max_stack){
                max_stack = stack_value;
            }
        }

        code.append(TAB).append(".limit stack ").append(max_stack).append(NL);
        code.append(TAB).append(".limit locals ").append(max+1).append(NL);

        code.append(method_code.toString());

        code.append(".end method\n");

        // unset method
        currentMethod = null;
        String string = code.toString();


        return string;
    }

    private String generateOpCondInstruction(OpCondInstruction opCondInstruction) {

        var condition = opCondInstruction.getCondition();
        var code = new StringBuilder();
        code.append(generators.apply(condition)); //We will always get a 0 or 1 on top of the stack.
        code.append("ifne ").append(opCondInstruction.getLabel());
        return code.toString();
    }

    private String generateSimpleOpCondInstruction(SingleOpCondInstruction opCondInstruction) {

        var condition = opCondInstruction.getCondition();

        var code = new StringBuilder();
        code.append(generators.apply(condition));
        code.append("ifne ").append(opCondInstruction.getLabel());
        return code.toString();
    }
    private String generateUnaryOpCondInstruction(UnaryOpInstruction opCondInstruction) {

        var code = new StringBuilder();
        code.append(generators.apply(opCondInstruction.getOperand()));
        var op = opCondInstruction.getOperation();

        var str = switch(op.getOpType()){
            case NOTB -> "iconst_1\nixor\n";
            default -> throw new NotImplementedException(op.getOpType());
        };
        code.append(str);
        return code.toString();
    }
    private String generateSingleOpInstruction(SingleOpInstruction singleOpInstruction){
        var code = new StringBuilder();
        code.append(generators.apply(singleOpInstruction.getSingleOperand()));
        return code.toString();
    }
    private String generateGoToInstruction(GotoInstruction gotoInstruction) {
        int b = 1;
        var code = new StringBuilder();
        var num = gotoInstruction.getLabel().substring(gotoInstruction.getLabel().length()-1);
        code.append("goto ").append(gotoInstruction.getLabel());
        return code.toString();
    }
    private int getStackModOfInstr(String inst){
        String subInst;
        if(inst.contains(" ")){
            subInst = inst.substring(0,inst.indexOf(' '));
        }
        else{
            subInst = inst;
        }
        if(subInst.contains("_")){
            subInst = subInst.substring(0,subInst.indexOf("_"));
        }
        //LOAD METE NA STACK
        switch (subInst){
            case "ireturn":
            case "areturn":
            case "return":
                break;
            case "aload":
            case "iload":
                return 1;
            case "istore":
            case "astore":
                return -1;
            case "ldc":
            case "iconst":
            case "bipush":
            case "sipush":
                return 1;
            case "iadd":
            case "imul":
            case "idiv":
            case "isub":
                return -1;
            case "ior":
            case "iand":
                return -1;
            case "ifne":
            case "iflt":
            case "ifgt":
                return -1;
            case "if":
                return -2;
            case "invokestatic":
                String a = inst.substring(inst.indexOf('(')+1,inst.indexOf(')'));
                int num_of_arg = inst.substring(inst.indexOf('(')+1,inst.indexOf(')')).length();
                if(inst.substring(inst.lastIndexOf(')')+1).equals("V")){
                    return -num_of_arg;
                }
                else{
                    return -num_of_arg + 1;
                }
                //We need to separate invokes because the invokestatic doesn't take as reference a objectref
            case "invokespecial":
            case "invokevirtual":
                //First get the num of arguments
                a = inst.substring(inst.indexOf('(')+1,inst.indexOf(')'));
                num_of_arg = inst.substring(inst.indexOf('(')+1,inst.indexOf(')')).length();
                if(inst.substring(inst.lastIndexOf(')')+1).equals("V")){
                    return -num_of_arg - 1;
                }
                else{
                    return -num_of_arg;
                }
            case "new":
            case "newarray":
                return 1;
            case "dup":
                return 1;
            case "putfield":
                return -1;
            case "getfield":
                return 1;
            case "pop":
                return -1;
        }
        return 0;

    }
    private String getTypeofString(ArrayType todo){
        var str = todo.getElementType().getTypeOfElement().name();
        switch (str) {
            case "INT32":
                return "I";
            case "BOOLEAN":
                return "Z";
            case "VOID":
                return "V";
            case "STRING":
                return  "Ljava/lang/String;";
            default:
                throw new NotImplementedException(todo);
        }

    }
    public String generateField(Field field) {
        //.field <access-spec> <field-name> <signature> [ = <value> ]
        var code = new StringBuilder();
        code.append(".field ");
        switch(field.getFieldAccessModifier().name()){
            case "DEFAULT":
                break;
            default:
                code.append(field.getFieldAccessModifier().name().toLowerCase()).append(SPACE);
                break;
        }
        code.append(field.getFieldName()).append(SPACE);
        var importClasses = ollirResult.getOllirClass().getImportedClasseNames();
        switch (field.getFieldType().getTypeOfElement().name()) {
            case "ARRAYREF":
                ArrayType b = (ArrayType) field.getFieldType();
                code.append("[").append(getTypeofString(b)).append(" ");
                break;
            case "STRING":
                code.append("[Ljava/lang/String; ");
                break;
            case "INT32":
                code.append("I ");
                break;
            case "BOOLEAN":
                code.append("Z ");
                break;
            case "VOID":
                code.append("V ");
                break;
            case "OBJECTREF":
                ClassType a = (ClassType) field.getFieldType();
                if(ollirResult.getOllirClass().getClassName().equals(a.getName())){
                    code.append("L").append(a.getName()).append(";");
                }else if(importClasses.contains(a.getName())){
                    String full_name = " ";
                    for(String g: ollirResult.getOllirClass().getImports()){
                        String verify = g.substring(g.lastIndexOf(".") + 1);
                        if(verify.equals(a.getName())){
                            full_name = g;
                            break;
                        }
                    }
                    full_name = full_name.replace('.','/');
                    code.append("L"+full_name+";");
                }else{
                    code.append("Ljava/lang/").append(a.getName()).append(";");
                }

                break;
            default:
                throw new NotImplementedException(field.getFieldType().getTypeOfElement().name());
        }
        if(field.isInitialized()){
            code.append(" = ").append(field.getInitialValue());
        }
        code.append(NL);
        return code.toString();
    }

    public String PutgenerateFieldInstruction(PutFieldInstruction putFieldInstruction) {
        //putfield(this, a.i32, 3.i32).V;
        //iconst_3
        //putfield MyClass/a I

        var code = new StringBuilder();
        var operands_list = putFieldInstruction.getOperands();
        var where_to_store = operands_list.get(0);
        code.append(generators.apply(where_to_store));
        var value_to_store = operands_list.get(2);
        if (value_to_store.isLiteral()){
            var getName = (LiteralElement)(value_to_store);
            code.append(generators.apply(getName));
        }
        else {
            code.append(generators.apply(value_to_store));
        }
        var operando_namee = (ClassType)(where_to_store.getType());
        var operando_name = operando_namee.getName();

        var field_name = (Operand)(operands_list.get(1));
        var return_putfield = "";
        switch (field_name.getType().getTypeOfElement().name()) {
            case "INT32":
                return_putfield = "I";
                break;
            case "BOOLEAN":
                return_putfield = "Z";
                break;
            case "VOID":
                return_putfield = "V";
                break;
            case "STRING":
                return_putfield =  "[Ljava/lang/String;";
                break;
            case "ARRAYREF":
                ArrayType b = (ArrayType) field_name.getType();
                return_putfield =  "["+getTypeofString(b);
                break;
            case "OBJECTREF":
                var importClasses = ollirResult.getOllirClass().getImportedClasseNames();
                ClassType a = (ClassType) field_name.getType();
                if(ollirResult.getOllirClass().getClassName().equals(a.getName())){
                    return_putfield = "L"+ a.getName() +";";
                }else if(importClasses.contains(a.getName())){
                    String full_name = " ";
                    for(String g: ollirResult.getOllirClass().getImports()){
                        String verify = g.substring(g.lastIndexOf(".") + 1);
                        if(verify.equals(a.getName())){
                            full_name = g;
                            break;
                        }
                    }
                    full_name = full_name.replace('.','/');
                    return_putfield = "L"+full_name+";";
                }else{
                    return_putfield = "Ljava/lang/" + a.getName() + ";";

                }

                break;

            default:
                throw new NotImplementedException(field_name.getType().getTypeOfElement().name());
        }

        code.append("putfield ").append(operando_name).append("/").append(field_name.getName()).append(SPACE).append(return_putfield).append(NL);

        return code.toString();
    }
    public String generateGetField(GetFieldInstruction getFieldInstruction){
        //t1.i32 :=.i32 getfield(this, a.i32).i32;

        //aload_0
        //getfield a I
        //istore_2
        var code = new StringBuilder();
        var operands_list = getFieldInstruction.getOperands();
        var operando_namee = (ClassType)(operands_list.get(0).getType());
        var where_to_store = operands_list.get(0);
        code.append(generators.apply(where_to_store));
        var operando_name = operando_namee.getName();
        var field_name = (Operand)(operands_list.get(1));
        var return_putfield = "";
        switch (field_name.getType().getTypeOfElement().name()) {
            case "INT32":
                return_putfield = "I";
                break;
            case "BOOLEAN":
                return_putfield = "Z";
                break;
            case "VOID":
                return_putfield = "V";
                break;
            case "ARRAYREF":
                //Only exists arrays of ints
                return_putfield = "[I";
                break;
            case "OBJECTREF":
                var importClasses = ollirResult.getOllirClass().getImportedClasseNames();
                ClassType a = (ClassType) field_name.getType();
                if(ollirResult.getOllirClass().getClassName().equals(a.getName())){
                    return_putfield = "L"+ a.getName() +";";
                }else if(importClasses.contains(a.getName())){
                    String full_name = " ";
                    for(String g: ollirResult.getOllirClass().getImports()){
                        String verify = g.substring(g.lastIndexOf(".") + 1);
                        if(verify.equals(a.getName())){
                            full_name = g;
                            break;
                        }
                    }
                    full_name = full_name.replace('.','/');
                    return_putfield = "L"+full_name+";";
                }else{
                    return_putfield = "Ljava/lang/" + a.getName() + ";";

                }

                break;
            default:
                throw new NotImplementedException(field_name.getType().getTypeOfElement().name());
        }

        code.append("getfield ").append(operando_name).append("/").append(field_name.getName()).append(SPACE).append(return_putfield).append(NL);

        return code.toString();
    }
    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();
        var lhs = assign.getDest();
        if(lhs instanceof ArrayOperand) {
            var reg_array = currentMethod.getVarTable().get(((ArrayOperand) lhs).getName()).getVirtualReg();
            if(reg_array>3){
                code.append("aload ").append(reg_array).append(NL);
            }
            else{
                code.append("aload_").append(reg_array).append(NL);
            }
            for (Element e : ((ArrayOperand) lhs).getIndexOperands()) {
                code.append(generators.apply(e));
            }
        }

        //We need to be sure that it isn't a iinc function
        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        if(assign.getRhs() instanceof BinaryOpInstruction){
            var b = ((BinaryOpInstruction) assign.getRhs()).getLeftOperand();
            var d = ((BinaryOpInstruction) assign.getRhs()).getRightOperand();
            if (b instanceof Operand){
                if((currentMethod.getVarTable().get(((Operand) b).getName()).getVirtualReg()==reg )){
                    code.append("iinc ").append(reg);
                    code.append(" ").append(((LiteralElement) ((BinaryOpInstruction) assign.getRhs()).getRightOperand()).getLiteral()).append(NL);
                    return code.toString();
                }
            }
            else if(d instanceof Operand){
                if(currentMethod.getVarTable().get(((Operand) d).getName()).getVirtualReg()==reg){
                    code.append("iinc ").append(reg);
                    code.append(" ").append(((LiteralElement) ((BinaryOpInstruction) assign.getRhs()).getLeftOperand()).getLiteral()).append(NL);
                    return code.toString();
                }
            }
        }

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination


        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }




        var i = assign.getTypeOfAssign().getTypeOfElement().name();
        var type = "";
        String chasep = "_";
        if(reg>3){
            chasep = " ";
        }
        switch (i){
            case "OBJECTREF":
                var typee = "astore";
                var mais = generators.apply(assign.getDest());

                var destino = assign.getRhs();
                if(destino instanceof CallInstruction){
                    var destin = (CallInstruction)destino;
                    if(destin.getInvocationType().name().equals("NEW")){
                        code.append(typee).append(chasep).append(reg).append(NL).append(mais);
                        //code.append(typee).append(chasep).append(reg).append(NL);
                        return code.toString();
                    }
                }

                // OQ É QUE AQUELE MAIS ta aqui a fazer?
                //code.append(typee).append(chasep).append(reg).append(NL).append(mais).append(NL);
                code.append(typee).append(chasep).append(reg).append(NL);
                return code.toString();
            case "INT32":
                if(lhs instanceof ArrayOperand){
                    type ="iastore";
                    code.append(type).append(NL);
                    return code.toString();
                }
                else {
                    type = "istore"+chasep;
                }
                break;
            case "BOOLEAN":
                type = "istore"+chasep;
                break;
            case "STRING":
                type = "astore"+chasep;
                break;
            case "ARRAYREF":
                type = "astore"+chasep;
                break;
            case "VOID":
                break;
            default:
                throw new NotImplementedException(i);
        }
        code.append(type).append(reg).append(NL);

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        if(Integer.parseInt(literal.getLiteral())<=5 && Integer.parseInt(literal.getLiteral())>=0){
            return "iconst_"+literal.getLiteral() + NL;
        }
        else if(Integer.parseInt(literal.getLiteral())<=127 && Integer.parseInt(literal.getLiteral())>=-128){
            return "bipush "+literal.getLiteral() + NL;
        }
        else if(Integer.parseInt(literal.getLiteral())<=32767 && Integer.parseInt(literal.getLiteral())>=-32768){
            return "sipush "+literal.getLiteral() + NL;
        }
        else{
            return "ldc " + literal.getLiteral() + NL;
        }
    }
    private String generateCallInstruction(CallInstruction callInstruction) {
        var code = new StringBuilder();
        var b = callInstruction.getInvocationType().name();
        switch(b){
            case "NEW":
                var metodo = currentMethod;
                var importClasses = currentMethod.getOllirClass().getImportedClasseNames();
                var operand = (Operand)(callInstruction.getCaller());

                //Check if it is a newarray instruction.
                if(operand.getName().equals("array")){
                    for(Element e: callInstruction.getArguments()){
                        code.append(generators.apply(e));
                    }
                    code.append("newarray int").append(NL);
                    break;
                }

                var operand_name = operand.getName();
                if(importClasses.contains(operand_name)){
                    String full_name = " ";
                    for(String a: currentMethod.getOllirClass().getImports()){
                        String verify = a.substring(a.lastIndexOf(".") + 1);
                        if(verify.equals(operand_name)){
                            full_name = a;
                            break;
                        }
                    }
                    full_name = full_name.replace('.','/');
                    code.append("new ").append(full_name).append(NL);

                }
                else{
                    code.append("new ").append(operand_name).append(NL);
                }
                break;
            case "invokespecial":
                // TODO: NAO ESTA A ACEITAR ARGUMENTOS
                importClasses = currentMethod.getOllirClass().getImportedClasseNames();
                var operando = (Operand)(callInstruction.getCaller());
                var operando_namee = (ClassType)(operando.getType());
                String operando_name = operando_namee.getName();
                var method = (LiteralElement)(callInstruction.getMethodName());
                var method_name = method.getLiteral();
                var return_type = callInstruction.getReturnType().getTypeOfElement().name();
                var ret = "";
                switch (return_type){
                    case "ARRAYREF":
                        ArrayType a = (ArrayType) callInstruction.getReturnType();
                        ret = "["+getTypeofString(a);
                        break;
                    case "INT32":
                        ret ="I";
                        break;
                    case "STRING":
                        ret = "Ljava/lang/String;";
                        break;
                    case "BOOLEAN":
                        ret="Z";
                        break;
                    case "VOID":
                        ret ="V";
                        break;
                    case "OBJECTREF":
                        var metodozinho = currentMethod;
                        var classe = metodozinho.getOllirClass().getClassName();
                        ClassType name = (ClassType) callInstruction.getReturnType();
                        var ope = name.getName();
                        if(classe.equals(ope)){
                            ret =  "L" + ope +";";
                        }else if(importClasses.contains(ope)){
                            String full_name = " ";
                            for(String g: currentMethod.getOllirClass().getImports()){
                                String verify = g.substring(g.lastIndexOf(".") + 1);
                                if(verify.equals(ope)){
                                    full_name = g;
                                    break;
                                }
                            }
                            full_name = full_name.replace('.','/');
                            ret =  "L"+full_name+";";
                        }
                        else{
                            ret =  "Ljava/lang/" + ope +";";
                        }
                        break;
                    default:
                        throw new NotImplementedException(return_type);
                }
                var arguments = callInstruction.getArguments();
                for(Element e: arguments){
                    code.append(generators.apply(e));
                }
                method_name = method_name.substring(1, method_name.length() - 1);
                if(method_name.equals("")){
                    method_name= "<init>";
                }
                importClasses = currentMethod.getOllirClass().getImportedClasseNames();
                if(importClasses.contains(operando_name)){
                    String full_name = " ";
                    for(String a: currentMethod.getOllirClass().getImports()){
                        String verify = a.substring(a.lastIndexOf(".") + 1);
                        if(verify.equals(operando_name)){
                            full_name = a;
                            break;
                        }
                    }
                    full_name = full_name.replace('.','/');
                    code.append(b).append(" ").append(full_name).append("/").append(method_name).append("(");

                }
                else{
                    code.append(b).append(" ").append(operando_name).append("/").append(method_name).append("(");
                }
                arguments = callInstruction.getArguments();

                //ARGUMENTS
                for(Element e: arguments){
                    var type = e.getType();
                    var str_type = type.getTypeOfElement().name();
                    switch (str_type){
                        case "ARRAYREF":
                            ArrayType a = (ArrayType) e.getType();
                            code.append("[").append(getTypeofString(a));
                            break;
                        case "STRING":
                            code.append("Ljava/lang/String;");
                            break;
                        case "INT32":
                            code.append("I");
                            break;
                        case "BOOLEAN":
                            code.append("Z");
                            break;
                        case "VOID":
                            code.append("V");
                            break;
                        case "OBJECTREF":
                            var metodozinho = currentMethod;
                            var classe = metodozinho.getOllirClass().getClassName();
                            ClassType name = (ClassType) e.getType();
                            var ope = name.getName();
                            if(classe.equals(ope)) {
                                code.append("L" + ope + ";");
                            }
                            else if(importClasses.contains(ope)){
                                String full_name = " ";
                                for(String g: currentMethod.getOllirClass().getImports()){
                                    String verify = g.substring(g.lastIndexOf(".") + 1);
                                    if(verify.equals(ope)){
                                        full_name = g;
                                        break;
                                    }
                                }
                                full_name = full_name.replace('.','/');
                                code.append("L"+full_name+";");
                            }else{
                                code.append("Ljava/lang/" + ope +";");
                            }
                            break;

                        default:
                            throw new NotImplementedException(return_type);
                    }
                    var l = 1;
                }
                code.append(")").append(ret).append(NL);
                if(callInstruction.isIsolated() && !ret.equals("V")){
                    code.append("pop").append(NL);
                }
                break;

            case "invokestatic":
                // TODO: NAO ESTA A ACEITAR ARGUMENTOS
                importClasses = currentMethod.getOllirClass().getImportedClasseNames();
                operando = (Operand)(callInstruction.getCaller());
                operando_name = operando.getName();
                method = (LiteralElement)(callInstruction.getMethodName());
                method_name = method.getLiteral();
                return_type = callInstruction.getReturnType().getTypeOfElement().name();
                ret = "";
                switch (return_type){
                    case "ARRAYREF":
                        ArrayType a = (ArrayType) callInstruction.getReturnType();
                        ret = "["+getTypeofString(a);
                        break;
                    case "INT32":
                        ret ="I";
                        break;
                    case "BOOLEAN":
                        ret="Z";
                        break;
                    case "STRING":
                        ret = "Ljava/lang/String;";
                        break;
                    case "VOID":
                        ret ="V";
                        break;
                    case "OBJECTREF":
                        var metodozinho = currentMethod;
                        var classe = metodozinho.getOllirClass().getClassName();
                        ClassType name = (ClassType) callInstruction.getReturnType();
                        var ope = name.getName();
                        if(classe.equals(ope)){
                            ret =  "L" + ope +";";
                        }else if(importClasses.contains(ope)){
                            String full_name = " ";
                            for(String g: currentMethod.getOllirClass().getImports()){
                                String verify = g.substring(g.lastIndexOf(".") + 1);
                                if(verify.equals(ope)){
                                    full_name = g;
                                    break;
                                }
                            }
                            full_name = full_name.replace('.','/');
                            ret =  "L"+full_name+";";
                        }
                        else{
                            ret =  "Ljava/lang/" + ope +";";
                        }
                        break;

                    default:
                        throw new NotImplementedException(return_type);
                }
                arguments = callInstruction.getArguments();
                for(Element e: arguments){
                    code.append(generators.apply(e));
                }
                method_name = method_name.substring(1, method_name.length() - 1);
                importClasses = currentMethod.getOllirClass().getImportedClasseNames();
                if(importClasses.contains(operando_name)){
                    String full_name = " ";
                    for(String a: currentMethod.getOllirClass().getImports()){
                        String verify = a.substring(a.lastIndexOf(".") + 1);
                        if(verify.equals(operando_name)){
                            full_name = a;
                            break;
                        }
                    }
                    full_name = full_name.replace('.','/');
                    code.append(b).append(" ").append(full_name).append("/").append(method_name).append("(");

                }
                else{
                    code.append(b).append(" ").append(operando_name).append("/").append(method_name).append("(");
                }
                //ARGUMENTS
                arguments = callInstruction.getArguments();
                for(Element e: arguments){
                    var type = e.getType();
                    var str_type = type.getTypeOfElement().name();
                    switch (str_type){
                        case "ARRAYREF":
                            ArrayType a = (ArrayType) e.getType();
                            code.append("[").append(getTypeofString(a));
                            break;
                        case "STRING":
                            code.append("Ljava/lang/String;");
                            break;
                        case "INT32":
                            code.append("I");
                            break;
                        case "BOOLEAN":
                            code.append("Z");
                            break;
                        case "VOID":
                            code.append("V");
                            break;
                        case "OBJECTREF":
                            var metodozinho = currentMethod;
                            var classe = metodozinho.getOllirClass().getClassName();
                            ClassType name = (ClassType) e.getType();
                            var ope = name.getName();
                            if(classe.equals(ope)){
                                code.append( "L" + ope +";");
                            }
                            else if(importClasses.contains(ope)){
                                String full_name = " ";
                                for(String g: currentMethod.getOllirClass().getImports()){
                                    String verify = g.substring(g.lastIndexOf(".") + 1);
                                    if(verify.equals(ope)){
                                        full_name = g;
                                        break;
                                    }
                                }
                                full_name = full_name.replace('.','/');
                                code.append("L"+full_name+";");
                            }
                            else{
                                code.append("Ljava/lang/" + ope +";");
                            }
                            break;

                        default:
                            throw new NotImplementedException(return_type);
                    }
                    var l = 1;
                }



                code.append(")").append(ret).append(NL);
                if(callInstruction.isIsolated() && !ret.equals("V")){
                    code.append("pop").append(NL);
                }
                break;
            case "invokevirtual":
                importClasses = currentMethod.getOllirClass().getImportedClasseNames();
                operando = (Operand)(callInstruction.getCaller());
                operando_namee = (ClassType)(operando.getType());
                operando_name = operando_namee.getName();
                method = (LiteralElement)(callInstruction.getMethodName());
                method_name = method.getLiteral();
                return_type = callInstruction.getReturnType().getTypeOfElement().name();
                ret = "";
                switch (return_type){
                    case "ARRAYREF":
                        ArrayType a = (ArrayType) callInstruction.getReturnType();
                        ret = "["+getTypeofString(a);
                        break;
                    case "INT32":
                        ret ="I";
                        break;
                    case "BOOLEAN":
                        ret="Z";
                        break;
                    case "STRING":
                        ret = "Ljava/lang/String;";
                        break;
                    case "VOID":
                        ret ="V";
                        break;
                    case "OBJECTREF":
                        var metodozinho = currentMethod;
                        var classe = metodozinho.getOllirClass().getClassName();
                        ClassType name = (ClassType) callInstruction.getReturnType();
                        var ope = name.getName();
                        if(classe.equals(ope)){
                            ret =  "L" + ope +";";
                        }else if(importClasses.contains(ope)){
                            String full_name = " ";
                            for(String g: currentMethod.getOllirClass().getImports()){
                                String verify = g.substring(g.lastIndexOf(".") + 1);
                                if(verify.equals(ope)){
                                    full_name = g;
                                    break;
                                }
                            }
                            full_name = full_name.replace('.','/');
                            ret =  "L"+full_name+";";
                        }
                        else{
                            ret =  "Ljava/lang/" + ope +";";
                        }
                        break;

                    default:
                        throw new NotImplementedException(return_type);
                }
                arguments = callInstruction.getArguments();
                if(operando.getName().equals("this")){
                    code.append("aload_0").append(NL);
                }
                else{
                    code.append(generators.apply(operando));
                }
                for(Element e: arguments){
                    code.append(generators.apply(e));
                }

                method_name = method_name.substring(1, method_name.length() - 1);
                importClasses = currentMethod.getOllirClass().getImportedClasseNames();
                if(importClasses.contains(operando_name)){
                    String full_name = " ";
                    for(String a: currentMethod.getOllirClass().getImports()){
                        String verify = a.substring(a.lastIndexOf(".") + 1);
                        if(verify.equals(operando_name)){
                            full_name = a;
                            break;
                        }
                    }
                    full_name = full_name.replace('.','/');
                    code.append(b).append(" ").append(full_name).append("/").append(method_name).append("(");

                }
                else{
                    code.append(b).append(" ").append(operando_name).append("/").append(method_name).append("(");
                }
                //ARGUMENTS
                arguments = callInstruction.getArguments();
                for(Element e: arguments){
                    var type = e.getType();
                    var str_type = type.getTypeOfElement().name();
                    switch (str_type){
                        case "ARRAYREF":
                            ArrayType a = (ArrayType) e.getType();
                            code.append("[").append(getTypeofString(a));
                            break;
                        case "STRING":
                            code.append("Ljava/lang/String;");
                            break;
                        case "INT32":
                            code.append("I");
                            break;
                        case "BOOLEAN":
                            code.append("Z");
                            break;
                        case "VOID":
                            code.append("V");
                            break;
                        case "OBJECTREF":
                            var metodozinho = currentMethod;
                            var classe = metodozinho.getOllirClass().getClassName();
                            ClassType name = (ClassType) e.getType();
                            var ope = name.getName();
                            if(classe.equals(ope)){
                                code.append( "L" + ope +";");
                            }
                            else if(importClasses.contains(ope)){
                                String full_name = " ";
                                for(String g: currentMethod.getOllirClass().getImports()){
                                    String verify = g.substring(g.lastIndexOf(".") + 1);
                                    if(verify.equals(ope)){
                                        full_name = g;
                                        break;
                                    }
                                }
                                full_name = full_name.replace('.','/');
                                code.append("L"+full_name+";");
                            }
                            else{
                                code.append("Ljava/lang/" + ope +";");
                            }
                            break;

                        default:
                            throw new NotImplementedException(return_type);
                    }
                    var l = 1;
                }

                code.append(")").append(ret).append(NL);
                if(callInstruction.isIsolated() && !ret.equals("V")){
                    code.append("pop").append(NL);
                }
                break;
            case "arraylength":
                operando = (Operand)(callInstruction.getCaller());
                code.append(generators.apply(operando));
                code.append("arraylength").append(NL);
                break;
            default:
                throw new NotImplementedException(b);
        }
        return code.toString();
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        var a = currentMethod.getVarTable().get(operand.getName()).getVarType().getTypeOfElement().name();
        String chasep = "_";
        if(reg>3){
            chasep = " ";
        }
        switch (a){
            case "OBJECTREF":
                return "aload"+ chasep + reg + NL;
            case "INT32":
                return "iload"+ chasep + reg + NL;
            case "BOOLEAN":
                return "iload"+ chasep + reg + NL;
            case "ARRAYREF":
                if(operand instanceof ArrayOperand) {
                    ArrayOperand array = (ArrayOperand) operand;
                    String l = "";
                    l += "aload"+ chasep + reg + NL;
                    for (var f : array.getIndexOperands()) {
                        l += generators.apply(f);
                    }
                    return l + "iaload " + NL;
                }
                else{
                    return "aload"+ chasep + reg + NL;
                }
            case "STRING":
                return "aload"+ chasep + reg + NL;
            case "VOID":
                throw new NotImplementedException("Void in Operand");
            case "THIS":
                return "aload_" + 0 + NL;
            default:
                throw new NotImplementedException(a);
        }
    }


    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            case DIV ->  "idiv";
            case SUB -> "isub";
            case LTH -> "isub" +
                    "\niflt cond_"+Integer.toString(label_size)
                    +"\ngoto cond_"+Integer.toString(label_size+1)+
                    "\ncond_"+Integer.toString(label_size)+":" +
                    "\niconst_1\ngoto cond_"+Integer.toString(label_size+2)+"\n" +
                    "\ncond_"+Integer.toString(label_size+1)+":" +
                    "\niconst_0\n"+
                    "\ncond_"+Integer.toString(label_size+2)+":";
            case GTE -> "isub" +
                    "\nifgt cond_"+Integer.toString(label_size)
                    +"\ngoto cond_"+Integer.toString(label_size+1)+
                    "\ncond_"+Integer.toString(label_size)+":" +
                    "\niconst_1\ngoto cond_"+Integer.toString(label_size+2)+"\n" +
                    "\ncond_"+Integer.toString(label_size+1)+":" +
                    "\niconst_0\n"+
                    "\ncond_"+Integer.toString(label_size+2)+":";
            case ANDB -> "iand";
            case ORB -> "ior";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };
        switch (binaryOp.getOperation().getOpType()) {
            case LTH -> label_size += 3;
            case GTE -> label_size += 3;
            default -> label_size = label_size;
        }

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: Hardcoded to int return type, needs to be expanded
        var b = 1;
        /**if (returnInst.hasReturnValue()){
         code.append("return").append(NL);
         }
         else{
         if(returnInst.
         }*/
        if(returnInst.getOperand()!=null) {
            code.append(generators.apply(returnInst.getOperand()));
            var type = returnInst.getReturnType().getTypeOfElement().name();
            switch (type) {
                case "ARRAYREF":
                    code.append("a");
                    break;
                case "STRING":
                    code.append("a");
                    break;
                case "INT32":
                    code.append("i");
                    break;
                case "BOOLEAN":
                    code.append("i");
                    break;
                case "VOID":
                    break;
                case "OBJECTREF":
                    code.append("a");
                    break;
                default:
                    throw new NotImplementedException(type);
            }
            code.append("return").append(NL);

            return code.toString();
        }
        else{
            code.append("return").append(NL);
            return code.toString();
        }
    }

}
