package tcode;

import icode.Generator;
import icode.quad.Quad;
import log.Log;
import syntax.symbol.SymbolTable;
import syntax.symbol.SymbolTableEntry;
import syntax.symbol.SymbolTableEntryType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 2/26/14
 * Time: 3:56 PM
 * To change this template use File | Settings | File Templates.
 */
public final class RegisterManager {
    public static final int VAL_REGISTER_COUNT = 6;
    public static final int OVERALL_REGISTER_COUNT = 11;
    
    public static List<Quad> finalQuads = new ArrayList<Quad>();

    public static final String TOS = "R6";
    public static final String FP = "R7";
    public static final String SL = "R8";
    public static final String SB = "R9";
    public static final String PC = "R10";

    private static int logicLabelCounter = 1;

    public static Log targetCodeFile = new Log("tcode.asm");

    private static String getLogicLabel() {
        return "LL" + logicLabelCounter++;
    }

//    private static  List<Register> registerDescriptors = initRegisterDescriptors();
    public static final Map<Integer, Register> registers = initRegisterDescriptors();
    private static SymbolTable symbolTable = SymbolTable.get();
    private static int currentFreeRegister = 0;

    private static Map<Integer, Register> initRegisterDescriptors() {
        Map<Integer, Register> registers =  new HashMap<Integer, Register>(VAL_REGISTER_COUNT);
        for(int i = 0; i < VAL_REGISTER_COUNT; i++) {
            registers.put(i, new Register(i));
        }
        return registers;
    }

    public static Register getFreeRegister() {
        int freeRegister = currentFreeRegister++;
        if(currentFreeRegister == VAL_REGISTER_COUNT)
        {
            currentFreeRegister = 0;
        }
        return registers.get(freeRegister);
    }

    public static Integer getContainingRegister(String symid) {
        for(Register r : registers.values())
        {
            for(String id : r.symids)
            {
                if(id.equals(symid))
                {
                    return r.registerNumber;
                }
            }
        }
        return null;
    }

    private static void getNumberInRegister(String register, int number) {
        checkLabelAndAddQuad(new Quad("SUB", register, register, null, null, "Zero out " + register));
        if(number != 0) {
            checkLabelAndAddQuad(new Quad("ADI", register, String.valueOf(number), null, null, "Put " + number + " in " + register));
        }
    }

    private static String indirect(String register) {
        return String.format("(%s)", register);
    }

//    MOV R5, SP ; Test Overflow
//    ADI R5, #-20 ; rtn,pfp,this,i,7
//    CMP R5, SL
//    BLT R5, OVERFLOW
//    MOV R3, FP ; Old Frame
//    MOV FP, SP ; New Frame
//    ADI SP, #-4 ; PFP
//    STR R3, (SP) ; Set PFP
//    ADI SP, #-4
//    STR R7, (SP) ; Set this on Stack
//    ADI SP, #-4

    /**
     *
     * @param q
     */
    private static void frame(Quad q) {
        int frameSize = -12;
        if(q != null) {
            frameSize -= symbolTable.getFuncSize(q.operand1);
        }

        checkLabelAndAddQuad(new Quad("MOV", "R1", TOS, null, null, "Copy TOS into R1"));
        checkLabelAndAddQuad(new Quad("ADI", "R1", String.valueOf(frameSize), null, null, "Space for RTN addr, PFP, this, and locals?"));
        checkLabelAndAddQuad(new Quad("CMP", "R1", SL, null, null, "Compare TOS and SL"));
        checkLabelAndAddQuad(new Quad("BLT", "R1", "OVERFLOW", null, null, "Overflow check"));

        checkLabelAndAddQuad(new Quad("MOV", "R1", FP, null, null, "Save FP in R1, this will be the PFP"));
        checkLabelAndAddQuad(new Quad("MOV", FP, TOS, null, null, "Set FP to Current Activation Record (FP = TOS)"));
        checkLabelAndAddQuad(new Quad("ADI", TOS, "-4", null, null, "Move TOS to where PFP should be stored"));
        checkLabelAndAddQuad(new Quad("STR", "R1", indirect(TOS), null, null, "Put PFP on the Activation Record (PFP = FP)"));
        checkLabelAndAddQuad(new Quad("ADI", TOS, "-4", null, null, "Move TOS to this*"));

        // Set the this*
        if(q == null) {
            // Set the this* for main
            checkLabelAndAddQuad(new Quad("LDA", "R5", "FREE", null, null, "Where is the FREE label?")); // This tells you where the next free space is.
            checkLabelAndAddQuad(new Quad("ADI", "R5", "4", null, null, "Increment by size of int to get where the actual free space is"));
            checkLabelAndAddQuad(new Quad("STR", "R5", "FREE", null, null, "Put the actual address of the FREE space at FREE"));
            checkLabelAndAddQuad(new Quad("STR", "R5", indirect(TOS), null, null, "Store the this* on the stack"));
        }
        else {
            // Set the this* for other functions. Expects this* in R2, loaded in loadData()
            checkLabelAndAddQuad(new Quad("STR", "R2", indirect(TOS), null, null, "Store the this* on the stack"));
        }
        checkLabelAndAddQuad(new Quad("ADI", TOS, "-4", null, null, "Move TOS to new top"));
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
            if("char".equals(entry.data.get("type"))
                    || "bool".equals(entry.data.get("type")))
            {
                operand = ".BYT";
            }
            else {
                operand = ".INT";
            }
            checkLabelAndAddQuad(new Quad(operand, entry.value, null, null, entry.symid, "Global data"));
        }
    }

    public static void produceTargetCode() {
        frame(null);
        call(null);
        checkLabelAndAddQuad(new Quad("TRP", "0", null, null, null, "Return from main - quit program"));

        for(Quad q : Generator.quads)
        {
            handleQuad(q);
        }
        doOverflowUnderflow();
        doGlobals();
        checkLabelAndAddQuad(new Quad(".INT", "0", null, null, "FREE", "The this*, will be set at the start"));
    }

    private static void handleQuad(Quad q) {
        checkLabelAndAddQuad(new Quad(null, null, null, null, q.label, q.comment));

        if("FUNC".equals(q.operator))
        {
            String label = q.label;
            int tempVars = symbolTable.getTotalTempVars(q.operand1); // operand 1 should be the func symid
            checkLabelAndAddQuad(new Quad("ADI", TOS, String.valueOf(-4 * tempVars), null, label, q.comment));
            checkLabelAndAddQuad(new Quad("MOV", "R4", TOS, null, null, q.comment));
            checkLabelAndAddQuad(new Quad("CMP", "R4", SL, null, null, q.comment));
            checkLabelAndAddQuad(new Quad("BLT", "R4", "OVERFLOW", null, null, q.comment));
        }
        else if("MOV".equals(q.operator))
        {
            loadData(q);
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment)); // R1 -> R3
            storeData(q);
        }
        else if("ADD".equals(q.operator)
                || "SUB".equals(q.operator)
                || "MUL".equals(q.operator)
                || "DIV".equals(q.operator))
        {
            loadData(q);
            checkLabelAndAddQuad(new Quad(q.operator, "R1", "R2", null, null, q.comment + " - Do op")); // R1 op R2 -> R1
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment + " - Copy result to R3")); // R1 -> R3
            storeData(q);
        }
        else if("ADI".equals(q.operator))
        {
            loadData(q);
            checkLabelAndAddQuad(new Quad(q.operator, "R1", q.operand2, null, null, q.comment + " - Add immediate " + q.operand2)); // R1 op R2 -> R1
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment + " - Copy result to R3")); // R1 -> R3
            storeData(q);
        }
        else if("WRITE".equals(q.operator)) {
            loadData(q);
            checkLabelAndAddQuad(new Quad("MOV", "R0", "R1", null, null, q.comment + " - Load value for trap")); // R1 -> R0
            checkLabelAndAddQuad(new Quad("TRP", q.operand2, null, null, null, q.comment + " - Trap")); // operand2 is trap number
        }
        else if("RTN".equals(q.operator)) {
            loadData(q);
            doReturn(q);
        }
        else if("NEWI".equals(q.operator)) {
            checkLabelAndAddQuad(new Quad("LDR", "R3", "FREE", null, null, q.comment + " - Get address of free space"));
            checkLabelAndAddQuad(new Quad("MOV", "R2", "R3", null, null, q.comment + " - Copy address of free"));
            checkLabelAndAddQuad(new Quad("ADI", "R2", q.operand1, null, null, q.comment + " - Increment this*"));
            checkLabelAndAddQuad(new Quad("STR", "R2", "FREE", null, null, q.comment + " - Save incremented this*"));
            storeData(q);
        }
        else if("NEW".equals(q.operator)) {
            loadData(q); // Get array size in R1

            checkLabelAndAddQuad(new Quad("LDR", "R3", "FREE", null, null, q.comment + " - Get this*"));
            checkLabelAndAddQuad(new Quad("LDR", "R3", "FREE", null, null, q.comment + " - Get this*"));

            getNumberInRegister("R2", symbolTable.getSize(symbolTable.get(q.operand1).data.get("type")));
            checkLabelAndAddQuad(new Quad("MUL", "R2", "R1", null, null, q.comment + " - Get offset"));
            checkLabelAndAddQuad(new Quad("ADD", "R2", "R3", null, null, q.comment + " - Increment this*"));
            checkLabelAndAddQuad(new Quad("STR", "R2", "FREE", null, null, q.comment + " - Save incremented this*"));
            storeData(q);
        }
        else if("FRAME".equals(q.operator)) {
            loadData(q);
            frame(q);
        }
        else if("PUSH".equals(q.operator)) {
            loadData(q);
//            int offset = symbolTable.getOffset(symbolTable.get(q.operand1));

            // Can't use loadData since it's the previous frame that has the vars.
//            checkLabelAndAddQuad(new Quad("LDR", "R4", FP, null, null, "Get current frame"));
//            checkLabelAndAddQuad(new Quad("ADI", "R4", "-4", null, null, "PFP"));
//            checkLabelAndAddQuad(new Quad("LDR", "R4", indirect("R4"), null, null, "Get previous frame"));
//            checkLabelAndAddQuad(new Quad("ADI", "R4", String.valueOf(offset), null, null, "Get address of var"));
//            checkLabelAndAddQuad(new Quad("LDR", "R1", indirect("R4"), null, null, "Load value of var into R1"));
            checkLabelAndAddQuad(new Quad("STR", "R1", indirect(TOS), null, null, "Put var on top of stack"));
            checkLabelAndAddQuad(new Quad("ADI", TOS, "-4", null, null, "Move TOS"));
        }
        else if("CALL".equals(q.operator)) {
            call(q);
        }
        else if("PEEK".equals(q.operator)) {
            checkLabelAndAddQuad(new Quad("LDR", "R3", indirect(TOS), null, null, q.comment));
            storeData(q);
        }
        else if("EQ".equals(q.operator)) {
            loadData(q);
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment));
            checkLabelAndAddQuad(new Quad("CMP", "R3", "R2", null, null, q.comment));
//            String logicLabelTrue = getLogicLabel();
//            String logicLabelEnd = getLogicLabel();
//            checkLabelAndAddQuad(new Quad("BRZ", "R3", logicLabelEnd, null, null, q.comment));

            // Handle false
//            getNumberInRegister("R3", 1);
//            checkLabelAndAddQuad(new Quad("JMP", logicLabelEnd, null, null, null, q.comment));

            // Handle true/end (same since true already puts a 0 in R3)
//            checkLabelAndAddQuad(new Quad(null, null, null, null, logicLabelEnd, q.comment)); // Label for the next thing?
            storeData(q); // Store 0 or 1
        }
        else if("NE".equals(q.operator)) {
            loadData(q);
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment));
            checkLabelAndAddQuad(new Quad("CMP", "R3", "R2", null, null, q.comment));
//            String logicLabelTrue = getLogicLabel();
            String logicLabelEnd = getLogicLabel();
            checkLabelAndAddQuad(new Quad("BRZ", "R3", logicLabelEnd, null, null, q.comment));

            // Handle false
            getNumberInRegister("R3", 1);
            checkLabelAndAddQuad(new Quad("JMP", logicLabelEnd, null, null, null, q.comment));

            // Handle true/end (same since true already puts a 0 in R3)
            checkLabelAndAddQuad(new Quad(null, null, null, null, logicLabelEnd, q.comment)); // Label for the next thing?
            storeData(q); // Store 0 or 1
        }
        else if("BF".equals(q.operator)) {
            loadData(q);
            checkLabelAndAddQuad(new Quad("BNZ", "R1", q.operand2, null, null, q.comment)); // Label for the next thing?
        }
        else if("BT".equals(q.operator)) {
            loadData(q);
            checkLabelAndAddQuad(new Quad("BRZ", "R1", q.operand2, null, null, q.comment)); // Label for the next thing?
        }
        else if("JMP".equals(q.operator)) {
//            loadData(q);
            checkLabelAndAddQuad(new Quad("JMP", q.operand1, null, null, null, q.comment)); // Label for the next thing?
        }
        else if("REF".equals(q.operator)) {
            loadData(q);
            checkLabelAndAddQuad(new Quad("MOV", "R3", "R1", null, null, q.comment)); // Label for the next thing?
            checkLabelAndAddQuad(new Quad("ADI", "R3", String.valueOf(symbolTable.getOffset(symbolTable.get(q.operand2))), null, null, q.comment)); // Label for the next thing?
            storeData(q);
        }


//        new Quad(s, s, s, null, label, q.comment);
    }

    /**
     * Puts the first source op in R2
     * Puts the second source op in R3, if applicable
     * @param q quad to load data for
     */
    private static void loadData(Quad q) {
//        String label = q.label;
        String loadOp;
        String datatype;

        boolean skipOperand1 = q.operand1 == null || "READ".equals(q.operator) || "FRAME".equals(q.operator)
                || "JMP".equals(q.operator);
        boolean skipOperand2 = q.operand2 == null || "WRITE".equals(q.operator) || "ADI".equals(q.operator)
                || "BF".equals(q.operator) || "BT".equals(q.operator)
                || "REF".equals(q.operator);

        SymbolTableEntry entry;
        if(!skipOperand1) {
            if("this".equals(q.operand1)) {
                checkLabelAndAddQuad(new Quad("MOV", "R1", FP, null, null, q.comment + " - Load data (heap)")); // Put FP in R1
                checkLabelAndAddQuad(new Quad("ADI", "R1", "-8", null, null, q.comment + " - Load data (heap)")); // Get the address of the this*
                checkLabelAndAddQuad(new Quad("LDR", "R1", indirect("R1"), null, null, q.comment + " - Load data (heap)")); // Get this*
            }
            else {
                entry = symbolTable.get(q.operand1);
                datatype = entry.data.get("type");
                if(SymbolTableEntryType.PARAM.equals(entry.kind)
                        || SymbolTableEntryType.LOCAL_VAR.equals(entry.kind)
                        || SymbolTableEntryType.TEMP_VAR.equals(entry.kind)) {
                    checkLabelAndAddQuad(new Quad("MOV", "R1", null, FP, null, q.comment + " - Put FP in R1"));
                    checkLabelAndAddQuad(new Quad("ADI", "R1", null, String.valueOf(symbolTable.getOffset(entry)), null, q.comment + " - FP + -offset"));
                    checkLabelAndAddQuad(new Quad("LDR", "R1", null, "(R1)", null, q.comment + " - Load stack var, R1")); // Load the value into R1
                }
                else if(SymbolTableEntryType.GLOBAL_LITERAL.equals(entry.kind)) {
                    if("int".equals(datatype)) {
                        loadOp = "LDR";
                    }
                    else if("char".equals(datatype) || "bool".equals(datatype)) {
                        loadOp = "LDB";
                    }
                    else {
                        loadOp = "LDR"; // todo: this will probably change
                    }
                    checkLabelAndAddQuad(new Quad(loadOp, "R1", q.operand1, null, null, q.comment + " - Load global label, R1")); // Load label of literal into R1
                }
                else if(SymbolTableEntryType.INSTANCE_VAR.equals(entry.kind)) {
                    checkLabelAndAddQuad(new Quad("MOV", "R1", FP, null, null, q.comment + " - Load data (heap)")); // Put FP in R1
                    checkLabelAndAddQuad(new Quad("ADI", "R1", "-8", null, null, q.comment + " - Load data (heap)")); // Get the address of the this*
                    checkLabelAndAddQuad(new Quad("LDR", "R1", indirect("R1"), null, null, q.comment + " - Load data (heap)")); // Get the address of the object on the heap

                    getNumberInRegister("R4", symbolTable.getOffset(entry));
                    checkLabelAndAddQuad(new Quad("ADD", "R1", "R4", null, null, q.comment + " - Load data (heap)")); // Base heap address in R1 + offset in R2 = address to write to

                    if("int".equals(datatype)) {
                        loadOp = "LDR";
                    }
                    else if("char".equals(datatype) || "bool".equals(datatype)) {
                        loadOp = "LDB";
                    }
                    else {
                        loadOp = "LDR"; // todo: this will probably change
                    }

                    checkLabelAndAddQuad(new Quad(loadOp, "R1", "(R1)", null, null, q.comment + " - Load data (heap)")); // Load actual value into R1
                }
                else if(SymbolTableEntryType.REF_MEMBER.equals(entry.kind)) {
                    // This is a temp var holding an address on the heap.
                    if("int".equals(datatype)) {
                        loadOp = "LDR";
                    }
                    else if("char".equals(datatype) || "bool".equals(datatype)) {
                        loadOp = "LDB";
                    }
                    else {
                        loadOp = "LDR"; // todo: this will probably change
                    }

                    // First treat it like a stack variable:
                    checkLabelAndAddQuad(new Quad("MOV", "R1", null, FP, null, q.comment + " - Load Ref member (stack portion)")); // Put FP in R2
                    checkLabelAndAddQuad(new Quad("ADI", "R1", null, String.valueOf(symbolTable.getOffset(entry)), null, q.comment + " - Load Ref member (stack portion)")); // FP + -offset
                    checkLabelAndAddQuad(new Quad("LDR", "R1", null, "(R1)", null, q.comment + " - Load Ref member (stack portion)")); // Load the value into R2

                    // Now we have the address on the heap and can load it into the register proper!
                    checkLabelAndAddQuad(new Quad(loadOp, "R1", indirect("R1"), null, null, q.comment + " - Load data (heap portion)")); // Get the address of the object on the heap
                }
            }
        }

        if(!skipOperand2) {
            if("this".equals(q.operand2)) {
                checkLabelAndAddQuad(new Quad("MOV", "R2", FP, null, null, q.comment + " - Load data (heap)")); // Put FP in R1
                checkLabelAndAddQuad(new Quad("ADI", "R2", "-8", null, null, q.comment + " - Load data (heap)")); // Get the address of the this*
                checkLabelAndAddQuad(new Quad("LDR", "R2", indirect("R2"), null, null, q.comment + " - Load data (heap)")); // Get this*
            }
            else {
                entry = symbolTable.get(q.operand2);
                datatype = entry.data.get("type");
                if(SymbolTableEntryType.PARAM.equals(entry.kind)
                        || SymbolTableEntryType.LOCAL_VAR.equals(entry.kind)
                        || SymbolTableEntryType.TEMP_VAR.equals(entry.kind)) {
                    checkLabelAndAddQuad(new Quad("MOV", "R2", null, FP, null, q.comment + " - Put FP in R1")); // Put FP in R2
                    checkLabelAndAddQuad(new Quad("ADI", "R2", null, String.valueOf(symbolTable.getOffset(entry)), null, q.comment + " - FP + -offset")); // FP + -offset
                    checkLabelAndAddQuad(new Quad("LDR", "R2", null, "(R2)", null, q.comment + " - Load stack var, R2")); // Load the value into R2
                }
                else if(SymbolTableEntryType.GLOBAL_LITERAL.equals(entry.kind)) {
                    if("int".equals(datatype)) {
                        loadOp = "LDR";
                    }
                    else if("char".equals(datatype) || "bool".equals(datatype)) {
                        loadOp = "LDB";
                    }
                    else {
                        loadOp = "LDR"; // todo: this will probably change
                    }

                    checkLabelAndAddQuad(new Quad(loadOp, "R2", null, q.operand2, null, q.comment + " - Load global label, R2")); // Load label of literal into R2
                }
                else if(SymbolTableEntryType.INSTANCE_VAR.equals(entry.kind)) {
                    checkLabelAndAddQuad(new Quad("MOV", "R2", FP, null, null, q.comment + " - Load data (heap)")); // Put FP in R2
                    checkLabelAndAddQuad(new Quad("ADI", "R2", "-8", null, null, q.comment + " - Load data (heap)")); // Get the address of the this*
                    checkLabelAndAddQuad(new Quad("LDR", "R2", indirect("R2"), null, null, q.comment + " - Load data (heap)")); // Get the address of the object on the heap

                    getNumberInRegister("R4", symbolTable.getOffset(entry));
                    checkLabelAndAddQuad(new Quad("ADD", "R2", "R4", null, null, q.comment + " - Load data (heap)")); // Base heap address in R2 + offset in R4 = address to read from

                    if("int".equals(datatype)) {
                        loadOp = "LDR";
                    }
                    else if("char".equals(datatype) || "bool".equals(datatype)) {
                        loadOp = "LDB";
                    }
                    else {
                        loadOp = "LDR"; // todo: this will probably change
                    }

                    checkLabelAndAddQuad(new Quad(loadOp, "R2", "(R2)", null, null, q.comment + " - Load data (heap)")); // Load actual value into R2
                }
                else if(SymbolTableEntryType.REF_MEMBER.equals(entry.kind)) {
                    // This is a temp var holding an address on the heap.
                    if("int".equals(datatype)) {
                        loadOp = "LDR";
                    }
                    else if("char".equals(datatype) || "bool".equals(datatype)) {
                        loadOp = "LDB";
                    }
                    else {
                        loadOp = "LDR"; // todo: this will probably change
                    }

                    // First treat it like a stack variable:
                    checkLabelAndAddQuad(new Quad("MOV", "R2", null, FP, null, q.comment + " - Load Ref member (stack portion)")); // Put FP in R2
                    checkLabelAndAddQuad(new Quad("ADI", "R2", null, String.valueOf(symbolTable.getOffset(entry)), null, q.comment + " - Load Ref member (stack portion)")); // FP + -offset
                    checkLabelAndAddQuad(new Quad("LDR", "R2", null, "(R2)", null, q.comment + " - Load Ref member (stack portion)")); // Load the value into R2

                    // Now we have the address on the heap and can load it into the register proper!
                    checkLabelAndAddQuad(new Quad(loadOp, "R2", indirect("R2"), null, null, q.comment + " - Load Ref member (heap portion)")); // Get the address of the object on the heap
                }
            }
        }

        // operand3 is destination, so there won't be a need to load that one
//        checkLabelAndAddQuad(new Quad("LD(R)", "<SOMETHING_I_DONT_KNOW_YET_SRC_1>", null, "R1", label, q.comment));
//
//        if(false) // obviously not known yet
//        {
//            checkLabelAndAddQuad(new Quad("LD(R)", "<SOMETHING_I_DONT_KNOW_YET_SRC_2>", null, "R2", label, q.comment));
//        }
    }

    /**
     * Expects result in R3, and writes to memory
     * @param q the quad with instructions
     */
    private static void storeData(Quad q) {
        String storeOp;
        SymbolTableEntry entry = symbolTable.get(q.operand3);
        if(SymbolTableEntryType.PARAM.equals(entry.kind)
                || SymbolTableEntryType.LOCAL_VAR.equals(entry.kind)
                || SymbolTableEntryType.TEMP_VAR.equals(entry.kind)) {
            checkLabelAndAddQuad(new Quad("MOV", "R1", FP, null, null, q.comment + " - Store data (stack)")); // Put FP in R1
            checkLabelAndAddQuad(new Quad("ADI", "R1", String.valueOf(symbolTable.getOffset(entry)), null, null, q.comment + " - Store data (stack)")); // FP + -offset
            checkLabelAndAddQuad(new Quad("STR", "R3", "(R1)", null, null, q.comment + " - Store data (stack)")); // Store value in R3 to address in R1
        }
        else if(SymbolTableEntryType.INSTANCE_VAR.equals(entry.kind)) {
            String datatype = entry.data.get("type");

            if("int".equals(datatype)) {
                storeOp = "STR";
            }
            else if("char".equals(datatype) || "bool".equals(datatype)) {
                storeOp = "STB";
            }
            else {
                storeOp = "STR"; // todo: this will probably change
            }

            checkLabelAndAddQuad(new Quad("MOV", "R1", FP, null, null, q.comment + " - Store data (heap)")); // Put FP in R1
            checkLabelAndAddQuad(new Quad("ADI", "R1", "-8", null, null, q.comment + " - Store data (heap)")); // Get the address of the this*
            checkLabelAndAddQuad(new Quad("LDR", "R1", indirect("R1"), null, null, q.comment + " - Store data (heap)")); // Get the address of this object on the heap

            getNumberInRegister("R2", symbolTable.getOffset(entry));
            checkLabelAndAddQuad(new Quad("ADD", "R1", "R2", null, null, q.comment + " - Store data (heap)")); // Base heap address in R1 + offset in R2 = address to write to
            checkLabelAndAddQuad(new Quad(storeOp, "R3", "(R1)", null, null, q.comment + " - Store data (heap)")); // Store value in R3 to address in R1
        }
        else if(SymbolTableEntryType.REF_MEMBER.equals(entry.kind)) {
            checkLabelAndAddQuad(new Quad("MOV", "R1", FP, null, null, q.comment + " - Store data (heap reference on stack)")); // Put FP in R1
            checkLabelAndAddQuad(new Quad("ADI", "R1", String.valueOf(symbolTable.getOffset(entry)), null, null, q.comment + " - Store data (heap reference on stack)")); // FP + -offset
            checkLabelAndAddQuad(new Quad("STR", "R3", "(R1)", null, null, q.comment + " - Store data (heap reference on stack)")); // Store value in R3 to address in R1
        }
//            String datatype = entry.data.get("type");
//
//            if("int".equals(datatype)) {
//                storeOp = "STR";
//            }
//            else if("char".equals(datatype) || "bool".equals(datatype)) {
//                storeOp = "STB";
//            }
//            else {
//                storeOp = "STR"; // todo: this will probably change
//            }
//
//            // First treat it like a stack variable:
//            checkLabelAndAddQuad(new Quad("MOV", "R2", null, FP, null, q.comment + " - Load Ref member (stack portion)")); // Put FP in R2
//            checkLabelAndAddQuad(new Quad("ADI", "R2", null, String.valueOf(symbolTable.getOffset(entry)), null, q.comment + " - Load Ref member (stack portion)")); // FP + -offset
//            checkLabelAndAddQuad(new Quad("LDR", "R2", null, "(R2)", null, q.comment + " - Load Ref member (stack portion)")); // Load the value into R2
//
//            // Now we have the address on the heap and can load it into the register proper!
//            checkLabelAndAddQuad(new Quad(storeOp, "R2", indirect("R2"), null, null, q.comment + " - Load Ref member (heap portion)")); // Get the address of the object on the heap
//        }
    }

    private static void checkLabelAndAddQuad(Quad q) {
        if(finalQuads.size() < 1) {
            finalQuads.add(q);
            return;
        }

        Quad lastQuad = finalQuads.get(finalQuads.size() - 1);
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
            finalQuads.add(q);
        }
    }

    private static void backPatch(String oldLabel, String newLabel) {
        Quad current;
        for(int i = finalQuads.size() - 1; i >= 0; i--) {
            current = finalQuads.get(i);
            if(oldLabel.equals(current.label)) {
//                log.debug("Backpatch replaced label");
                current.label = newLabel;
            }
            if(oldLabel.equals(current.operand1)) {
//                log.debug("Backpatch replaced operand1");
                current.operand1 = newLabel;
            }
            if(oldLabel.equals(current.operand2)) {
//                log.debug("Backpatch replaced operand2");
                current.operand1 = newLabel;
            }
            if(oldLabel.equals(current.operand3)) {
//                log.debug("Backpatch replaced operand3");
                current.operand1 = newLabel;
            }
            if(oldLabel.equals(current.comment)) {
//                log.debug("Backpatch replaced comment");
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
