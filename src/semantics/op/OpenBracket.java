package semantics.op;

import syntax.symbol.SymbolTableEntry;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 2/11/14
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class OpenBracket extends Operator {
    public OpenBracket() {
        super("[");
    }

    @Override
    public String opResult(SymbolTableEntry s1, SymbolTableEntry s2) {
        return null;
    }
}
