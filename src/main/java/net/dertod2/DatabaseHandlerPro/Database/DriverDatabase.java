package net.dertod2.DatabaseHandlerPro.Database;

import net.dertod2.DatabaseHandlerPro.Data.DriverHandler;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class DriverDatabase
        extends AbstractDatabase {
    public DriverDatabase(String host, Integer port, String database, String username, String password) {
        super(host, port, database, username, password);

        try {
            Class.forName(getType().getDriverPackage());
            this.logger.info(String.format("Database Driver '%1$s' successfully loaded.", getType().name()));
        } catch (ClassNotFoundException exc) {
            exc.printStackTrace();
        }
    }

    public List<String> getColumns(String tableName) throws SQLException {
        Connection connection = getConnection();

        ResultSet resultSet = connection.getMetaData().getColumns(null, null, tableName, null);
        List<String> columnList = new ArrayList<>();

        for (; resultSet.next(); columnList.add(resultSet.getString("COLUMN_NAME"))) ;

        ((DriverHandler) getHandler()).closeConnection(connection, null, resultSet);
        return columnList;
    }

    public boolean tableExist(String tableName) {
        try {
            Connection connection = getConnection();

            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet resultSet = databaseMetaData.getTables(null, null, tableName, null);
            boolean result = resultSet.next();

            ((DriverHandler) getHandler()).closeConnection(connection, null, resultSet);
            return result;
        } catch (Exception exc) {
            exc.printStackTrace();
            return true;
        }
    }


    public List<String> getAllTables() {
        List<String> tables = new ArrayList<>();

        try {
            Connection connection = getConnection();

            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet resultSet = databaseMetaData.getTables(null, null, "%", null);

            while (resultSet.next()) {
                tables.add(resultSet.getString("TABLE_NAME"));
            }

            ((DriverHandler) getHandler()).closeConnection(connection, null, resultSet);
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        }

        return tables;
    }

}
