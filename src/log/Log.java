package log;

import global.ProgramConstants;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/15/14
 * Time: 6:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class Log {
    private BufferedWriter writer = null;
    private String logFileName;

    public Log(String logFileName) {
        if(LogLevel.NONE.equals(ProgramConstants.logLevel)) {
            this.logFileName = logFileName;
            return;
        }

        try {
            writer = new BufferedWriter(new FileWriter(logFileName));
            LogManager.registerLog(this);
        }
        catch (IOException e) {
            System.out.println("Could not open a log file.");
        }
    }

    /**
     * Writes the string only if log level is debug
     * @param s message to log
     */
    public void debug(String s) {
        try {
            if(writer != null && LogLevel.DEBUG.equals(ProgramConstants.logLevel)) {
                writer.write(s);
                writer.newLine();
                writer.flush();
            }
        }
        catch (IOException e) {

        }
    }

    /**
     * Writes the string only if log level is not none
     * @param s message to log
     */
    public void log(String s) {
        try {
            if(writer != null && !LogLevel.NONE.equals(ProgramConstants.logLevel)) {
                writer.write(s);
                writer.newLine();
                writer.flush();
            }
        }
        catch (IOException e) {

        }
    }

    /**
     * Writes the string, unconditionally
     * @param s message to write
     */
    public void write(String s) {
        try {
            if(writer == null && LogLevel.NONE.equals(ProgramConstants.logLevel)) {
                writer = new BufferedWriter(new FileWriter(logFileName));
                LogManager.registerLog(this);
            }

            if(writer != null) {
                writer.write(s);
                writer.newLine();
                writer.flush();
            }
        }
        catch (IOException e) {
            System.out.println("Could not open log file successfully.");
        }
    }

    public void close() {
        if(writer != null) {
            try {
                writer.close();
            }
            catch(Exception e) {

            }
        }
    }
}
