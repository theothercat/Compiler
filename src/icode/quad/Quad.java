package icode.quad;

import log.Log;
import syntax.symbol.SymbolTableEntry;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 2/18/14
 * Time: 2:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class Quad {
    public String label = null;
    public String operator;
    public String operand1; // Source operand 1. In REF, the object will be here.
    public String operand2; // Source operand 2. Operations like MOV will have a null operand here.  For ADI, the immediate value will be here. In REF, the member will be here.
    public String operand3; // This is where the result should be stored.
    public String comment;
    public boolean tcode = false;

    public Quad(String op, String o1, String o2, String o3) {
        operator = op;
        operand1 = o1;
        operand2 = o2;
        operand3 = o3;

        comment = operator
                + (operand1 == null ? "" : (" " + operand1))
                + (operand2 == null ? "" : (" " + operand2))
                + (operand3 == null ? "" : (" " + operand3));
    }

    public Quad(String op, String o1, String o2, String o3, String label) {
        operator = op;
        operand1 = o1;
        operand2 = o2;
        operand3 = o3;
        this.label = label;

        comment = operator
                + (operand1 == null ? "" : (" " + operand1))
                + (operand2 == null ? "" : (" " + operand2))
                + (operand3 == null ? "" : (" " + operand3));

    }

    public Quad(String op, String o1, String o2, String o3, String label, String comment) {
        operator = op;
        operand1 = o1;
        operand2 = o2;
        operand3 = o3;
        this.label = label;
        this.comment = comment;

        tcode = true; // This is used for tcode quads.
    }


    public Quad(String op, SymbolTableEntry o1, String o2, String o3, String label) {
        operator = op;
        operand1 = o1 == null ? null : o1.symid;
        operand2 = o2;
        operand3 = o3;
        this.label = label;

        comment = operator
                + (o1 == null ? "" : (" " + o1.value))
                + (o2 == null ? "" : (" " + o2))
                + (o3 == null ? "" : (" " + o3));

    }

    public Quad(String op, SymbolTableEntry o1, SymbolTableEntry o2, SymbolTableEntry o3) {
        operator = op;
        operand1 = o1 == null ? null : o1.symid;
        operand2 = o2 == null ? null : o2.symid;
        operand3 = o3 == null ? null : o3.symid;

        comment = operator
                + (o1 == null ? "" : (" " + o1.value))
                + (o2 == null ? "" : (" " + o2.value))
                + (o3 == null ? "" : (" " + o3.value));

    }

    public Quad(String op, String o1, SymbolTableEntry o2, SymbolTableEntry o3) {
        operator = op;
        operand1 = o1;
        operand2 = o2 == null ? null : o2.symid;
        operand3 = o3 == null ? null : o3.symid;

        comment = operator
                + (o1 == null ? "" : (" " + o1))
                + (o2 == null ? "" : (" " + o2.value))
                + (o3 == null ? "" : (" " + o3.value));

    }

    // For read/write
    public Quad(String op, SymbolTableEntry o1, String o2, SymbolTableEntry o3) {
        operator = op;
        operand1 = o1 == null ? null : o1.symid;
        operand2 = o2;
        operand3 = o3 == null ? null : o3.symid;

        comment = operator
                + (o1 == null ? "" : (" " + o1.value))
                + (o2 == null ? "" : (" " + o2))
                + (o3 == null ? "" : (" " + o3.value));

    }

    // For read/write
    public Quad(String op, String o1, String o2, SymbolTableEntry o3) {
        operator = op;
        operand1 = o1;
        operand2 = o2;
        operand3 = o3 == null ? null : o3.symid;

        comment = operator
                + (o1 == null ? "" : (" " + o1))
                + (o2 == null ? "" : (" " + o2))
                + (o3 == null ? "" : (" " + o3.value));

    }

    public Quad(String label) {
        operator = null;
        operand1 = null;
        operand2 = null;
        operand3 = null;
        comment = "Empty label";
        this.label = label;
    }

    public void fillData(Quad q) {
        operator = q.operator;
        operand1 = q.operand1;
        operand2 = q.operand2;
        operand3 = q.operand3;
        comment = q.comment;
        // Keep the same label.
    }

    @Override
    public String toString() {
        return (label == null ? "" : (tcode ? (label + " ") : (label + ": ")))
                + operator
                + (operand1 == null ? "" : (" " + operand1))
                + (operand2 == null ? "" : (" " + operand2))
                + (operand3 == null ? "" : (" " + operand3))
                + (comment == null ? "" : " ; " + comment);
    }
}
