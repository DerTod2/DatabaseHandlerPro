package net.dertod2.DatabaseHandlerPro.Exceptions;

import java.io.Serial;

public class UnhandledDataTypeException
        extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -208305598190317378L;
    private final Class<?> dataType;

    public UnhandledDataTypeException(Class<?> dataType) {
        this.dataType = dataType;
    }
    public String getMessage() {
        return "The DataType " + this.dataType.getName() + " can not be handled by the Database driver";
    }

}
