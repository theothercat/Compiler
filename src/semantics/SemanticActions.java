package semantics;

import log.Log;
import semantics.err.SemanticsException;
import semantics.op.Operator;
import semantics.op.OperatorStack;
import semantics.record.sar.RecordType;
import semantics.record.sar.SemanticActionRecord;
import syntax.AnalyzerConstants;
import syntax.symbol.SymbolTable;
import syntax.symbol.SymbolTableEntry;
import syntax.symbol.SymbolTableEntryType;

import java.util.HashMap;

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
        semanticLog.debug("tPush: " + type);
        semanticRecordStack.push(
                new SemanticActionRecord(type, RecordType.TYPE));
    }

    public static void vPush(String vIdentifier) {
        semanticLog.debug("vPush: " + vIdentifier);
        String symid = symbolTable.find(vIdentifier);
        semanticRecordStack.push(
                new SemanticActionRecord(
                        symid,
                        RecordType.SYMID
                ));
    }

    public static void thisPush() {
        semanticLog.debug("thisPush: found arg this");
        semanticRecordStack.push(
                new SemanticActionRecord(
                            "thisPlaceholderSAR",
                            RecordType.THIS_PLACEHOLDER
                        )
        );
    }

    public static void voidPush() {
        semanticRecordStack.push(SemanticActionRecord.getRecord(
                "voidReturnPlaceholderSAR",
                RecordType.VOID_RETURN
        ));
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
                semanticRecordStack.push(
                        SemanticActionRecord.getRecord(
                                symbolTable.find(top_sar.data),
                                RecordType.SYMID
                        ));
            }
        }
        else if(RecordType.FUNC.equals(top_sar.type)) {
            semanticLog.debug("Checking for function " + top_sar.subRecords.get("id") + " with params " + top_sar.subRecords.get("args"));

            SymbolTableEntry foundFunction = symbolTable.findFunctionInClass(top_sar);

            if(foundFunction == null) {
                throw new SemanticsException("Function " + top_sar.subRecords.get("id") + " with params " + top_sar.subRecords.get("args") + " does not exist");
            }
            semanticLog.debug("Function found.");

            String refType = foundFunction.data.get("returnType");
            if(refType == null) {
                throw new SemanticsException ("No return type for " + foundFunction.value);
            }
            else if("void".equals(refType)) {
                voidPush();
            }
            else {
                HashMap<String, String> data = new HashMap<String, String>(1);
                data.put("type", refType);

                semanticRecordStack.push(SemanticActionRecord.getRecord(
                        symbolTable.add(SymbolTableEntryType.REFERENCE, data).symid,
                        RecordType.REFERENCE
                ));
            }
        }
        else if(RecordType.ARR_INDEX.equals(top_sar.type)) {
            semanticLog.debug("Checking for " + top_sar.toString());

            SymbolTableEntry array = symbolTable.get(top_sar.data);

            if(array == null) {
                throw new SemanticsException(top_sar.toString() + " does not exist");
            }
            semanticLog.debug("Array found.");
            if(array.data == null) {
                throw new SemanticsException("Array " + top_sar.toString() + " has no data!");
            }

            String itemType = array.data.get("type");
            if(array.data == null) {
                throw new SemanticsException("Array " + top_sar.toString() + " has no type!");
            }
            itemType = itemType.substring(2, itemType.length()); // Chop off the @: since it's an item in the array.

            HashMap<String, String> data = new HashMap<String, String>(1);
            data.put("type", itemType);

            semanticRecordStack.push(SemanticActionRecord.getRecord(
                    symbolTable.add(SymbolTableEntryType.REFERENCE, data).symid,
                    RecordType.REFERENCE
            ));
        }
        else {
            throw new SemanticsException(top_sar.type + " not handled by iExist");
        }
    }

    public static void tExist() throws SemanticsException  {
        SemanticActionRecord top_sar = semanticRecordStack.pop();
        semanticLog.debug("tExist: " + top_sar.data);

        if(RecordType.TYPE.equals(top_sar.type)) {
            if(!AnalyzerConstants.TYPES.contains(top_sar.data) // Hardcoded primitive types
                    && !symbolTable.classExists(top_sar.data)) {
                throw new SemanticsException("Type does not exist: " + top_sar.toString());
            }
            else {
                semanticLog.debug("Type exists.");
            }
        }
        else {
            throw new SemanticsException(top_sar.type.name() + " is not a type SAR");
        }
    }

    public static void rExist() throws SemanticsException {
        SemanticActionRecord member = semanticRecordStack.pop(); // Identifier, func, etc.
        SemanticActionRecord object = semanticRecordStack.pop(); // Symid

        semanticLog.debug("rExist: " + member.toString() + " in object " + object.toString());
        if(member == null || object == null) {
            throw new SemanticsException("Not enough args for rExist");
        }

        if(RecordType.FUNC.equals(member.type)) {
            semanticLog.debug("Checking for function " + member.subRecords.get("id") + " with params " + member.subRecords.get("args"));

            SymbolTableEntry foundFunction = symbolTable.findFunction(object, member);
            if(foundFunction == null) {
                throw new SemanticsException("Function " + member.subRecords.get("id") + " with params " + member.subRecords.get("args") + " does not exist");
            }
            String accessMod = foundFunction.data.get("accessMod");
            if(!"public".equals(accessMod)) {
                throw new SemanticsException("Function " + member.subRecords.get("id") + " with params " + member.subRecords.get("args") + " is not public");
            }
            semanticLog.debug("Function found.");

            String refType = foundFunction.data.get("returnType");
            if(refType == null) {
                throw new SemanticsException ("No return type for " + foundFunction.value);
            }
            else if("void".equals(refType)) {
                semanticRecordStack.push(SemanticActionRecord.getRecord(
                        "voidReturnPlaceholderSAR",
                        RecordType.VOID_RETURN
                ));
            }
            else {
                HashMap<String, String> data = new HashMap<String, String>(1);
                data.put("type", refType);

                semanticRecordStack.push(SemanticActionRecord.getRecord(
                        symbolTable.add(SymbolTableEntryType.REFERENCE, data).symid,
                        RecordType.REFERENCE
                ));
            }
        }
        else // Assumes object.data contains a symid.
        {
            SymbolTableEntry obj = symbolTable.get(object.data);
            String objType = obj.data.get("type");
            String symid = symbolTable.findInScope(member.data, "g." + objType);
            if(symid == null) {
                throw new SemanticsException (member.toString() + " does not exist inside " + object.toString());
            }
            String accessMod = symbolTable.get(symid).data.get("accessMod");
            if(!"public".equals(accessMod)) {
                throw new SemanticsException("Function " + member.subRecords.get("id") + " with params " + member.subRecords.get("args") + " is not public");
            }
            semanticLog.debug("Reference found.");

            String refType = symbolTable.get(symid).data.get("type");
            if(refType == null) {
                throw new SemanticsException ("No type for " + member.data);
            }
            HashMap<String, String> data = new HashMap<String, String>(1);
            data.put("type", refType);

            ;
            semanticRecordStack.push(SemanticActionRecord.getRecord(
                    symbolTable.add(SymbolTableEntryType.REFERENCE, data).symid,
                    RecordType.REFERENCE));
        }

        semanticLog.debug("Reference exists.");
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

    public static void closeBracket() throws SemanticsException {
        semanticLog.debug("closeBracket, looking for opening bracket...");
        while(!"[".equals(operatorStack.peek().Symbol)) {
            semanticLog.debug("closeBracket: found " + operatorStack.peek().Symbol);
            doExpression();
        }
        semanticLog.debug("...closeBracket, found opening bracket!");
        operatorStack.pop();
    }

    public static void comma() throws SemanticsException {
        semanticLog.debug("Parsing current argument...");
        while(!"(".equals(operatorStack.peek().Symbol)) {
            semanticLog.debug("Found " + operatorStack.peek().Symbol);
            doExpression();
        }
        semanticLog.debug("...Current argument complete");
    }

    public static void CD(String identifier) throws SemanticsException {
        semanticLog.debug("Checking constructor declaration...");
        String scope = symbolTable.getScope();
        if("g".equals(scope)) {
            throw new SemanticsException("Constructor cannot be declared in global scope!");
        }

        String className = scope.substring(scope.lastIndexOf(".") + 1, scope.length());
        if(!className.equals(identifier)){
            throw new SemanticsException("Constructor name must match class name.");
        }
        semanticLog.debug("Constructor declaration is valid.");
    }

    public static void doIf() throws SemanticsException {
        semanticLog.debug("if, checking if arg is type bool");
        checkType("bool");
        semanticLog.debug("if, bool check passed");
    }

    public static void doWhile() throws SemanticsException {
        semanticLog.debug("while, checking if arg is type bool");
        checkType("bool");
        semanticLog.debug("while, bool check passed");
    }

    private static void checkType(String type) throws SemanticsException {
        SemanticActionRecord arg = semanticRecordStack.pop();
        if(arg == null) {
            throw new SemanticsException("Not enough args for if/while");
        }
        String symid;
        if(RecordType.IDENTIFIER.equals(arg.type)) {
            symid = symbolTable.find(arg.data);
            if(symid == null) {
                throw new SemanticsException("checkType could not find identifier " + arg.data + " in symbol table from scope " + symbolTable.getScope());
            }
        }
        else {
            symid = arg.data;
        }
        SymbolTableEntry entry = symbolTable.get(symid);
        if(entry == null) {
            throw  new SemanticsException("Could not find symbol " + arg.data + " in the symbol table");
        }
        if(entry.data == null) {
            throw  new SemanticsException(arg.data + " symbol table entry contains no data");
        }

        if(!type.equals(entry.data.get("type"))) {
            throw  new SemanticsException("checkType() expected argument of type " + type + ", got " + entry.data.get("type"));
        }
    }

    public static void doReturn() throws SemanticsException {
        SymbolTableEntry currentScope = symbolTable.findCurrentScopeEntry(); // Should be a function entry
        if(currentScope == null) {
            throw new SemanticsException("Could not find current scope in the symbol table");
        }
        if(!SymbolTableEntryType.METHOD.equals(currentScope.kind)) {
            throw new SemanticsException("Current scope is not a function");
        }
        if(currentScope.data == null) {
            throw new SemanticsException("Current scope has no data");
        }
        if(currentScope.data.get("returnType") == null) {
            throw new SemanticsException("Current scope has no return type"); // Yes, this includes constructors which have a hidden return type!
        }

        EOE(); // Finish the stack.

        // todo: how is this supposed to work with void returns of return; ???
        String expectedReturnType = currentScope.data.get("returnType");
        SemanticActionRecord final_sar = semanticRecordStack.pop();
        if(final_sar == null) {
            throw new SemanticsException("Not enough args for return type");
        }
        else if(RecordType.VOID_RETURN.equals(final_sar.type)) {
            if(!"void".equals(expectedReturnType)) {
                throw new SemanticsException("Expected return type of " + expectedReturnType + ", found void");
            }
            else {
                semanticLog.debug("void return value is valid for function " + currentScope.scope + "." + currentScope.value);
                return;
            }
        }

        SymbolTableEntry returnValue = symbolTable.get(final_sar.data);
        if(returnValue == null) {
            throw new SemanticsException("Expected a symid, got " + final_sar.data);
        }
        if(returnValue.data == null) {
            throw new SemanticsException("Return value is missing a type");
        }
        if(!expectedReturnType.equals(returnValue.data.get("type"))) {
            if(AnalyzerConstants.TYPES.contains(expectedReturnType)) // Already checked for void, so this is int, char, or bool
            {
                throw new SemanticsException("Expected return type of " + expectedReturnType + ", found " + returnValue.data.get("type"));
            }
            else if(!"null".equals(returnValue.data.get("type"))) // Expected return type is a created class, but we didn't return null *or* an object of that class.
            {
                throw new SemanticsException("Expected return type of " + expectedReturnType + ", found " + returnValue.data.get("type"));
            }
        }
        semanticLog.debug("Return value is valid for function " + currentScope.scope + "." + currentScope.value);
    }

    public static void cin() throws SemanticsException {
        EOE();
        checkSpecial(SemanticCheck.CIN);
    }

    public static void cout() throws SemanticsException {
        EOE();
        checkSpecial(SemanticCheck.COUT);
    }

    public static void atoi() throws SemanticsException {
        semanticLog.debug("atoi, checking if arg is type char");
        checkType("char");
        semanticLog.debug("atoi, char check passed");

        HashMap<String, String> data = new HashMap<String, String>(1);
        data.put("type", "int");
        semanticRecordStack.push(SemanticActionRecord.getRecord(
                symbolTable.add(SymbolTableEntryType.REFERENCE, data).symid,
                RecordType.REFERENCE
        ));
    }

    public static void itoa() throws SemanticsException {
        semanticLog.debug("itoa, checking if arg is type int");
        checkType("int");
        semanticLog.debug("itoa, int check passed");

        HashMap<String, String> data = new HashMap<String, String>(1);
        data.put("type", "char");
        semanticRecordStack.push(SemanticActionRecord.getRecord(
                symbolTable.add(SymbolTableEntryType.REFERENCE, data).symid,
                RecordType.REFERENCE
        ));
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
        boolean trim = false;
        while(!RecordType.BAL.equals(
                (sar = semanticRecordStack.pop()).type))
        {
            trim = true;
            paramList += sar.data + ", ";
        }

        paramList = paramList.substring(0, trim ? (paramList.length() - 2) : 1) + "]";
        semanticRecordStack.push(
                new SemanticActionRecord(
                        paramList,
                        RecordType.ARG_LIST
                )
        );
    }

    // todo: use the version that did the check here, or the other version I think is probably correct?
//    public static void func() throws SemanticsException {
//        SemanticActionRecord arglist = semanticRecordStack.pop();
//        SemanticActionRecord funcName = semanticRecordStack.pop();
//        semanticLog.debug("Checking for function " + funcName.data + " with params " + arglist.data);
//
//        SymbolTableEntry foundFunction = symbolTable.findFunction(funcName, arglist);
//        if(foundFunction == null) {
//            throw new SemanticsException("Function " + funcName.data + " with params " + arglist.data + " does not exist");
//        }
//        semanticLog.debug("Function found.");
//        semanticRecordStack.push(SemanticActionRecord.getRecord(
//                foundFunction.symid,
//                RecordType.FUNC
//        ));
//    }

    public static void func() throws SemanticsException {
        SemanticActionRecord arglist = semanticRecordStack.pop();
        SemanticActionRecord funcName = semanticRecordStack.pop();

        if(arglist == null || funcName == null) {
            throw new SemanticsException("Not enough args to build func_sar");
        }

        semanticRecordStack.push(SemanticActionRecord.getRecord(
                funcName,
                arglist
        ));
    }

    public static void arr() throws SemanticsException {
        SemanticActionRecord expression = semanticRecordStack.peek();
        semanticLog.debug("arr(), checking if expression is type int");
        checkType("int");
        semanticLog.debug("arr(), int check passed");

        SemanticActionRecord identifier = semanticRecordStack.pop();
        if(identifier == null) {
            throw new SemanticsException("arr(), not enough args for array identifier");
        }
        String symid = symbolTable.find(identifier.data);
        if(symid == null) {
            throw new SemanticsException("arr(), could not find identifier " + identifier.data + " in scope " + symbolTable.getScope());
        }
        SymbolTableEntry entry = symbolTable.get(symid);
        if(entry.data == null) {
            throw new SemanticsException("arr(), symbol " + symid + " for identifier " + identifier.data + " has no data");
        }
        String arrType = entry.data.get("type");
        if(arrType == null) {
            throw new SemanticsException("arr(), symbol " + symid + " for identifier " + identifier.data + " has no type");
        }

        if(!arrType.startsWith("@:")) {
            throw new SemanticsException("arr(), " + identifier.data + " of type " + arrType + " is not an array");
        }

        HashMap<String, String> data = new HashMap<String, String>(1);
//        String typeData = arrType.substring(2, arrType.length()); // It's an element of the array, now.
        data.put("type", arrType);
        data.put("index", expression.data);

        semanticRecordStack.push(SemanticActionRecord.getRecord(
                symbolTable.add(SymbolTableEntryType.REFERENCE, data).symid,
                RecordType.ARR_INDEX
        ));
    }

    public static void newObj() throws SemanticsException {
        semanticLog.debug("Constructor check...");
        SemanticActionRecord argList = semanticRecordStack.pop();
        if(!RecordType.ARG_LIST.equals(argList.type)) {
            throw new SemanticsException("Constructor expected argument list, got " + argList.type);
        }
        SemanticActionRecord identifier = semanticRecordStack.pop();
        if(!RecordType.TYPE.equals(identifier.type)) {
            throw new SemanticsException("Constructor expected type, got " + identifier.type);
        }

        SymbolTableEntry foundFunction = symbolTable.findConstructor(identifier.data, argList.data);
        if(foundFunction == null) {
            throw new SemanticsException("Constructor " + identifier.data + " with params " + argList.data + " does not exist");
        }
//        String accessMod = foundFunction.data.get("accessMod");
//        if(!"public".equals(accessMod)) {
//            throw new SemanticsException("Constructor " + identifier.data + " with params " + argList.data + " is not public");
//        }
        semanticLog.debug("Constructor found.");

        HashMap<String, String> data = new HashMap<String, String>(1);
        data.put("type", identifier.data);

        semanticRecordStack.push(SemanticActionRecord.getRecord(
                symbolTable.add(SymbolTableEntryType.REFERENCE, data).symid,
                RecordType.REFERENCE
        ));
    }

    public static void new_arrBrackets() throws SemanticsException {
        SemanticActionRecord expression = semanticRecordStack.peek();
        semanticLog.debug("new[], checking if arg is type int");
        checkType("int");
        semanticLog.debug("new[], int check passed");

        SemanticActionRecord arrType = semanticRecordStack.peek();
        if(arrType == null) {
            throw new SemanticsException("new[]: Not enough args to get array type");
        }

        tExist(); // Will check if the type is a valid type.
        if("void".equals(arrType.data) || "null".equals(arrType.data)) // Void and null can't be made into arrays.  All other types are fine, provided they exist.
        {
            throw new SemanticsException("new[]: " + arrType.data + " is not a valid array type");
        }

        HashMap<String, String> data = new HashMap<String, String>(1);
        String typeData = "@:" + arrType.data;
        data.put("type", typeData);
        data.put("length", expression.data);

        semanticRecordStack.push(SemanticActionRecord.getRecord(
                symbolTable.add(SymbolTableEntryType.REFERENCE, data).symid,
                RecordType.NEW_ARR
        ));
    }

    public static void EOE() throws SemanticsException {
        while(operatorStack.peek() != null) {
            doExpression();
        }
    }

    public static void closeParen() throws SemanticsException {
        semanticLog.debug("closeParen, looking for opening parenthesis...");
        while(!"(".equals(operatorStack.peek().Symbol)) {
            semanticLog.debug("Found " + operatorStack.peek().Symbol);
            doExpression();
        }
        semanticLog.debug("...closeParen, found opening parenthesis!");
        operatorStack.pop();
    }

    private static void checkSpecial(SemanticCheck checkType) throws SemanticsException {
        SemanticActionRecord exp_sar = semanticRecordStack.pop();
        if(exp_sar == null) {
            throw new SemanticsException("Not enough args for " + checkType.name());
        }

        semanticLog.debug("Checking validity of arg " + exp_sar.data + " with behavior " + checkType.name());
        if(SemanticCheck.CIN.equals(checkType)
                || SemanticCheck.COUT.equals(checkType)) {
            // todo: do I need type checks?
            if(RecordType.SYMID.equals(exp_sar.type)
                    || (SemanticCheck.COUT.equals(checkType)
                    && (RecordType.REFERENCE.equals(exp_sar.type) || RecordType.LITERAL.equals(exp_sar.type)))) // Can only use a literal with cout.
            {
                SymbolTableEntry entry = symbolTable.get(exp_sar.data);
                if(entry.data == null) {
                    throw new SemanticsException("No data for " + exp_sar.data);
                }
                if(!entry.data.get("type").equals("int")
                    && !entry.data.get("type").equals("char")) {
                    throw new SemanticsException("cin/cout expected int or char type, got " + entry.data.get("type"));
                }
            }
            else {
                throw new SemanticsException("Cannot use SAR of type " + exp_sar.type.name() + " with "+ checkType.name());
            }
        }
        semanticLog.debug(checkType.name() + " validity check passed."); // If it failed there would have been an exception.
    }

    public static void doExpression() throws SemanticsException {
        Operator op = operatorStack.pop();

        SemanticActionRecord s1 = semanticRecordStack.pop();
        SemanticActionRecord s2 = semanticRecordStack.pop();
        if(RecordType.VOID_RETURN.equals(s1.type)) {
            throw new SemanticsException(
                    "Found non-operand of type void, used in expression with operand "
                            + op.Symbol
                            + " and symid "
                            + s2.data);
        }
        else if(RecordType.VOID_RETURN.equals(s2.type)) {
            throw new SemanticsException(
                    "Found non-operand of type void, used in expression with operand "
                            + op.Symbol
                            + " and symid "
                            + s1.data);
        }
        SymbolTableEntry e1 = RecordType.THIS_PLACEHOLDER.equals(s1.type)
                ? SymbolTableEntry.THIS_PLACEHOLDER
                : symbolTable.get(s1.data); // Assume is symid by now, else this would fail
        SymbolTableEntry e2 = RecordType.THIS_PLACEHOLDER.equals(s2.type)
                ? SymbolTableEntry.THIS_PLACEHOLDER
                : symbolTable.get(s2.data);

        if(e1 == null) {
            throw new SemanticsException("Symid " + s1.data + " not found in symbol table");
        }
        else if(e2 == null) {
            throw new SemanticsException("Symid " + s2.data + " not found in symbol table");
        }

        semanticLog.debug("Evaluating expression: " + s2.toString() + " " + op.Symbol + " " + s1.toString());

        String type = op.opResult(e1, e2);
        if(type == null) {
            String typeString = "";
            if(e1.data != null && e2.data != null) {
                typeString = ", of types " + e2.data.get("type") + " and " + e1.data.get("type");
            }
//                String t1 = e1.data.get("type");
            throw new SemanticsException("Can't perform operation " + op.Symbol
                    + " on operands " + e2.value + " and " + e1.value
                    + typeString
                    + " in scope " + e1.scope
            );
        }
        semanticLog.debug("Expression is valid. Creating new reference of type " + type);
        HashMap<String, String> data = new HashMap<String, String>(1);
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

    private static enum SemanticCheck {
        COUT,
        CIN,
        RETURN
    }
}
