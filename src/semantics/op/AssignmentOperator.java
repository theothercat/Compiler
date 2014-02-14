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
public class AssignmentOperator extends Operator {
    public AssignmentOperator() {
        super("=");
    }

    @Override
    public String opResult(SymbolTableEntry s1, SymbolTableEntry s2) {
        if(SymbolTableEntryType.METHOD.equals(s1.kind)
                || SymbolTableEntryType.METHOD.equals(s2.kind)) {
            return null;
        }
        else if(s1.data == null || s2.data == null) {
            return null; // Shouldn't happen?
        }

        String t1 = s1.data.get("type");
        String t2 = s1.data.get("type");

        if(t1 == null || t2 == null) {
            return null; // Shouldn't happen?
        }

        if(t1.equals(t2)) {
            return t1;
        }
        return null;
    }
}
