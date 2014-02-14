package semantics.op;

import syntax.symbol.SymbolTableEntry;
import syntax.symbol.SymbolTableEntryType;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 2/11/14
 * Time: 1:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultiplyOperator extends Operator {
    public MultiplyOperator() {
//        super(getPrecedence("*"));
        super("*");
    }

    @Override
    public String opResult(SymbolTableEntry s1, SymbolTableEntry s2) {
        if(SymbolTableEntryType.METHOD.equals(s1.kind)
                || SymbolTableEntryType.METHOD.equals(s2.kind)) {
            return null;
        }
        else if(s1.data == null || s2.data == null) {
            return null;
        }

        if("int".equals(s1.data.get("type"))
                && "int".equals(s2.data.get("type")))
            return "int";
        return null;
    }
}
