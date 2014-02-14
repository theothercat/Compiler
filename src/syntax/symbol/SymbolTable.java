package syntax.symbol;

import log.Log;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/13/14
 * Time: 5:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class SymbolTable implements Map<String, SymbolTableEntry> {
    private static Log symLog = new Log("symbol_table.log");
    private static SymbolTable theInstance = new SymbolTable();

    private static Map<String, SymbolTableEntry> innerTable;
    private static Map<String, List<String>> scopesToSymIdsMap;
    private static String scope = "g";

    private SymbolTable() {
        innerTable = new HashMap<String, SymbolTableEntry>();
        scopesToSymIdsMap = new HashMap<String, List<String>>();
        scopesToSymIdsMap.put("g", new ArrayList<String>());
    }

    public static SymbolTable get() {
        return theInstance;
    }

    public boolean identifierExists(String identifier) {
        return iExists(identifier, scope);
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
        for(SymbolTableEntry entry : innerTable.values()) {
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

    public SymbolTableEntry add(String lexeme, SymbolTableEntryType type, Map<String, String> data) throws DuplicateSymbolException {
        if(this.isDuplicateEntry(lexeme)) {
            throw new DuplicateSymbolException(lexeme, scope);
        }

        SymbolTableEntry newEntry = new SymbolTableEntry(scope, type.name().substring(0, 1), lexeme, type, data);
        innerTable.put(newEntry.symid, newEntry);

//        if(!SymbolTableEntryType.PARAM.equals(newEntry.kind)) {
            scopesToSymIdsMap.get(scope).add(newEntry.symid);
//        }

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

    public SymbolTableEntry add(SymbolTableEntryType type, Map<String, String> data) {
        SymbolTableEntry newEntry = new SymbolTableEntry(scope, type.name().substring(0, 1), type, data);
        innerTable.put(newEntry.symid, newEntry);

//        if(!SymbolTableEntryType.PARAM.equals(newEntry.kind)) {
        scopesToSymIdsMap.get(scope).add(newEntry.symid);
//        }

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
            SymbolTableEntry newEntry = new SymbolTableEntry("g", "G", lexeme, SymbolTableEntryType.GLOBAL_LITERAL, data);

            innerTable.put(newEntry.symid, newEntry);
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
//            symLog.debug("Literal already exists in symbol table and was not added.");
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

//    public void checkDuplicates() throws DuplicateSymbolException {
//        for(List<String> symids : scopesToSymIdsMap.values()) {
//            for(String symid : symids) {
//                SymbolTableEntry s1 = innerTable.get(symid);
//                for(String symid2 : symids) {
//                    if(!symid.equals(symid2) // Can't be the same symbol
//                        && isDuplicate(innerTable.get(symid), innerTable.get(symid2))) {
//                    }
//                }
//            }
//        }
//    }

//    public boolean isDuplicate(SymbolTableEntry s1, SymbolTableEntry s2) {
//        if(s1.value.equals(s2.value)) {
//            if((SymbolTableEntryType.METHOD.equals(s1.kind)
//                    || SymbolTableEntryType.METHOD.equals(s2.kind))) // One is a method
//            {
//                if(!s1.kind.equals(s2.kind)) // One is a method, and the other is not
//                {
//                    return false; // Possible to have a method and a variable that share a name.
//                }
//                else {
//                    return isMethodMatch(s1.data, s2.data); // Both are methods, so find out if they are the same.
//                }
//            }
//            else {
//                return true; // If they aren't methods, they can't share a name.
//            }
//        }
//        return false; // If the names aren't the same, they aren't a duplicate!
//    }

    /**
     * Based on my testing in Java, these things are irrelevant here:
     * -accessMod
     * -returnType
     *
     * We only care about Param (order, arity, and type)
     *
     * @param funcIdentifier the identifier for the function
     * @param paramList the symids of args when the function is called
     * @return whether or not a matching function is found
     */
    public boolean functionExists(String funcIdentifier, String paramList)
    {
        return functionExists(funcIdentifier, paramList, scope);
    }


    /**
     * Based on my testing in Java, these things are irrelevant here:
     * -accessMod
     * -returnType
     *
     * We only care about Param (order, arity, and type)
     *
     * @param funcIdentifier the identifier for the function
     * @param paramList the symids of args when the function is called
     * @return whether or not a matching function is found
     */
    public boolean functionExists(String funcIdentifier, String paramList, String scope)
    {
        SymbolTableEntry entry;
        List<String> paramIdsPassed = parseParamIds(paramList);
        List<String> paramIdsSymbolTable;
        for(String symid : scopesToSymIdsMap.get(scope))
        {
            entry = innerTable.get(symid);
            if(!SymbolTableEntryType.METHOD.equals(entry.kind)) { continue; }

            paramIdsSymbolTable = parseParamIds(entry.data.get("Param"));

            // Arity?
            if(paramIdsPassed.size() != paramIdsSymbolTable.size()) {
                return false; // Can't match if there are a different number of params
            }

            // Order and type are checked together
            for(int i = 0; i < paramIdsPassed.size(); i++) {
                SymbolTableEntryType paramType1 = innerTable.get(paramIdsPassed.get(i)).kind;
                SymbolTableEntryType paramType2 = innerTable.get(paramIdsSymbolTable.get(i)).kind;

                if(!paramType1.equals(paramType2)) {
                    return false; // Can't match if the param types are different
                }
            }
            return true; // Nothing different about the params
        }
        return false; // No scope adjustment?
    }

    // Gets symids of all params from the string list
    private List<String> parseParamIds(String paramList) {
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
}
