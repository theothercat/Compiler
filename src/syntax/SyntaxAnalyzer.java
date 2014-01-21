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
import static syntax.AnalyzerConstants.CLOSING_BRACKET;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/13/14
 * Time: 8:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class SyntaxAnalyzer {
    private static final List<String> TYPES = Arrays.asList("int", "char", "bool", "void"/*, "class_name"*/);
    private static final String CLASS_STR = "class";

    private static Log syntaxLog = null;

    private SymbolTable symbolTable;
    private LexicalAnalyzer lex;
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


        compilation_unit();
    }

    /**
     * Grabs the first token and starts the entire pass.
     */
    private void compilation_unit() {
        if(passFailed) { return; }

        while(!KW_VOID.equals(lex.nextToken().lexeme)) {
            class_declaration();
        }
    }

    /**
     * Class keyword should be current token.
     *
     * Optional? No?
     *
     * Complete? Probably.
     */
    private void class_declaration() {
        if(passFailed) { return; }

        if(!KW_CLASS.equals(lex.getToken())) {
            failGrammar("class_declaration", "expected 'class' keyword, found " + lex.getToken().lexeme);
            passFailed = true;
            return;
        }
        if(!TokenType.IDENTIFIER.equals(lex.nextToken().type)) // todo: this should be class_name
        {
            failGrammar("class_declaration", "expected class_name, found " + lex.getToken().lexeme);
            passFailed = true;
            return;
        }
        if(!OPENING_BRACE.equals(lex.nextToken())) {
            failGrammar("class_declaration", "expected opening brace, found " + lex.getToken().lexeme);
            passFailed = true;
            return;
        }
        while(!OPENING_BRACE.equals(lex.nextToken())) {
            class_member_declaration();
        }
    }

    /**
     * Current token should be part of class_member_declaration;
     * use getToken() rather than nextToken().
     *
     * Optional? No?
     *
     * Complete? No!
     */
    private void class_member_declaration() {
        if(passFailed) { return; }


    }

    // todo: this might be wrong?
    private void if_while() {
        if(passFailed) { return; }

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
        if(passFailed) { return; }

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
            expressionz();
        }
        else if(TokenType.IDENTIFIER.equals(thisToken.type)) // todo: identifier should call the appropriate method instead?
        {
            // Found an identifier.

            fn_arr_member();
            member_refz();
            expressionz();
        }
        else if(EXPRESSION_TOKENS.contains(thisToken.lexeme)) // true, false, and null
        {
            expressionz();
        }
        else if(TokenType.CHARACTER.equals(thisToken.type)) // Character literals are a single token, so the token type can just be checked
        {
            character_literal();
            if(!SINGLE_QUOTE.equals(thisToken.lexeme)) {
                failGrammar("expression", "expected single quote, found " + thisToken.lexeme);
                passFailed = true;
                return;
            }
            expressionz();
        }
        else // Expression is required, so if it isn't anything else, it has to be a numeric literal
        {
            numeric_literal();

            // todo: handling to tell it whether we are expecting numeric literal specifically, or just any expression
            if(passFailed)
            {
                failGrammar("expression", "expected expression, found " + thisToken.lexeme);
            }
        }
    }

    /**
     * Optional? If
     */
    public void numeric_literal() {
        if(passFailed) { return; }

        Token thisToken = lex.nextToken();
        if(PLUS_TOKEN.equals(thisToken.lexeme)
                || MINUS_TOKEN.equals(thisToken.lexeme)) {
            lex.nextToken();
        }
        number();
    }

    /**
     * Optional? Only after the first call.
     * Complete? Probably.
     */
    public void number() {
        if(passFailed) { return; }

        if(!NUMBER_TOKENS.contains(lex.getToken().lexeme)) {
            failGrammar("number", "expected number, found " + lex.getToken().lexeme);
            passFailed = true;
            return;
        }

        if(NUMBER_TOKENS.contains(lex.peek().lexeme)) {
            number();
        }
    }

    /**
     * Since characters are parsed as a single token, this skips a lot of processing.
     */
    public void character_literal() {
        if(passFailed) { return; }

        character();
    }

    public void character() {
        if(passFailed) { return; }

        if(lex.getToken().lexeme.length() == 3) {
            char c = lex.getToken().lexeme.charAt(1);
            if(c < 32
                    || c > 126) {
                failGrammar("character", "expected printable ascii character, found " + lex.getToken().lexeme);
                passFailed = true;
                return;
            }

        }
        else if(lex.getToken().lexeme.length() > 3) // Nonprintable ASCII
        {
            if(!BACKSLASH.equals(lex.getToken().lexeme.charAt(1)))
            {
                failGrammar("character", "expected backslash (or only one character within apostrophes), found " + lex.getToken().lexeme);
                passFailed = true;
                return;
            }
            else {
                char c = lex.getToken().lexeme.charAt(1);
                if(c >= 32
                        && c <= 126) {
                    failGrammar("character", "expected nonprintable ascii character, found " + lex.getToken().lexeme);
                    passFailed = true;
                    return;
                }
            }
        }
    }

    /**
     * fn_arr_member
     * Optional: If syntax doesn't work for this, it can safely be ignored.
     *
     * Uses peek, so if we don't have fn_arr_member, we will not modify the token.
     */
    public void fn_arr_member() {
        if(passFailed) { return; }

        Token thisToken = lex.peek();

        if(OPENING_PARENTHESIS.equals(thisToken.lexeme)) {
            lex.nextToken(); // We are now at the opening parenthesis.

            if(!CLOSING_PARENTHESIS.equals(lex.nextToken())) {
                argument_list();
            }
        }
        else if(OPENING_BRACKET.equals(thisToken.lexeme)) {
            lex.nextToken(); // We are now at the opening bracket.

            expression();
            if(!CLOSING_PARENTHESIS.equals(lex.nextToken())) {
                failGrammar("fn_arr_member", "expected closing parenthesis, found " + lex.getToken().lexeme);
                passFailed = true;
                return;
            }
        }
    }

    /**
     * Non-optional. When this is optional, presence/absence
     * should be checked in the calling function.
     *
     * Coming from fn_arr_member, the parenthesis check will
     * put the token on the first part of the expression,
     * so no token advancement will be needed.
     *
     * lex.getToken() should be at the first token of the
     * expression
     *
     * Complete? Probably.
     */
    public void argument_list() {
        if(passFailed) { return; }

        expression();

        Token nextToken = lex.peek();
        while(LIST_TOKEN.equals(nextToken.lexeme)) {
            lex.nextToken(); // Sets current token to the comma

            expression();

            nextToken = lex.peek();
        }
    }

    /**
     * Optional only.
     *
     * Complete? Probably.
     */
    public void member_refz() {
        if(passFailed) { return; }

        if(DOT_TOKEN.equals(lex.peek().lexeme)) {
            lex.nextToken(); // Current token is now "."

            if(!TokenType.IDENTIFIER.equals(lex.nextToken().type)) {
                failGrammar("member_refz", "expected identifier, found " + lex.getToken().lexeme);
                passFailed = true;
                return;
            }
            fn_arr_member();
            member_refz();
        }
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
     *
     * Complete? Probably.
     */
    public void assignment_expression() {
        if(passFailed) { return; }

        Token thisToken = lex.nextToken();
        if(KW_THIS.equals(thisToken.lexeme)) {
            return;
        }
        else if(KW_NEW.equals(thisToken.lexeme)) {
            type();
            new_declaration();
        }
        else if(KW_ATOI.equals(thisToken.lexeme)
                || KW_ITOA.equals(thisToken.lexeme)) {
            if(!OPENING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                failGrammar("assignment_expression",  thisToken.lexeme + "' not followed by opening parenthesis");
                passFailed = true;
                return;
            }
            expression();
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

    /**
     * Not optional.
     *
     * Complete? Probably.
     */
    public void new_declaration() {
        if(passFailed) { return; }

        Token typeToken = lex.nextToken();
        if(OPENING_PARENTHESIS.equals(typeToken.lexeme))
        {
            if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                argument_list();
                if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                    failGrammar("new_declaration", "expected closing parenthesis, found " + lex.getToken().lexeme);
                    passFailed = true;
                    return;
                }
            }
        }
        else if(OPENING_BRACKET.equals(typeToken.lexeme)) {
            expression();
            if(!CLOSING_BRACKET.equals(lex.nextToken().lexeme)) {
                failGrammar("new_declaration", "expected closing bracket, found " + lex.getToken().lexeme);
                passFailed = true;
                return;
            }
        }

        failGrammar("new_declaration", "new_declaration expected opening parenthesis or opening bracket, found " + lex.getToken().lexeme);
        passFailed = true;
        return;
    }

    /**
     * This is required, but failures happen in identifier() rather than here
     *
     * Complete? Discuss with professor
     *
     * todo: discuss with prof welborn
     */
    public void type() {
        if(passFailed) { return; }

        Token typeToken = lex.getToken();
        if(!isType(typeToken.lexeme))
        {
            identifier();
        }
    }

    /**
     * Fails if identifier is not present
     *
     * Complete? Discuss with professor
     *
     * todo: discuss with prof welborn
     */
    public void identifier() {
        if(passFailed) { return; }

        if(!TokenType.IDENTIFIER.equals(lex.getToken().type)) {
            failGrammar("type", "expected type or identifier, found " + lex.getToken().lexeme);
            passFailed = true;
            return;
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
