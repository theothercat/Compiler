package lex;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/6/14
 * Time: 5:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class Token {
    public String lexeme;
    public TokenType type;

    public Token(String lex, TokenType t) {
        lexeme = lex;
        type = t;
    }
}
