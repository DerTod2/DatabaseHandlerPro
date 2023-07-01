package net.dertod2.DatabaseHandlerPro.Exceptions;

import net.dertod2.DatabaseHandlerPro.Data.LoadHelper;

import java.io.Serial;

public class FilterNotAllowedException
        extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -208305598190317378L;
    private final String method;
    private final LoadHelper.Filter filter;

    public FilterNotAllowedException(String method, LoadHelper.Filter filter) {
        this.method = method;
        this.filter = filter;
    }

    public String getMessage() {
        return "The Filter '" + this.filter.name() + "' is not allowed for the method '" + this.method + "'";
    }

}
