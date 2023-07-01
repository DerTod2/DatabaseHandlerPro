package net.dertod2.DatabaseHandlerPro.Data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.dertod2.DatabaseHandlerPro.Exceptions.MultiplePrimaryKeysException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.*;

public class TableCache {
    private static final Map<String, TableCache> cache = new HashMap<>();

    private final String className;
    private final List<Column> uniques;
    private final Map<Column, Field> fields;
    private final Map<String, Column> columns;
    private final Map<Column, Type> types;
    private final Map<Column, String> names;
    private final List<Column> layout;
    private final Map<UUID, Boolean> initializes;
    private String tableName;
    private Constructor<? extends TableRow> constructor;
    private Column primary;

    public TableCache(Class<? extends TableRow> clazz) {
        this.className = clazz.getName();
        this.tableName = clazz.getAnnotation(TableInfo.class).name();

        if (this.tableName.length() == 0) this.tableName = clazz.getSimpleName().toLowerCase();

        try {
            this.constructor = clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException exc) {
            System.err.println("The class '" + clazz.getSimpleName() + "' needs an empty constructor to allow all database operations for DatabaseHandler!");
            exc.printStackTrace();
            System.exit(1);
        }

        List<Column> uniques = new ArrayList<>();

        Map<Column, Field> fields = new HashMap<>();
        Map<String, Column> columns = new HashMap<>();
        Map<Column, Type> types = new HashMap<>();
        Map<Column, String> names = new HashMap<>();

        List<Field> classFields = getAllDeclaredFields(new ArrayList<>(), clazz);
        for (Field field : classFields) {
            Column column = field.getAnnotation(Column.class);
            if (column == null)
                continue;
            field.setAccessible(true);

            String name = column.name();
            if (name.length() == 0) name = field.getName().toLowerCase();

            fields.put(column, field);
            columns.put(name, column);
            types.put(column, field.getGenericType());
            names.put(column, name);

            switch (column.columnType()) {


                case Primary:
                    if (this.primary != null) throw new MultiplePrimaryKeysException(clazz.getName());
                    this.primary = column;

                case Unique:
                    uniques.add(column);
            }


        }
        List<Column> layout = new ArrayList<>(fields.keySet());
        layout.sort(new ColumnClassSorter());

        this.uniques = ImmutableList.copyOf(uniques);

        this.fields = ImmutableMap.copyOf(fields);
        this.columns = ImmutableMap.copyOf(columns);
        this.types = ImmutableMap.copyOf(types);
        this.names = ImmutableMap.copyOf(names);

        this.layout = ImmutableList.copyOf(layout);

        this.initializes = new HashMap<>();
    }

    public static TableCache getCache(Class<? extends TableRow> clazz) {
        return getCache(clazz, null);
    }

    public static TableCache getCache(Class<? extends TableRow> clazz, Handler handler) {
        if (!cache.containsKey(clazz.getName())) cache.put(clazz.getName(), new TableCache(clazz));

        TableCache tableCache = cache.get(clazz.getName());
        if (handler != null && !tableCache.initializes.containsKey(handler.handlerUniqueId)) {
            try {
                handler.updateTable(tableCache);
            } catch (SQLException exc) {
                exc.printStackTrace();
            }
            tableCache.initializes.put(handler.handlerUniqueId, Boolean.TRUE);
        }

        return tableCache;
    }

    public static List<Field> getAllDeclaredFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllDeclaredFields(fields, type.getSuperclass());
        }

        return fields;
    }


    public String getName() {
        return this.className;
    }


    public String getTable() {
        return this.tableName;
    }


    public <T extends TableRow> TableRow createClass() {
        try {
            return this.constructor.newInstance();
        } catch (Exception exc) {
            return null;
        }
    }


    public boolean hasPrimaryKey() {
        return (this.primary != null);
    }


    public boolean hasUniqueKeys() {
        return (this.uniques.size() > 0);
    }

    public boolean hasColumn(String columnName) {
        return this.columns.containsKey(columnName);
    }

    public Column getPrimaryKey() {
        return this.primary;
    }

    public List<Column> getUniqueKeys() {
        return this.uniques;
    }

    public Map<Column, Field> getColumnFields() {
        return this.fields;
    }

    public Map<String, Column> getStringColumns() {
        return this.columns;
    }

    public List<Column> getLayout() {
        return this.layout;
    }

    public Type getType(Column column) {
        return this.types.get(column);
    }

    public Field getField(Column column) {
        return this.fields.get(column);
    }

    public Column getColumn(String columnName) {
        return this.columns.get(columnName);
    }

    public String getName(Column column) {
        return this.names.get(column);
    }

    public static class ColumnClassSorter
            implements Comparator<Column> {
        public int compare(Column o1, Column o2) {
            if (o1.order() != -1 && o2.order() != -1) {
                if (o1.order() > o2.order()) return 1;
                if (o1.order() < o2.order()) return -1;

            }
            return 0;
        }
    }

}
