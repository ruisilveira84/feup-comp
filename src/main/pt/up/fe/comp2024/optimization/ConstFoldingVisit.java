package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;

public class ConstFoldingVisit extends PreorderJmmVisitor<SymbolTable, Boolean> {
    private int statusAcc = 0; // Variable to track how many times the optimization has been applied

    @Override
    protected void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryOp); // Add a visit for binary operations

        setDefaultValue(() -> null); // Set default value for visits
    }

    private Boolean visitBinaryOp(JmmNode jmmNode, SymbolTable table) {
        // Get the operator of the binary operation
        String op = jmmNode.get("op");

        // Visit child nodes before calculating the result
        visit(jmmNode.getChildren().get(0), table);
        visit(jmmNode.getChildren().get(1), table);

        // Get the child nodes
        JmmNode right = jmmNode.getChildren().get(1);
        JmmNode left = jmmNode.getChildren().get(0);

        int result = 0;

        // Check if both children are integer literals
        if (right.getKind().equals("IntegerLiteral") && left.getKind().equals("IntegerLiteral")) {
            // Get the values of the integer literals
            Integer leftValue = Integer.parseInt(left.get("value"));
            Integer rightValue = Integer.parseInt(right.get("value"));

            // Calculate the result based on the operator
            switch (op) {
                case "*" -> result = leftValue * rightValue;
                case "/" -> result = leftValue / rightValue;
                case "+" -> result = leftValue + rightValue;
                case "-" -> result = leftValue - rightValue;
                default -> {}
            }

            // Display the calculated result
            System.out.println("Result: " + result);

            // Create a new node to store the resulting value
            JmmNode newNode = new JmmNodeImpl("IntegerLiteral");
            newNode.put("value", String.valueOf(result));

            // Get the parent node of the current node
            JmmNode parentNode = jmmNode.getParent();
            // Get the index of the current node in the parent
            int index = jmmNode.getIndexOfSelf();
            // Remove the current node from the parent and add the new node at the same index
            parentNode.removeJmmChild(index);
            parentNode.add(newNode, index);

            // Increment the applied optimization counter
            statusAcc++;
        }

        return false; // Indicates that the optimization did not cause structural changes in the code
    }

    // Method to reset the optimization status
    public void resetFoldStatus() {
        this.statusAcc = 0;
    }

    // Method to get the optimization status
    public int getFoldStatus() {
        return this.statusAcc;
    }
}
