package net.dertod2.DatabaseHandlerPro.Data;

import net.dertod2.DatabaseHandlerPro.Database.DriverDatabase;
import net.dertod2.DatabaseHandlerPro.Exceptions.NoTableColumnException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class CombinedHandler extends DriverHandler {
    public CombinedHandler(DriverDatabase driverDatabase) {
        super(driverDatabase);
    }

    public void insert(TableRow tableRow) throws IllegalArgumentException, IllegalAccessException, SQLException, IOException {
        Connection connection = this.abstractDatabase.getConnection();
        insert(tableRow, connection);
        closeConnection(connection, null, null);
    }

    public <T extends TableRow> void insert(List<T> entries) throws IllegalArgumentException, IllegalAccessException, SQLException, IOException {
        if (entries.size() <= 0)
            return;
        Connection connection = this.abstractDatabase.getConnection();
        for (TableRow tableRow : entries) insert(tableRow, connection);
        closeConnection(connection, null, null);
    }

    protected void insert(TableRow tableRow, Connection connection) throws IllegalArgumentException, IllegalAccessException, SQLException, IOException {
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
            columnList.append("`").append(tableCache.getName(column)).append("`, ");
            valueList.append("?, ");
        }

        if (columnList.length() > 0) columnList.delete(columnList.length() - 2, columnList.length());
        if (valueList.length() > 0) valueList.delete(valueList.length() - 2, valueList.length());

        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `" + tableCache.getTable() + "` (" + columnList + ") VALUES (" + valueList + ");", 1);

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
            if (resultSet.next()) tableRow.setColumn(primaryKey, Integer.valueOf(resultSet.getInt(1)));

        }
        tableRow.isLoaded = true;
        closeConnection(null, preparedStatement, resultSet);
    }

    public <T extends TableRow> boolean remove(Class<T> tableRow, LoadHelper loadHelper) throws SQLException, IOException {
        PreparedStatement preparedStatement;
        if (loadHelper == null) loadHelper = new LoadHelper();

        Connection connection = this.abstractDatabase.getConnection();


        TableCache tableCache = TableCache.getCache(tableRow, this);
        String where = loadHelper.buildWhereQueue(this.abstractDatabase.getType());

        if (where.length() > 0) {
            preparedStatement = connection.prepareStatement("DELETE FROM `" + tableCache.getTable() + "`" + where + ";");
            fillWhereQueue(tableCache, loadHelper, preparedStatement);
        } else {
            preparedStatement = connection.prepareStatement("TRUNCATE TABLE `" + tableCache.getTable() + "`;");
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
            if (column.columnType() == Column.ColumnType.Primary || column.autoIncrement() || (
                    rows != null && !rows.isEmpty() && !rows.contains(tableCache.getName(column))))
                continue;
            if (set.length() > 0) {
                set.append(", `").append(tableCache.getName(column)).append("` = ?");
                continue;
            }
            set.append("`").append(tableCache.getName(column)).append("` = ?");
        }


        int index = 1;

        String where = loadHelper.buildWhereQueue(this.abstractDatabase.getType());
        System.out.println("UPDATE `" + tableCache.getTable() + "` SET " + set + where + ";");
        PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `" + tableCache.getTable() + "` SET " + set + where + ";");

        for (Column column : columnList.keySet()) {
            if (column.columnType() == Column.ColumnType.Primary || column.autoIncrement() || (
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

            if (column.columnType() == Column.ColumnType.Primary || column.autoIncrement())
                continue;
            if (set.length() > 0) {
                set.append(",  `").append(tableCache.getName(column)).append("` = ?");
                continue;
            }
            set.append("`").append(tableCache.getName(column)).append("` = ?");
        }


        int index = 1;

        String where = loadHelper.buildWhereQueue(this.abstractDatabase.getType());
        PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `" + tableCache.getTable() + "` SET " + set + where + ";");

        for (String columnName : content.keySet()) {
            Column column = tableCache.getColumn(columnName);

            if (column.columnType() == Column.ColumnType.Primary || column.autoIncrement())
                continue;
            set(index++, preparedStatement, content.get(columnName), tableCache.getType(column));
        }

        fillWhereQueue(index, tableCache, loadHelper, preparedStatement);

        boolean returnResult = (preparedStatement.executeUpdate() > 0);
        closeConnection(connection, preparedStatement, null);

        return returnResult;
    }

    public <T extends TableRow> List<T> load(Class<T> tableRow, LoadHelper loadHelper) throws SQLException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException {
        if (loadHelper == null) loadHelper = new LoadHelper();

        TableCache tableCache = TableCache.getCache(tableRow, this);
        Connection connection = this.abstractDatabase.getConnection();

        StringBuilder get = new StringBuilder();
        StringBuilder last = new StringBuilder();

        List<Column> tableLayout = tableCache.getLayout();
        for (Column value : tableLayout) {
            if (get.length() > 0) get.append(", ");
            get.append("`").append(tableCache.getName(value)).append("`");
        }

        if (loadHelper.groupBy.size() > 0) {
            last.append(" GROUP BY ");

            for (String field : loadHelper.groupBy) {
                last.append("`").append(field).append("`, ");
            }

            last.delete(last.length() - 2, last.length());
        }

        if (loadHelper.columnSorter.size() > 0) {
            last.append(" ORDER BY ");

            for (String field : loadHelper.columnSorter.keySet()) {
                last.append("`").append(field).append("` ").append(loadHelper.columnSorter.get(field)).append(", ");
            }

            last.delete(last.length() - 2, last.length());
        }

        if (loadHelper.limit > 0) last.append(" LIMIT ").append(loadHelper.limit);
        if (loadHelper.offset > 0) last.append(" OFFSET ").append(loadHelper.offset);

        String where = loadHelper.buildWhereQueue(this.abstractDatabase.getType());
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT " + get + " FROM `" + tableCache.getTable() + "`" + where + last + ";");
        fillWhereQueue(tableCache, loadHelper, preparedStatement);

        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet == null) return new ArrayList<>();
        List<T> results = new ArrayList<>();

        while (resultSet.next()) {
            T tableRow1 = tableRow.newInstance();

            for (Column column : tableLayout) {
                tableRow1.setColumn(column, get(resultSet, column, tableCache.getType(column)));
            }

            tableRow1.isLoaded = true;
            results.add(tableRow1);
        }

        closeConnection(connection, preparedStatement, resultSet);

        return results;
    }

    public <T extends TableRow> long count(Class<T> tableRow, LoadHelper loadHelper) throws SQLException, IllegalArgumentException, IOException {
        Connection connection = this.abstractDatabase.getConnection();
        long result = 0L;

        TableCache tableCache = TableCache.getCache(tableRow, this);

        String where = loadHelper.buildWhereQueue(this.abstractDatabase.getType());
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT COUNT(*) AS elements FROM `" + tableCache.getTable() + "`" + where + ";");

        fillWhereQueue(tableCache, loadHelper, preparedStatement);

        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            result = resultSet.getLong("elements");
        }

        closeConnection(connection, preparedStatement, resultSet);

        return result;
    }

    void addColumn(TableCache tableCache, Column column) throws SQLException {
        Connection connection = this.abstractDatabase.getConnection();

        List<Column> layout = tableCache.getLayout();

        String whereToAdd = (column.order() == -1) ? "" : ((column.order() == 1 && layout.size() > 1) ? (" BEFORE `" + tableCache.getName(layout.get(column.order() + 1)) + "`") : ((column.order() > 1 && layout.size() >= column.order()) ? (" AFTER `" + tableCache.getName(layout.get(column.order() - 1)) + "`") : ""));

        PreparedStatement preparedStatement = connection.prepareStatement("ALTER TABLE `" + tableCache.getTable() + "` ADD `" + tableCache.getName(column) + "` " + toDatabaseType(tableCache.getType(column)) + ((column.columnType() == Column.ColumnType.Unique) ? " UNIQUE" : "") + whereToAdd + ";");
        preparedStatement.execute();

        closeConnection(connection, preparedStatement, null);
    }

    void delColumn(TableCache tableCache, String columnName) throws SQLException {
        Connection connection = this.abstractDatabase.getConnection();

        PreparedStatement preparedStatement = connection.prepareStatement("ALTER TABLE `" + tableCache.getTable() + "` DROP `" + columnName + "`;");
        preparedStatement.execute();

        closeConnection(connection, preparedStatement, null);
    }

}
