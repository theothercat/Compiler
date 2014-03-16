package tcode;

import icode.ICodeGenerator;
import icode.quad.Quad;
import log.Log;
import syntax.symbol.SymbolTable;
import syntax.symbol.SymbolTableEntry;
import syntax.symbol.SymbolTableEntryType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 2/26/14
 * Time: 3:56 PM
 * To change this template use File | Settings | File Templates.
 */
public final class TCodeGenerator {
    public static final int VAL_REGISTER_COUNT = 6;

    public static List<Quad> finalQuads = new ArrayList<Quad>();

    public static final String TOS = "R6";
    public static final String FP = "R7";
    public static final String SL = "R8";
    public static final String SB = "R9";
    public static final String PC = "R10";

    public static Log targetCodeFile = new Log("tcode.asm");
    private static SymbolTable symbolTable = SymbolTable.get();

    private static int logicLabelCounter = 1;

    private static String getLogicLabel() {
        return "LL" + logicLabelCounter++;
    }

    private static void putNumberInRegister(String register, int number) {
        checkLabelAndAddQuad(new Quad("SUB", register, register, null, null, "Zero out " + register));
        if(number != 0) {
            checkLabelAndAddQuad(new Quad("ADI", register, String.valueOf(number), null, null, "Put " + number + " in " + register));
        }
    }

    private static String indirect(String register) {
        return String.format("(%s)", register);
    }

    /**
     *
     * @param q
     */
    private static void frame(Quad q) {
        String comment = q == null ? "FRAME main - " : (q.comment + " - ");
        int frameSize = -12;
        if(q != null) {
            frameSize -= symbolTable.getFuncSize(q.operand1);
        }
        else {
            frameSize -= symbolTable.getFuncSize(symbolTable.findInScope("main", "g"));
        }

        checkLabelAndAddQuad(new Quad("MOV", "R1", TOS, null, null, comment + "Copy TOS into R1"));
        checkLabelAndAddQuad(new Quad("ADI", "R1", String.valueOf(frameSize), null, null, comment + "Space for RTN addr, PFP, this, and locals?"));
        checkLabelAndAddQuad(new Quad("CMP", "R1", SL, null, null, comment + "Compare TOS and SL"));
        checkLabelAndAddQuad(new Quad("BLT", "R1", "OVERFLOW", null, null, comment + "Overflow check"));

        checkLabelAndAddQuad(new Quad("MOV", "R1", FP, null, null, comment + "Save FP in R1, this will be the PFP"));
        checkLabelAndAddQuad(new Quad("MOV", FP, TOS, null, null, comment + "Set FP to Current Activation Record (FP = TOS)"));
        checkLabelAndAddQuad(new Quad("ADI", TOS, "-4", null, null, comment + "Move TOS to where PFP should be stored"));
        checkLabelAndAddQuad(new Quad("STR", "R1", indirect(TOS), null, null, comment  + "Put PFP on the Activation Record (PFP = FP)"));
        checkLabelAndAddQuad(new Quad("ADI", TOS, "-4", null, null, comment + "Move TOS to this*"));


        // Set the this*
        if(q == null) {
            // Set the this* for main
            checkLabelAndAddQuad(new Quad("LDA", "R5", "FREE", null, null, comment + "Where is the FREE label?")); // This tells you where the next free space is.
            checkLabelAndAddQuad(new Quad("ADI", "R5", "4", null, null, comment + "Increment by size of int to get where the actual free space is"));
            checkLabelAndAddQuad(new Quad("STR", "R5", "FREE", null, null, comment + "Put the actual address of the FREE space at FREE"));
            checkLabelAndAddQuad(new Quad("STR", "R5", indirect(TOS), null, null, comment + "Store the this* on the stack"));
        }
        else {
            // Set the this* for other functions. Expects this* in R2, loaded before this function was called.
            checkLabelAndAddQuad(new Quad("STR", "R2", indirect(TOS), null, null, comment + "Store the this* on the stack"));
        }
        checkLabelAndAddQuad(new Quad("ADI", TOS, "-4", null, null, comment + "Move TOS to new top"));
    }

    private static void call(Quad q) {
        String function;
        if(q == null) {
            function = symbolTable.findInScope("main", "g");
        }
        else {
            function = q.operand1;
        }
        // Do return address
        checkLabelAndAddQuad(new Quad("MOV", "R1", PC, null, null, "PC incremented by 1 instruction"));
        checkLabelAndAddQuad(new Quad("ADI", "R1", "18", null, null, "Compute return address"));
        checkLabelAndAddQuad(new Quad("STR", "R1", indirect(FP), null, null, "Compute return address"));
        checkLabelAndAddQuad(new Quad("JMP", function, null, null, null, "Call function"));
    }

    private static void doReturn(Quad q) {
        if(q.operand1 != null) {
            loadDataInRegister("R1", q.operand1, q.comment, true);
        }

        checkLabelAndAddQuad(new Quad("MOV", TOS, FP, null, null, "De-allocate current activation record (TOS = FP)"));
        checkLabelAndAddQuad(new Quad("MOV", "R2", TOS, null, null, "Copy TOS"));
        checkLabelAndAddQuad(new Quad("CMP", "R2", SB, null, null, "Compare TOS and SB"));
        checkLabelAndAddQuad(new Quad("BGT", "R2", "UNDERFLOW", null, null, "Underflow check"));

        checkLabelAndAddQuad(new Quad("LDR", "R2", indirect(FP), null, null, "Return address pointed to by FP"));
        checkLabelAndAddQuad(new Quad("MOV", "R3", FP, null, null, "Point at PFP in Activation Record"));
        checkLabelAndAddQuad(new Quad("ADI", "R3", "-4", null, null, "Point at PFP in Activation Record"));
        checkLabelAndAddQuad(new Quad("LDR", FP, indirect("R3"), null, null, "FP = PFP"));
        if(q.operand1 != null) // Has a return value
        {
            checkLabelAndAddQuad(new Quad("STR", "R1", indirect(TOS), null, null, "Put return value on the stack"));
        }
        checkLabelAndAddQuad(new Quad("JMR", "R2", null, null, null, "Return from function call"));
    }

    private static void doOverflowUnderflow() {
        String overflowString = "Stack overflow";
        String underflowString = "Stack underflow";

        checkLabelAndAddQuad(new Quad("LDA", "R1", "OVERFLOW_S", null, "OVERFLOW", "Print overflow message and quit"));
        for(int i = 0; i < overflowString.length(); i++) {
            checkLabelAndAddQuad(new Quad("LDB", "R0", indirect("R1"), null, null, null));
            checkLabelAndAddQuad(new Quad("TRP", "3", null, null, null, null));
            checkLabelAndAddQuad(new Quad("ADI", "R1", "1", null, null, null));

            if(i == overflowString.length()) {
                break;
            }
        }
        checkLabelAndAddQuad(new Quad("LDB", "R0", indirect("R1"), null, null, null)); // Do a newline character (not shown in the above string)
        checkLabelAndAddQuad(new Quad("TRP", "3", null, null, null, null));
        checkLabelAndAddQuad(new Quad("TRP", "0", null, null, null, "Overflow terminate"));

        checkLabelAndAddQuad(new Quad("LDA", "R1", "UNDERFLOW_S", null, "UNDERFLOW", "Print underflow message and quit"));
        for(int i = 0; i < underflowString.length(); i++) {
            checkLabelAndAddQuad(new Quad("LDB", "R0", indirect("R1"), null, null, null));
            checkLabelAndAddQuad(new Quad("TRP", "3", null, null, null, null));
            checkLabelAndAddQuad(new Quad("ADI", "R1", "1", null, null, null));

            if(i == underflowString.length()) {
                break;
            }
        }
        checkLabelAndAddQuad(new Quad("LDB", "R0", indirect("R1"), null, null, null)); // Do a newline character (not shown in the above string)
        checkLabelAndAddQuad(new Quad("TRP", "3", null, null, null, null));
        checkLabelAndAddQuad(new Quad("TRP", "0", null, null, null, "Underflow terminate"));

        // Do message string globals.
        checkLabelAndAddQuad(new Quad(".BYT", String.format("'%s'", overflowString.charAt(0)), null, null, "OVERFLOW_S", "Overflow string"));
        for(int i = 1; i < overflowString.length(); i++) {
            checkLabelAndAddQuad(new Quad(".BYT", String.format("'%s'", overflowString.charAt(i)), null, null, null, null));
        }
        checkLabelAndAddQuad(new Quad(".BYT", "'\\n'", null, null, null, null));

        checkLabelAndAddQuad(new Quad(".BYT", String.format("'%s'", underflowString.charAt(0)), null, null, "UNDERFLOW_S", "Underflow string"));
        for(int i = 0; i < underflowString.length(); i++) {
            checkLabelAndAddQuad(new Quad(".BYT", String.format("'%s'", underflowString.charAt(i)), null, null, null, null));
        }
        checkLabelAndAddQuad(new Quad(".BYT", "'\\n'", null, null, null, null));
    }

    private static void doGlobals() {
        String operand;
        for(SymbolTableEntry entry : symbolTable.getGlobalSymbols()) {
//            if(SymbolTable.THIS_PLACEHOLDER.equals(entry)) { continue; }

            if("null".equals(entry.data.get("type"))) {
                checkLabelAndAddQuad(new Quad(".INT", "0", null, null, entry.symid, "Global data"));
            }
            else if("bool".equals(entry.data.get("type"))) // true and false globals
            {
                if("true".equals(entry.value)) {
                    checkLabelAndAddQuad(new Quad(".BYT", "0", null, null, entry.symid, "Global true"));
                }
                else // if("false".equals(entry.value))
                {
                    checkLabelAndAddQuad(new Quad(".BYT", "1", null, null, entry.symid, "Global false"));
                }

            }
            else {
                if("char".equals(entry.data.get("type")))
                {
                    operand = ".BYT";
                }
                else {
                    operand = ".INT";
                }
                checkLabelAndAddQuad(new Quad(operand, entry.value, null, null, entry.symid, "Global data"));
            }
            checkLabelAndAddQuad(new Quad(".BYT", "'!'", null, null, "EXCLAMATION", "debug")); // todo: remove debug code
            checkLabelAndAddQuad(new Quad(".BYT", "'S'", null, null, "S_IS_FOR_STACK", "debug")); // todo: remove debug code
            checkLabelAndAddQuad(new Quad(".BYT", "'H'", null, null, "H_IS_FOR_HEAP", "debug")); // todo: remove debug code
            checkLabelAndAddQuad(new Quad(".BYT", "'^'", null, null, "CARROT", "debug")); // todo: remove debug code

            checkLabelAndAddQuad(new Quad(".BYT", "'-'", null, null, "HYPHEN", "debug")); // todo: remove debug code
            checkLabelAndAddQuad(new Quad(".BYT", "' '", null, null, "SPACE", "debug")); // todo: remove debug code
            checkLabelAndAddQuad(new Quad(".BYT", "'\\n'", null, null, "NEWLINE", "debug")); // todo: remove debug code
            checkLabelAndAddQuad(new Quad(".BYT", "'~'", null, null, "TILDE", "debug")); // todo: remove debug code

            checkLabelAndAddQuad(new Quad(".BYT", "'A'", null, null, "AEF1", "debug")); // todo: remove debug code
            checkLabelAndAddQuad(new Quad(".BYT", "'E'", null, null, "AEF2", "debug")); // todo: remove debug code
            checkLabelAndAddQuad(new Quad(".BYT", "'F'", null, null, "AEF3", "debug")); // todo: remove debug code
            checkLabelAndAddQuad(new Quad(".BYT", "'N'", null, null, "NEWI1", "debug")); // todo: remove debug code
            checkLabelAndAddQuad(new Quad(".BYT", "'E'", null, null, "NEWI2", "debug")); // todo: remove debug code
            checkLabelAndAddQuad(new Quad(".BYT", "'W'", null, null, "NEWI3", "debug")); // todo: remove debug code
            checkLabelAndAddQuad(new Quad(".BYT", "'I'", null, null, "NEWI4", "debug")); // todo: remove debug code
        }
    }

    public static void produceTargetCode() {
        // Do handling of null*
        if(symbolTable.findInScope("null", "g") != null) {
            checkLabelAndAddQuad(new Quad("LDA", "R0", SymbolTable.NULL_SYMID, null, null, "Get address of null*"));
            checkLabelAndAddQuad(new Quad("STR", "R0", SymbolTable.NULL_SYMID, null, null, "Point null* at itself"));
        }

        // Do main
        frame(null);
        call(null);
        checkLabelAndAddQuad(new Quad("TRP", "0", null, null, null, "Return from main - quit program"));

        for(Quad q : ICodeGenerator.quads)
        {
            try {
                handleQuad(q);
            }
            catch(Exception e) {
                System.out.println("Exception in quad " + q.toString() + ": " + e.toString());
                return;
            }
        }
        doOverflowUnderflow();
        doGlobals();
        checkLabelAndAddQuad(new Quad(".INT", "0", null, null, "FREE", "The this*, will be set at the start"));
    }

    private static void handleQuad(Quad q) {
        if(q.label != null) {
            checkLabelAndAddQuad(new Quad(null, null, null, null, q.label, q.comment));
        }

        if("FUNC".equals(q.operator))
        {
            int tempVars = symbolTable.getTotalTempVars(q.operand1); // operand 1 should be the func symid
            checkLabelAndAddQuad(new Quad("ADI", TOS, String.valueOf(-4 * tempVars), null, null, q.comment));
            checkLabelAndAddQuad(new Quad("MOV", "R4", TOS, null, null, q.comment));
            checkLabelAndAddQuad(new Quad("CMP", "R4", SL, null, null, q.comment));
            checkLabelAndAddQuad(new Quad("BLT", "R4", "OVERFLOW", null, null, q.comment));
        }
        else if("MOV".equals(q.operator))
        {
            loadDataInRegister("R1", q.operand1, q.comment, true);
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment)); // R1 -> R3
            storeDataInRegister("R3", q.operand3);
        }
        else if("ADD".equals(q.operator)
                || "SUB".equals(q.operator)
                || "MUL".equals(q.operator)
                || "DIV".equals(q.operator))
        {
            loadData(q, true);
            checkLabelAndAddQuad(new Quad(q.operator, "R1", "R2", null, null, q.comment + " - Do op")); // R1 op R2 -> R1
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment + " - Copy result to R3")); // R1 -> R3
            storeData(q);
        }
        else if("MULI".equals(q.operator)) {
            loadDataInRegister("R1", q.operand1, q.comment, true);
            putNumberInRegister("R2", Integer.parseInt(q.operand2));
            checkLabelAndAddQuad(new Quad("MUL", "R1", "R2", null, null, q.comment + " - Multiply immediate " + q.operand2)); // R1 op R2 -> R1
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment + " - Multiply immediate " + q.operand2)); // R1 op R2 -> R1
            storeData(q);
        }
        else if("ADI".equals(q.operator))
        {
            loadData(q, true);
            checkLabelAndAddQuad(new Quad("ADI", "R1", q.operand2, null, null, q.comment + " - Add immediate " + q.operand2)); // R1 op R2 -> R1
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment + " - Copy result to R3")); // R1 -> R3
            storeData(q);
        }
        else if("WRITE".equals(q.operator)) {
            loadDataInRegister("R0", q.operand1, q.comment, true);
            checkLabelAndAddQuad(new Quad("TRP", q.operand2, null, null, null, q.comment + " - Trap")); // operand2 is trap number
        }
        else if("RTN".equals(q.operator)) {
            doReturn(q);
        }
        else if("NEWI".equals(q.operator)) {
            checkLabelAndAddQuad(new Quad("LDR", "R3", "FREE", null, null, q.comment + " - Get address of free space"));

            checkLabelAndAddQuad(new Quad("MOV", "R2", "R3", null, null, q.comment + " - Copy address of free"));
            checkLabelAndAddQuad(new Quad("ADI", "R2", q.operand1, null, null, q.comment + " - Increment this*"));

            checkLabelAndAddQuad(new Quad("STR", "R2", "FREE", null, null, q.comment + " - Save incremented this*"));

            putAddressToStoreNewInRegister("R1", symbolTable.get(q.operand3), true);
            checkLabelAndAddQuad(new Quad("STR", "R3", indirect("R1"), null, null, q.comment + " - Put the object's heap address where it belongs "));
        }
        else if("NEW".equals(q.operator)) {
            loadDataInRegister("R1", q.operand1, q.comment, true); // Get computed array size in R1
            checkLabelAndAddQuad(new Quad("LDR", "R3", "FREE", null, null, q.comment + " - Get this*"));
            checkLabelAndAddQuad(new Quad("ADD", "R1", "R3", null, null, q.comment + " - FREE + sizeof(array)"));

            checkLabelAndAddQuad(new Quad("STR", "R1", "FREE", null, null, q.comment + " - Save incremented FREE"));
            putAddressToStoreNewArrayInRegister("R1", symbolTable.get(q.operand3), true); // Get the address that should hold the base heap address

            checkLabelAndAddQuad(new Quad("STR", "R3", indirect("R1"), null, null, q.comment + " - Put the heap address where it belongs "));

        }
        else if("AEF".equals(q.operator)) {
            putAddressToStoreNewArrayInRegister("R1", symbolTable.get(q.operand1), true); // Get the address that holds the base heap address
            checkLabelAndAddQuad(new Quad("LDR", "R1", indirect("R1"), null, null, q.comment + " - Load the heap address of the array"));

            loadDataInRegister("R2", q.operand2, q.comment, true); // Load offset - this will be an integer

            putNumberInRegister("R3", symbolTable.getSize(symbolTable.get(q.operand1).data.get("type"))); // Get size of data type
            checkLabelAndAddQuad(new Quad("MUL", "R3", "R2", null, null, q.comment + " - array itemsize * offset"));
            checkLabelAndAddQuad(new Quad("ADD", "R3", "R1", null, null, q.comment + " - Get heap address of item in array"));



            putAddressToStoreRefsInRegister("R1", symbolTable.get(q.operand3), true);
            checkLabelAndAddQuad(new Quad("STR", "R3", indirect("R1"), null, null, q.comment + " - Store heap address to stack location of array_item temp var"));
        }
        else if("REF".equals(q.operator)) {
            putHeapBaseAddressInRegister("R1", q.operand1, true); // Load base address - this will be an array.

            checkLabelAndAddQuad(new Quad("ADI", "R1", null, String.valueOf(symbolTable.getOffset(symbolTable.get(q.operand2))), null, q.comment + " - Add offset to base address"));

            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment));

            putAddressToStoreRefsInRegister("R1", symbolTable.get(q.operand3), true); // Put stack address of q.operand3 into R1
            checkLabelAndAddQuad(new Quad("STR", "R3", indirect("R1"), null, null, "Store heap address for ref"));
        }
        else if("FRAME".equals(q.operator)) {
            if("this".equals(q.operand2)) {
                putThisPointerInRegister("R2", true);
            }
            else {
                putHeapBaseAddressInRegister("R2", q.operand2, true);
            }
//            debug("R2", "HYPHEN", true, true);

            frame(q);
        }
        else if("PUSH".equals(q.operator)) {
            loadDataInRegister("R1", q.operand1, q.comment, false);

            checkLabelAndAddQuad(new Quad("STR", "R1", indirect(TOS), null, null, "Put var on top of stack"));
            checkLabelAndAddQuad(new Quad("ADI", TOS, "-4", null, null, "Move TOS"));
        }
        else if("CALL".equals(q.operator)) {
            call(q);
        }
        else if("PEEK".equals(q.operator)) {
            checkLabelAndAddQuad(new Quad("LDR", "R3", indirect(TOS), null, null, q.comment));
            storeDataInRegister("R3", q.operand3);
        }
        else if("EQ".equals(q.operator)) {
            loadData(q, true);
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment));
            checkLabelAndAddQuad(new Quad("CMP", "R3", "R2", null, null, q.comment));

            storeData(q); // Store 0 or 1
        }
        else if("NE".equals(q.operator)) {
            loadData(q, true);
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment));
            checkLabelAndAddQuad(new Quad("CMP", "R3", "R2", null, null, q.comment));

            String logicLabelEnd = getLogicLabel();
            String logicLabelTrue = getLogicLabel();
            checkLabelAndAddQuad(new Quad("BNZ", "R3", logicLabelTrue, null, null, q.comment));

            // Handle false
            checkLabelAndAddQuad(new Quad("ADI", "R3", "1", null, null, q.comment)); // Turn a zero into a 1.
            checkLabelAndAddQuad(new Quad("JMP", logicLabelEnd, null, null, null, q.comment));

            // Handle true
            checkLabelAndAddQuad(new Quad(null, null, null, null, logicLabelTrue, q.comment)); // Label to jump to if it really is less than.
            putNumberInRegister("R3", 0); // Guaranteed zero.

            // Handle true/end (same since true already puts a 0 in R3)
            checkLabelAndAddQuad(new Quad(null, null, null, null, logicLabelEnd, q.comment)); // Label for the next thing
            storeData(q); // Store 0 or 1
        }
        else if("LT".equals(q.operator)) {
            loadData(q, true);
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment));
            checkLabelAndAddQuad(new Quad("CMP", "R3", "R2", null, null, q.comment));

            String logicLabelTrue = getLogicLabel();
            String logicLabelEnd = getLogicLabel();
            checkLabelAndAddQuad(new Quad("BLT", "R3", logicLabelTrue, null, null, q.comment));

            // Handle false
            checkLabelAndAddQuad(new Quad("ADI", "R3", "1", null, null, q.comment)); // We already dropped out if it's negative. If it's 0 or higher, it's now guaranteed positive.
            checkLabelAndAddQuad(new Quad("JMP", logicLabelEnd, null, null, null, q.comment));

            // Handle true
            checkLabelAndAddQuad(new Quad(null, null, null, null, logicLabelTrue, q.comment)); // Label to jump to if it really is less than.
            putNumberInRegister("R3", 0);
            checkLabelAndAddQuad(new Quad("JMP", logicLabelEnd, null, null, null, q.comment));

            // Store the value
            checkLabelAndAddQuad(new Quad(null, null, null, null, logicLabelEnd, q.comment)); // Label for the next thing
            storeData(q); // Store 0 or 1
        }
        else if("GT".equals(q.operator)) {
            loadData(q, true);
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment));
            checkLabelAndAddQuad(new Quad("CMP", "R3", "R2", null, null, q.comment));

            String logicLabelTrue = getLogicLabel();
            String logicLabelEnd = getLogicLabel();
            checkLabelAndAddQuad(new Quad("BGT", "R3", logicLabelTrue, null, null, q.comment));

            // Handle false
            checkLabelAndAddQuad(new Quad("ADI", "R3", "-1", null, null, q.comment)); // We already dropped out if it's positive. If it's 0 or lower, it's now guaranteed negative.
            checkLabelAndAddQuad(new Quad("JMP", logicLabelEnd, null, null, null, q.comment));

            // Handle true
            checkLabelAndAddQuad(new Quad(null, null, null, null, logicLabelTrue, q.comment)); // Label to jump to if it really is less than.
            putNumberInRegister("R3", 0);
            checkLabelAndAddQuad(new Quad("JMP", logicLabelEnd, null, null, null, q.comment));

            // Store the value
            checkLabelAndAddQuad(new Quad(null, null, null, null, logicLabelEnd, q.comment)); // Label for the next thing
            storeData(q); // Store 0 or 1
        }
        else if("LE".equals(q.operator)) {
            loadData(q, true);
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment));
            checkLabelAndAddQuad(new Quad("CMP", "R3", "R2", null, null, q.comment));

            String logicLabelEnd = getLogicLabel();
            checkLabelAndAddQuad(new Quad("BGT", "R3", logicLabelEnd, null, null, q.comment));
            checkLabelAndAddQuad(new Quad("BRZ", "R3", logicLabelEnd, null, null, q.comment));

            // Handle less than (true)
            putNumberInRegister("R3", 0);

            // Store the value
            checkLabelAndAddQuad(new Quad(null, null, null, null, logicLabelEnd, q.comment)); // Label for the next thing?
            storeData(q); // Store 0 or 1
        }
        else if("GE".equals(q.operator)) {
            loadData(q, true);
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment));
            checkLabelAndAddQuad(new Quad("CMP", "R3", "R2", null, null, q.comment));

            String logicLabelEnd = getLogicLabel();
            checkLabelAndAddQuad(new Quad("BLT", "R3", logicLabelEnd, null, null, q.comment));
            checkLabelAndAddQuad(new Quad("BRZ", "R3", logicLabelEnd, null, null, q.comment));

            // Handle greater than (true)
            putNumberInRegister("R3", 0);

            // Store the value
            checkLabelAndAddQuad(new Quad(null, null, null, null, logicLabelEnd, q.comment)); // Label for the next thing
            storeData(q); // Store 0 or 1
        }
        else if("AND".equals(q.operator)) {
            loadData(q, true);
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment));
            checkLabelAndAddQuad(new Quad("AND", "R3", "R2", null, null, q.comment));
            storeData(q); // Store 0 or 1
        }
        else if("OR".equals(q.operator)) {
            loadData(q, true);
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment));
            checkLabelAndAddQuad(new Quad("OR", "R3", "R2", null, null, q.comment));
            storeData(q); // Store 0 or 1
        }
        else if("BF".equals(q.operator)) {
            loadData(q, true);
            checkLabelAndAddQuad(new Quad("BNZ", "R1", q.operand2, null, null, q.comment));
        }
        else if("BT".equals(q.operator)) {
            loadData(q, true);
            checkLabelAndAddQuad(new Quad("BRZ", "R1", q.operand2, null, null, q.comment));
        }
        else if("JMP".equals(q.operator)) {
            checkLabelAndAddQuad(new Quad("JMP", q.operand1, null, null, null, q.comment));
        }
        else if("READ".equals(q.operator)) {
            SymbolTableEntry entry = symbolTable.get(q.operand3);
            String trpNumber = "int".equals(entry.data.get("type"))
                    ? "2"
                    : "4";

            checkLabelAndAddQuad(new Quad("TRP", trpNumber, null, null, null, q.comment));
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R0", null, null, q.comment));
            storeData(q);
        }
    }

    /**
     * Puts the first source op in R2
     * Puts the second source op in R3, if applicable
     * @param q quad to load data for
     */
    private static void loadData(Quad q, boolean isCurrentFrame) {

        boolean skipOperand1 = q.operand1 == null || "READ".equals(q.operator) || "FRAME".equals(q.operator)
                || "JMP".equals(q.operator);
        boolean skipOperand2 = q.operand2 == null || "WRITE".equals(q.operator) || "ADI".equals(q.operator)
                || "BF".equals(q.operator) || "BT".equals(q.operator)
                || "REF".equals(q.operator)
                || "NEW".equals(q.operator)
                || "MULI".equals(q.operator);

        if(!skipOperand1) {
            loadDataInRegister("R1", q.operand1, q.comment, isCurrentFrame);
        }
        if(!skipOperand2) {
            loadDataInRegister("R2", q.operand2, q.comment, isCurrentFrame);
        }
    }

    private static void frameThisInRegister(String putOperandAddressInThisRegister, String operand, boolean isCurrentFrame) {
        SymbolTableEntry entry = symbolTable.get(operand);

        if(SymbolTableEntryType.INSTANCE_VAR.equals(entry.kind)) {
            putThisPointerInRegister(putOperandAddressInThisRegister, isCurrentFrame);
        }
        else {
            putFramePointerInRegister(putOperandAddressInThisRegister, isCurrentFrame);
        }
        checkLabelAndAddQuad(new Quad("ADI", putOperandAddressInThisRegister, String.valueOf(symbolTable.getOffset(entry)), null, null, "Base + offset")); // Get the address of the var

        // Must load the data at this location. Either a stack or instance var points to the heap address of whatever we want.
        checkLabelAndAddQuad(new Quad("LDR", putOperandAddressInThisRegister, indirect(putOperandAddressInThisRegister), null, null, "Load heap address")); // Get the address of the var
    }


    private static void putHeapBaseAddressInRegister(String putOperandAddressInThisRegister, String operand, boolean isCurrentFrame) {
        SymbolTableEntry entry = symbolTable.get(operand);

        if(SymbolTableEntryType.INSTANCE_VAR.equals(entry.kind)) {
            // Instance object or array
            putThisPointerInRegister(putOperandAddressInThisRegister, isCurrentFrame);
            checkLabelAndAddQuad(new Quad("ADI", putOperandAddressInThisRegister, String.valueOf(symbolTable.getOffset(entry)), null, null, "Base + offset")); // Get the address of the var
        }
        else {
            // Stack object, stack array, or AEF/REF pointing to an object
            putFramePointerInRegister(putOperandAddressInThisRegister, isCurrentFrame);
            checkLabelAndAddQuad(new Quad("ADI", putOperandAddressInThisRegister, String.valueOf(symbolTable.getOffset(entry)), null, null, "Base + offset")); // Get the address of the var
            checkLabelAndAddQuad(new Quad("LDR", putOperandAddressInThisRegister, indirect(putOperandAddressInThisRegister), null, null, "Load *object* heap address from stack")); // Get the address of the var

            if(SymbolTableEntryType.ARR_ITEM.equals(entry.kind) || SymbolTableEntryType.REF_MEMBER.equals(entry.kind))
            {
                // We're looking for an object inside an array.
                checkLabelAndAddQuad(new Quad("LDR", putOperandAddressInThisRegister, indirect(putOperandAddressInThisRegister), null, null, "Load *object* heap address from stack")); // Get the address of the var
            }
        }
    }

    private static void putFramePointerInRegister(String register, boolean isCurrentFrame) {
        checkLabelAndAddQuad(new Quad("MOV", register, FP, null, null, "Get FP (stack address)"));

        if(!isCurrentFrame) {
            // We need to load data that's at the previous frame, so get the PFP and use that as FP
            checkLabelAndAddQuad(new Quad("ADI", register, "-4", null, null, "PFP stack address"));
            checkLabelAndAddQuad(new Quad("LDR", register, indirect(register), null, null, "Get PFP"));
        }
    }

    private static void putThisPointerInRegister(String register, boolean isCurrentFrame) {
        putFramePointerInRegister(register, isCurrentFrame);
        checkLabelAndAddQuad(new Quad("ADI", register, "-8", null, null, "this* stack address")); // Get the address of the this*
        checkLabelAndAddQuad(new Quad("LDR", register, indirect(register), null, null, "Get this* (heap address)"));

//        debug(register, "HYPHEN", true, true);
    }

    // To store NEWI heap address
    private static void putAddressToStoreNewInRegister(String putAddressInThisRegister, SymbolTableEntry entry, boolean isCurrentFrame) {
        if(SymbolTableEntryType.INSTANCE_VAR.equals(entry.kind)) {
            putThisPointerInRegister(putAddressInThisRegister, isCurrentFrame);
            checkLabelAndAddQuad(new Quad("ADI", putAddressInThisRegister, String.valueOf(symbolTable.getOffset(entry)), null, null, "Base + offset")); // Get the address of the var
        }
        else {
            putFramePointerInRegister(putAddressInThisRegister, isCurrentFrame);
            checkLabelAndAddQuad(new Quad("ADI", putAddressInThisRegister, String.valueOf(symbolTable.getOffset(entry)), null, null, "Base + offset")); // Get the address of the var

            if(SymbolTableEntryType.ARR_ITEM.equals(entry.kind)
                    || SymbolTableEntryType.REF_MEMBER.equals(entry.kind)) {
                checkLabelAndAddQuad(new Quad("LDR", putAddressInThisRegister, indirect(putAddressInThisRegister), null, null, "Get heap address for array/ref"));
            }
        }
    }

    // To store NEW heap address
    private static void putAddressToStoreNewArrayInRegister(String putAddressInThisRegister, SymbolTableEntry entry, boolean isCurrentFrame) {
        if(SymbolTableEntryType.INSTANCE_VAR.equals(entry.kind)) {
            putThisPointerInRegister(putAddressInThisRegister, isCurrentFrame);
            checkLabelAndAddQuad(new Quad("ADI", putAddressInThisRegister, String.valueOf(symbolTable.getOffset(entry)), null, null, "Base + offset")); // Get the address of the var
        }
        else {
            putFramePointerInRegister(putAddressInThisRegister, isCurrentFrame);
            checkLabelAndAddQuad(new Quad("ADI", putAddressInThisRegister, String.valueOf(symbolTable.getOffset(entry)), null, null, "Base + offset")); // Get the address of the var

            // No nested arrays, so we're done.
        }
    }

    // For AEF/REF, always a temp variable of some sort
    private static void putAddressToStoreRefsInRegister(String putAddressInThisRegister, SymbolTableEntry entry, boolean isCurrentFrame) {
        putFramePointerInRegister(putAddressInThisRegister, isCurrentFrame);
        checkLabelAndAddQuad(new Quad("ADI", putAddressInThisRegister, String.valueOf(symbolTable.getOffset(entry)), null, null, "Base + offset")); // Get the address of the var
    }


    private static void putVariableAddressInRegister(String putAddressInThisRegister, SymbolTableEntry entry, boolean isCurrentFrame) {
        if(SymbolTableEntryType.INSTANCE_VAR.equals(entry.kind)) {
            putThisPointerInRegister(putAddressInThisRegister, isCurrentFrame);
            checkLabelAndAddQuad(new Quad("ADI", putAddressInThisRegister, String.valueOf(symbolTable.getOffset(entry)), null, null, "Base + offset")); // Get the address of the var
        }
        else {
            putFramePointerInRegister(putAddressInThisRegister, isCurrentFrame);
            checkLabelAndAddQuad(new Quad("ADI", putAddressInThisRegister, String.valueOf(symbolTable.getOffset(entry)), null, null, "Base + offset")); // Get the address of the var

            if(SymbolTableEntryType.ARR_ITEM.equals(entry.kind)
                    || SymbolTableEntryType.REF_MEMBER.equals(entry.kind)) {
                checkLabelAndAddQuad(new Quad("LDR", putAddressInThisRegister, indirect(putAddressInThisRegister), null, null, "Get heap address for array/ref"));
            }
        }
    }

    private static boolean isLoadByte(SymbolTableEntry entry) {
        boolean isOnHeap = SymbolTableEntryType.INSTANCE_VAR.equals(entry.kind) || SymbolTableEntryType.ARR_ITEM.equals(entry.kind) || SymbolTableEntryType.REF_MEMBER.equals(entry.kind);
        boolean isByte = "char".equals(entry.data.get("type")) || "bool".equals(entry.data.get("type"));
        return isOnHeap && isByte;
    }

    private static void loadDataInRegister(String loadToThisRegister, String operand, String comment, boolean isCurrentFrame) {
        if("this".equals(operand)) {
            putThisPointerInRegister(loadToThisRegister, isCurrentFrame);
            return;
        }

        String loadOp;
        SymbolTableEntry entry = symbolTable.get(operand);
        String datatype = entry.data.get("type");
        if(SymbolTableEntryType.GLOBAL_LITERAL.equals(entry.kind)) {
            if("char".equals(datatype) || "bool".equals(datatype)) {
                loadOp = "LDB";
            }
            else {
                loadOp = "LDR"; // Everything other than char and bool is int size - either an int or an int-sized mem address.
            }
            checkLabelAndAddQuad(new Quad(loadOp, loadToThisRegister, operand, null, null, "Load var contents"));
        }
        else {
            putVariableAddressInRegister(loadToThisRegister, entry, isCurrentFrame);
            checkLabelAndAddQuad(new Quad(isLoadByte(entry) ? "LDB" : "LDR", loadToThisRegister, indirect(loadToThisRegister), null, null, "Load data at address")); // Load the value into R1
        }
    }

    /**
     * Expects result in R3, and writes to memory
     */
    private static void storeDataInRegister(String storeValueInThisRegisterToMemory, String operand) {
        String registerToStoreTempMemAddressesIn = "R4"; // Values are usually stored in 1, 2, or 3, so 4 should be safe to use.
        if("this".equals(operand)) {
            System.out.println("'this' is not a valid destination operand!");
//            putThisPointerInRegister(registerToStoreTempMemAddressesIn, true);
            return;
        }

        String storeOp;
        SymbolTableEntry entry = symbolTable.get(operand);
        String datatype = entry.data.get("type");
        if(SymbolTableEntryType.GLOBAL_LITERAL.equals(entry.kind)) {
            System.out.println("Why are you overwriting a global literal? You are a terrible programmer!");
            if("char".equals(datatype) || "bool".equals(datatype)) {
                storeOp = "STB";
            }
            else {
                storeOp = "STR"; // Everything other than char and bool is int size - either an int or an int-sized mem address.
            }
            checkLabelAndAddQuad(new Quad(storeOp, storeValueInThisRegisterToMemory, operand, null, null, "Store to global label"));
        }
        else {
            putVariableAddressInRegister(registerToStoreTempMemAddressesIn, entry, true);
            checkLabelAndAddQuad(new Quad(isLoadByte(entry) ? "STB" : "STR", storeValueInThisRegisterToMemory, indirect(registerToStoreTempMemAddressesIn), null, null, "Store data at address")); // Load the value into R1
        }
    }
    private static void storeData(Quad q) {
        storeDataInRegister("R3", q.operand3);
    }

    private static void debug(String register, String label, boolean isRegisterInt, boolean offsetWithNewline) {
        checkLabelAndAddQuad(new Quad("MOV", "R5", register, null, null, "debug"));
        if(offsetWithNewline) {
            checkLabelAndAddQuad(new Quad("LDB", register, "NEWLINE", null, null, "debug"));
            checkLabelAndAddQuad(new Quad("TRP", "3", null, null, null, "debug"));
        }

        checkLabelAndAddQuad(new Quad("LDB", register, label, null, null, "debug"));
        checkLabelAndAddQuad(new Quad("TRP", "3", null, null, null, "debug"));

        checkLabelAndAddQuad(new Quad("MOV", "R0", "R5", null, null, "debug"));
        checkLabelAndAddQuad(new Quad("TRP", isRegisterInt ? "1" : "3", null, null, null, "debug"));

        checkLabelAndAddQuad(new Quad("LDB", register, label, null, null, "debug"));
        checkLabelAndAddQuad(new Quad("TRP", "3", null, null, null, "debug"));

        if(offsetWithNewline) {
            checkLabelAndAddQuad(new Quad("LDB", register, "NEWLINE", null, null, "debug"));
            checkLabelAndAddQuad(new Quad("TRP", "3", null, null, null, "debug"));
        }
        checkLabelAndAddQuad(new Quad("MOV", register, "R5", null, null, "debug"));
    }

    private static void checkLabelAndAddQuad(Quad q) {
        if(finalQuads.size() < 1) {
            finalQuads.add(q);
            return;
        }

        Quad lastQuad = finalQuads.get(finalQuads.size() - 1);
        if(lastQuad.operator == null) {
            // This is label-only
//            if(q.label == null) {
                lastQuad.fillData(q);
//            }
        }
        else {
            finalQuads.add(q);
        }
   //            else {
//                lastQuad.fillData(q);
//                backPatch(lastQuad.label, q.label);
//            }
//        }
//        else {
//            finalQuads.add(q);
//        }
    }

    // This should never be called...
    private static void backPatch(String oldLabel, String newLabel) {
        Quad current;
        for(int i = finalQuads.size() - 1; i >= 0; i--) {
            current = finalQuads.get(i);
            if(oldLabel.equals(current.label)) {
                current.label = newLabel;
            }
            if(oldLabel.equals(current.operand1)) {
                current.operand1 = newLabel;
            }
            if(oldLabel.equals(current.operand2)) {
                current.operand2 = newLabel;
            }
            if(oldLabel.equals(current.operand3)) {
                current.operand3 = newLabel;
            }
            if(oldLabel.equals(current.comment)) {
                current.comment = newLabel;
            }
        }
    }
    
    public static void dumpQuads() {
        for(Quad q : finalQuads) {
            targetCodeFile.write(q.toString());
        }
    }
}
