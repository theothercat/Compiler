package semantics.op;

import syntax.AnalyzerConstants;
import syntax.symbol.SymbolTableEntry;
import syntax.symbol.SymbolTableEntryType;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 2/11/14
 * Time: 1:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class EqualityOperator extends Operator {
    public EqualityOperator(String operator) {
        super(operator);
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

        String t1 = s1.data.get("type");
        String t2 = s2.data.get("type");
        if("void".equals(t1) || "void".equals(t2)) {
            return null;
        }
        else if(t1.equals(t2)) {
            return "bool";
        }
        else if(("null".equals(t1) && !AnalyzerConstants.TYPES.contains(t2))
            || ("null".equals(t2) && !AnalyzerConstants.TYPES.contains(t1))) {
            return "bool"; // One is null, and the other is a class.
        }
        return null;
    }
}
