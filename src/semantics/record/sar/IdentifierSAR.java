package semantics.record.sar;

import semantics.record.RecordType;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 2/3/14
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class IdentifierSAR extends SemanticActionRecord {
    public IdentifierSAR(String d) {
        super(d, RecordType.IDENTIFIER);
    }

    @Override
    public String toString() {
        return new StringBuilder("Identifier ")
                .append(data)
                .toString();
    }
}
