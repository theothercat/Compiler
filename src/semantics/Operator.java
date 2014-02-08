package semantics;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/28/14
 * Time: 11:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class Operator {
    public String Symbol;
    public int Precedence;

    public Operator(String s, int p) {
        Symbol = s;
        Precedence = p;
    }

    public Operator(String s) {
        Symbol = s;
    }

    @Override
    public String toString() {
        return Symbol;
    }
}
