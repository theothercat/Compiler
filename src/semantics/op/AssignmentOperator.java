package semantics.op;

import semantics.err.SemanticsException;
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
public class AssignmentOperator extends Operator {
    public AssignmentOperator() {
        super("=");
    }

    @Override
    public String opResult(SymbolTableEntry s1, SymbolTableEntry s2) throws SemanticsException {
        if(SymbolTableEntryType.METHOD.equals(s1.kind)
                || SymbolTableEntryType.METHOD.equals(s2.kind)) {
            return null;
        }
        else if(s1.data == null || s2.data == null) {
            return null; // Shouldn't happen?
        }
        else if(!SymbolTableEntryType.INSTANCE_VAR.equals(s2.kind)
                && !SymbolTableEntryType.PARAM.equals(s2.kind)
                && !SymbolTableEntryType.LOCAL_VAR.equals(s2.kind)
                && !SymbolTableEntryType.ARR_ITEM.equals(s2.kind)
                && !SymbolTableEntryType.REF_MEMBER.equals(s2.kind)) {
            throw new SemanticsException("Expression LHS must be a variable! Got " + s2.kind);
        }

        String t1 = s1.data.get("type");
        String t2 = s2.data.get("type");

        if(t1 == null || t2 == null) {
            return null; // Shouldn't happen?
        }
        if("this".equals(t1)) {
            return t2; // If RHS is 'this', return type is LHS
        }
        else if("void".equals(t1) || "void".equals(t2)) {
            return null;
        }
        else if(t1.equals(t2)) {
            return t1;
        }
        else if("null".equals(t1) && !AnalyzerConstants.TYPES.contains(t2)) {
            return t2; // todo: should I be returning type t2, or null?
        }
        else if("null".equals(t2) && !AnalyzerConstants.TYPES.contains(t1)) {
            return t1;
        }

        return null;
    }
}
