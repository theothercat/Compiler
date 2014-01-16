import global.ProgramConstants;
import lex.LexicalAnalyzer;
import lex.Token;
import log.Log;
import log.LogLevel;
import syntax.SyntaxAnalyzer;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/6/14
 * Time: 4:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class Main {
    public static void main(String[] args) {
        try {
            BufferedReader file = new BufferedReader(new FileReader(args[0]));
            LexicalAnalyzer la = new LexicalAnalyzer(file);
            Token peek;
            Token read;

            if(args.length > 1) {
                ProgramConstants.logLevel = LogLevel.DEBUG;
            }

            new SyntaxAnalyzer(la).pass();

            Log tokenLog = new Log("token_test.log");

            while(true) {
                peek = la.peek();
                read = la.nextToken();
                if(read == null) { return; }

                tokenLog.debug("Found token \"" + read.lexeme + "\" and classified it as " + read.type.name());
                if(!(peek.lexeme.equals(read.lexeme) && peek.type.equals(read.type))) {
                    System.out.println("Peek found token \"" + peek.lexeme + "\" and classified it as " + peek.type.name());
                }
            }
        }
        catch(Exception e) {
            System.out.println("Exception in main(): " + e.getMessage());
        }
    }
}
