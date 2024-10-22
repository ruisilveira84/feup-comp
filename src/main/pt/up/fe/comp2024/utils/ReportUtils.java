package pt.up.fe.comp2024.utils;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.List;

public class ReportUtils {
    public static Report buildErrorReport(Stage stage, JmmNode node, String message) {

        return Report.newError(
                stage,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                message,
                null);
    }

    public static Report buildWarnReport(Stage stage, JmmNode node, String message) {

        return Report.newWarn(
                stage,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                message,
                null);
    }

    public static Report buildLogReport(Stage stage, JmmNode node, String message) {

        return Report.newLog(
                stage,
                NodeUtils.getLine(node),
                NodeUtils.getColumn(node),
                message,
                null);
    }

    /**
     * Returns true if there are any ERROR reports.
     *
     * @param reports
     * @return
     */
    public static boolean anyError(List<Report> reports) {

        return reports.stream().anyMatch(r -> r.getType() == ReportType.ERROR);
    }
}
