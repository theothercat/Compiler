package semantics;

import semantics.record.RecordType;
import semantics.record.SemanticActionRecord;
import syntax.symbol.SymbolTable;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/28/14
 * Time: 11:32 AM
 * To change this template use File | Settings | File Templates.
 */
public final class SemanticManager {
//    public static Stack<SemanticAction> semanticActionStack = new Stack<SemanticAction>("sa_stack.log");
    public static Stack<SemanticActionRecord> semanticRecordStack = new Stack<SemanticActionRecord>("sar_stack.log");
    public static Stack<Operator> operatorStack = new Stack<Operator>("op_stack.log");

    private static SymbolTable symbolTable = SymbolTable.get(); // A reference to the symbol table singleton.

//    private Stack<String> refStack = new Stack<String>("ref_stack.log");

    /**
     * Semantic routines follow below.
     */

    public void iPush(String id_sar) { semanticRecordStack.push(new SemanticActionRecord(id_sar, RecordType.IDENTIFIER)); }

//    public void lpush(String literal) {
//        Sym
//        semanticRecordStack.push();
//    }

    public void oPush(Operator o)
    {
        Operator tos = operatorStack.peek();
        if(o.Precedence > tos.Precedence)
        {
            operatorStack.push(o);
            return;
        }
        else
        {
            // todo: something special
        }
    }

    public void iExist() {
        SemanticActionRecord top_sar = semanticRecordStack.pop();
        //if(SymbolTable.identifierExists(top_sar))

    }
}
