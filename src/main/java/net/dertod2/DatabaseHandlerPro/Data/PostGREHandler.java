package net.dertod2.DatabaseHandlerPro.Data;

import com.google.common.collect.ImmutableList;
import net.dertod2.DatabaseHandlerPro.Database.DriverDatabase;
import net.dertod2.DatabaseHandlerPro.Database.Pooler.PooledConnection;
import net.dertod2.DatabaseHandlerPro.Database.PostGREDatabase;
import net.dertod2.DatabaseHandlerPro.Exceptions.NoTableColumnException;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PostGREHandler
        extends DriverHandler {
    public PostGREHandler(PostGREDatabase postGREDatabase) {
        super((DriverDatabase) postGREDatabase);
    }

    public void insert(TableRow tableRow) throws SQLException, IllegalArgumentException, IllegalAccessException, IOException {
        Connection connection = this.abstractDatabase.getConnection();
        if (!copyInsert((List<TableRow>) ImmutableList.of(tableRow), connection)) {
            insert(tableRow, connection);
        }

        closeConnection(connection, null, null);
    }

    public <T extends TableRow> void insert(List<T> entries) throws SQLException, IllegalArgumentException, IllegalAccessException, IOException {
        if (entries.size() <= 0)
            return;
        Connection connection = this.abstractDatabase.getConnection();
        if (!copyInsert(entries, connection)) {
            for (TableRow tableRow : entries) insert(tableRow, connection);

        }
        closeConnection(connection, null, null);
    }

    public void insert(TableRow tableRow, Connection connection) throws IllegalArgumentException, IllegalAccessException, SQLException, IOException {
        TableCache tableCache = TableCache.getCache(tableRow.getClass(), this);


        ResultSet resultSet = null;

        Map<Column, Object> dataList = tableRow.getColumns();
        Iterator<Column> iterator = dataList.keySet().iterator();

        StringBuilder columnList = new StringBuilder();
        StringBuilder valueList = new StringBuilder();

        while (iterator.hasNext()) {
            Column column = iterator.next();
            if (column.autoIncrement() || column.columnType() == Column.ColumnType.Primary) {
                continue;
            }
            columnList.append(tableCache.getName(column)).append(", ");
            valueList.append("?, ");
        }


        if (columnList.length() > 0) columnList.delete(columnList.length() - 2, columnList.length());
        if (valueList.length() > 0) valueList.delete(valueList.length() - 2, valueList.length());

        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO " + tableCache.getTable() + " (" + columnList + ") VALUES (" + valueList + ");", 1);

        iterator = dataList.keySet().iterator();

        int index = 1;
        while (iterator.hasNext()) {
            Column column = iterator.next();
            if (column.autoIncrement() || column.columnType() == Column.ColumnType.Primary)
                continue;
            Object columnValue = dataList.get(column);

            set(index++, preparedStatement, columnValue, tableCache.getType(column));
        }

        preparedStatement.executeUpdate();

        boolean hasPrimaryKey = tableCache.hasPrimaryKey();
        Column primaryKey = tableCache.getPrimaryKey();

        if (hasPrimaryKey) {
            resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                tableRow.setColumn(primaryKey, Integer.valueOf(resultSet.getInt(tableCache.getName(primaryKey))));
            }
        }

        tableRow.isLoaded = true;
        closeConnection(null, preparedStatement, resultSet);
    }

    private <T extends TableRow> boolean copyInsert(List<T> entries, Connection connection) {
        try {
            CopyManager copyManager = ((PGConnection) ((PooledConnection) connection).getRawConnection()).getCopyAPI();
            StringBuilder stringBuilder = new StringBuilder();
            int batchSize = 75;


            Map<String, List<TableRow>> tableEntryList = new HashMap<>();
            TableCache tableCache = TableCache.getCache(entries.get(0).getClass(), this);

            for (TableRow tableRow : entries) {
                if (tableCache.hasPrimaryKey()) return false;

                if (!tableEntryList.containsKey(tableCache.getTable()))
                    tableEntryList.put(tableCache.getTable(), new ArrayList<>());
                tableEntryList.get(tableCache.getTable()).add(tableRow);
            }

            for (String tableName : tableEntryList.keySet()) {
                PushbackReader pushBackReader = new PushbackReader(new StringReader(""), 10000);
                List<TableRow> tableList = tableEntryList.get(tableName);

                for (int i = 0; i < tableList.size(); i++) {
                    TableRow tableRow = tableList.get(i);
                    List<Column> columnList = tableCache.getLayout();

                    for (Column column : columnList) {
                        Object data = tableRow.getColumn(column);
                        Type type = tableCache.getType(column);

                        IncludedTypes primitiveWrapper = IncludedTypes.getByObject(type.getTypeName());
                        if (primitiveWrapper == IncludedTypes.String) {
                            stringBuilder.append("'").append((String) data).append("',");
                            continue;
                        }
                        if (primitiveWrapper == IncludedTypes.Unknown) {
                            stringBuilder.append("'").append(this.abstractDatabase.getDataType(type.getTypeName()).setResult(data)).append("',");
                            continue;
                        }
                        stringBuilder.append(data).append(",");
                    }


                    stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
                    stringBuilder.append("\n");

                    if (i % 75 == 0) {
                        pushBackReader.unread(stringBuilder.toString().toCharArray());
                        copyManager.copyIn("COPY " + tableName + " FROM STDIN WITH CSV", pushBackReader);
                        stringBuilder.delete(0, stringBuilder.length());
                    }

                    tableRow.isLoaded = true;
                }

                pushBackReader.unread(stringBuilder.toString().toCharArray());
                copyManager.copyIn("COPY " + tableName + " FROM STDIN WITH CSV", pushBackReader);
            }

            return true;
        } catch (SQLException | IOException | IllegalArgumentException | IllegalAccessException sQLException) {


            return false;
        }
    }

    public <T extends TableRow> boolean remove(Class<T> tableRow, LoadHelper loadHelper) throws SQLException, IOException {
        PreparedStatement preparedStatement;
        if (loadHelper == null) loadHelper = new LoadHelper();

        Connection connection = this.abstractDatabase.getConnection();


        TableCache tableCache = TableCache.getCache(tableRow, this);
        String where = loadHelper.buildWhereQueue(this.abstractDatabase.getType());

        if (where.length() > 0) {
            preparedStatement = connection.prepareStatement("DELETE FROM " + tableCache.getTable() + where + ";");
            fillWhereQueue(tableCache, loadHelper, preparedStatement);
        } else {
            preparedStatement = connection.prepareStatement("TRUNCATE TABLE " + tableCache.getTable() + ";");
        }

        preparedStatement.executeUpdate();
        closeConnection(connection, preparedStatement, null);

        return true;
    }

    public boolean update(TableRow tableRow, LoadHelper loadHelper, List<String> rows) throws SQLException, IllegalArgumentException, IllegalAccessException, IOException {
        if (!tableRow.isLoaded) return false;

        if (loadHelper == null) loadHelper = new LoadHelper();

        Connection connection = this.abstractDatabase.getConnection();


        TableCache tableCache = TableCache.getCache(tableRow.getClass(), this);
        Map<Column, Object> columnList = tableRow.getColumns();

        StringBuilder set = new StringBuilder();

        for (Column column : columnList.keySet()) {
            if (column.columnType() == Column.ColumnType.Primary || (
                    rows != null && !rows.isEmpty() && !rows.contains(tableCache.getName(column))))
                continue;
            if (set.length() > 0) {
                set.append(",  ").append(tableCache.getName(column)).append(" = ?");
                continue;
            }
            set.append(tableCache.getName(column)).append(" = ?");
        }


        int index = 1;

        String where = loadHelper.buildWhereQueue(this.abstractDatabase.getType());
        PreparedStatement preparedStatement = connection.prepareStatement("UPDATE " + tableCache.getTable() + " SET " + set + where + ";");

        for (Column column : columnList.keySet()) {
            if (column.columnType() == Column.ColumnType.Primary || (
                    rows != null && !rows.isEmpty() && !rows.contains(tableCache.getName(column))))
                continue;
            Object columnValue = columnList.get(column);
            set(index++, preparedStatement, columnValue, tableCache.getType(column));
        }

        fillWhereQueue(index, tableCache, loadHelper, preparedStatement);

        boolean returnResult = (preparedStatement.executeUpdate() > 0);
        closeConnection(connection, preparedStatement, null);

        return returnResult;
    }

    public boolean update(TableRow tableRow, LoadHelper loadHelper, Map<String, Object> content) throws SQLException, IllegalArgumentException, IllegalAccessException, IOException {
        if (content == null || content.isEmpty())
            throw new NullPointerException("The specificRows argument can't be null");
        if (loadHelper == null) loadHelper = new LoadHelper();

        Connection connection = this.abstractDatabase.getConnection();


        TableCache tableCache = TableCache.getCache(tableRow.getClass(), this);
        StringBuilder set = new StringBuilder();

        for (String columnName : content.keySet()) {
            Column column = tableCache.getColumn(columnName);
            if (column == null) throw new NoTableColumnException(columnName, tableCache);

            if (column.columnType() == Column.ColumnType.Primary)
                continue;
            if (set.length() > 0) {
                set.append(",  ").append(tableCache.getName(column)).append(" = ?");
                continue;
            }
            set.append(tableCache.getName(column)).append(" = ?");
        }


        int index = 1;

        String where = loadHelper.buildWhereQueue(this.abstractDatabase.getType());
        PreparedStatement preparedStatement = connection.prepareStatement("UPDATE " + tableCache.getTable() + " SET " + set + where + ";");

        for (String columnName : content.keySet()) {
            Column column = tableCache.getColumn(columnName);

            if (column.columnType() == Column.ColumnType.Primary)
                continue;
            set(index++, preparedStatement, content.get(columnName), tableCache.getType(column));
        }

        fillWhereQueue(index, tableCache, loadHelper, preparedStatement);

        boolean returnResult = (preparedStatement.executeUpdate() > 0);
        closeConnection(connection, preparedStatement, null);

        return returnResult;
    }

    public <T extends TableRow> List<T> load(Class<T> tableRow, LoadHelper loadHelper) throws IllegalArgumentException, IllegalAccessException, SQLException, InstantiationException, IOException, SecurityException {
        if (loadHelper == null) loadHelper = new LoadHelper();

        TableCache tableCache = TableCache.getCache(tableRow, this);
        Connection connection = this.abstractDatabase.getConnection();

        StringBuilder get = new StringBuilder();
        StringBuilder last = new StringBuilder();

        List<Column> tableLayout = tableCache.getLayout();
        for (Column value : tableLayout) {
            if (get.length() > 0) get.append(", ");
            get.append(tableCache.getName(value));
        }

        if (loadHelper.groupBy.size() > 0) {
            last.append(" GROUP BY ");

            for (String field : loadHelper.groupBy) {
                last.append(field).append(", ");
            }

            last.delete(last.length() - 2, last.length());
        }

        if (loadHelper.columnSorter.size() > 0) {
            last.append(" ORDER BY ");

            for (String field : loadHelper.columnSorter.keySet()) {
                last.append(field).append(" ").append(loadHelper.columnSorter.get(field)).append(", ");
            }

            last.delete(last.length() - 2, last.length());
        }

        if (loadHelper.limit > 0) last.append(" LIMIT ").append(loadHelper.limit);
        if (loadHelper.offset > 0) last.append(" OFFSET ").append(loadHelper.offset);

        String where = loadHelper.buildWhereQueue(this.abstractDatabase.getType());
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT " + get + " FROM " + tableCache.getTable() + where + last + ";");
        fillWhereQueue(tableCache, loadHelper, preparedStatement);

        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet == null) return new ArrayList<>();
        List<T> results = new ArrayList<>();

        while (resultSet.next()) {

            TableRow tableRow1 = tableRow.newInstance();

            for (Column column : tableLayout) {
                tableRow1.setColumn(column, get(resultSet, column, tableCache.getType(column)));
            }

            tableRow1.isLoaded = true;
            results.add((T) tableRow1);
        }

        closeConnection(connection, preparedStatement, resultSet);

        return results;
    }


    public <T extends TableRow> long count(Class<T> tableRow, LoadHelper loadHelper) throws SQLException, IllegalArgumentException, IOException {
        Connection connection = this.abstractDatabase.getConnection();


        long result = 0L;

        TableCache tableCache = TableCache.getCache(tableRow, this);

        String where = loadHelper.buildWhereQueue(this.abstractDatabase.getType());
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT COUNT(*) AS elements FROM " + tableCache.getTable() + where + ";");

        fillWhereQueue(tableCache, loadHelper, preparedStatement);

        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            result = resultSet.getLong("elements");
        }

        closeConnection(connection, preparedStatement, resultSet);
        return result;
    }

    public boolean exist(TableRow tableRow) throws SQLException, IllegalArgumentException, IllegalAccessException, IOException {
        PreparedStatement preparedStatement;
        ResultSet resultSet;
        boolean returnResult;
        Connection connection = this.abstractDatabase.getConnection();


        TableCache tableCache = TableCache.getCache(tableRow.getClass(), this);

        if (tableCache.hasPrimaryKey()) {
            Column column = tableCache.getPrimaryKey();
            Object primaryKey = tableRow.getColumn(column);

            preparedStatement = connection.prepareStatement("SELECT * FROM " + tableCache.getTable() + " WHERE " + tableCache.getName(column) + " = ?;");
            set(1, preparedStatement, primaryKey, tableCache.getType(column));

            resultSet = preparedStatement.executeQuery();
            returnResult = resultSet.next();
        } else {
            Map<Column, Object> columnList = tableRow.getColumns();
            Iterator<Column> iterator = columnList.keySet().iterator();

            StringBuilder stringBuilder = new StringBuilder();

            while (iterator.hasNext()) {
                Column column = iterator.next();
                if (columnList.get(column) == null)
                    continue;
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(" AND ").append(tableCache.getName(column));
                } else {
                    stringBuilder.append(tableCache.getName(column));
                }

                stringBuilder.append(" = ?");
            }

            preparedStatement = connection.prepareStatement("SELECT * FROM " + tableCache.getTable() + " WHERE " + stringBuilder + ";");
            iterator = columnList.keySet().iterator();

            int index = 1;
            while (iterator.hasNext()) {
                Column column = iterator.next();
                Object value = columnList.get(column);
                if (value == null)
                    continue;
                set(index++, preparedStatement, value, tableCache.getType(column));
            }

            resultSet = preparedStatement.executeQuery();
            returnResult = resultSet.next();
        }

        closeConnection(connection, preparedStatement, resultSet);
        return returnResult;
    }

    protected void createTable(TableCache tableCache) throws SQLException {
        Connection connection = this.abstractDatabase.getConnection();


        StringBuilder columnBuilder = new StringBuilder();
        List<Column> columnList = tableCache.getLayout();
        Iterator<Column> iterator = columnList.iterator();

        while (iterator.hasNext()) {
            Column column = iterator.next();

            columnBuilder.append(tableCache.getName(column)).append(" ");
            columnBuilder.append((column.autoIncrement() || column.columnType() == Column.ColumnType.Primary) ? "BIGSERIAL" : toDatabaseType(tableCache.getType(column)));
            if (column.columnType() != Column.ColumnType.Normal) {
                columnBuilder.append((column.columnType() == Column.ColumnType.Primary) ? " PRIMARY KEY" : " UNIQUE");
            }
            if (iterator.hasNext()) columnBuilder.append(", ");

        }
        PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE " + tableCache.getTable() + " (" + columnBuilder + ");");
        preparedStatement.execute();

        closeConnection(connection, preparedStatement, null);
    }

    protected void addColumn(TableCache tableCache, Column column) throws SQLException {
        Connection connection = this.abstractDatabase.getConnection();

        PreparedStatement preparedStatement = connection.prepareStatement("ALTER TABLE " + tableCache.getTable() + " ADD " + tableCache.getName(column) + " " + toDatabaseType(tableCache.getType(column)) + ((column.columnType() == Column.ColumnType.Unique) ? " UNIQUE" : "") + ";");
        preparedStatement.execute();

        closeConnection(connection, preparedStatement, null);
    }

    protected void delColumn(TableCache tableCache, String columnName) throws SQLException {
        Connection connection = this.abstractDatabase.getConnection();

        PreparedStatement preparedStatement = connection.prepareStatement("ALTER TABLE " + tableCache.getTable() + " DROP " + columnName + ";");
        preparedStatement.execute();

        closeConnection(connection, preparedStatement, null);
    }

    String toDatabaseType(Type type) {
        switch (IncludedTypes.getByObject(type.getTypeName())) {
            default:
                throw new IncompatibleClassChangeError();
            case Boolean:
            case Byte:
            case Short:
            case Int:
            case Char:
            case Unknown:
            case String:
            case Double:
            case Float:
            case Long:
            case Timestamp:
                break;
        }

        return "TIMESTAMP WITHOUT TIME ZONE";
    }

}
