package semantics;

import log.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 1/28/14
 * Time: 11:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class Stack<T> {
    private Log stackLog = null;
    private T tos;
    private List<T> stack = new ArrayList<T>();

    public Stack() { }
    public Stack(String filename) { stackLog = new Log(filename); }

    public void push(T new_top) {
        stackLog.debug("Pushed new stack object " + new_top.toString());
        stack.add(0, new_top);
    }

    public T pop() {
        return stack.remove(0);
    }

    public T peek() {
        return stack.get(0);
    }

    public void dumpData() {
        StringBuilder s = new StringBuilder("Current stack contents:\n")
                .append("-------------------------------------\n");
        for(T item : stack) {
            s.append(item.toString()).append('\n');
        }
        s.append('\n');
        stackLog.log(s.toString());
    }

    public void closeLogs() {
        stackLog.close();
    }
}
