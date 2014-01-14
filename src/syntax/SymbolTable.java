package syntax;

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
    private Map<String, SymbolTableEntry> innerTable = new HashMap<String, SymbolTableEntry>();

    public SymbolTable() { }

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
}
