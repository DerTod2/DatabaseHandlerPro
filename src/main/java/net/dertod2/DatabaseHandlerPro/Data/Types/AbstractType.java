package net.dertod2.DatabaseHandlerPro.Data.Types;

import net.dertod2.DatabaseHandlerPro.Database.AbstractDatabase;

import java.lang.reflect.Type;

public abstract class AbstractType {
    private final String[] classPath;
    protected AbstractDatabase abstractDatabase;

    public AbstractType(String... classPath) {
        this.classPath = classPath;
    }

    public String[] getClassPath() {
        return this.classPath;
    }

    public void database(AbstractDatabase abstractDatabase) {
        this.abstractDatabase = abstractDatabase;
    }

    /**
     * Should convert the object value to an object of the database type equivalent to be saved into the Table
     */
    public abstract String setResult(Object paramObject);

    /**
     * Returns the string representation of this Object to an new instance of this object<br />
     * The generic type is for multi-dimensional implementations
     */
    public abstract Object getResult(String paramString, Type[] paramArrayOfType);
}
