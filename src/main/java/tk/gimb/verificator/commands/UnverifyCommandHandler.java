package tk.gimb.verificator.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.chat.TextComponent;
import tk.gimb.verificator.Main;
import tk.gimb.verificator.managers.ObjectManager;
import tk.gimb.verificator.storage.ConnectionPool;

public class UnverifyCommandHandler implements CommandExecutor {
    private final Main plugin;

    public UnverifyCommandHandler(final Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (args.length == 0) {
            return false;
        }

        final String userToUnverify = args[0];

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
            @Override
            public void run() {

                try {

                    final Connection conn = ConnectionPool.getConnection();
                    final PreparedStatement stmt = conn
                            .prepareStatement("DELETE FROM " + ObjectManager.SQL_TABLE_NAME + " WHERE ign = ?");
                    stmt.setString(1, userToUnverify);
                    stmt.executeUpdate();

                    final TextComponent messageText = new TextComponent("Account unverified!");
                    messageText.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                    sender.spigot().sendMessage(messageText);

                } catch (final SQLException e) {
                    e.printStackTrace();
                    final TextComponent messageText = new TextComponent(
                            "An error occured");
                    messageText.setColor(net.md_5.bungee.api.ChatColor.RED);
                    sender.spigot().sendMessage(messageText);
                }

            }
        });

        return true;
    }

}