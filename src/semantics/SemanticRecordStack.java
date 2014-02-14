package semantics;

import log.Log;
import semantics.record.sar.SemanticActionRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/28/14
 * Time: 11:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class SemanticRecordStack {
    private Log stackLog = null;
//    private SemanticActionRecord tos;
    private List<SemanticActionRecord> stack = new ArrayList<SemanticActionRecord>();

    public SemanticRecordStack() { }
    public SemanticRecordStack(String filename) { stackLog = new Log(filename); }

    public void push(SemanticActionRecord new_top) {
        stackLog.debug("Pushed new stack object " + new_top.toString());
        stack.add(0, new_top);
    }

    public SemanticActionRecord pop() {
        stackLog.debug("Popped stack object " + stack.get(0).toString());
        return stack.remove(0);
    }

    public SemanticActionRecord peek() {
        return stack.get(0);
    }

    public void dumpData() {
        StringBuilder s = new StringBuilder("Current stack contents:\n")
                .append("-------------------------------------\n");
        for(SemanticActionRecord item : stack) {
            s.append(item.toString()).append('\n');
        }
        s.append('\n');
        stackLog.log(s.toString());
    }

    public void closeLogs() {
        stackLog.close();
    }
}
