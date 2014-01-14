package syntax;

import lex.LexicalAnalyzer;
import lex.Token;
import lex.TokenType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/13/14
 * Time: 8:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class SyntaxAnalyzer {
    private static final List<String> TYPES = Arrays.asList("int", "char", "bool", "void"/*, "class_name"*/);

    private SymbolTable symbolTable;
    private LexicalAnalyzer lex;
    private Token currentToken;
    private int lineNumber;
    private int symbolTableValue = 0;

    public SyntaxAnalyzer(LexicalAnalyzer lex) {
        this.lex = lex;
        this.lineNumber = 0;
        this.symbolTable = new SymbolTable();
    }

    public void createSymbolTable() throws Exception {
        while(true) {
            ++lineNumber;
            currentToken = lex.nextToken();
            if(isType(currentToken.lexeme)) {
                Token nextToken = lex.nextToken();
                if(TokenType.IDENTIFIER.equals(currentToken.type)) {
                    symbolTable.put(currentToken.lexeme, symbolTableValue++);
                }
                else {
                    throw new Exception("Bad declaration on line " + lineNumber + ": type '" + currentToken.lexeme + "' not followed by identifier"); // todo: fix this?
                }
            }
        }
    }

//    public boolean isDeclaration() {
//        return TokenType.IDENTIFIER.equals(currentToken.type)
//                && isType(lex.peek().lexeme);
//    }

    public boolean isType(String s) {
        return TYPES.contains(s);
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
