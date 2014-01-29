package syntax.symbol;

import log.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    private static Map<String, SymbolTableEntry> innerTable = new HashMap<String, SymbolTableEntry>();
    private static String scope = "g";

    private SymbolTable() { }

    public static SymbolTable get() {
        return theInstance;
    }

    public static boolean identifierExists(String identifier) {
        return iExists(identifier, scope);
    }

    public static boolean identifierExists(String identifier, String className) {
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
    public static String find(String identifier) {
        return iFinder(identifier, scope);
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
    private static String iFinder(String identifier, String scope) {
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

    public SymbolTableEntry add(String lexeme, SymbolTableEntryType type, Map<String, String> data) {
        SymbolTableEntry newEntry = new SymbolTableEntry(scope, type.name().substring(0, 1), lexeme, type, data);
        innerTable.put(newEntry.symid, newEntry);
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

    public String getScope() { return scope; }

    public void setScope(String newScope) {
        scope = newScope;
    }

    public void closeLogs() {
        symLog.close();
    }
}
