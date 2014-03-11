package syntax.symbol;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 2/11/14
 * Time: 9:08 AM
 * To change this template use File | Settings | File Templates.
 */
public class DuplicateSymbolException extends Exception {
    public static SymbolTable symbolTable = SymbolTable.get();

    public DuplicateSymbolException(String identifier, String scope) {
        super(new StringBuilder("Duplicate identifier ")
                .append(identifier)
                .append(" in scope ")
                .append(scope)
                .toString()
        );
    }
}
