package lex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/6/14
 * Time: 4:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class LexicalAnalyzer {
    private BufferedReader file;
    private List<Token> tokens;
    private Token currentToken;
    private String currentLine = "";

    public LexicalAnalyzer(BufferedReader file)
    {
        this.file = file;
    }

    public Token getToken() {
        return currentToken;
    }

    public Token nextToken() throws Exception // todo: handle exceptions
    {
        if(currentLine.isEmpty()) {
            currentLine = file.readLine();
        }
        String[] split = currentLine.split("[ \t]+");
        String lexeme = getRealToken(split[0]);
        currentLine.replaceFirst(lexeme, "");
        currentToken = parse(lexeme);
        return currentToken;
    }

    public String getRealToken(String s) {
        if(s.endsWith(",") || s.endsWith(";") || s.endsWith(")")) {
            return s.substring(0, s.length() - 1);
        }
        else if(s.startsWith("(")) {
            return s.substring(1, s.length());
        }
        else
        {
            int index = s.indexOf("+");
            if(index == 0) // Already
            {

            }
        }

        else if(s.indexOf())
        return s;
    }
}
