package tk.gimb.verificator.storage;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class ConnectionPool {
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    public static void setupPool(final String jdbcUrl, final String user, final String password) {
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);

        config.setDriverClassName("org.mariadb.jdbc.Driver");

        config.addDataSourceProperty("cachePrepStmts", "true");
	    config.addDataSourceProperty("prepStmtCacheSize", "250");
	    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
	    ds = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public static void closePool() throws SQLException {
        ds.unwrap(HikariDataSource.class).close();
    }

    private ConnectionPool(){}
}
