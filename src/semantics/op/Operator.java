package semantics.op;

import semantics.err.SemanticsException;
import semantics.record.sar.SemanticActionRecord;
import syntax.symbol.SymbolTableEntry;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/28/14
 * Time: 11:55 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Operator {
    public String Symbol;
    public int Precedence;

    public Operator(int p) {
        Precedence = p;
    }

    public Operator(String s) {
        Symbol = s;
        Precedence = getPrecedence(s);
    }

    @Override
    public String toString() {
        return Symbol + " (" + Precedence + ")";
    }

    /**
     * Returns the type (int, boolean, etc.) of the result of the operation
     * @param s1
     * @param s2
     * @return type (null if not valid)
     */
    public abstract String opResult(SymbolTableEntry s1, SymbolTableEntry s2) throws SemanticsException;

    protected static int getPrecedence(String operator) {
        if("*".equals(operator)
                || "/".equals(operator)
                || "%".equals(operator)) {
            return 13;
        }
        else if("+".equals(operator)
                || "-".equals(operator)) {
            return 11;
        }
        else if(">".equals(operator)
                || "<".equals(operator)
                || ">=".equals(operator)
                || "<=".equals(operator)) {
            return 9;
        }
        else if("==".equals(operator)
                || "!=".equals(operator)) {
            return 7;
        }
        else if("&&".equals(operator)) {
            return 5;
        }
        else if("||".equals(operator)) {
            return 3;
        }
        else if("=".equals(operator)) {
            return 1;
        }
        return -500; // todo: something better
    }

    public static Operator get(String opSymbol) {
        if("+".equals(opSymbol)) {
            return new AddOperator();
        }
        if("-".equals(opSymbol)) {
            return new SubtractOperator();
        }
        if("*".equals(opSymbol)) {
            return new MultiplyOperator();
        }
        if("/".equals(opSymbol)) {
            return new DivideOperator();
        }
        else if("=".equals(opSymbol)) {
            return new AssignmentOperator();
        }
        else if("(".equals(opSymbol)) {
            return new OpenParenthesis();
        }
        else if("[".equals(opSymbol)) {
            return new OpenBracket();
        }
        else if("<".equals(opSymbol)
                || ">".equals(opSymbol)
                || "<=".equals(opSymbol)
                || ">=".equals(opSymbol)) {
            return new ComparisonOperator(opSymbol);
        }
        else if("&&".equals(opSymbol)) {
            return new AndOperator();
        }
        else if("||".equals(opSymbol)) {
            return new OrOperator();
        }
        else if("==".equals(opSymbol)
                || "!=".equals(opSymbol))
        {
            return new EqualityOperator(opSymbol);
        }
        return null;
    }
}
