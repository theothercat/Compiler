package semantics;

import icode.ICodeGenerator;
import icode.quad.Quad;
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
    public static List<String> labelStack = new ArrayList<String>();
    public static int labelGen = 1;

    private static SymbolTable symbolTable = SymbolTable.get(); // A reference to the symbol table singleton.
    private static Log semanticLog = new Log("semantic_actions.log");
    private static Log iLog = new Log("actions_icode.log");

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
                StringBuilder response = new StringBuilder("Function ")
                        .append(top_sar.subRecords.get("id"))
                        .append(" with params ")
                        .append(top_sar.subRecords.get("args"));
                if(!"[]".equals(top_sar.subRecords.get("args"))) {
                    response.append(", of type [");
                    List<String> symids = symbolTable.parseParamIds(top_sar.subRecords.get("args"));
                    for(int i = 0; i < symids.size(); i++)
                    {
                        response.append(symbolTable.get(symids.get(i)).data.get("type"));
                        if(i < (symids.size() - 1)) {
                            response.append(", ");
                        }
                    }
                    response.append("]");
                }
                throw new SemanticsException(response.append(" does not exist").toString());
            }
            semanticLog.debug("Function found.");

            String refType = foundFunction.data.get("returnType");
            if(refType == null) {
                throw new SemanticsException("No return type for " + foundFunction.value);
            }
            else {
                generateFuncQuads(null, foundFunction, top_sar.subRecords.get("args"));

                if("void".equals(refType)) {
                    voidPush();
                }
                else {
                    HashMap<String, String> data = new HashMap<String, String>(1);
                    data.put("type", refType);

                    SymbolTableEntry newEntry = symbolTable.add(SymbolTableEntryType.TEMP_VAR, data);
                    semanticRecordStack.push(SemanticActionRecord.getRecord(
                            newEntry.symid,
                            RecordType.TEMP_VAR
                    ));
                    ICodeGenerator.addQuad("PEEK", (String)null, (String) null, newEntry);
                }
            }
        }
        else if(RecordType.ARR_INDEX.equals(top_sar.type)) {
            semanticLog.debug("Checking for " + top_sar.toString());

            // We already did some checking back in arr() so this *should* exist.
            SymbolTableEntry array = symbolTable.get(top_sar.data); // Get the array itself
            SymbolTableEntry index = symbolTable.get(top_sar.subRecords.get("index")); // Get the index of the array

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

            SymbolTableEntry newEntry = symbolTable.add(SymbolTableEntryType.ARR_ITEM, data);
            semanticRecordStack.push(SemanticActionRecord.getRecord(
                    newEntry.symid,
                    RecordType.TEMP_VAR
            ));
            ICodeGenerator.addQuad("AEF", array, index, newEntry); // newEntry should have baseAddress + size(1 or 4) * offset
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
                StringBuilder response = new StringBuilder("Function ")
                        .append(member.subRecords.get("id"))
                        .append(" with params ")
                        .append(member.subRecords.get("args"));
                if(!"[]".equals(member.subRecords.get("args"))) {
                    response.append(", of type [");
                    List<String> symids = symbolTable.parseParamIds(member.subRecords.get("args"));
                    for(int i = 0; i < symids.size(); i++)
                    {
                        response.append(symbolTable.get(symids.get(i)).data.get("type"));
                        if(i < (symids.size() - 1)) {
                            response.append(", ");
                        }
                    }
                    response.append("]");
                }
                throw new SemanticsException(response.append(" does not exist").toString());
            }
            String accessMod = foundFunction.data.get("accessMod");
            if(!"public".equals(accessMod)) {
                StringBuilder response = new StringBuilder("Function ")
                        .append(member.subRecords.get("id"))
                        .append(" with params ")
                        .append(member.subRecords.get("args"));
                if(!"[]".equals(member.subRecords.get("args"))) {
                    response.append(", of type [");
                    List<String> symids = symbolTable.parseParamIds(member.subRecords.get("args"));
                    for(int i = 0; i < symids.size(); i++)
                    {
                        response.append(symbolTable.get(symids.get(i)).data.get("type"));
                        if(i < (symids.size() - 1)) {
                            response.append(", ");
                        }
                    }
                    response.append("]");
                }
                throw new SemanticsException(response.append(" is not public").toString());
            }
            semanticLog.debug("Function found.");


            String refType = foundFunction.data.get("returnType");
            if(refType == null) {
                throw new SemanticsException ("No return type for " + foundFunction.value);
            }
            else {
                SymbolTableEntry container = symbolTable.get(
                        RecordType.IDENTIFIER.equals(object.type)
                                ? symbolTable.find(object.data)
                                : object.data);
                generateFuncQuads(container, foundFunction, member.subRecords.get("args"));

                if("void".equals(refType)) {
                    semanticRecordStack.push(SemanticActionRecord.getRecord(
                            "voidReturnPlaceholderSAR",
                            RecordType.VOID_RETURN
                    ));
                }
                else {
                    HashMap<String, String> data = new HashMap<String, String>(1);
                    data.put("type", refType);

                    SymbolTableEntry newEntry = symbolTable.add(SymbolTableEntryType.TEMP_VAR, data);
                    ICodeGenerator.addQuad("PEEK", (String)null, (String) null, newEntry);

                    semanticRecordStack.push(SemanticActionRecord.getRecord(
                            newEntry.symid,
                            RecordType.TEMP_VAR
                    ));
                }
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
                throw new SemanticsException("Member " + member.data + " is not public");
            }
            semanticLog.debug("Reference found.");

            String refType = symbolTable.get(symid).data.get("type");
            if(refType == null) {
                throw new SemanticsException ("No type for " + member.data);
            }
            HashMap<String, String> data = new HashMap<String, String>(1);
            data.put("type", refType);

            SymbolTableEntry newEntry = symbolTable.add(SymbolTableEntryType.REF_MEMBER, data);
            doQuads("REF", obj, symbolTable.get(symid), newEntry);
            semanticRecordStack.push(SemanticActionRecord.getRecord(
                    newEntry.symid,
                    RecordType.MEMBER_REF));
        }
        semanticLog.debug("Reference exists.");
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
        SymbolTableEntry boolParam = checkType("bool");
        semanticLog.debug("if, bool check passed");
        String label = "SKIPIF" + labelGen++;
        ICodeGenerator.addQuad(new Quad("BF", boolParam, label, null));
        labelStack.add(0, label);
    }

    public static void endIf() throws SemanticsException {
        ICodeGenerator.addQuad(labelStack.remove(0));
    }

    public static void endElse() throws SemanticsException {
        ICodeGenerator.addQuad(labelStack.remove(0));
    }

    public static void beginElse() throws SemanticsException {
        String label = "SKIPELSE" + labelGen++;
        ICodeGenerator.addQuad(new Quad("JMP", label, null, (String) null));
        ICodeGenerator.addQuad(labelStack.remove(0));
        labelStack.add(0, label);
    }

    public static void doWhile() throws SemanticsException {
        semanticLog.debug("while, checking if arg is type bool");
        SymbolTableEntry boolParam = checkType("bool");
        semanticLog.debug("while, bool check passed");
        String label = "ENDWHILE" + labelGen++;
        ICodeGenerator.addQuad("BF", boolParam, label, null);
        labelStack.add(0, label);
    }

    public static void beginWhile() {
        String label = "BEGIN" + labelGen++;
        labelStack.add(0, label);
        ICodeGenerator.addQuad(label);
    }

    public static void endWhile() {
        String endLabel = labelStack.remove(0);
        String beginLabel = labelStack.remove(0);
        ICodeGenerator.addQuad(new Quad("JMP", beginLabel, null, (String) null));
        ICodeGenerator.addQuad(endLabel);
    }

    private static SymbolTableEntry checkType(String type) throws SemanticsException {
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
        return entry;
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
                ICodeGenerator.addQuad("RTN", null, null, (String) null);
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
        ICodeGenerator.addQuad("RTN", returnValue, (SymbolTableEntry) null, null);
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
        SymbolTableEntry charData = checkType("char");
        semanticLog.debug("atoi, char check passed");

        HashMap<String, String> data = new HashMap<String, String>(1);
        data.put("type", "int");
        SymbolTableEntry newEntry = symbolTable.add(SymbolTableEntryType.TEMP_VAR, data);
        semanticRecordStack.push(SemanticActionRecord.getRecord(
                newEntry.symid,
                RecordType.TEMP_VAR
        ));
        ICodeGenerator.addQuad("ATOI", charData, (String)null, newEntry);
    }

    public static void itoa() throws SemanticsException {
        semanticLog.debug("itoa, checking if arg is type int");
        SymbolTableEntry intData = checkType("int");
        semanticLog.debug("itoa, int check passed");

        HashMap<String, String> data = new HashMap<String, String>(1);
        data.put("type", "char");
        SymbolTableEntry newEntry = symbolTable.add(SymbolTableEntryType.TEMP_VAR, data);
        semanticRecordStack.push(SemanticActionRecord.getRecord(
                newEntry.symid,
                RecordType.TEMP_VAR
        ));
        ICodeGenerator.addQuad("ITOA", intData, (String)null, newEntry);
    }

    public static void BAL() {
        semanticLog.debug("Begin argument list");
        semanticRecordStack.push(
                new SemanticActionRecord("", RecordType.BAL));
    }
    public static void EAL() {
        semanticLog.debug("End argument list");
        SemanticActionRecord sar;
        String paramList = "[";
        List<String> params = new ArrayList<String>();
        while(!RecordType.BAL.equals(
                (sar = semanticRecordStack.pop()).type))
        {
            params.add(0, sar.data); // Get the params in the correct order.
        }

        if(params.size() < 1) {
            paramList = "[]";
        }
        else {
            for(String s : params) {
                paramList += s + ", ";
            }
            paramList = paramList.substring(0, paramList.length() - 2) + "]";
        }

        semanticRecordStack.push(
                new SemanticActionRecord(
                        paramList,
                        RecordType.ARG_LIST
                )
        );
    }

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
        SymbolTableEntry index = checkType("int");
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
        data.put("type", arrType);
        semanticRecordStack.push(SemanticActionRecord.getRecord(
                symid,
                RecordType.ARR_INDEX,
                index.symid
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
            StringBuilder response = new StringBuilder("Constructor ")
                    .append(identifier.data)
                    .append(" with params ")
                    .append(argList.data);
            if(!"[]".equals(argList.data)) {
                response.append(", of type [");
                List<String> symids = symbolTable.parseParamIds(argList.data);
                for(int i = 0; i < symids.size(); i++)
                {
                    response.append(symbolTable.get(symids.get(i)).data.get("type"));
                    if(i < (symids.size() - 1)) {
                        response.append(", ");
                    }
                }
                response.append("]");
            }
            throw new SemanticsException(response.append(" does not exist").toString());
        }
        semanticLog.debug("Constructor found.");

        HashMap<String, String> data = new HashMap<String, String>(1);
        data.put("type", identifier.data);

        SymbolTableEntry newEntry = symbolTable.add(SymbolTableEntryType.TEMP_VAR, data);

        int classSize = symbolTable.sizeOf(identifier.data);
        ICodeGenerator.addQuad("NEWI", String.valueOf(classSize), (String) null, newEntry);
        generateFuncQuads(newEntry, foundFunction, argList.data);

        SymbolTableEntry newEntry2 = symbolTable.add(SymbolTableEntryType.TEMP_VAR, data);
        semanticRecordStack.push(SemanticActionRecord.getRecord(
                newEntry2.symid,
                RecordType.TEMP_VAR
        ));
        ICodeGenerator.addQuad("PEEK", (String) null, (String) null, newEntry2);
    }

    public static void new_arrBrackets() throws SemanticsException {
        semanticLog.debug("new[], checking if arg is type int");
        SymbolTableEntry arrSize = checkType("int");
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

        SymbolTableEntry arraySizeVar = symbolTable.add(SymbolTableEntryType.TEMP_VAR, data);
        SymbolTableEntry newEntry = symbolTable.add(SymbolTableEntryType.TEMP_VAR, data);
        semanticRecordStack.push(SemanticActionRecord.getRecord(
                newEntry.symid,
                RecordType.NEW_ARR
        ));
        int arrItemSize;
        if("char".equals(arrType.data) || "bool".equals(arrType.data)) {
            arrItemSize = 1;
        }
        else { arrItemSize = 4; }

        ICodeGenerator.addQuad(
                "MULI",
                arrSize,
                String.valueOf(arrItemSize),
                arraySizeVar
        );

        ICodeGenerator.addQuad("NEW", arraySizeVar, (String) null, newEntry);

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
            if(RecordType.SYMID.equals(exp_sar.type)
                    || RecordType.MEMBER_REF.equals(exp_sar.type)
                    || (SemanticCheck.COUT.equals(checkType)
                    && (RecordType.TEMP_VAR.equals(exp_sar.type) || RecordType.LITERAL.equals(exp_sar.type)))) // Can only use a literal with cout.
            {
                SymbolTableEntry entry = symbolTable.get(exp_sar.data);
                if(entry.data == null) {
                    throw new SemanticsException("No data for " + exp_sar.data);
                }
                if(!entry.data.get("type").equals("int")
                    && !entry.data.get("type").equals("char")) {
                    throw new SemanticsException("cin/cout expected int or char type, got " + entry.data.get("type"));
                }
                generateIOQuads(entry, checkType);
            }
            else {
                throw new SemanticsException("Cannot use SAR of type " + exp_sar.type.name() + " with "+ checkType.name());
            }
        }
        semanticLog.debug(checkType.name() + " validity check passed."); // If it failed there would have been an exception.
    }

    private static void generateIOQuads(SymbolTableEntry entry, SemanticCheck checkType) {
        boolean isCin = SemanticCheck.CIN.equals(checkType);
        String intCharArg = "int".equals(entry.data.get("type")) ? "1" : "3";
        if(isCin) {
            ICodeGenerator.addQuad("READ", intCharArg, (SymbolTableEntry) null, entry);
        }
        else {
            ICodeGenerator.addQuad("WRITE", entry, intCharArg, null);
        }
    }

    private static void generateFuncQuads(SymbolTableEntry container, SymbolTableEntry member, String paramList) {
        if(container == null) {
            ICodeGenerator.addQuad("FRAME", member, "this", null);
        }
        else {
            ICodeGenerator.addQuad("FRAME", member, container, null);
        }
        if(paramList!= null) {
            List<String> paramSymIds = symbolTable.parseParamIds(paramList);
            for(int i = 0; i < paramSymIds.size(); i++)
            {
                ICodeGenerator.addQuad("PUSH", symbolTable.get(paramSymIds.get(i)), (SymbolTableEntry) null, null);
            }
        }

        ICodeGenerator.addQuad("CALL", member, (SymbolTableEntry) null, null);
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
        SymbolTableEntry rhs = RecordType.THIS_PLACEHOLDER.equals(s1.type)
                ? SymbolTable.THIS_PLACEHOLDER
                : symbolTable.get(s1.data); // Assume is symid by now, else this would fail
        SymbolTableEntry lhs = RecordType.THIS_PLACEHOLDER.equals(s2.type)
                ? SymbolTable.THIS_PLACEHOLDER
                : symbolTable.get(s2.data);

        if(rhs == null) {
            throw new SemanticsException("Symid " + s1.data + " not found in symbol table");
        }
        else if(lhs == null) {
            throw new SemanticsException("Symid " + s2.data + " not found in symbol table");
        }

        semanticLog.debug("Evaluating expression: (" + s2.toString() + ") " + op.Symbol + " (" + s1.toString() + ")");

        String type = op.opResult(rhs, lhs);
        if(type == null) {
            String typeString = "";
            if(rhs.data != null && lhs.data != null) {
                typeString = ", of types " + lhs.data.get("type") + " and " + rhs.data.get("type");
            }
            throw new SemanticsException("Can't perform operation " + op.Symbol
                    + " on operands " + lhs.value + " and " + rhs.value
                    + typeString
                    + " in scope " + rhs.scope
            );
        }
        semanticLog.debug("Expression is valid. Creating new temp variable of type " + type);
        HashMap<String, String> data = new HashMap<String, String>(1);
        data.put("type", type);


        SymbolTableEntry newEntry = symbolTable.add(SymbolTableEntryType.TEMP_VAR, data);
        doQuads(op.Symbol, lhs, rhs, newEntry);
        semanticRecordStack.push(SemanticActionRecord.getRecord(
                newEntry.symid,
                RecordType.TEMP_VAR
        ));
    }

    private static void doQuads(String symbol, SymbolTableEntry lhs, SymbolTableEntry rhs, SymbolTableEntry newEntry) {
        if("=".equals(symbol)) {
            iLog.debug("Assignment expression, generating icode");
            ICodeGenerator.addQuad("MOV", rhs, (SymbolTableEntry) null, lhs);
        }
        else if("+".equals(symbol)) {
            iLog.debug("Add expression, generating icode");
            if(SymbolTableEntryType.GLOBAL_LITERAL.equals(lhs.kind)) {
                ICodeGenerator.addQuad("ADI", rhs, lhs.value, newEntry); // Addition is commutative, so we can flip the params so the immediate is in slot 2.
            }
            else if(SymbolTableEntryType.GLOBAL_LITERAL.equals(rhs.kind)) {
                ICodeGenerator.addQuad("ADI", lhs, rhs.value, newEntry);
            }
            else {
                ICodeGenerator.addQuad("ADD", lhs, rhs, newEntry);
            }
        }
        else if("-".equals(symbol)) {
            iLog.debug("Subtract expression, generating icode");
            ICodeGenerator.addQuad("SUB", lhs, rhs, newEntry);
        }
        else if("*".equals(symbol)) {
            iLog.debug("Multiply expression, generating icode");
            ICodeGenerator.addQuad("MUL", lhs, rhs, newEntry);
        }
        else if("/".equals(symbol)) {
            iLog.debug("Divide expression, generating icode");
            ICodeGenerator.addQuad("DIV", lhs, rhs, newEntry);
        }
        else if("==".equals(symbol)) {
            iLog.debug("Equals expression, generating icode");
            ICodeGenerator.addQuad("EQ", lhs, rhs, newEntry);
        }
        else if("!=".equals(symbol)) {
            iLog.debug("Does-not-equal expression, generating icode");
            ICodeGenerator.addQuad("NE", lhs, rhs, newEntry);
        }
        else if("<".equals(symbol)) {
            iLog.debug("Less-than expression, generating icode");
            ICodeGenerator.addQuad("LT", lhs, rhs, newEntry);
        }
        else if(">".equals(symbol)) {
            iLog.debug("Greater-than expression, generating icode");
            ICodeGenerator.addQuad("GT", lhs, rhs, newEntry);
        }
        else if("<=".equals(symbol)) {
            iLog.debug("Less-than-or-equals expression, generating icode");
            ICodeGenerator.addQuad("LE", lhs, rhs, newEntry);
        }
        else if(">=".equals(symbol)) {
            iLog.debug("Greater-than-or-equals expression, generating icode");
            ICodeGenerator.addQuad("GE", lhs, rhs, newEntry);
        }
        else if("&&".equals(symbol)) {
            iLog.debug("And expression, generating icode");
            ICodeGenerator.addQuad("AND", lhs, rhs, newEntry);
        }
        else if("||".equals(symbol)) {
            iLog.debug("Equals expression, generating icode");
            ICodeGenerator.addQuad("OR", lhs, rhs, newEntry);
        }
        else if("REF".equals(symbol)) {
            ICodeGenerator.addQuad("REF", lhs, rhs, newEntry);
        }
    }

    private static enum SemanticCheck {
        COUT,
        CIN,
    }
}
