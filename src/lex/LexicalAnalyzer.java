package lex;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.RegularExpression;
import com.sun.xml.internal.ws.util.StringUtils;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/6/14
 * Time: 4:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class LexicalAnalyzer {
    private static final List<String> KEYWORDS = Arrays.asList("if", "while", "true", "false", "return",
            // todo: these are all unconfirmed
            "int", "char", "boolean");

    private static RegularExpression numEx = new RegularExpression("^\\d+$");
    private static RegularExpression charEx = new RegularExpression("^'.'$");
    private static RegularExpression identEx = new RegularExpression("^[_a-zA-Z][_a-zA-Z0-9]*$");

    private BufferedReader file;
    private List<Token> tokens;
    private Token currentToken;
    private String currentLine;

    public LexicalAnalyzer(BufferedReader file)
    {
        this.file = file;
        this.currentLine = "";
    }

    public Token getToken() {
        return currentToken;
    }

    public Token nextToken()
    {
        try {
            // If we already used the last line, get a new one from the file
            // If the rest of the line is a comment, skip to the next line
            while(currentLine == null || currentLine.isEmpty()
                    || (currentLine.length() > 1 && currentLine.startsWith("//"))) {
                currentLine = file.readLine().trim();
            }

            // Build the token and remove it from the line
            String[] split = currentLine.split("[ \t]+");
            currentToken = getRealToken(split[0]);
            currentLine = currentLine.replaceFirst(Pattern.quote(currentToken.lexeme), "").trim();
            return currentToken;
        }
        catch(Exception e) {
            return null;
        }
    }

    public Token getRealToken(String s) {
        if(s == null || s.isEmpty()) {
            return null; //todo: throw exception?
        }

        if((s.length() > 2)
                && charEx.matches(s.substring(0, 3))) {
            return new Token(s.substring(0, 3), TokenType.CHARACTER);
        }
        if(isSymbol(s.charAt(0))) {
            return new Token(s.substring(0,1), TokenType.SYMBOL);
        }
        if(s.length() > 1 && isSymbol(s.substring(0,2))) {
            return new Token(s.substring(0,2), TokenType.SYMBOL);
        }
        if(isPunctuation(s.charAt(0))) {
            return new Token(s.substring(0, 1), TokenType.PUNCTUATION);
        }

        for(int i = 1; i < s.length(); i++){
//            if((i < s.length() - 3)
//                    && charEx.matches(s.substring(i, i+3))) {
//                return buildToken(s.substring(0, i));
//            }
            if((i < s.length() - 1)
                    && isSymbol(s.substring(i, i+1))) {
                return buildToken(s.substring(0,i));
            }
            if(isSymbol(s.charAt(i))) {
                return buildToken(s.substring(0,i));
            }
            if(isPunctuation(s.charAt(i))) {
                return buildToken(s.substring(0, i));
            }
        }
        return buildToken(s);
    }

    /**
     * Takes a string that's been checked for punctuation, figures out
     * what kind of token it is, and builds and returns the appropriate
     * object
     * @param s
     * @return token
     */
    public Token buildToken(String s) {
        if(numEx.matches(s)) {
            return new Token(s, TokenType.NUMBER);
        }
//        else if(charEx.matches(s)) {
//            return new Token(s, TokenType.CHARACTER);
//        }
        else if(isKeyword(s)) {
            return new Token(s, TokenType.KEYWORD);
        }
        else if(identEx.matches(s)) { // todo: make type declarations their own TokenType so we can not classify an identifier if it's in the wrong place?
            return new Token(s, TokenType.IDENTIFIER);
        }
        else

        return new Token(s, TokenType.UNKNOWN);
    }

    public String checkOperands(String s) {
        if(s == null || s.isEmpty()) {
            return s; //todo: throw exception?
        }



        if(isSymbol(s.charAt(0))) {
            return s.substring(0,1);
        }
        if(s.length() > 1 && isSymbol(s.substring(0,2))) {
            return s.substring(0,2);
        }

        for(int i = 1; i < s.length(); i++){
            if((i < s.length() - 1)
                    && isSymbol(s.substring(i, i+1))) {
                return s.substring(0, i);
            }
            if(isSymbol(s.charAt(i))) {
                return s.substring(0, i-1);
            }
        }
        return s;
    }

    public boolean isSymbol(char c) {
        return (c == '+') || (c == '-') || (c == '*') || (c == '/')
                || (c == '%') // todo: is this something we need?
                || (c == '<') || (c == '>')
                || (c == '=')
                || (c == '[')
                || (c == ']')
                || (c == '{')
                || (c == '}')
                || (c == '(')
                || (c == ')');
    }

    private boolean isSymbol(String s) {
        return "&&".equals(s) || "||".equals(s);
    }

    private boolean isPunctuation(char c) {
        return (c == ',') || (c == ';') || (c == '.')
                // todo: verify these
                || (c == '!') || (c == '?') //|| (c == '\"')
                || (c == ':');
    }

    private boolean isKeyword(String s) {
        return KEYWORDS.contains(s);
    }
}
