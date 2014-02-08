package semantics;

import log.Log;
import semantics.record.RecordType;
import semantics.record.sar.SemanticActionRecord;
import syntax.symbol.SymbolTable;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/28/14
 * Time: 11:32 AM
 * To change this template use File | Settings | File Templates.
 */
public final class SemanticActions {
    public static Stack<SemanticActionRecord> semanticRecordStack = new Stack<SemanticActionRecord>("sar_stack.log");
    public static Stack<Operator> operatorStack = new Stack<Operator>("op_stack.log");

    private static SymbolTable symbolTable = SymbolTable.get(); // A reference to the symbol table singleton.
    private static Log semanticLog = new Log("semantic_actions.log");

    /**
     * Semantic routines follow below.
     */

    public static void iPush(String identifier) {
        semanticLog.debug("iPush: " + identifier);
        semanticRecordStack.push(
                new SemanticActionRecord(identifier, RecordType.IDENTIFIER));
    }

    public static void vPush() {

    }

    public void lpush(String literal) {
//        Sym
//        semanticRecordStack.push();
    }

    public static void oPush(String opcode)
    {
        semanticLog.debug("oPush: " + opcode);

        Operator newOp = new Operator(opcode);
        Operator tos = operatorStack.peek();
//        if(o.Precedence > tos.Precedence)
//        {
            operatorStack.push(newOp);
//            return;
//        }
//        else
//        {
//            // todo: something special
//        }
    }

    public static void tPush(String type) {
        semanticLog.debug("tPush: " + type);
        semanticRecordStack.push(
                new SemanticActionRecord(type, RecordType.TYPE));
    }

    public static void BAL() {

    }
    public static void EAL() {

    }
    public static void closeBracket() {

    }
    public static void comma() {

    }
    public static void CD() {

    }
    public static void doIf() {

    }
    public static void doWhile() {

    }
    public static void doReturn() {

    }
    public static void cin() {

    }
    public static void cout() {

    }
    public static void atoi() {

    }
    public static void itoa() {

    }
    public static void func() {

    }
    public static void arr() {

    }







    public static void lPush(String literalLexeme) {
        semanticLog.debug("lPush: " + literalLexeme);
        semanticRecordStack.push(
                new SemanticActionRecord(literalLexeme, RecordType.LITERAL));
    }

    public static void iExist() throws Exception {
        SemanticActionRecord top_sar = semanticRecordStack.pop();
        semanticLog.debug("iExist: " + top_sar.data);

        if(RecordType.IDENTIFIER.equals(top_sar.type)) {
            if(!SymbolTable.identifierExists(top_sar.data)) {
                throw new Exception(top_sar.toString() + " does not exist");
            }
            else {
                semanticLog.debug("Symbol exists.");
            }
        }
        else {
            throw new Exception(top_sar.type + " is not an identifier?");
        }
    }

    // todo: using iexist here?
    public static void tExist() throws Exception {
        SemanticActionRecord top_sar = semanticRecordStack.pop();
        semanticLog.debug("tExist: " + top_sar.data);

        if(RecordType.TYPE.equals(top_sar.type)) {
            if(!SymbolTable.identifierExists(top_sar.data)) {
                throw new Exception(top_sar.toString() + " does not exist");
            }
            else {
                semanticLog.debug("Symbol exists.");
            }
        }
        else {
            throw new Exception(top_sar.type + " is not a type?");
        }
    }
    public static void rExist() {

    }
    public static void newObj() {

    }
    public static void new_arrBrackets() {

    }

    public static void EOE() {

    }

    public static void closeParen() {

    }

    public static void closeLogs() {
        semanticLog.close();
        semanticRecordStack.closeLogs();
        operatorStack.closeLogs();
    }
}
