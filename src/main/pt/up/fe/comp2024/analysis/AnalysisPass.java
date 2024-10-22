package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

/**
 * Represents an analysis pass.
 */
public interface AnalysisPass {

    /**
     * Analyses the given node.
     *
     * @param root  the root node that will be visited for analysis
     * @param table the symbol table
     * @return a list of reports with the results of the analysis
     */
    List<Report> analyze(JmmNode root, SymbolTable table);

}