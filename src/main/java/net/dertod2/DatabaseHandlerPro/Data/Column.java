package net.dertod2.DatabaseHandlerPro.Data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Column {
    String name();

    ColumnType columnType() default ColumnType.Normal;

    boolean autoIncrement() default false;

    /**
     * Set this to -1 to ignore the order and let the database decide<br />
     * Use 1 and higher to declare a sort order, never use the 0!
     */
    int order() default -1;

    enum ColumnType {
        Normal,
        Unique,
        Primary
    }

}
