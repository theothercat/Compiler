package semantics.record;

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
}
