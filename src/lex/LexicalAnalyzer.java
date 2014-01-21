package lex;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.RegularExpression;
import log.Log;

import java.io.BufferedReader;
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
    private static final List<String> KEYWORDS = Arrays.asList(
            "atoi",
            "bool",
            "class", "char", "cin", "cout",
            "else",
            "false",
            "if", "int", "itoa",
            "main",
            "new", "null",
            "object",
            "public", "private",
            "return",
            "string",
            "this", "true",
            "void",
            "while");
    private static final int READ_AHEAD_LIMIT = 100000;

    private static RegularExpression numEx = new RegularExpression("^\\d+$");
    private static RegularExpression charEx = new RegularExpression("^'.'$");
    private static RegularExpression identEx = new RegularExpression("^[_a-zA-Z][_a-zA-Z0-9]*$");

    private static Log lexLog = null;

    private BufferedReader file;
    private Token currentToken;
    private String currentLine;
    private int lineNumber;

    public LexicalAnalyzer(BufferedReader file)
    {
        this.file = file;
        this.currentLine = "";
        this.lineNumber = 0;

        if(lexLog == null) {
            lexLog = new Log("lex.log");
        }
    }

    public Token getToken() {
        return currentToken;
    }

    public Token nextToken()
    {
        try {
            // If we already used the last line, get a new one from the file
            // If the rest of the line is a comment, skip to the next line
            while(currentLine != null
                    && (currentLine.isEmpty()
                    || (currentLine.length() > 1 && currentLine.startsWith("//")))) {
                ++lineNumber;
                currentLine = file.readLine();
                if(currentLine != null) { currentLine = currentLine.trim(); }
            }
            if(currentLine == null) {
                currentToken = null;
                lexLog.debug("nextToken() reached end of file and returned null");
                return null;
            }

            // Build the token and remove it from the line
            String[] split = currentLine.split("[ \t]+");
            currentToken = getRealToken(split[0]);
            currentLine = currentLine.replaceFirst(Pattern.quote(currentToken.lexeme), "").trim();
            lexLog.debug("nextToken() got new token '" + currentToken.lexeme + "' of type " + currentToken.type.name());
            return currentToken;
        }
        catch(Exception e) {
            System.out.println("Exception in nextToken(): line " + lineNumber);
            if(currentToken != null) {
                System.out.println("Current token: type = " + currentToken.type + ", lexeme = " + currentToken.lexeme);
            }
            e.printStackTrace();
            return null;
        }
    }

    // Peeks ahead. If more data needs to be taken from the file,
    // the file will be marked and will be reset when the peek ends.
    public Token peek()
    {
        int thisLineNumber = lineNumber;
        try {
            String peekLine = (currentLine == null || currentLine.trim().isEmpty() || currentToken == null)
                    ? "" : currentLine;

            // If we already used the last line, get a new one from the file
            // If the rest of the line is a comment, skip to the next line
            file.mark(READ_AHEAD_LIMIT);
            if(peekLine.isEmpty()
                    || (peekLine.length() > 1 && peekLine.startsWith("//"))) {
                while(peekLine != null && (peekLine.isEmpty()
                                || (peekLine.length() > 1 && peekLine.startsWith("//")))) {
                    ++thisLineNumber;
                    peekLine = file.readLine();

                    if(peekLine != null) { peekLine = peekLine.trim(); }
                }
            }
            file.reset();

            if(peekLine == null) {
                lexLog.debug("peek() reached end of file and returned null");
                return null;
            }

            // Build the token and remove it from the line
            String[] split = peekLine.split("[ \t]+");
            Token returnToken = getRealToken(split[0]);
            lexLog.debug("nextToken() got new token '" + returnToken.lexeme + "' of type " + returnToken.type.name());
            return returnToken;
        }
        catch(Exception e) {
            System.out.println("Exception in peek(): line " + thisLineNumber);
            if(currentToken != null) {
                System.out.println("Current token (before the token we are trying to get): type = " + currentToken.type + ", lexeme = " + currentToken.lexeme);
            }
            e.printStackTrace();
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

    private boolean isSymbol(char c) {
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

    public int getLineNumber() {
        return lineNumber;
    }
}
