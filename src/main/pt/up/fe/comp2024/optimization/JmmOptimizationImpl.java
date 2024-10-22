package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {

    // Method to convert the semantic result (JmmSemanticsResult) to OLLIR
    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());
        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }


    // Method to perform optimizations on the semantic result
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {

        // Check if optimization is enabled in the configuration
        if(semanticsResult.getConfig().containsKey("optimize")
                && semanticsResult.getConfig().get("optimize").equals("true")) {

            // Variables to monitor changes in constant folding and propagation
            int accFold = 1;
            int accProp = 1;

            // Create visitors to perform constant folding and propagation
            ConstFoldingVisit FoldVisitor = new ConstFoldingVisit();
            ConstPropagationVisit PropVisitor = new ConstPropagationVisit();

            // Loop while there are changes in constant folding or propagation
            while (accFold != 0) {
                FoldVisitor.resetFoldStatus();
                FoldVisitor.visit(semanticsResult.getRootNode());

                PropVisitor.resetPropStatus();
                //PropVisitor.visit(semanticsResult.getRootNode());

                accFold = FoldVisitor.getFoldStatus();
                System.out.println("Constant Folding Status: " + accFold);

                accProp = PropVisitor.getPropStatus();
                System.out.println("Constant Propagation Status: " + accProp);

            }

        }

        return semanticsResult;
    }

    // Method to perform optimizations on the OLLIR result
    @Override
    public OllirResult optimize(OllirResult ollirResult) {

        System.out.println("Optimize");
        //TODO: Do your OLLIR-based optimizations here

        return ollirResult;
    }


}
