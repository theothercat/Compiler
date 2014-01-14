package syntax;

import lex.LexicalAnalyzer;
import lex.Token;
import lex.TokenType;
import syntax.symbol.SymbolTable;
import syntax.symbol.SymbolTableEntry;
import syntax.symbol.SymbolTableEntryType;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/13/14
 * Time: 8:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class SyntaxAnalyzer {
    private static final List<String> TYPES = Arrays.asList("int", "char", "bool", "void", "class"/*, "class_name"*/);
    private static final String CLASS_STR = "class";

    private SymbolTable symbolTable;
    private LexicalAnalyzer lex;
    private Token currentToken;
    private int symbolTableValue = 0;
    private String scope;
    private boolean passFailed = false;

    public SyntaxAnalyzer(LexicalAnalyzer lex) {
        this.lex = lex;
        this.scope = "g.";
        this.symbolTable = new SymbolTable();
    }

    public void createSymbolTable() throws Exception {
        while(!passFailed) {
            currentToken = lex.nextToken();
            if(currentToken == null) {
                return;
            }
            type();
        }
    }

    public void type() {
        Token typeToken = lex.getToken();
        if(isType(typeToken.lexeme)) {
            Token expectedIdentifier = lex.peek();
            if(!TokenType.IDENTIFIER.equals(expectedIdentifier.type)) {
                System.out.println("Bad declaration on line " + lex.getLineNumber() + ": type '" + currentToken.lexeme + "' not followed by identifier"); // todo: fix this?
                passFailed = true;
                return;
            }
            symbolTable.put(expectedIdentifier.lexeme, new SymbolTableEntry(scope, "symid", expectedIdentifier.lexeme, getSymbolTableEntryType(typeToken.lexeme), "data"));
        }
    }

    public SymbolTableEntryType getSymbolTableEntryType(String typeTokenLexeme) {
        if(CLASS_STR.equals(typeTokenLexeme)) {
            scope += lex.peek().lexeme;
            return SymbolTableEntryType.CLASS;
        }

        return null;
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
