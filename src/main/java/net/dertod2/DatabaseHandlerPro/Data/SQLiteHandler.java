package net.dertod2.DatabaseHandlerPro.Data;

import net.dertod2.DatabaseHandlerPro.Database.DriverDatabase;
import net.dertod2.DatabaseHandlerPro.Database.SQLiteDatabase;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SQLiteHandler
        extends CombinedHandler {
    public SQLiteHandler(SQLiteDatabase sqLiteDatabase) {
        super((DriverDatabase) sqLiteDatabase);
    }

    public boolean exist(TableRow tableRow) throws SQLException, IllegalArgumentException, IllegalAccessException, IOException {
        PreparedStatement preparedStatement;
        Connection connection = this.abstractDatabase.getConnection();


        TableCache tableCache = TableCache.getCache(tableRow.getClass(), this);

        if (tableCache.hasPrimaryKey()) {
            Column column = tableCache.getPrimaryKey();
            Object primaryKey = tableRow.getColumn(column);

            preparedStatement = connection.prepareStatement("SELECT * FROM `" + tableCache.getTable() + "` WHERE " + tableCache.getName(column) + " = ?;");
            set(1, preparedStatement, primaryKey, tableCache.getType(column));
        } else {

            Map<Column, Object> columnList = tableRow.getColumns();
            Iterator<Column> iterator = columnList.keySet().iterator();

            StringBuilder stringBuilder = new StringBuilder();

            while (iterator.hasNext()) {
                Column column = iterator.next();
                if (columnList.get(column) == null)
                    continue;
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(" AND `").append(tableCache.getName(column)).append("`");
                } else {
                    stringBuilder.append("`").append(tableCache.getName(column)).append("`");
                }

                stringBuilder.append(" = ?");
            }

            preparedStatement = connection.prepareStatement("SELECT * FROM `" + tableCache.getTable() + "` WHERE " + stringBuilder + ";");
            iterator = columnList.keySet().iterator();

            int index = 1;
            while (iterator.hasNext()) {
                Column column = iterator.next();
                Object value = columnList.get(column);
                if (value == null)
                    continue;
                set(index++, preparedStatement, value, tableCache.getType(column));
            }
        }


        ResultSet resultSet = preparedStatement.executeQuery();
        boolean returnResult = resultSet.next();

        closeConnection(connection, preparedStatement, resultSet);
        return returnResult;
    }

    void createTable(TableCache tableCache) throws SQLException {
        Connection connection = this.abstractDatabase.getConnection();


        StringBuilder columnBuilder = new StringBuilder();
        List<Column> columnList = tableCache.getLayout();
        Iterator<Column> iterator = columnList.iterator();

        while (iterator.hasNext()) {
            Column column = iterator.next();

            columnBuilder.append("`").append(tableCache.getName(column)).append("` ");
            columnBuilder.append(toDatabaseType(tableCache.getType(column)));
            if (column.columnType() != Column.ColumnType.Normal) {
                columnBuilder.append((column.columnType() == Column.ColumnType.Primary) ? " PRIMARY KEY AUTOINCREMENT" : " UNIQUE");
            }
            if (iterator.hasNext()) columnBuilder.append(", ");

        }
        PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `" + tableCache.getTable() + "` (" + columnBuilder + ");");
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
            case Long:
            case Char:
            case Unknown:
            case String:
            case Double:
            case Float:
            case Timestamp:
                break;
        }

        return "TIMESTAMP";
    }

}

