package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;

public class NodeUtils {

    public static int getLine(JmmNode node) {

        return getIntegerAttribute(node, "lineStart", "-1");
    }

    public static int getColumn(JmmNode node) {

        return getIntegerAttribute(node, "colStart", "-1");
    }

    public static int getIntegerAttribute(JmmNode node, String attribute, String defaultVal) {
        String line = node.getOptional(attribute).orElse(defaultVal);
        return Integer.parseInt(line);
    }

    public static boolean getBooleanAttribute(JmmNode node, String attribute, String defaultVal) {
        String line = node.getOptional(attribute).orElse(defaultVal);
        return Boolean.parseBoolean(line);
    }


}
