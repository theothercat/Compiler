package syntax;

import lex.LexicalAnalyzer;
import lex.Token;
import lex.TokenType;
import log.Log;
import syntax.symbol.SymbolTable;
import syntax.symbol.SymbolTableEntry;
import syntax.symbol.SymbolTableEntryType;

import java.util.Arrays;
import java.util.List;

import static syntax.AnalyzerConstants.*;

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

    private static Log syntaxLog = null;

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

        if(syntaxLog == null) {
            syntaxLog = new Log("syntax.log");
        }
    }

    public void pass() throws Exception {
        if(passFailed) { return; }

        lex.nextToken();
        statement();
    }

    // todo: this might be wrong?
    private void if_while() {
        if(!OPENING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
            failGrammar("statement, if/while", "expected opening parenthesis, found " + lex.getToken().lexeme);
            passFailed = true;
            return;
        }
        expression();
        if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
            failGrammar("statement, if/while", "expected opening parenthesis, found " + lex.getToken().lexeme);
            passFailed = true;
            return;
        }
        statement();
    }

    // todo: this might be wrong?
    private void expression_with_semicolon() {
        expression();

        if(!SEMICOLON.equals(lex.nextToken().lexeme)) {
            failGrammar("statement, cin/cout", "expected semicolon, found " + lex.getToken().lexeme);
            passFailed = true;
            return;
        }
    }

    /**
     * Complete? Hopefully.
     */
    public void statement() {
        if(passFailed) { return; }

        Token thisToken = lex.getToken();

        if(KW_WHILE.equals(thisToken.lexeme)) {
            if_while(); // todo: should this be if_while?
        }
        else if(KW_IF.equals(thisToken.lexeme)) {
            if_while(); // todo: should this be if_while?

            thisToken = lex.peek();
            if(KW_ELSE.equals(thisToken.lexeme)) {
                lex.nextToken(); // current token is peek
                lex.nextToken();
                statement();
            }
        }
        else if(KW_RETURN.equals(thisToken.lexeme)) {
            thisToken = lex.nextToken();

            if(!SEMICOLON.equals(thisToken.lexeme)) {
                expression_with_semicolon();
            }
            return;
        }
        else if(KW_CIN.equals(thisToken.lexeme))
        {
            if(!CIN_TOKEN.equals(lex.nextToken().lexeme)) {
                failGrammar("statement, cin", "expected '>>', found " + lex.getToken().lexeme);
                passFailed = true;
                return;
            }
            expression_with_semicolon();
        }
        else if(KW_COUT.equals(thisToken.lexeme))
        {
            if(!COUT_TOKEN.equals(lex.nextToken().lexeme)) {
                failGrammar("statement, cout", "expected '<<', found " + lex.getToken().lexeme);
                passFailed = true;
                return;
            }
            expression_with_semicolon();
        }
        else if(OPENING_BRACE.equals(lex.nextToken().lexeme)) {
            while(!CLOSING_BRACE.equals(lex.getToken().lexeme)) {
                statement();
                lex.nextToken(); // todo: is this necessary?
            }
        }
        else {
            expression_with_semicolon();
        }


    }

    /**
     * Always required.
     *
     * Complete? Maybe.
     */
    public void expression() {
        if(passFailed) { return; }

        Token thisToken = lex.getToken();
        if(OPENING_PARENTHESIS.equals(thisToken.lexeme)) {
            expression();
            if(!CLOSING_PARENTHESIS.equals(lex.nextToken())) {
                failGrammar("expression", "expected closing parenthesis, found " + lex.getToken());
                passFailed = true;
                return;
            }
        }
        else if(TokenType.IDENTIFIER.equals(thisToken.type)) {
            // Found an identifier.

            // todo: add optionals
        }
        else if(!EXPRESSION_TOKENS.contains(thisToken.lexeme)
                    && !TokenType.NUMBER.equals(thisToken.type)
                    && !TokenType.CHARACTER.equals(thisToken.type))
        {
            failGrammar("expression", "expected expression, found " + thisToken.lexeme);
            passFailed = true;
            return;
        }
        expressionz();
    }

    public void fn_arr_member() {
        if(passFailed) { return; }

        Token thisToken = lex.peek();

        if(OPENING_PARENTHESIS.equals(thisToken.lexeme)) {
            lex.nextToken(); // We are now at the opening parenthesis.

            if(!CLOSING_PARENTHESIS.equals(lex.nextToken())) {
                argument_list();
            }
        }
        else if(OPENING_BRACKET)
    }

    /**
     * This is only ever an optional part of the grammar.
     *
     * Starts by peeking to check for an accepted token.
     * If one is found, it will be consumed, and another
     * method will be called.
     *
     * Complete? Probably.
     */
    public void expressionz() {
        if(passFailed) { return; }

        Token thisToken = lex.peek(); // todo: check
        if(ASSIGNMENT_OPERATOR.equals(thisToken.lexeme)) {
            lex.nextToken();
            assignment_expression();
        }
        else if(EXPRESSIONZ_TOKENS.contains(thisToken.lexeme)) {
            lex.nextToken();
            expression();
        }
    }

    /**
     * Current token is "="
     * Immediately skip to next token.
     *
     * Requires something after the equal sign.
     */
    public void assignment_expression() {
        if(passFailed) { return; }

        Token thisToken = lex.nextToken();
        if(KW_THIS.equals(thisToken.lexeme)) {
            if(!SEMICOLON.equals(lex.nextToken().lexeme)) {
                failGrammar("assignment_expression",  "type '" + currentToken.lexeme + "' not followed by identifier");
                passFailed = true;
                return;
            }
        }
        else if(KW_NEW.equals(thisToken.lexeme)) {
            // todo: implement this
        }
        else if(KW_ATOI.equals(thisToken.lexeme)
                || KW_ITOA.equals(thisToken.lexeme)) {
            if(!OPENING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                failGrammar("assignment_expression",  thisToken.lexeme + "' not followed by opening parenthesis");
                passFailed = true;
                return;
            }
            expression(); // required?
            if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                failGrammar("assignment_expression",  thisToken.lexeme + "' not followed by closing parenthesis");
                passFailed = true;
                return;
            }
        }
        else {
            expression();
        }
    }

    public void failGrammar(String ruleFailed, String detailMessage) {
        System.out.println(String.format("Bad %s on line %d: %s", ruleFailed, lex.getLineNumber(), detailMessage));
        passFailed = true;
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
