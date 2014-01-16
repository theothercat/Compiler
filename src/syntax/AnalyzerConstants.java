package syntax;

import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/15/14
 * Time: 4:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class AnalyzerConstants {
    public static final List<String> EXPRESSION_TOKENS = Arrays.asList("true", "false", "null");
    public static final List<String> EXPRESSIONZ_TOKENS = Arrays.asList("&&", "||", "==", "!=", "<=", ">=", "<", ">", "+", "-", "*", "/");
    public static final List<String> STATEMENT_TOKENS = Arrays.asList("if", "while", "return", "cout", "cin");

    public static final String SEMICOLON = ";";
    public static final String ASSIGNMENT_OPERATOR = "=";
    public static final String OPENING_PARENTHESIS = "(";
    public static final String CLOSING_PARENTHESIS = ")";
    public static final String OPENING_BRACE = "{";
    public static final String CLOSING_BRACE = "}";
    public static final String CIN_TOKEN = ">>";
    public static final String COUT_TOKEN= "<<";

    public static final String KW_THIS = "this";
    public static final String KW_NEW = "new";
    public static final String KW_ATOI = "atoi";
    public static final String KW_ITOA = "itoa";

    public static final String KW_IF = "if";
    public static final String KW_ELSE = "else";
    public static final String KW_WHILE = "while";
    public static final String KW_RETURN = "return";
    public static final String KW_CIN = "cin";
    public static final String KW_COUT = "cout";

}
