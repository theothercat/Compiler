import global.ProgramConstants;
import icode.Generator;
import lex.LexicalAnalyzer;
import lex.Token;
import lex.TokenType;
import log.Log;
import log.LogLevel;
import log.LogManager;
import syntax.SyntaxAnalyzer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/6/14
 * Time: 4:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class Main {
    private static List<String> VALID_DEBUG_ARGS = Arrays.asList("-d", "d", "-debug", "debug");

    public static void main(String[] args) {
        SyntaxAnalyzer sa;
        LexicalAnalyzer la;

        if(args.length > 1) {
            ProgramConstants.logLevel = isDebugArg(args[1]) ? LogLevel.DEBUG : LogLevel.STANDARD;
        }

        try {
//            FileReader file = new FileReader(args[0]);
            la = new LexicalAnalyzer(args[0]); //file);


            sa = new SyntaxAnalyzer(la);
            sa.pass();

            sa.pass();
            la.closeFile();

        }
        catch(Exception e) {
            System.out.println("Exception in main(): ");
            e.printStackTrace();
            // todo: File closing!
        }
//        tokenTest(args[0]);
        Generator.dumpQuads();
        LogManager.cleanup();
    }

    private static void tokenTest(String filename) {
        Token peek;
        Token read;
        LexicalAnalyzer la;

//        try {
            la = new LexicalAnalyzer(/*new BufferedReader(new FileReader(*/filename);//));
//        }
//        catch (IOException e) {
//            return;
//        }

        Log tokenLog = new Log("token_test.log");

        while(true) {
            peek = la.peek();
            read = la.nextToken();
            if(TokenType.EMPTY.equals(read.type)) { return; }

            tokenLog.debug("Found token \"" + read.lexeme + "\" and classified it as " + read.type.name());
            if(!(peek.lexeme.equals(read.lexeme) && peek.type.equals(read.type))) {
                System.out.println("Peek found token \"" + peek.lexeme + "\" and classified it as " + peek.type.name());
            }
        }
    }

    private static boolean isDebugArg(String s) {
        return s != null
                && VALID_DEBUG_ARGS.contains(s.toLowerCase());
    }
}
