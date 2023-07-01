package net.dertod2.DatabaseHandlerPro.Exceptions;

import java.io.Serial;

public class MultiplePrimaryKeysException
        extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 5639979434567450015L;
    private final String className;

    public MultiplePrimaryKeysException(String className) {
        /* 12 */
        this.className = className;
        /*    */
    }

    public String getMessage() {
        return "There are multiple primary keys in class '" + this.className + "' when only one primary key is allowed!";
    }

}