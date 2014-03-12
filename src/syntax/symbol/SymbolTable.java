package syntax.symbol;

import log.Log;
import semantics.record.sar.RecordType;
import semantics.record.sar.SemanticActionRecord;
import syntax.AnalyzerConstants;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/13/14
 * Time: 5:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class SymbolTable implements Map<String, SymbolTableEntry> {
    public static final String NULL_SYMID = "NULL";
    public static final SymbolTableEntry THIS_PLACEHOLDER = new SymbolTableEntry("", "THIS", "this", SymbolTableEntryType.GLOBAL_LITERAL, null);

    private static Log symLog = new Log("symbol_table.log");
    private static Log symDump = new Log("symbol_table_dump.log");
    private static SymbolTable theInstance = new SymbolTable();

    private static Map<String, SymbolTableEntry> innerTable;
    private static Map<String, List<String>> scopesToSymIdsMap;
    private static String scope = "g";

    private SymbolTable() {
        innerTable = new HashMap<String, SymbolTableEntry>();
        scopesToSymIdsMap = new HashMap<String, List<String>>();
        scopesToSymIdsMap.put("g", new ArrayList<String>());

        // Deal with THIS_PLACEHOLDER
        THIS_PLACEHOLDER.data = new HashMap<String, String>(1);
        THIS_PLACEHOLDER.data.put("type", "this");
    }

    /**
     * Looks up the current scope in the symbol table
     * @return symbol table entry for the current class or function (hopefully)
     */
    public SymbolTableEntry findCurrentScopeEntry() {
        if("g".equals(scope)) {
            symLog.log("Tried to find entry for global scope in the symbol table!!!");
            return null;
        }
        String containingScope = scope.substring(0, scope.lastIndexOf("."));
        String identifier = scope.substring(scope.lastIndexOf(".") + 1, scope.length());
        for(String symid : scopesToSymIdsMap.get(containingScope)) {
            SymbolTableEntry entry = innerTable.get(symid);

            if(entry.value.equals(identifier)) {
                symLog.debug("Successfully found entry for current scope in the symbol table");
                return entry; // We can't have duplicate names within a scope, and we know it exists, so all we care is the name.
            }
        }
        symLog.log("Failed to find entry for current scope in the symbol table");
        return null;
    }

    public String findCurrentClassScope() {
        if("g".equals(scope)) {
            symLog.log("Tried to find current class scope but was already in global scope!!!");
            return null;
        }
        String classScope =
                scope.substring(
                        scope.indexOf(".") + 1, // This cuts off the "g." part of scope
                        scope.substring(scope.indexOf(".") + 1, scope.length()) // Gets scope minus the "g." part. If scope is g.Cat.meow, this gets Cat.meow
                        .indexOf(".") + 2 // The second '.' in overall scope.  If scope is g.Cat.meow, this gets the '.' between Cat and meow.  The + 2 is to account for the "g." that got chopped of before the index was computed.
                );
        symLog.debug("Class scope is " + classScope);
        return classScope;
    }

    public static SymbolTable get() {
        return theInstance;
    }

    public boolean identifierExists(String identifier) {
        return iExists(identifier, scope);
    }

    public boolean classExists(String className) {
        return iExists(className, "g");
    }

    public boolean identifierExists(String identifier, String className) {
        return iExists(identifier, ("g." + className));
    }

    /**
     * Helper function, checks in nested scopes
     * @param identifier
     * @param scope
     * @return true or false
     */
    private static boolean iExists(String identifier, String scope) {
        SymbolTableEntry entry;
        for(String symid : scopesToSymIdsMap.get(scope)) {
            entry = innerTable.get(symid);

            if(entry.scope.equals(scope)
                    && entry.value.equals(identifier)) {
                return true;
            }
        }
        if(!"g".equals(scope)) {
            return iExists(identifier, scope.substring(0, scope.lastIndexOf(".")));
        }
        return false;
    }

    /**
     * Looks up an identifier in the symbol table and returns its SymId
     * @param identifier
     * @return symid of the identifier
     */
    public String find(String identifier) {
        return iFinder(identifier, scope);
    }

    public int getTotalTempVars(String funcSymId) {
        int totalTempVars = 0;
        for(String symid : scopesToSymIdsMap.get(innerTable.get(funcSymId).scope + "." + innerTable.get(funcSymId).value))
        {
            if(SymbolTableEntryType.LOCAL_VAR.equals(innerTable.get(symid).kind)
                    || SymbolTableEntryType.TEMP_VAR.equals(innerTable.get(symid).kind))
            {
                totalTempVars += 1;
            }
        }
        return totalTempVars;
    }

    public int getFuncSize(String funcSymId) {
        int totalTempVars = 0;
        for(String symid : scopesToSymIdsMap.get(innerTable.get(funcSymId).scope + "." + innerTable.get(funcSymId).value))
        {
            if(SymbolTableEntryType.LOCAL_VAR.equals(innerTable.get(symid).kind)
                    || SymbolTableEntryType.TEMP_VAR.equals(innerTable.get(symid).kind)
                    || SymbolTableEntryType.PARAM.equals(innerTable.get(symid).kind)
                    || SymbolTableEntryType.REF_MEMBER.equals(innerTable.get(symid).kind)
                    || SymbolTableEntryType.ARR_ITEM.equals(innerTable.get(symid).kind))
            {
                totalTempVars += 1;
            }
        }
        return totalTempVars * 4;
    }

    /**
     * Looks up an identifier in the symbol table and returns its SymId
     * @param member
     * @param scope
     * @return symid of the identifier
     */
    public String findInScope(String member, String scope) {
        for(String sym : scopesToSymIdsMap.get(scope))
        {
            if(innerTable.get(sym).value.equals(member)) {
                return sym;
            }
        }
        return null;
    }

    public void doSize(String classIdentifier) {
        String classSymId = findInScope(classIdentifier, "g");
        Map<String, String> data = new HashMap<String, String>(1);
        data.put("classSize", String.valueOf(sizeOf(classIdentifier)));
        innerTable.get(classSymId).data = data;
    }

//    /**
//     * Find within a class?
//     * @param identifier
//     * @param className
//     * @return
//     */
//    public static boolean find(String identifier, String className) {
//        return iExists(identifier, ("g." + className));
//    }

    /**
     * Helper function, checks in nested scopes
     * @param identifier
     * @param scope
     * @return symid, null if not found
     */
    private String iFinder(String identifier, String scope) {
        for(SymbolTableEntry entry : innerTable.values()) {
            if(entry.scope.equals(scope)
                    && entry.value.equals(identifier)) {
                return entry.symid;
            }
        }
        if(!"g".equals(scope)) {
            return iFinder(identifier, scope.substring(0, scope.lastIndexOf(".")));
        }
        return null;
    }


    @Override
    public int size() { return innerTable.size(); }

    @Override
    public boolean isEmpty() { return innerTable.isEmpty(); }

    @Override
    public boolean containsKey(Object key) { return innerTable.containsKey(key); }

    @Override
    public boolean containsValue(Object value) { return innerTable.containsValue(value); }

    @Override
    public SymbolTableEntry get(Object key)  { return innerTable.get(key); }

    @Override
    public SymbolTableEntry put(String key, SymbolTableEntry value) { return innerTable.put(key, value); }

    @Override
    public SymbolTableEntry remove(Object key) { return innerTable.remove(key); }

    @Override
    public void putAll(Map m) { innerTable.putAll(m); }

    @Override
    public void clear() { innerTable.clear(); }

    @Override
    public Set<String> keySet() { return innerTable.keySet(); }

    @Override
    public Collection<SymbolTableEntry> values() { return innerTable.values(); }

    @Override
    public Set<Entry<String, SymbolTableEntry>> entrySet() { return innerTable.entrySet(); }

    private boolean isDuplicateEntry(String lexeme) {
        List<String> symids = scopesToSymIdsMap.get(scope);
        for(String s : symids) {
            if(lexeme.equals(innerTable.get(s).value)) {
                return true;
            }
        }
        return false;
    }

    public int getSizeForOffset(String typeName) {
        if("bool".equals(typeName)
                || "char".equals(typeName)) {
            return 1;
        }
        else {
            return 4;
        }
    }

    public int getSize(String typeName) {
        String datatype;
        if(typeName.startsWith("@")) {
            datatype = typeName.substring(2, typeName.length());
        }
        else {
            datatype = typeName;
        }


        if("bool".equals(datatype)
                || "char".equals(datatype)) {
            return 1;
        }
        else {
            return 4;
        }
    }

    public SymbolTableEntry add(String lexeme, SymbolTableEntryType type, Map<String, String> data, boolean isMethod) throws DuplicateSymbolException {
        if(this.isDuplicateEntry(lexeme)) {
            throw new DuplicateSymbolException(lexeme, scope);
        }

        if(isMethod) {
            String oldScope = scope;
            scope = scope.substring(0, scope.lastIndexOf("."));

            SymbolTableEntry returnVal = add(lexeme, type, data);
            scope = oldScope;
            return returnVal;
        }
        else {
            return add(lexeme, type, data);
        }
    }

    private SymbolTableEntry add(String lexeme, SymbolTableEntryType type, Map<String, String> data) throws DuplicateSymbolException {
        if(this.isDuplicateEntry(lexeme)) {
            throw new DuplicateSymbolException(lexeme, scope);
        }

        SymbolTableEntry newEntry = new SymbolTableEntry(scope, type.name().substring(0, 1), lexeme, type, data);
        innerTable.put(newEntry.symid, newEntry);

            scopesToSymIdsMap.get(scope).add(newEntry.symid);

        // Logging
        symLog.log("Added new entry to symbol table: " + newEntry.symid);
        symLog.log("\t" + "identifier = " + newEntry.value);
        symLog.log("\t" + "scope = " + newEntry.scope);
        symLog.log("\t" + "type = " + newEntry.kind.name());
        if(newEntry.data == null) {
            symLog.log("\t" + "data = none;");
        }
        else {
            symLog.log("\t" + "data =");
            for(String s : data.keySet()) {
                symLog.log(s + " : " + data.get(s));
            }
        }
        return newEntry;
    }

    private int getMaxOffset(String scope) {
        int maxOffset = 0;
        int temp;
        SymbolTableEntry entry;
        for(String symid : scopesToSymIdsMap.get(scope)) {
            entry = innerTable.get(symid);
            if(SymbolTableEntryType.PARAM.equals(entry.kind)
                    || SymbolTableEntryType.LOCAL_VAR.equals(entry.kind)
                    || SymbolTableEntryType.TEMP_VAR.equals(entry.kind)
                    || SymbolTableEntryType.ARR_ITEM.equals(entry.kind)
                    || SymbolTableEntryType.REF_MEMBER.equals(entry.kind)) {
                temp = Integer.parseInt(innerTable.get(symid).data.get("offset"));
                if(temp > maxOffset) {
                    maxOffset = temp;
                }
            }
        }
        return maxOffset;
    }

    public int getOffset(SymbolTableEntry entry) {
        if(SymbolTableEntryType.PARAM.equals(entry.kind)
                || SymbolTableEntryType.LOCAL_VAR.equals(entry.kind)
                || SymbolTableEntryType.TEMP_VAR.equals(entry.kind)
                || SymbolTableEntryType.ARR_ITEM.equals(entry.kind)
                || SymbolTableEntryType.REF_MEMBER.equals(entry.kind)) {
            return -12 - Integer.parseInt(entry.data.get("offset"));
        }
        return Integer.parseInt(entry.data.get("offset"));
    }

    public SymbolTableEntry add(SymbolTableEntryType type, Map<String, String> data) {
        int offset = getMaxOffset(scope) + 4;
        data.put("offset", String.valueOf(offset));

        SymbolTableEntry newEntry = new SymbolTableEntry(scope, type.name().substring(0, 1), type, data);
        innerTable.put(newEntry.symid, newEntry);

        scopesToSymIdsMap.get(scope).add(newEntry.symid);

        // Logging
        symLog.log("Added new entry to symbol table: " + newEntry.symid);
        symLog.log("\t" + "identifier = " + newEntry.value);
        symLog.log("\t" + "scope = " + newEntry.scope);
        symLog.log("\t" + "type = " + newEntry.kind.name());
        if(newEntry.data == null) {
            symLog.log("\t" + "data = none;");
        }
        else {
            symLog.log("\t" + "data =");
            for(String s : data.keySet()) {
                symLog.log(s + " : " + data.get(s));
            }
        }
        return newEntry;
    }

    public void addLiteral(String lexeme, Map<String, String> data) {
        if(!iExists(lexeme, "g")) // Look for literal in global scope
        {
            SymbolTableEntry newEntry;
            if("null".equals(lexeme)) {
                newEntry = new SymbolTableEntry("g", NULL_SYMID, lexeme, data);
            }
            else {
                newEntry = new SymbolTableEntry("g", "G", lexeme, SymbolTableEntryType.GLOBAL_LITERAL, data);
            }

            innerTable.put(newEntry.symid, newEntry);
            scopesToSymIdsMap.get("g").add(newEntry.symid);
            symLog.log("Added new literal to symbol table: " + newEntry.symid);
            symLog.log("\t" + "identifier = " + newEntry.value);
            symLog.log("\t" + "scope = " + newEntry.scope);
            symLog.log("\t" + "type = " + newEntry.kind.name());
            if(newEntry.data == null) {
                symLog.log("\t" + "data = none;");
            }
            else {
                symLog.log("\t" + "data =");
                for(String s : data.keySet()) {
                    symLog.log(s + " : " + data.get(s));
                }
            }
        }
        else {
            symLog.log("Literal already exists in symbol table and was not added.");
        }
    }

    public String getScope() { return scope; }

    public void setScope(String newScope) {
        scope = newScope;
        if(scopesToSymIdsMap.get(newScope) == null) {
            scopesToSymIdsMap.put(newScope, new ArrayList<String>());
        }
    }

    /**
     * Based on my testing in Java, these things are irrelevant here:
     * -accessMod
     * -returnType
     *
     * We only care about Param (order, arity, and type)
     *
     * @param container the container of the function
     * @param member the record containing function info
     * @return whether or not a matching function is found
     */
    public SymbolTableEntry findFunction(SemanticActionRecord container, SemanticActionRecord member) {

        if(!RecordType.FUNC.equals(member.type)) {
            return null; // Must have a func_sar for this to work.
        }

        SymbolTableEntry theObj;
        if(RecordType.IDENTIFIER.equals(container.type)) {
            theObj = innerTable.get(iFinder(container.data, scope));
        }
        else if(RecordType.TEMP_VAR.equals(container.type)
                || RecordType.SYMID.equals(container.type)
                || RecordType.MEMBER_REF.equals(container.type)) // Can't be a literal because those are only primitives.
        {
            theObj = innerTable.get(container.data);
        }
        else {
            return null; // Not a type of SAR we can use to look up what we need?
        }

        // Look up the class the object is made of.
        if(theObj.data == null)
        {
            return null; // Doesn't have data.
        }
        String className = theObj.data.get("type");
        return fFinder(
                member,
                "g." + className);
    }

    public SymbolTableEntry findFunctionInClass(SemanticActionRecord func) {
        if(!RecordType.FUNC.equals(func.type)) {
            return null; // Must have a func_sar for this to work.
        }

        return fFinder(
                func,
                "g." + findCurrentClassScope()
        );
    }

    public SymbolTableEntry findConstructor(String identifier, String argList) {
        SymbolTableEntry entry;
        List<String> paramIdsPassed = parseParamIds(argList);
        List<String> paramIdsSymbolTable;
        boolean allParamsMatch;
        for(String symid : scopesToSymIdsMap.get("g." + identifier)) {
            entry = innerTable.get(symid);

            if(!entry.value.equals(identifier)) {
                continue; // Identifiers don't match; try a different method.
            }
            else if(!SymbolTableEntryType.METHOD.equals(entry.kind)) {
                continue; // This isn't a method, try something else.
            }

            paramIdsSymbolTable = parseParamIds(entry.data.get("Param"));
            if(paramIdsPassed.size() != paramIdsSymbolTable.size()) {
                continue; // Arity - can't match if there are a different number of params
            }

            // Order and type are checked together
            allParamsMatch = true;
            for(int i = 0; i < paramIdsPassed.size(); i++) {
                String paramType1 = innerTable.get(paramIdsPassed.get(i)).data.get("type");
                String paramType2 = innerTable.get(paramIdsSymbolTable.get(i)).data.get("type");

                if(!paramType1.equals(paramType2)) {
                    allParamsMatch = false; // Can't match if the param types are different
                    break; // No point checking if other params match
                }
            }
            if(allParamsMatch)
            {
                return entry; // Nothing different about the params, so it's the same method.
            }
        }
        return null;
    }


    /**
//     * Based on my testing in Java, these things are irrelevant here:
//     * -accessMod
//     * -returnType
     *
//     * We only care about Param (order, arity, and type)
     *
     * @param function the sar containing function identifier and params info
     * @param containerScope the scope in which to search for the member
     * @return whether or not a matching function is found
     */
    public SymbolTableEntry fFinder(SemanticActionRecord function, String containerScope)
    {
        SymbolTableEntry entry;
        List<String> paramIdsPassed = parseParamIds(function.subRecords.get("args"));
        List<String> paramIdsSymbolTable;
        boolean allParamsMatch;
        for(String symid : scopesToSymIdsMap.get(containerScope))
        {
            entry = innerTable.get(symid);
            if(!entry.value.equals(function.subRecords.get("id"))) {
                continue; // Identifiers don't match; try a different method.
            }
            else if(!SymbolTableEntryType.METHOD.equals(entry.kind)) {
                continue; // This isn't a method, try something else.
            }

            paramIdsSymbolTable = parseParamIds(entry.data.get("Param"));
            if(paramIdsPassed.size() != paramIdsSymbolTable.size()) {
                continue; // Arity - can't match if there are a different number of params
            }

            // Order and type are checked together
            allParamsMatch = true;
            for(int i = 0; i < paramIdsPassed.size(); i++) {
                String paramType1 = innerTable.get(paramIdsPassed.get(i)).data.get("type");
                String paramType2 = innerTable.get(paramIdsSymbolTable.get(i)).data.get("type");

                if(!paramType1.equals(paramType2)) {
                    allParamsMatch = false; // Can't match if the param types are different
                    break; // No point checking if other params match
                }
            }
            if(allParamsMatch)
            {
                return entry; // Nothing different about the params, so it's the same method.
            }
        }
        return null; // No scope adjustment?
    }

    // Gets symids of all params from the string list
    public List<String> parseParamIds(String paramList) {
        List<String> paramIds = new ArrayList<String>();
        if("[]".equals(paramList)) {
            return paramIds;
        }

        String plist = paramList.substring(1, paramList.length() - 1); // Chop off array brackets.
        String[] params = plist.split(", ");
        for(int i = 0; i < params.length; i++) {
            paramIds.add(params[i]);
        }
        return paramIds;
    }

    public int sizeOf(String className) {
        int size = 0;
        SymbolTableEntry entry;
        for(String symid : scopesToSymIdsMap.get("g." + className))
        {
            entry = innerTable.get(symid);
            if(SymbolTableEntryType.INSTANCE_VAR.equals(entry.kind)) {
                if(entry.data != null && entry.data.get("type") != null) {
                    if("bool".equals(entry.data.get("type"))
                            || "char".equals(entry.data.get("type")))
                    {
                        size += 1;
                    }
                    else {
                        size += 4;
                    }
                }
            }
        }
        return size;
    }

    public void dumpSymbolTable() {
        SymbolTableEntry entry;
        for(String symid : innerTable.keySet()) {
            entry = innerTable.get(symid);
            symDump.log(symid);
            symDump.log("\t" + "identifier = " + entry.value);
            symDump.log("\t" + "scope = " + entry.scope);
            symDump.log("\t" + "type = " + entry.kind.name());
            if(entry.data == null) {
                symDump.log("\t" + "data = none;");
            }
            else {
                symDump.log("\t" + "data =");
                for(String s : entry.data.keySet()) {
                    symDump.log(s + " : " + entry.data.get(s));
                }
            }
        }
    }

    public static List<SymbolTableEntry> getGlobalSymbols() {
        List<SymbolTableEntry> globalSymbols = new ArrayList<SymbolTableEntry>();
        SymbolTableEntry entry;
        for(String symid: scopesToSymIdsMap.get("g")) {
            entry = innerTable.get(symid);
            if(SymbolTableEntryType.GLOBAL_LITERAL.equals(entry.kind)) {
                globalSymbols.add(entry);
            }
        }
        return globalSymbols;
    }
}
