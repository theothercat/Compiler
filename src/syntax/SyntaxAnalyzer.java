package syntax;

import lex.LexicalAnalyzer;
import lex.Token;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/13/14
 * Time: 8:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class SyntaxAnalyzer {
    private LexicalAnalyzer lex;

    public SyntaxAnalyzer(LexicalAnalyzer lex) {
        this.lex = lex;
    }

    public void createSymbolTable() {

    }

    public boolean isTerminal(Token t) {
        return t.lexeme.equals("{"); // todo: add in the rest
    }

    /**
     * Terminals are:
     * {
     * }
     * ;
     * (
     * )     *
     */

}
