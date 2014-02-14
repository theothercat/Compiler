package semantics.record.sar;

import semantics.record.RecordType;

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

    public SemanticActionRecord(String d, RecordType r) {
        data = d;
        type = r;
    }

    @Override
    public String toString() {
        return new StringBuilder(type.name())
                .append(" ")
                .append(data)
                .toString();
    }

    public static SemanticActionRecord getRecord(String d, RecordType r) {
        if(RecordType.IDENTIFIER.equals(r)) {
            return new IdentifierSAR(d);
        }
//        if(RecordType.SYMID.equals(r)) {
            return new SemanticActionRecord(d, r);
//        }
//        return null;
    }
}
