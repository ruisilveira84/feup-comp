package pt.up.fe.comp2024.optimization;

public class OllirExprResult {

    public static final OllirExprResult EMPTY = new OllirExprResult("", "");

    private final String computation;
    private final String code;

    public OllirExprResult(String code, String computation) {
        this.code = code;
        this.computation = computation;
    }

    public OllirExprResult(String code) {
        this(code, "");
    }

    public OllirExprResult(String code, StringBuilder computation) {
        this(code, computation.toString());
    }

    public String getComputation() {
        return computation;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "OllirNodeResult{" +
                "computation='" + computation + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}
