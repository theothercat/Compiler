package icode;

import icode.quad.Quad;
import log.Log;
import syntax.symbol.SymbolTableEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 2/18/14
 * Time: 2:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class ICodeGenerator {
    private static final String STATIC_INIT = "STATIC_INIT_PLACEHOLDER";

    public static Log quadFile = new Log("quad.icode"); // The actual quads

    public static List<Quad> staticInitQuads = new ArrayList<Quad>();
    public static List<Quad> quads = new ArrayList<Quad>();

    private static boolean doStaticInit;

    public static void activateStaticInit() {
        doStaticInit = true;
    }

    public static void deactivateStaticInit() {
        doStaticInit = false;
    }

    public static void prepareStaticInit() {
        staticInitQuads = new ArrayList<Quad>();
    }

    public static void doStaticInitPlaceholder() {
        quads.add(new Quad(STATIC_INIT, STATIC_INIT, STATIC_INIT, STATIC_INIT, STATIC_INIT)); // Add placeholder for static init
    }

    private static List<Quad> getActiveQuadsList() {
        return doStaticInit ? staticInitQuads : quads;
    }

    public static void addQuad(String op, String o1, String o2, SymbolTableEntry o3) {
        Quad q = new Quad(op, o1, o2, o3);
        checkLabelAndAddQuad(q, getActiveQuadsList());
    }

    public static void addQuad(Quad q) {
        checkLabelAndAddQuad(q, getActiveQuadsList());
    }

    public static void addQuad(String op, String o1, String o2, String o3) {
        Quad q = new Quad(op, o1, o2, o3);
        checkLabelAndAddQuad(q, getActiveQuadsList());
    }

    public static void addQuad(String op, SymbolTableEntry o1, SymbolTableEntry o2, SymbolTableEntry o3) {
        Quad q = new Quad(op, o1, o2, o3);
        checkLabelAndAddQuad(q, getActiveQuadsList());
    }

    public static void addQuad(String op, String o1, SymbolTableEntry o2, SymbolTableEntry o3) {
        Quad q = new Quad(op, o1, o2, o3);
        checkLabelAndAddQuad(q, getActiveQuadsList());
    }

    public static void addQuad(String op, SymbolTableEntry o1, String o2, SymbolTableEntry o3) {
        Quad q = new Quad(op, o1, o2, o3);
        checkLabelAndAddQuad(q, getActiveQuadsList());
    }

    public static void addQuad(String op, SymbolTableEntry o1, String o2, String o3, String label) {
        Quad q = new Quad(op, o1, o2, o3, label);
        checkLabelAndAddQuad(q, getActiveQuadsList());
    }

    public static void addQuad(String label) {
        Quad q = new Quad(label);
        checkLabelAndAddQuad(q, getActiveQuadsList());
    }

    private static void checkLabelAndAddQuad(Quad q, List<Quad> quads) {
        if(quads.size() < 1) {
            quads.add(q);
            return;
        }

        Quad lastQuad = quads.get(quads.size() - 1);
        if(lastQuad.operator == null) // This is label-only
        {
            if(q.label == null) {
                lastQuad.fillData(q);
            }
            else {
                lastQuad.fillData(q);
                backPatch(lastQuad.label, q.label);
            }
        }
        else {
            quads.add(q);
        }
    }

    private static void backPatch(String oldLabel, String newLabel) {
        Quad current;
        for(int i = quads.size() - 1; i >= 0; i--) {
            current = quads.get(i);
            if(oldLabel.equals(current.label)) {
                current.label = newLabel;
            }
            if(oldLabel.equals(current.operand1)) {
                current.operand1 = newLabel;
            }
            if(oldLabel.equals(current.operand2)) {
                current.operand1 = newLabel;
            }
            if(oldLabel.equals(current.operand3)) {
                current.operand1 = newLabel;
            }
            if(oldLabel.equals(current.comment)) {
                current.comment = newLabel;
            }
        }
    }

    public static void replaceStaticInit() {
        Quad current;

        List<Quad> copy = new ArrayList<Quad>();

        for(int i = 0; i < quads.size(); i++) {
            current = quads.get(i);
            if(STATIC_INIT.equals(current.label)) {
                for(int j = 0; j < staticInitQuads.size(); j++) {
                    copy.add(staticInitQuads.get(j));
                }
            }
            else {
                copy.add(current);
            }
        }
        quads = copy;
    }

    public static void dumpQuads() {
        for(Quad q : quads) {
            quadFile.log(q.toString());
        }
    }
}
