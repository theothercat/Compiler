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
    private static Log symLog = null;

    private Map<String, SymbolTableEntry> innerTable = new HashMap<String, SymbolTableEntry>();
    private String scope = "g";

    public SymbolTable() {
        if(symLog == null) {
            symLog = new Log("symbol_table.log");
        }
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

    public void setScope(String newScope) { scope = newScope; }
}
