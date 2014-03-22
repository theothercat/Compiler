package semantics.record.sar;

import syntax.symbol.SymbolTable;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/29/14
 * Time: 4:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class SemanticActionRecord {
    public String data;
    public RecordType type;
    public HashMap<String, String> subRecords;

    public SemanticActionRecord(String d, RecordType r) {
        data = d;
        type = r;
    }

    public SemanticActionRecord(String d, RecordType r, HashMap<String, String> sub) {
        data = d;
        type = r;
        subRecords = sub;
    }

    @Override
    public String toString() {
        boolean isSymId = RecordType.SYMID.equals(type);
        StringBuilder sb = new StringBuilder(isSymId ? SymbolTable.get().get(data).kind.name() : type.name());
        if(isSymId) {
            sb.append(' ')
            .append(SymbolTable.get().get(data).value)
            .append(" of type ")
            .append(SymbolTable.get().get(data).data.get("type"));
        }
        else {
            sb.append(subRecords == null ? (" " + data + " ") : "");
        }
        return sb.append(subRecords == null ? "" : " " + subRecords.get("id"))
                .append(subRecords == null ? "" : " " + subRecords.get("args"))
                .toString();
    }

    public static SemanticActionRecord getRecord(SemanticActionRecord identifier, SemanticActionRecord argList) {
        HashMap<String, String> sub = new HashMap<String, String>(2);
        sub.put("id", identifier.data);
        sub.put("args", argList.data);
        return new SemanticActionRecord(identifier.data, RecordType.FUNC, sub);
    }

    public static SemanticActionRecord getRecord(String d, RecordType r, String indexSymId) {
        HashMap<String, String> sub = new HashMap<String, String>(1);
        sub.put("index", indexSymId);
        return new SemanticActionRecord(d, r, sub);
    }

    public static SemanticActionRecord getRecord(String d, RecordType r) {
//        if(RecordType.SYMID.equals(r)) {
            return new SemanticActionRecord(d, r);
//        }
//        return null;
    }
}
