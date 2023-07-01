package net.dertod2.DatabaseHandlerPro.Database;

import java.io.File;
import java.sql.*;

public class SQLiteDatabase extends DriverDatabase {
    private SQLiteConnection connection;

    public SQLiteDatabase(File database) {
        super(null, null, database.getAbsolutePath(), null, null);
    }

    public DatabaseType getType() {
        return DatabaseType.SQLite;
    }

    public Connection getConnection() {
        try {
            if (this.connection != null && !this.connection.isClosed()) return this.connection;
            this.connection = new SQLiteConnection(DriverManager.getConnection(getConnectionString()));

            return this.connection;
        } catch (SQLException exc) {
            exc.printStackTrace();
            return null;
        }
    }

    protected String getConnectionString() {
        return "jdbc:sqlite:" + this.database;
    }

    public boolean tableExist(String tableName) {
        Connection connection = getConnection();

        try {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet resultSet = databaseMetaData.getTables(null, null, tableName, null);
            return resultSet.next();
        } catch (Exception exc) {
            exc.printStackTrace();
            return true;
        }
    }

}
