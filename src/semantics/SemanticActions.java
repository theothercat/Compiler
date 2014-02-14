package semantics;

import log.Log;
import semantics.op.Operator;
import semantics.op.OperatorStack;
import semantics.record.RecordType;
import semantics.record.sar.SemanticActionRecord;
import syntax.AnalyzerConstants;
import syntax.symbol.SymbolTable;
import syntax.symbol.SymbolTableEntry;
import syntax.symbol.SymbolTableEntryType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/28/14
 * Time: 11:32 AM
 * To change this template use File | Settings | File Templates.
 */
public final class SemanticActions {
    public static SemanticRecordStack semanticRecordStack = new SemanticRecordStack("sar_stack.log");
    public static OperatorStack operatorStack = new OperatorStack("op_stack.log");

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

    public static void lPush(String literal) {
        semanticLog.debug("lPush: " + literal);
        semanticRecordStack.push(
                new SemanticActionRecord(symbolTable.find(literal), RecordType.LITERAL));
    }

    public static void oPush(String opcode) throws SemanticsException
    {
        semanticLog.debug("oPush: " + opcode);
        operatorStack.push(opcode);
    }

    public static void tPush(String type) {
//        semanticLog.debug("tPush: " + type);
//        semanticRecordStack.push(
//                new SemanticActionRecord(type, RecordType.TYPE));
    }

    public static void vPush(String variable) {
//        // todo: should be pushing the symid?????
//        semanticLog.debug("vPush: " + variable);
//        semanticRecordStack.push(
//                new SemanticActionRecord(variable, RecordType.TYPE));
    }

    public static void iExist() throws SemanticsException {
        SemanticActionRecord top_sar = semanticRecordStack.pop();
        semanticLog.debug("iExist: " + top_sar.data);

        if(RecordType.IDENTIFIER.equals(top_sar.type)) {
            if(!symbolTable.identifierExists(top_sar.data)) {
                throw new SemanticsException (top_sar.toString() + " does not exist.");
            }
            else {
                semanticLog.debug("Symbol exists.");
                semanticRecordStack.push(SemanticActionRecord.getRecord(symbolTable.find(top_sar.data), RecordType.SYMID));
            }
        }
        else {
            throw new SemanticsException(top_sar.type + " is not an identifier?");
        }
    }

    // todo: using iexist here?
    public static void tExist() throws SemanticsException  {
//        SemanticActionRecord top_sar = semanticRecordStack.pop();
//        semanticLog.debug("tExist: " + top_sar.data);
//
//        if(RecordType.TYPE.equals(top_sar.type)) {
//            if(!AnalyzerConstants.TYPES.contains(top_sar.data)
//                    && !SymbolTable.identifierExists(top_sar.data)) {
//                throw new Exception(top_sar.toString() + " type does not exist");
//            }
//            else {
//                semanticLog.debug("Symbol exists.");
//            }
//        }
//        else {
//            throw new Exception(top_sar.type + " is not a type SAR?");
//        }
    }

    public static void rExist() throws SemanticsException {
        SemanticActionRecord member = semanticRecordStack.pop();
        SemanticActionRecord object = semanticRecordStack.pop(); // Symid

        semanticLog.debug("rExist: " + member.toString() + " in object " + object.toString());

        SymbolTableEntry obj = symbolTable.get(object.data);
        String objType = obj.data.get("type");
        String symid = symbolTable.findInScope(member.data, "g." + objType);

        if(symid == null) {
            throw new SemanticsException (member.toString() + " does not exist inside " + object.toString());
        }
        semanticLog.debug("Reference exists.");
        semanticRecordStack.push(SemanticActionRecord.getRecord(symid, RecordType.SYMID));


//        if(RecordType.IDENTIFIER.equals(top_sar.type)) {
//            if(!symbolTable.identifierExists(top_sar.data)) {
//                throw new SemanticsException (top_sar.toString() + " does not exist.");
//            }
//            else {
//                semanticLog.debug("Symbol exists.");
//                semanticRecordStack.push(SemanticActionRecord.getRecord(symbolTable.find(top_sar.data), RecordType.SYMID));
//            }
//        }
//        else {
//            throw new SemanticsException(top_sar.type + " is not an identifier?");
//        }
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

    public static void BAL() {
        semanticLog.debug("Begin argument list");
        semanticRecordStack.push(
                new SemanticActionRecord("", RecordType.BAL));
//        );
    }
    public static void EAL() {
        semanticLog.debug("End argument list");
        SemanticActionRecord sar;
        String paramList = "[";
        while(!RecordType.BAL.equals(
                (sar = semanticRecordStack.pop()).type))
        {
            paramList += sar.data + ", ";
        }
        paramList = paramList.substring(0, paramList.length() - 2) + "]";
        semanticRecordStack.push(new SemanticActionRecord(paramList, RecordType.ARG_LIST));
    }

    public static void func() throws SemanticsException {
        SemanticActionRecord arglist = semanticRecordStack.pop();
        SemanticActionRecord funcName = semanticRecordStack.pop();
        semanticLog.debug("Checking for function " + funcName.data + " with params " + arglist.data);

        if(!symbolTable.functionExists()) {
            throw new SemanticsException("Function " + funcName.data + " with params " + arglist.data " does not exist");
        }
    }

    public static void arr() {

    }

    public static void newObj() {

    }

    public static void new_arrBrackets() {

    }

    public static void EOE() throws SemanticsException {
        while(operatorStack.peek() != null) {
            doExpression();
        }
    }

    public static void closeParen() throws SemanticsException {
        semanticLog.debug("Looking for opening parenthesis...");
        while(!"(".equals(operatorStack.peek().Symbol)) {
            semanticLog.debug("Found " + operatorStack.peek().Symbol);
            doExpression();
        }
        semanticLog.debug("...Found opening parenthesis!");
        operatorStack.pop();
    }

    public static void doExpression() throws SemanticsException {
        SemanticActionRecord s1 = SemanticActions.semanticRecordStack.pop();
        SemanticActionRecord s2 = SemanticActions.semanticRecordStack.pop();

        SymbolTableEntry e1 = symbolTable.get(s1.data); // Assume is symid by now, else this would fail
        SymbolTableEntry e2 = symbolTable.get(s2.data);

        if(e1 == null || e2 == null) {
            System.out.println("These were supposed to be symids!");
        }

        Operator op = operatorStack.pop();
        semanticLog.debug("Evaluating expression: " + s2.toString() + " " + op.Symbol + " " + s1.toString());

        String type = op.opResult(e1, e2);
        if(type == null) {
            throw new SemanticsException("Can't perform operation " + op.Symbol
                    + " on operands " + e2.value + " and " + e1.value
                    + " in scope " + e1.scope
            );
        }
        semanticLog.debug("Expression is valid. Creating new reference of type " + type);
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("type", type);

        semanticRecordStack.push(SemanticActionRecord.getRecord(
                symbolTable.add(SymbolTableEntryType.REFERENCE, data).symid,
                RecordType.REFERENCE
        ));
    }
//        SymbolTableEntry e1 = symbolTable.get(s1.data); // Assume is symid by now, else this would fail
//        SymbolTableEntry e2 = symbolTable.get(s2.data);
//
//        try {
//            String type = o.opResult(e1, e2);
//            if(type == null) {
//
//            }
//            if("+".equals(o.Symbol)) {
//                if(SymbolTableEntryType.CLASS.equals(e1.kind) || SymbolTableEntryType.CLASS.equals(e2.kind)) {
//                    throw new Exception("How did a class entry end up on the semantic action stack?");
//                }
//                else if(SymbolTableEntryType.METHOD.equals(e1.kind) || SymbolTableEntryType.METHOD.equals(e2.kind)) // One is a method
//                {
//                    return false; // Can't add two methods, or method and non-method.
//                }
//                else // Neither is a method.
//                {
//                    // Both have data.get
//                    String t1 = e2.data.get("type");
//                    String t2 = e2.data.get("type");
//
//                    if(t1.equals(t2)) {
//                        return true; // todo: ????
//                    }
//                }
//                return false; // Can't add a method and a non-method.
//            }
//        }
//        catch (NullPointerException e) {
//            e.printStackTrace();
//        }
//    }
}
