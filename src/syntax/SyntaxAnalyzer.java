package syntax;

import lex.LexicalAnalyzer;
import lex.Token;
import lex.TokenType;
import log.Log;
import syntax.symbol.SymbolTable;
import syntax.symbol.SymbolTableEntry;
import syntax.symbol.SymbolTableEntryType;

import java.util.*;

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
    private static final List<String> TYPES = Arrays.asList("int", "char", "bool", "void");
    private static final String CLASS_STR = "class";

    private static Log syntaxLog = null;

    private SymbolTable symbolTable;
    private LexicalAnalyzer lex;
    private String scope;
    private boolean passFailed = false;

    public SyntaxAnalyzer(LexicalAnalyzer lex) {
        this.lex = lex;
        this.symbolTable = new SymbolTable();

        if(syntaxLog == null) {
            syntaxLog = new Log("syntax.log");
        }
    }

    public void pass() throws Exception {
        if(passFailed) { return; }

//        lex.nextToken();
        compilation_unit();
    }

    /**
     * Grabs the first token and starts the entire pass.
     *
     * Uses: getToken()
     *
     * Optional? Doesn't matter.
     *
     * Complete? Round 2.
     */
    private void compilation_unit() {
        if(passFailed) { return; }

        while(!KW_VOID.equals(lex.nextToken().lexeme)) {
            syntaxLog.debug("Assuming class declaration");
            class_declaration(); // Don't advance token
        }

        if(!KW_MAIN.equals(lex.nextToken().lexeme)) {
            failGrammar("compilation_unit", "expected 'main' keyword, found " + lex.getToken().lexeme);
            return;
        }
        if(!OPENING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
            failGrammar("compilation_unit", "expected opening parenthesis, found " + lex.getToken().lexeme);
            return;
        }
        if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
            failGrammar("compilation_unit", "expected closing parenthesis, found " + lex.getToken().lexeme);
            return;
        }
        lex.nextToken(); // Advance token - method_body() calls getToken()
        method_body();
    }

    /**
     * Class keyword should be current token.
     *
     * Uses getToken()
     *
     * Optional? No.
     *
     * Complete? Round 2.5.
     */
    private void class_declaration() {
        if(passFailed) { return; }

        if(!KW_CLASS.equals(lex.getToken().lexeme)) {
            failGrammar("class_declaration", "expected 'class' keyword, found " + lex.getToken().lexeme);
            return;
        }

        if(TokenType.IDENTIFIER.equals(lex.nextToken().type)) // todo: this should be class_name
        {
            syntaxLog.debug("Found identifier for class declaration, adding class '" + lex.getToken().lexeme + "' to symbol table");
            addToSymbolTable(lex.getToken().lexeme, SymbolTableEntryType.CLASS, null); // todo: empty hashmap?
        }
        else {
            failGrammar("class_declaration", "expected class_name, found " + lex.getToken().lexeme);
            return;
        }

        if(OPENING_BRACE.equals(lex.peek())) {
            concatScope(lex.getToken().lexeme);
            lex.nextToken(); // Advance the token now that we've adjusted the scope
        }
        else {
            failGrammar("class_declaration", "expected opening brace, found " + lex.getToken().lexeme);
            return;
        }

        while(!CLOSING_BRACE.equals(lex.nextToken())) {
            syntaxLog.debug("Expecting class member declaration");
            class_member_declaration(); // Don't advance token
        }
        trimScope();
    }

    private void trimScope() {
        String newScope = symbolTable.getScope().substring(0, symbolTable.getScope().lastIndexOf("."));
        syntaxLog.debug("Adjusting scope from " + symbolTable.getScope() + " to " + newScope);
        symbolTable.setScope(newScope);
    }

    private void concatScope(String identifier) {
        String newScope = symbolTable.getScope() + "." + identifier;
        syntaxLog.debug("Adjusting scope from " + symbolTable.getScope() + " to " + newScope);
        symbolTable.setScope(newScope);
    }

    /**
     * Current token should be part of class_member_declaration.
     *
     * Uses: getToken()
     *
     * Optional? No?
     *
     * Complete? Round 2.
     */
    private void class_member_declaration() {
        if(passFailed) { return; }

        if(MODIFIER_TOKENS.contains(lex.getToken().lexeme)) {
            HashMap<String, String> data = new HashMap<String, String>(2);
            data.put("accessMod", lex.getToken().lexeme);

            lex.nextToken(); // Advance token - type() calls getToken()
            type();
            data.put("type", lex.getToken().lexeme);

            lex.nextToken(); // Advance token - identifier() calls getToken()
            identifier();
            if(passFailed) { return; }

            String identifier = lex.getToken().lexeme;
            lex.nextToken(); // Advance token - field_declaration() calls getToken()
            field_declaration(identifier, data); // Actual class member will be declared in here.
        }
        else {
            syntaxLog.debug("Did not find a modifier, assuming constructor declaration.");
            constructor_declaration(); // Do not advance token - current token was not modifier, so it should be identifier.
        }
    }

    /**
     * Uses getToken()
     * Ends with: current token is semicolon (or fail)
     *
     * Optional? No.
     *
     * Complete? Probably.
     */
    private void field_declaration(String identifier, HashMap<String, String> data) {
        if(passFailed) { return; }

        if(OPENING_PARENTHESIS.equals(lex.getToken().lexeme)) {
            // This is a method declaration.
            concatScope(identifier);

            if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                // We have some params.

                parameter_list(data); // Do not advance token - closing parenthesis check does advancement
                if(CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                    failGrammar("field_declaration", "expected closing bracket, found " + lex.getToken().lexeme);
                    return;
                }
                addToSymbolTable(identifier, SymbolTableEntryType.METHOD, data);
            }
            lex.nextToken(); // Advance token - method_body() calls getToken()
            method_body();
        }
        else {
            if(OPENING_BRACKET.equals(lex.getToken().lexeme)) {
                if(!CLOSING_BRACKET.equals(lex.nextToken().lexeme)) {
                    failGrammar("field_declaration", "expected closing bracket, found " + lex.getToken().lexeme);
                    return;
                }
                data.put("type", "@:" + data.get("type"));
                lex.nextToken(); // Advance token for getToken() here
            }

            // Seems like a good time to add to symbol table.
            addToSymbolTable(identifier, SymbolTableEntryType.INSTANCE_VAR, data);

            if(ASSIGNMENT_OPERATOR.equals(lex.getToken().lexeme)) {
                lex.nextToken(); // Advance token - assignment_expression() uses getToken()
                assignment_expression();
                lex.nextToken(); // Advance token for getToken()
            }

            if(!SEMICOLON.equals(lex.getToken().lexeme)) {
                failGrammar("field_declaration", "expected semicolon, found " + lex.getToken().lexeme);
            }
        }
    }

        /**
         * Uses getToken()
         *
         * Optional? Not once it's been called.
         *
         * Declares a constructor.
         *
         * Complete? Probably.
         */
    private void constructor_declaration() {
        if(passFailed) { return; }

        if(!TokenType.IDENTIFIER.equals(lex.getToken().type))
        {
            failGrammar("constructor_declaration", "expected class_member_declaration (modifier or constructor_declaration), found " + lex.getToken().lexeme);
            return;
        }
        concatScope(lex.getToken().lexeme); // We are now in the constructor's scope.

        if(!OPENING_PARENTHESIS.equals(lex.nextToken().lexeme))
        {
            failGrammar("constructor_declaration", "expected class_member_declaration (modifier or constructor_declaration), found " + lex.getToken().lexeme);
            return;
        }

        HashMap<String, String> data = new HashMap<String, String>(); // Data map to add params to.
        if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {

            parameter_list(data); // Do not advance token - closing parenthesis check does advancement

            if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                failGrammar("constructor_declaration", "expected closing parenthesis, found " + lex.getToken().lexeme);
                return;
            }
        }
        addToSymbolTable(lex.getToken().lexeme, SymbolTableEntryType.METHOD, data);
        // todo: add logging
        lex.nextToken(); // Advance token - method_body() calls getToken()
        method_body();
    }

    /**
     * Uses getToken()
     * Ends with: current token is closing brace
     *
     * Optional? No.
     *
     * Complete? Probably.
     */
    private void method_body() {
        if(passFailed) { return; }

        if(!OPENING_BRACE.equals(lex.getToken().lexeme)) {
            failGrammar("method_body", "expected opening brace, found " + lex.getToken().lexeme);
            return;
        }

        if(!CLOSING_BRACE.equals(lex.nextToken().lexeme)) {
            while(isVariableDeclaration())
            {
                variable_declaration(); // Don't advance token - we are at the first type/identifier of the variable_declaration()
                // Do not post-advance token; while-loop requires advancement via nextToken() since statement() won't post-advance
            }

            while(!CLOSING_BRACE.equals(lex.nextToken().lexeme))
            {
                statement(); // Don't advance token - closing brace check does advancement
            }
            trimScope();
        }
    }

    /**
     * Helper method to determine whether we are dealing with a variable_declaration or a statement.
     * @return whether or not this is a variable_declaration
     */
    private boolean isVariableDeclaration() {
        return TYPES.contains(lex.getToken().lexeme) // statements can't begin with primitive types; only variable_declarations can.
                || (TokenType.IDENTIFIER.equals(lex.getToken().type) // An identifier can be a type, but it's only a variable_declaration if it's followed by another identifier.
                && TokenType.IDENTIFIER.equals(lex.peek().type));
    }

    /**
     * Uses getToken()
     * Use of peek means that this will expect a nextToken()
     *
     * Optional? Yes.
     *
     * Complete? Probably.
     */
    private void variable_declaration() {
        if(passFailed) { return; }

        HashMap<String, String> data = new HashMap<String, String>();
        type(); // Don't advance - type() uses getToken() and this should be at the correct position for that.
        if(passFailed) { return; }
        data.put("type", lex.getToken().lexeme);

        lex.nextToken(); // Advance token - identifier() uses getToken()
        identifier();
        if(passFailed) { return; }
        String identifier = lex.getToken().lexeme; // Save the identifier.

        Token thisToken = lex.nextToken(); // Advance token
        if(OPENING_BRACKET.equals(thisToken.lexeme)) {
            if(!CLOSING_BRACKET.equals(lex.nextToken().lexeme)) {
                failGrammar("variable_declaration", "expected closing bracket, found " + lex.getToken().lexeme);
                return;
            }
            data.put("type", "@:" + data.get("type")); // Update type to reflect that this is an array.
            thisToken = lex.nextToken(); // Advance token for next check.
        }

        // Seems like a good time to add this to the symbol table.
        addToSymbolTable(identifier, SymbolTableEntryType.LOCAL_VAR, data);

        if(ASSIGNMENT_OPERATOR.equals(thisToken.lexeme)) {
            lex.nextToken(); // Advance token - assignment_expression() uses getToken()
            assignment_expression();
            thisToken = lex.nextToken(); // Advance token for next check.
        }

        if(!SEMICOLON.equals(thisToken.lexeme)) // MUST use getToken() in case the previous token checks failed
        {
            failGrammar("variable_declaration", "expected semicolon, found " + lex.getToken().lexeme);
            return;
        }


    }

    /**
     * Expects token to be where needed in parameter()
     *
     * Optional? No.
     *
     * Complete? Probably.
     */
    private void parameter_list(HashMap<String, String> data) {
        if(passFailed) { return; }

        List<String> paramIds = new ArrayList<String>();

        paramIds.add(parameter());


        while(!LIST_TOKEN.equals(lex.peek().lexeme)) {
            lex.nextToken(); // Advance to comma
            lex.nextToken(); // Advance token - parameter() uses getToken()
            paramIds.add(parameter());
        }

        String paramList = "[";
        for(int i = 0; i < paramIds.size(); i++) {
            paramList += (paramIds.get(i) + ", ");
        }
        paramList = paramList.substring(0, paramList.length() - 1) + "]";
        syntaxLog.debug("Adding params to data: " + paramList);
        data.put("Param", paramList);
    }

    /**
     * Expects the token to be in the position required for type()
     *
     * Optional? No.
     *
     * Complete? Probably.
     *
     * @return The symid of the parameter that was declared.
     */
    private String parameter() {
        if(passFailed) { return ""; }

        HashMap<String, String> paramData = new HashMap<String, String>();
        type();
        if(passFailed) { return ""; }

        paramData.put("type", lex.getToken().lexeme);
        paramData.put("accessMod", "private");

        lex.nextToken(); // Advance token - identifier() uses getToken()
        identifier();
        if(passFailed) { return ""; }

        String paramId = lex.getToken().lexeme;

        Token thisToken = lex.peek();
        if(OPENING_BRACKET.equals(thisToken.lexeme)) {
            lex.nextToken(); // Advance token to opening bracket
            if(!CLOSING_BRACKET.equals(lex.nextToken().lexeme)) {
                failGrammar("parameter", "expected closing bracket, found " + lex.getToken().lexeme);
            }
            paramData.put("type", "@:" + paramData.get("type")); // Update type to reflect that this is an array
        }
        return addToSymbolTable(paramId, SymbolTableEntryType.PARAM, paramData);
    }

    private String addToSymbolTable(String symbolLexeme, SymbolTableEntryType type, Map<String, String> data) {
        return symbolTable.add(symbolLexeme, type, data).symid;
    }

    /**
     * Used for if and while;
     * Uses getToken()
     *
     * Shorthand for 'if' and 'while' cases to get:
     * Opening parenthesis
     * Expression
     * Closing parenthesis
     * Statement
     */
    private void if_while() {
        if(passFailed) { return; }

        if(!OPENING_PARENTHESIS.equals(lex.getToken().lexeme)) {
            failGrammar("statement, if/while", "expected opening parenthesis, found " + lex.getToken().lexeme);
            return;
        }
        expression(); // Use nextToken() after this
        if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
            failGrammar("statement, if/while", "expected opening parenthesis, found " + lex.getToken().lexeme);
            return;
        }
        lex.nextToken(); // Advance token - statement() uses getToken()
        statement();
    }

    /**
     * Shorthand for an expression with a semicolon check.
     * Used in several places within statement()
     *
     * Uses getToken()
     */
    private void expression_with_semicolon() {
        if(passFailed) { return; }

        expression();

        if(!SEMICOLON.equals(lex.nextToken().lexeme)) {
            failGrammar("statement, cin/cout", "expected semicolon, found " + lex.getToken().lexeme);
            return;
        }
    }

    /**
     * Uses getToken()
     *
     * Optional? No.
     *
     * Complete? Hopefully.
     */
    public void statement() {
        if(passFailed) { return; }

        Token thisToken = lex.getToken();
        if(KW_WHILE.equals(thisToken.lexeme)) {
            lex.nextToken(); // Advance token - if_while() uses getToken()
            if_while();
        }
        else if(KW_IF.equals(thisToken.lexeme)) {
            lex.nextToken(); // Advance token - if_while() uses getToken()
            if_while();

            thisToken = lex.peek();
            if(KW_ELSE.equals(thisToken.lexeme)) {
                lex.nextToken(); // Advance token to the else token
                lex.nextToken(); // Advance token - statement() uses getToken()
                statement();
            }
        }
        else if(KW_RETURN.equals(thisToken.lexeme)) {
            if(!SEMICOLON.equals(lex.nextToken().lexeme)) {
                expression_with_semicolon(); // Don't advance token - semicolon check did advancement already.
            }
        }
        else if(KW_CIN.equals(thisToken.lexeme))
        {
            if(!CIN_TOKEN.equals(lex.nextToken().lexeme)) {
                failGrammar("statement, cin", "expected '>>', found " + lex.getToken().lexeme);
                return;
            }
            lex.nextToken(); // Advance token - expression_with_semicolon() uses getToken()
            expression_with_semicolon();
        }
        else if(KW_COUT.equals(thisToken.lexeme))
        {
            if(!COUT_TOKEN.equals(lex.nextToken().lexeme)) {
                failGrammar("statement, cout", "expected '<<', found " + lex.getToken().lexeme);
                return;
            }
            lex.nextToken(); // Advance token - expression_with_semicolon() uses getToken()
            expression_with_semicolon();
        }
        else if(OPENING_BRACE.equals(thisToken.lexeme)) {
            while(!CLOSING_BRACE.equals(lex.nextToken().lexeme)) {
                statement(); // Don't advance token - closing brace check did advancement already
            }
        }
        else {
            syntaxLog.debug("Assuming statement is expression_with_semicolon.");
            expression_with_semicolon(); // Don't advance token - checking of all other possible statements ensures this is the right token.
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
                return;
            }

        }
        else if(lex.getToken().lexeme.length() > 3) // Nonprintable ASCII
        {
            if(!BACKSLASH.equals(lex.getToken().lexeme.charAt(1)))
            {
                failGrammar("character", "expected backslash (or only one character within apostrophes), found " + lex.getToken().lexeme);
                return;
            }
            else {
                char c = lex.getToken().lexeme.charAt(1);
                if(c >= 32
                        && c <= 126) {
                    failGrammar("character", "expected nonprintable ascii character, found " + lex.getToken().lexeme);
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
            lex.nextToken(); // Advance token - assignment_expression() uses getToken()
            assignment_expression();
        }
        else if(EXPRESSIONZ_TOKENS.contains(thisToken.lexeme)) {
            lex.nextToken();
            expression();
        }
    }

    /**
     * Uses getToken()
     *
     * Requires something after the equal sign.
     *
     * Complete? Probably.
     */
    public void assignment_expression() {
        if(passFailed) { return; }

        Token thisToken = lex.getToken();
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
                return;
            }
            expression();
            if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                failGrammar("assignment_expression",  thisToken.lexeme + "' not followed by closing parenthesis");
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
                    return;
                }
            }
        }
        else if(OPENING_BRACKET.equals(typeToken.lexeme)) {
            expression();
            if(!CLOSING_BRACKET.equals(lex.nextToken().lexeme)) {
                failGrammar("new_declaration", "expected closing bracket, found " + lex.getToken().lexeme);
                return;
            }
        }

        failGrammar("new_declaration", "new_declaration expected opening parenthesis or opening bracket, found " + lex.getToken().lexeme);
        return;
    }

    /**
     * This is required, but failures happen in inner identifier() rather than here
     *
     * Uses getToken()
     * Ends with: currentToken.type is identifier
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
            failGrammar("identifier", "expected (type or) identifier, found " + lex.getToken().lexeme); // todo: this comes from multiple places. fix it to work for all?
            return;
        }
    }

    public boolean isType(String s) {
        return TYPES.contains(s);
    }
}
