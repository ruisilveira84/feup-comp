package pt.up.fe.comp2024.utils;

import org.fusesource.jansi.AnsiConsole;

import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.fusesource.jansi.Ansi.ansi;

public class Formatter {

    static {
        AnsiConsole.systemInstall();
    }

    public static String warnMsg(String msg) {

        return ansi().fg(YELLOW).bold().a(msg).reset().toString();
    }

    public static String errorMsg(String msg) {

        return ansi().fg(RED).bold().a(msg).reset().toString();
    }
}
