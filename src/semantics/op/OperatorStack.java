package semantics.op;

import log.Log;
import semantics.SemanticActions;
import semantics.err.SemanticsException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/28/14
 * Time: 11:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class OperatorStack {
    private Log opstackLog = null;
//    private Operator tos;
    private List<Operator> stack = new ArrayList<Operator>();

    public OperatorStack(String filename) { opstackLog = new Log(filename); }

    public void push(String new_top) throws SemanticsException {
        Operator op = Operator.get(new_top);
        Operator top = peek();
        if(top != null
                && !"(".equals(new_top) // Don't mess with anything if there is an open paren.
                && !"[".equals(new_top) // Don't mess with anything if there is an open bracket.
                && shouldPopStack(top, op)) {
            opstackLog.debug("Must pop higher precedence operator.");
            SemanticActions.doExpression();
        }
        opstackLog.debug("Pushed new stack object " + op.toString());
        stack.add(0, op);
    }

    private boolean shouldPopStack(Operator topOp, Operator newOp) {
        if("=".equals(topOp.Symbol)) {
            return topOp.Precedence > newOp.Precedence;
        }
        else {
            return topOp.Precedence >= newOp.Precedence;
        }
    }

//    private void doExpression() throws Exception {

//        throw new Exception("Can't do operation " + peek().Symbol + " on " + s1.data + " and " + s2.data);
//    }

    public Operator pop() {
        return stack.isEmpty() ? null : stack.remove(0);
    }

    public Operator peek() {
        return stack.isEmpty() ? null : stack.get(0);
    }

    public void dumpData() {
        StringBuilder s = new StringBuilder("Current stack contents:\n")
                .append("-------------------------------------\n");
        for(Operator op: stack) {
            s.append(op.toString()).append('\n');
        }
        s.append('\n');
        opstackLog.log(s.toString());
    }
}
