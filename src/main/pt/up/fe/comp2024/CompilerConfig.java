package pt.up.fe.comp2024;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CompilerConfig {

    private static final String INPUT_FILE = "input.txt";
    private static final String OPTIMIZE = "optimize";
    private static final String REGISTER = "registerAllocation";


    static Map<String, String> shortToLong = new HashMap<>();

    static {
        shortToLong.put("i", CompilerConfig.INPUT_FILE);
        shortToLong.put("o", CompilerConfig.OPTIMIZE);
        shortToLong.put("r", CompilerConfig.REGISTER);
    }


    public static Optional<File> getInputFile(Map<String, String> config) {
        var inputFile = config.get(INPUT_FILE);


        if (inputFile == null) {
            return Optional.empty();
        }

        return Optional.of(new File(inputFile));
    }

    public static boolean getOptimize(Map<String, String> config) {
        return Boolean.parseBoolean(config.getOrDefault(OPTIMIZE, "false"));
    }

    public static int getRegisterAllocation(Map<String, String> config) {
        return Integer.parseInt(config.getOrDefault(REGISTER, "-1"));
    }


    public static Map<String, String> getDefault() {

        var config = new HashMap<String, String>();

        config.put(CompilerConfig.OPTIMIZE, "false");
        config.put(CompilerConfig.REGISTER, "-1");

        return config;
    }

    private static String getLongOpt(String shortOpt) {

        return shortToLong.get(shortOpt);
    }

    private static boolean isShortOpt(String shortOpt) {

        return shortToLong.containsKey(shortOpt);
    }

    public static Map<String, String> parseArgs(String[] args) {

        // default options for config
        var config = getDefault();

        for (String arg : args) {

            if (!arg.startsWith("-")) {
                throw new RuntimeException("Arguments should start with '-'");
            }

            String shortOption = arg.substring(1, 2);
            if (!isShortOpt(shortOption)) {
                throw new RuntimeException("Unrecognized option '-" + shortOption + "'");
            }

            String value = "true";
            if (arg.length() > 2) {
                String equalSign = arg.substring(2, 3);
                if (equalSign.equals("=")) {

                    value = arg.substring(3);
                }
            }

            config.put(getLongOpt(shortOption), value);
        }

        if (!config.containsKey(INPUT_FILE)) {

            throw new RuntimeException("Expected an input file, use '-i=<PATH_TO_FILE>'");
        }

        // make sure we save the absolute path of the input file
        var inputFile = new File(config.get(INPUT_FILE));
        if (!inputFile.isFile()) {
            throw new RuntimeException("Could not find input file '" + inputFile + "'");
        }

        var absolutePath = inputFile.getAbsolutePath();
        config.put(INPUT_FILE, absolutePath);

        // Verify if values are valid
        getOptimize(config);
        getRegisterAllocation(config);

        return config;
    }


}
