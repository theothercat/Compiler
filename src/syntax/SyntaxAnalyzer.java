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
    private boolean passTwo = false;
    private boolean passFailed = false;

    public SyntaxAnalyzer(LexicalAnalyzer lex) {
        this.lex = lex;
        this.symbolTable = SymbolTable.get();

        if(syntaxLog == null) {
            syntaxLog = new Log("syntax.log");
        }
    }

    public void closeLogs() {
        syntaxLog.close();
        lex.closeLogs();
        symbolTable.closeLogs();
    }

    public void pass() throws Exception {
        try {
            if(passFailed) { return; }

//        lex.nextToken();
            compilation_unit();
            passTwo = true;
        }
        catch (NullPointerException e) {
//            syntaxLog.log("Error: unexpected end of file on line " + lex.getLineNumber());
//            System.out.println("Error: unexpected end of file on line " + lex.getLineNumber());
        }

        System.out.println();
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
            if(TokenType.EMPTY.equals(lex.getToken().type)) {
                failGrammar("compilation_unit", "'void' keyword or class_declaration");
                return;
            }

            syntaxLog.debug("Assuming class declaration");
            class_declaration(); // Don't advance token
            if(passFailed) { return; }
        }

        if(KW_MAIN.equals(lex.nextToken().lexeme)) // We already found void, so use nextToken()
        {
            concatScope("main");
            HashMap<String, String> data = new HashMap<String, String>(3);
            data.put("returnType", "void");
            data.put("Param", "[]");
            data.put("accessMod", "public");
            addToSymbolTable("main", SymbolTableEntryType.METHOD, data);
        }
        else {
            failGrammar("compilation_unit", "'main' keyword");
            return;
        }

        if(!OPENING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
            failGrammar("compilation_unit", "opening parenthesis");
            return;
        }
        if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
            failGrammar("compilation_unit", "closing parenthesis");
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
            failGrammar("class_declaration", "'class' keyword");
            return;
        }

        if(TokenType.IDENTIFIER.equals(lex.nextToken().type)) // todo: this should be class_name
        {
            syntaxLog.debug("Found identifier for class declaration, adding class '" + lex.getToken().lexeme + "' to symbol table");
            addToSymbolTable(lex.getToken().lexeme, SymbolTableEntryType.CLASS, null); // todo: empty hashmap?
        }
        else {
            failGrammar("class_declaration", "class_name");
            return;
        }

        if(OPENING_BRACE.equals(lex.peek().lexeme)) {
            concatScope(lex.getToken().lexeme);
            lex.nextToken(); // Advance the token now that we've adjusted the scope
        }
        else {
            failGrammar("class_declaration", "opening brace");
            return;
        }

        while(!CLOSING_BRACE.equals(lex.nextToken().lexeme)) {
            if(TokenType.EMPTY.equals(lex.getToken().type)) {
                failGrammar("class_declaration", "closing brace");
                return;
            }

            syntaxLog.debug("Assuming class member declaration");
            class_member_declaration(); // Don't advance token
            if(passFailed) { return; }
        }
        trimScope();
    }

    private void trimScope() {
        String oldScope = symbolTable.getScope();
        if(oldScope.equals("g")) {
            System.out.println("Can't trim scope!");
            System.out.println(String.format("Failure on line %d, symbol %s: can't trim scope!", lex.getLineNumber(), lex.getToken().lexeme));
            return;
        }
        String newScope = symbolTable.getScope().substring(0, symbolTable.getScope().lastIndexOf("."));
        syntaxLog.debug("Adjusting scope from " + symbolTable.getScope() + " to " + newScope);
        symbolTable.setScope(newScope);
    }

    private void concatScope(String identifier) {
        String newScope = symbolTable.getScope() + "." + identifier;
        syntaxLog.debug("Adjusting scope from " + symbolTable.getScope() + " to " + newScope);
        symbolTable.setScope(newScope);
    }

//    /**
//     * Gets token without NPE, fails if needed.
//     */
//    private Token nextToken() {
//        Token t = lex.nextToken();
//        if(TokenType.EMPTY.equals(t.type)) {
//            passFailed = true;
//            syntaxLog.log("Unexpected end of file on line " + lex.getLineNumber());
//            System.out.println("Unexpected end of file on line " + lex.getLineNumber());
//        }
//        return t;
//    }

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

            if(!TokenType.IDENTIFIER.equals(lex.nextToken().type)) {
                failGrammar("class_member_declaration", "identifier");
                return;
            }
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
            data.put("returnType", data.get("type"));
            data.remove("type");
            concatScope(identifier);

            if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                // We have some params.

                parameter_list(data); // Do not advance token - closing parenthesis check does advancement
                if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                    failGrammar("field_declaration", "closing parenthesis");
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
                    failGrammar("field_declaration", "closing bracket");
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
                failGrammar("field_declaration", "semicolon");
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
            failGrammar("constructor_declaration", "class_member_declaration (modifier or constructor_declaration)");
            return;
        }
        concatScope(lex.getToken().lexeme); // We are now in the constructor's scope.

        if(!OPENING_PARENTHESIS.equals(lex.nextToken().lexeme))
        {
            failGrammar("constructor_declaration", "class_member_declaration (modifier or constructor_declaration)");
            return;
        }

        HashMap<String, String> data = new HashMap<String, String>(); // Data map to add params to.
        if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {

            parameter_list(data); // Do not advance token - closing parenthesis check does advancement

            if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                failGrammar("constructor_declaration", "closing parenthesis");
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
            failGrammar("method_body", "opening brace");
            return;
        }

        if(!CLOSING_BRACE.equals(lex.nextToken().lexeme)) {
            while(isVariableDeclaration())
            {
                variable_declaration(); // Don't advance token - we are at the first type/identifier of the variable_declaration()

                if(passFailed) { return; }
                lex.nextToken(); // Post-advance to the token to be checked for a variable_declaration
            }

            while(!CLOSING_BRACE.equals(lex.getToken().lexeme))
            {
                if(TokenType.EMPTY.equals(lex.getToken().type)) {
                    failGrammar("method_body", "closing brace or statement");
                    return;
                }

                statement();
                lex.nextToken(); // Advance token for next check

                if(passFailed) { return; }
            }
        }
        trimScope();
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

        if(!TokenType.IDENTIFIER.equals(lex.nextToken().type)) {
            failGrammar("variable_declaration", "identifier"); // todo: this comes from multiple places. fix it to work for all?
            return;
        }
        String identifier = lex.getToken().lexeme; // Save the identifier.

        Token thisToken = lex.nextToken(); // Advance token
        if(OPENING_BRACKET.equals(thisToken.lexeme)) {
            if(!CLOSING_BRACKET.equals(lex.nextToken().lexeme)) {
                failGrammar("variable_declaration", "closing bracket");
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
            if(passFailed) { return; }
            thisToken = lex.nextToken(); // Advance token for next check.
        }

        if(!SEMICOLON.equals(thisToken.lexeme)) // MUST use getToken() in case the previous token checks failed
        {
            failGrammar("variable_declaration", "semicolon");
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


        while(LIST_TOKEN.equals(lex.peek().lexeme)) {
            lex.nextToken(); // Advance to comma
            lex.nextToken(); // Advance token - parameter() uses getToken()

            if(TokenType.EMPTY.equals(lex.getToken().type)) {
                failGrammar("parameter_list", "parameter");
                return;
            }

            paramIds.add(parameter());

            if(passFailed) { return; }
        }

        String paramList = "[";
        for(int i = 0; i < paramIds.size(); i++) {
            paramList += (paramIds.get(i) + ", ");
        }
        paramList = paramList.substring(0, paramList.length() - 2) + "]";
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

        if(!TokenType.IDENTIFIER.equals(lex.nextToken().type)) {
            failGrammar("parameter", "identifier");
        }
        if(passFailed) { return ""; }

        String paramId = lex.getToken().lexeme;

        Token thisToken = lex.peek();
        if(OPENING_BRACKET.equals(thisToken.lexeme)) {
            lex.nextToken(); // Advance token to opening bracket
            if(!CLOSING_BRACKET.equals(lex.nextToken().lexeme)) {
                failGrammar("parameter", "closing bracket");
            }
            paramData.put("type", "@:" + paramData.get("type")); // Update type to reflect that this is an array
        }
        return addToSymbolTable(paramId, SymbolTableEntryType.PARAM, paramData);
    }

    private String addToSymbolTable(String symbolLexeme, SymbolTableEntryType type, Map<String, String> data) {
        return symbolTable.add(symbolLexeme, type, data).symid;
    }
    private void addLiteralToSymbolTable(String symbolLexeme, Map<String, String> data) {
        symbolTable.addLiteral(symbolLexeme, data);
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
            failGrammar("statement, if/while", "opening parenthesis");
            return;
        }
        lex.nextToken(); // Advance token - expression() uses getToken()
        expression(); // Use nextToken() after this
        if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
            failGrammar("statement, if/while", "closing parenthesis");
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
        if(passFailed) { return; }

        if(!SEMICOLON.equals(lex.nextToken().lexeme)) {
            failGrammar("statement", "semicolon");
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
                failGrammar("statement, cin", "'>>'");
                return;
            }
            lex.nextToken(); // Advance token - expression_with_semicolon() uses getToken()
            expression_with_semicolon();
        }
        else if(KW_COUT.equals(thisToken.lexeme))
        {
            if(!COUT_TOKEN.equals(lex.nextToken().lexeme)) {
                failGrammar("statement, cout", "'<<'");
                return;
            }
            lex.nextToken(); // Advance token - expression_with_semicolon() uses getToken()
            expression_with_semicolon();
        }
        else if(OPENING_BRACE.equals(thisToken.lexeme)) {
            while(!CLOSING_BRACE.equals(lex.nextToken().lexeme)) {
                if(TokenType.EMPTY.equals(lex.getToken().type)) {
                    failGrammar("statement", "closing brace or statement");
                    return;
                }

                if(passFailed) { return; }

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
            lex.nextToken(); // Advance token - expression() calls getToken()
            expression();
            if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                failGrammar("expression", "closing parenthesis");
                return;
            }
            expressionz();
        }
        else if(TokenType.IDENTIFIER.equals(thisToken.type)) // todo: identifier should call the appropriate method instead?
        {
            // Found an identifier.

            fn_arr_member();
            if(passFailed) { return; }
            member_refz();
            if(passFailed) { return; }
            expressionz();
        }
        else if(EXPRESSION_TOKENS.contains(thisToken.lexeme)) // true, false, and null
        {
            if(!passTwo) {
                HashMap<String, String> data = new HashMap<String, String>(1);
                if(KW_NULL.equals(thisToken.lexeme)) {
                    data.put("type", "void");
                }
                else {
                    data.put("type", "bool");
                }
                addLiteralToSymbolTable(thisToken.lexeme, data);
            }

            expressionz();
        }
        else if(TokenType.CHARACTER.equals(thisToken.type)
                || TokenType.NUMBER.equals(thisToken.type)) // Character literals and numbers are a single token, so the token type can just be checked
        {
            if(!passTwo) {
                HashMap<String, String> data = new HashMap<String, String>(1);
                if(TokenType.CHARACTER.equals(thisToken.type)) {
                    data.put("type", "char");
                }
                else // TokenType.NUMBER.equals(thisToken.type)
                {
                    data.put("type", "int");
                }
                addLiteralToSymbolTable(thisToken.lexeme, data);
            }

            expressionz();
        }
        else // It wasn't any kind of expression!
        {
            // Expression is required, so if none of these is it, we failed.
            failGrammar("expression", "expression");
        }
    }

//    /**
//     * Optional? Only after the first call.
//     * Complete? Probably.
//     */
//    public void number() {
//        if(passFailed) { return; }
//
//        if(!NUMBER_TOKENS.contains(lex.getToken().lexeme)) {
//            return;
//        }
//
//        if(NUMBER_TOKENS.contains(lex.peek().lexeme)) {
//            number();
//        }
//    }

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

            if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                argument_list();

                if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                    failGrammar("fn_arr_member", "closing parenthesis");
                }
            }
        }
        else if(OPENING_BRACKET.equals(thisToken.lexeme)) {
            lex.nextToken(); // We are now at the opening bracket.
            lex.nextToken(); // Advance token - expression() calls getToken()

            expression();
            if(!CLOSING_BRACKET.equals(lex.nextToken().lexeme)) {
                failGrammar("fn_arr_member", "closing bracket");
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
            lex.nextToken(); // Advance token - expression() calls getToken()

            expression();

            nextToken = lex.peek();
            if(passFailed) { return; }
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
            if(passTwo) {
                String className = lex.getToken().lexeme; // todo: do something with this
            }

            lex.nextToken(); // Current token is now "."

            if(!TokenType.IDENTIFIER.equals(lex.nextToken().type)) {
                failGrammar("member_refz", "identifier");
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
            lex.nextToken(); // Advance to the = token.
            lex.nextToken(); // Advance token - assignment_expression() uses getToken()
            assignment_expression();
        }
        else if(EXPRESSIONZ_TOKENS.contains(thisToken.lexeme)) {
            lex.nextToken(); // Advance to the expressionZ token.
            lex.nextToken(); // Advance token - expression() uses getToken()
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
            lex.nextToken();
            type();
            lex.nextToken();
            new_declaration();
        }
        else if(KW_ATOI.equals(thisToken.lexeme)
                || KW_ITOA.equals(thisToken.lexeme)) {
            if(!OPENING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                failGrammar("assignment_expression",  "opening parenthesis");
                return;
            }
            lex.nextToken(); // Advance token - expression() calls getToken()
            expression();
            if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                failGrammar("assignment_expression", "closing parenthesis");
                return;
            }
        }
        else {
            expression(); // Don't advance token - the token we checked wasn't used yet.
        }
    }

    public void failGrammar(String ruleFailed, String expected) {
        Token t = lex.getToken();

        System.out.println(String.format("Bad %s on line %d: expected %s, found %s",
                ruleFailed,
                lex.getLineNumber(),
                expected,
                TokenType.EMPTY.equals(t.type)
                        ? "end of line"
                        : t.lexeme
        ));
        passFailed = true;
    }

    /**
     * Not optional.
     *
     * Complete? Probably.
     */
    public void new_declaration() {
        if(passFailed) { return; }

        Token typeToken = lex.getToken();
        if(OPENING_PARENTHESIS.equals(typeToken.lexeme))
        {
            if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                argument_list();
                if(!CLOSING_PARENTHESIS.equals(lex.nextToken().lexeme)) {
                    failGrammar("new_declaration", "closing parenthesis");
                    return;
                }
            }
        }
        else if(OPENING_BRACKET.equals(typeToken.lexeme)) {
            lex.nextToken(); // Advance token - expression() uses getToken()
            expression();
            if(!CLOSING_BRACKET.equals(lex.nextToken().lexeme)) {
                failGrammar("new_declaration", "closing bracket");
                return;
            }
        }
        else {
            failGrammar("new_declaration", "opening parenthesis or opening bracket");
        }
    }

    /**
     * Optional? No.
     *
     * Uses getToken()
     *
     * Complete? Round 2
     *
     */
    public void type() {
        if(passFailed) { return; }

        Token typeToken = lex.getToken();
        if(!isType(typeToken.lexeme) && !TokenType.IDENTIFIER.equals(typeToken.type))
        {
            failGrammar("type", "type or identifier");
        }
    }

    public boolean isType(String s) {
        return TYPES.contains(s);
    }
}
