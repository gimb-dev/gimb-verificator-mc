package tk.gimb.verificator;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.plugin.java.JavaPlugin;

import net.luckperms.api.LuckPerms;
import tk.gimb.verificator.commands.IdentifyCommandHandler;
import tk.gimb.verificator.commands.UnverifyCommandHandler;
import tk.gimb.verificator.commands.VerificationCommandHandler;
import tk.gimb.verificator.commands.VerifyCommandHandler;
import tk.gimb.verificator.managers.ObjectManager;
import tk.gimb.verificator.storage.ConnectionPool;

public class Main extends JavaPlugin
{
    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        // Load LuckPerms
        this.luckPerms = getServer().getServicesManager().load(LuckPerms.class);
        // Load configuration
        createConfig();
        getLogger().info("Loading configuration");
        ObjectManager.ConfigurationObject = getConfig();

        // Connect to SQL database
        getLogger().info("Setting up storage");
        final String storageType = ObjectManager.ConfigurationObject.getString("storage-type");
        if (storageType.equalsIgnoreCase("mariadb")) {
            // Load & escape values
            final String host = ObjectManager.ConfigurationObject.getString("storage.host");
            final String db_name = ObjectManager.ConfigurationObject.getString("storage.database");

            final String user = ObjectManager.ConfigurationObject.getString("storage.user");
            final String pass = ObjectManager.ConfigurationObject.getString("storage.pass");

            final String jdbcUrl = String.format("jdbc:%s://%s/%s", storageType, host, db_name);
            ConnectionPool.setupPool(jdbcUrl, user, pass);

            final String sql = "CREATE TABLE IF NOT EXISTS " + ObjectManager.SQL_TABLE_NAME + "("
                    + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " + "username VARCHAR(255) NOT NULL UNIQUE, "
                    + "ign TINYTEXT, " + "vk TINYTEXT, " + "expiration TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            try {
                final PreparedStatement stmt = ConnectionPool.getConnection().prepareStatement(sql);
                stmt.executeUpdate();
            } catch (final SQLException e) {
                e.printStackTrace();
            }

        } else {
            getLogger().info("Unknown storage type!");
            this.getPluginLoader().disablePlugin(this);
        }
        getLogger().info("Registring command handlers");
        this.getCommand("verification").setExecutor(new VerificationCommandHandler());
        this.getCommand("verify").setExecutor(new VerifyCommandHandler(this, this.luckPerms));
        this.getCommand("identify").setExecutor(new IdentifyCommandHandler(this));
        this.getCommand("unverify").setExecutor(new UnverifyCommandHandler(this));
    }

    @Override
    public void onDisable() {
        getLogger().info("Closing DB connections");
        try {
            ConnectionPool.closePool();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    private void createConfig() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            final File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                getLogger().info("Creating config.yml");
                saveDefaultConfig();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
