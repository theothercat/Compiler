import lex.LexicalAnalyzer;
import lex.Token;

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
            Token t;
            while((t = la.nextToken()) != null) {
                System.out.println("Found token \"" + t.lexeme + "\" and classified it as " + t.type.name());
            }
        }
        catch(Exception e) { }
    }
}
