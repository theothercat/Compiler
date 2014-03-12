package syntax.symbol;

import lex.Token;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/13/14
 * Time: 5:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class SymbolTableEntry
{
    private static int id = 1;

    public String scope;
    public String symid;
    public String value;
    public SymbolTableEntryType kind;
    public Map<String, String> data;

    public SymbolTableEntry(String scope, String symid, String value, SymbolTableEntryType kind, Map<String, String> data) {
        this.scope = scope;
        this.symid = symid + (id++);
        this.value = value;
        this.kind = kind;
        this.data = data;
    }

    public SymbolTableEntry(String scope, String symid, SymbolTableEntryType kind, Map<String, String> data) {
        this.scope = scope;
        this.symid = symid + (id++);
        this.value = this.symid;
        this.kind = kind;
        this.data = data;
    }

    // Constructor for null/this symid
    public SymbolTableEntry(String scope, String symid, String value, Map<String, String> data) {
        this.scope = scope;
        this.symid = symid;
        this.value = value;
        this.kind = SymbolTableEntryType.GLOBAL_LITERAL;
        this.data = data;
    }
}
