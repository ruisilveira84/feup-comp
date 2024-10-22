package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.TreeMap;
import java.util.Map;

public class ConstPropagationVisit extends PreorderJmmVisitor<SymbolTable, Boolean> {

    // Map to store variable names and their corresponding constant values
    private final Map<String, JmmNode> variables = new TreeMap<>();
    // Counter to keep track of the number of substitutions made
    private int statusAcc = 0;

    @Override
    public void buildVisitor() {
        // Adding visit methods for different kinds of nodes
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);

        // Setting the default return value for visits to null
        setDefaultValue(() -> null);
    }

    // Clears the variables map at the start of each method declaration
    private Boolean visitMethodDecl(JmmNode method, SymbolTable table) {
        variables.clear();
        return false;
    }

    // Replaces a variable reference with its corresponding constant value if it exists
    private Boolean visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        boolean assignBool = !varRefExpr.getParent().getKind().equals("AssignStmt");

        if (assignBool || varRefExpr != varRefExpr.getParent().getChild(0)){
            JmmNode temp = variables.get(varRefExpr.get("name")); // Get the constant value for the variable

            if (temp != null) {
                varRefExpr.replace(temp); // Replace the variable reference with the constant value
                statusAcc++;
            }
        }

        return false;
    }

    // Stores the constant value assigned to a variable and removes the assignment statement
    private Boolean visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        JmmNode variable = assignStmt.getChild(0);
        JmmNode constant = assignStmt.getChild(1);

        if (!constant.getKind().equals("BinaryExpr")) {
            variables.put(variable.get("name"), constant); // Store the variable and its constant value
            assignStmt.detach(); // Remove the assignment statement from the AST
        }

        return false;
    }

    // Resets the substitution counter
    public void resetPropStatus() {
        this.statusAcc = 0;
    }

    // Returns the current substitution counter
    public int getPropStatus() {
        return this.statusAcc;
    }
}
