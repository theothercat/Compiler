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
            Token peek;
            Token read;

            while(true) {
                peek = la.peek();
                read = la.nextToken();
                if(read == null) { return; }

                System.out.println("Found token \"" + read.lexeme + "\" and classified it as " + read.type.name());
                System.out.println("Peek matches? " + (peek.lexeme.equals(read.lexeme) && peek.type.equals(read.type)));
            }
        }
        catch(Exception e) {
            System.out.println("Exception in main(): " + e.getMessage());
        }
    }
}
