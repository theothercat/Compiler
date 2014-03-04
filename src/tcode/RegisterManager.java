package tcode;

import icode.Generator;
import icode.quad.Quad;
import log.Log;
import sun.awt.Symbol;
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

    public static final String TOS = "R6";
    public static final String FP = "R7";
    public static final String SL = "R8";
    public static final String SB = "R9";
    public static final String PC = "R10";

    public static Log targetCodeFile = new Log("tcode.asm");

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
        targetCodeFile.write(new Quad("SUB", register, register, null, null, "Zero out " + register).toString());
        if(number != 0) {
            targetCodeFile.write(new Quad("ADI", register, String.valueOf(number), null, null, "Put " + number + " in " + register).toString());
        }
    }

    private static String indirect(String register) {
        return String.format("(%s)", register);
    }

    private static void fakeAMainCall() {
        targetCodeFile.write(new Quad("MOV", "R1", TOS, null, null, "Copy TOS into R1").toString());
        targetCodeFile.write(new Quad("ADI", "R1", "-12", null, null, "Space for RTN addr, PFP, and this?").toString());
        targetCodeFile.write(new Quad("CMP", "R1", SL, null, null, "Compare TOS and SL").toString());
        targetCodeFile.write(new Quad("BLT", "R1", "OVERFLOW", null, null, "Overflow check").toString());

        targetCodeFile.write(new Quad("MOV", "R1", FP, null, null, "Save FP in R1, this will be the PFP").toString());
        targetCodeFile.write(new Quad("MOV", FP, TOS, null, null, "Set FP to Current Activation Record (FP = TOS)").toString());
        targetCodeFile.write(new Quad("ADI", TOS, "-4", null, null, "Move TOS to where PFP should be stored").toString());
        targetCodeFile.write(new Quad("STR", "R1", indirect(FP), null, null, "Put PFP on the Activation Record (PFP = FP)").toString());
        targetCodeFile.write(new Quad("ADI", TOS, "-4", null, null, "Move TOS to this*").toString());
        // Don't bother putting anything in the this* for main.
        targetCodeFile.write(new Quad("ADI", TOS, "-4", null, null, "Move TOS to new top").toString());

        targetCodeFile.write(new Quad("MOV", "R1", PC, null, null, "PC incremented by 1 instruction").toString());
        targetCodeFile.write(new Quad("ADI", "R1", "18", null, null, "Compute return address").toString());
        targetCodeFile.write(new Quad("STR", "R1", indirect(FP), null, null, "Compute return address").toString());
        targetCodeFile.write(new Quad("JMP", symbolTable.findInScope("main", "g"), null, null, null, "Return from main - quit program").toString());
        targetCodeFile.write(new Quad("TRP", "0", null, null, null, "Return from main - quit program").toString());
        targetCodeFile.write("");
    }

    private static void doReturn(Quad q) {
        targetCodeFile.write(new Quad("MOV", TOS, FP, null, null, "De-allocate current activation record (TOS = FP)").toString());
        targetCodeFile.write(new Quad("MOV", "R1", TOS, null, null, "Copy TOS").toString());
        targetCodeFile.write(new Quad("CMP", "R1", SB, null, null, "Compare TOS and SB").toString());
        targetCodeFile.write(new Quad("BGT", "R1", "UNDERFLOW", null, null, "Underflow check").toString());

        targetCodeFile.write(new Quad("LDR", "R1", indirect(FP), null, null, "Return address pointed to by FP").toString());
        targetCodeFile.write(new Quad("ADI", FP, "-4", null, null, "Point at PFP in Activation Record").toString());
        targetCodeFile.write(new Quad("LDR", FP, indirect(FP), null, null, "FP = PFP").toString());
        if(q.operand1 != null) // Has a return value
        {
            targetCodeFile.write(new Quad("STR", "R1", indirect(TOS), null, null, "Put return value on the stack").toString());
        }
        targetCodeFile.write(new Quad("JMR", "R1", null, null, null, "Return from function call").toString());
    }

    private static void doOverflowUnderflow() {
        targetCodeFile.write("");
        String overflowString = "Stack overflow";
        String underflowString = "Stack underflow";

        targetCodeFile.write(new Quad("LDA", "R1", "OVERFLOW_S", null, "OVERFLOW", "Print overflow message and quit").toString());
        for(int i = 0; i < overflowString.length(); i++) {
            targetCodeFile.write(new Quad("LDB", "R0", indirect("R1"), null, null, null).toString());
            targetCodeFile.write(new Quad("TRP", "3", null, null, null, null).toString());
            targetCodeFile.write(new Quad("ADI", "R1", "1", null, null, null).toString());

            if(i == overflowString.length()) {
                break;
            }
        }
        targetCodeFile.write(new Quad("LDB", "R0", indirect("R1"), null, null, null).toString()); // Do a newline character (not shown in the above string)
        targetCodeFile.write(new Quad("TRP", "3", null, null, null, null).toString());
        targetCodeFile.write(new Quad("TRP", "0", null, null, null, "Overflow terminate").toString());
        targetCodeFile.write("");

        targetCodeFile.write(new Quad("LDA", "R1", "UNDERFLOW_S", null, "UNDERFLOW", "Print underflow message and quit").toString());
        for(int i = 0; i < underflowString.length(); i++) {
            targetCodeFile.write(new Quad("LDB", "R0", indirect("R1"), null, null, null).toString());
            targetCodeFile.write(new Quad("TRP", "3", null, null, null, null).toString());
            targetCodeFile.write(new Quad("ADI", "R1", "1", null, null, null).toString());

            if(i == underflowString.length()) {
                break;
            }
        }
        targetCodeFile.write(new Quad("LDB", "R0", indirect("R1"), null, null, null).toString()); // Do a newline character (not shown in the above string)
        targetCodeFile.write(new Quad("TRP", "3", null, null, null, null).toString());
        targetCodeFile.write(new Quad("TRP", "0", null, null, null, "Underflow terminate").toString());
        targetCodeFile.write("");

        // Do message string globals.
        targetCodeFile.write(new Quad(".BYT", String.format("'%s'", overflowString.charAt(0)), null, null, "OVERFLOW_S", "Overflow string").toString());
        for(int i = 1; i < overflowString.length(); i++) {
            targetCodeFile.write(new Quad(".BYT", String.format("'%s'", overflowString.charAt(i)), null, null, null, null).toString());
        }
        targetCodeFile.write(new Quad(".BYT", "'\\n'", null, null, null, null).toString());
        targetCodeFile.write("");

        targetCodeFile.write(new Quad(".BYT", String.format("'%s'", underflowString.charAt(0)), null, null, "UNDERFLOW_S", "Underflow string").toString());
        for(int i = 0; i < underflowString.length(); i++) {
            targetCodeFile.write(new Quad(".BYT", String.format("'%s'", underflowString.charAt(i)), null, null, null, null).toString());
        }
        targetCodeFile.write(new Quad(".BYT", "'\\n'", null, null, null, null).toString());
    }

    private static void doGlobals() {
        targetCodeFile.write("");

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
            targetCodeFile.write(new Quad(operand, entry.value, null, null, entry.symid, "Global data").toString());
        }
    }

    public static void produceTargetCode() {
        fakeAMainCall();
        for(Quad q : Generator.quads)
        {
            handleQuad(q);
        }
        doOverflowUnderflow();
        doGlobals();
    }

    private static void handleQuad(Quad q) {
        if("FUNC".equals(q.operator))
        {
            String label = q.label;
            int tempVars = symbolTable.getTotalTempVars(q.operand1); // operand 1 should be the func symid
            targetCodeFile.write("");
            targetCodeFile.write(new Quad("ADI", TOS, String.valueOf(4 * tempVars), null, label, q.comment).toString());
            targetCodeFile.write(new Quad("MOV", "R5", TOS, null, null, q.comment).toString());
            targetCodeFile.write(new Quad("CMP", "R5", SL, null, null, q.comment).toString());
            targetCodeFile.write(new Quad("BLT", "R5", "OVERFLOW", null, null, q.comment).toString());
        }
        else if("MOV".equals(q.operator))
        {
            loadData(q);
            targetCodeFile.write(new Quad("MOV", "R3", null, null, null, q.comment).toString()); // R1 -> R3
            storeData(q);
        }
        else if("ADD".equals(q.operator)
                || "SUB".equals(q.operator)
                || "MUL".equals(q.operator)
                || "DIV".equals(q.operator))
        {
            loadData(q);
            targetCodeFile.write(new Quad(q.operator, "R1", "R2", null, null, q.comment + " - Do op").toString()); // R1 op R2 -> R1
            targetCodeFile.write(new Quad("MOV", "R3", "R1", null, null, q.comment + " - Copy result to R3").toString()); // R1 -> R3
            storeData(q);
        }
        else if("ADI".equals(q.operator))
        {
            loadData(q);
            targetCodeFile.write(new Quad(q.operator, "R1", q.operand2, null, null, q.comment + " - Add immediate " + q.operand2).toString()); // R1 op R2 -> R1
            targetCodeFile.write(new Quad("MOV", "R3", "R1", null, null, q.comment + " - Copy result to R3").toString()); // R1 -> R3
            storeData(q);
        }
        else if("WRITE".equals(q.operator)) {
            loadData(q);
            targetCodeFile.write(new Quad("MOV", "R0", null, "R1", null, q.comment + " - Load value for trap").toString()); // R1 -> R0
            targetCodeFile.write(new Quad("TRP", q.operand2, null, null, null, q.comment + " - Trap").toString()); // operand2 is trap number
        }
        else if("RTN".equals(q.operator)) {
            doReturn(q);
        }

//        new Quad(s, s, s, null, label, q.comment);
    }

    /**
     * Puts the first source op in R2
     * Puts the second source op in R3, if applicable
     * @param q quad to load data for
     */
    private static void loadData(Quad q) {
        String label = q.label;
        String loadOp;
        String datatype;

//        if("RTN".equals(q.operator)) // Special case: we want the value put into R3 where dest operands go
//        {
//            if(q.operand1 != null) {
//                SymbolTableEntry entry = symbolTable.get(q.operand1);
//                if(SymbolTableEntryType.PARAM.equals(entry.kind)
//                        || SymbolTableEntryType.LOCAL_VAR.equals(entry.kind)
//                        || SymbolTableEntryType.TEMP_VAR.equals(entry.kind)) {
//                    targetCodeFile.write(new Quad("MOV", "R3", null, FP, label, q.comment + " - Put FP in R3").toString());
//                    targetCodeFile.write(new Quad("ADI", "R1", null, String.valueOf(symbolTable.getOffset(entry)), null, q.comment + " - FP + -offset").toString());
//                    targetCodeFile.write(new Quad("LDR", "R1", null, "(R1)", null, q.comment + " - Load stack var, R3").toString()); // Load the value into R1
//
//                }
//                else if(SymbolTableEntryType.GLOBAL_LITERAL.equals(entry.kind)) {
//                    targetCodeFile.write(new Quad("LDR", "R1", null, q.operand1, null, q.comment + " - Load global label, R1").toString()); // Load label of literal into R1
//
//                    return "char".equals(entry.data.get("type"))
//                            ? DataHandling.CHAR
//                            : DataHandling.INT;
//                }
//                return;
//            }
//        }
        boolean skipOperand1 = "READ".equals(q.operator);
        boolean skipOperand2 = "WRITE".equals(q.operator) || "ADI".equals(q.operator) || q.operand2 == null;

        SymbolTableEntry entry;
        if(!skipOperand1) {
            entry = symbolTable.get(q.operand1);
            datatype = entry.data.get("type");
            if(SymbolTableEntryType.PARAM.equals(entry.kind)
                    || SymbolTableEntryType.LOCAL_VAR.equals(entry.kind)
                    || SymbolTableEntryType.TEMP_VAR.equals(entry.kind)) {
                if("int".equals(datatype)) {
                    loadOp = "LDR";
                }
                else if("char".equals(datatype) || "bool".equals(datatype)) {
                    loadOp = "LDB";
                }
                else {
                    loadOp = "LDR"; // todo: this will probably change
                }

                targetCodeFile.write(new Quad("MOV", "R1", null, FP, label, q.comment + " - Put FP in R1").toString());
                targetCodeFile.write(new Quad("ADI", "R1", null, String.valueOf(symbolTable.getOffset(entry)), null, q.comment + " - FP + -offset").toString());
                targetCodeFile.write(new Quad(loadOp, "R1", null, "(R1)", null, q.comment + " - Load stack var, R1").toString()); // Load the value into R1
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
                targetCodeFile.write(new Quad(loadOp, "R1", null, q.operand1, null, q.comment + " - Load global label, R1").toString()); // Load label of literal into R1
            }
        }

        if(!skipOperand2) {
            entry = symbolTable.get(q.operand2);
            datatype = entry.data.get("type");
            if(SymbolTableEntryType.PARAM.equals(entry.kind)
                    || SymbolTableEntryType.LOCAL_VAR.equals(entry.kind)
                    || SymbolTableEntryType.TEMP_VAR.equals(entry.kind)) {
                if("int".equals(datatype)) {
                    loadOp = "LDR";
                }
                else if("char".equals(datatype) || "bool".equals(datatype)) {
                    loadOp = "LDB";
                }
                else {
                    loadOp = "LDR"; // todo: this will probably change
                }

                targetCodeFile.write(new Quad("MOV", "R2", null, FP, null, q.comment + " - Put FP in R1").toString()); // Put FP in R2
                targetCodeFile.write(new Quad("ADI", "R2", null, String.valueOf(symbolTable.getOffset(entry)), null, q.comment + " - FP + -offset").toString()); // FP + -offset
                targetCodeFile.write(new Quad(loadOp, "R2", null, "(R2)", null, q.comment + " - Load stack var, R2").toString()); // Load the value into R2
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

                targetCodeFile.write(new Quad(loadOp, "R2", null, q.operand2, null, q.comment + " - Load global label, R2").toString()); // Load label of literal into R2
            }
        }

        // operand3 is destination, so there won't be a need to load that one
//        targetCodeFile.write(new Quad("LD(R)", "<SOMETHING_I_DONT_KNOW_YET_SRC_1>", null, "R1", label, q.comment).toString());
//
//        if(false) // obviously not known yet
//        {
//            targetCodeFile.write(new Quad("LD(R)", "<SOMETHING_I_DONT_KNOW_YET_SRC_2>", null, "R2", label, q.comment).toString());
//        }
    }

    /**
     * Expects result in R3, and writes to memory
     * @param q the quad with instructions
     */
    private static void storeData(Quad q) {
        SymbolTableEntry entry = symbolTable.get(q.operand3);
        if(SymbolTableEntryType.PARAM.equals(entry.kind)
                || SymbolTableEntryType.LOCAL_VAR.equals(entry.kind)
                || SymbolTableEntryType.TEMP_VAR.equals(entry.kind)) {
            targetCodeFile.write(new Quad("MOV", "R1", null, FP, null, q.comment).toString()); // Put FP in R1
            targetCodeFile.write(new Quad("ADI", "R1", null, String.valueOf(symbolTable.getOffset(entry)), null, q.comment).toString()); // FP + -offset
            targetCodeFile.write(new Quad("STR", "R3", null, "(R1)", null, q.comment).toString()); // Store value in R3 to address in R1
        }
        else {
            targetCodeFile.write(new Quad("ST(R)", "R3", null, "<SOMETHING_I_DONT_KNOW_YET_SRC_1>", null, q.comment).toString());
        }
    }

    private static enum DataHandling {
        CHAR,
        INT
    }
}
