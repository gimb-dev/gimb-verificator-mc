package tk.gimb.verificator.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.chat.TextComponent;
import tk.gimb.verificator.Main;
import tk.gimb.verificator.managers.ObjectManager;
import tk.gimb.verificator.storage.ConnectionPool;

public class IdentifyCommandHandler implements CommandExecutor {
    private final Main plugin;

    public IdentifyCommandHandler(final Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (args.length == 0) {
            return false;
        }

        final String userToCheck = args[0];

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    final Connection conn = ConnectionPool.getConnection();
                    final PreparedStatement stmt = conn.prepareStatement(
                            "SELECT username FROM " + ObjectManager.SQL_TABLE_NAME + " WHERE ign = ?");
                    stmt.setString(1, userToCheck);

                    final ResultSet rs = stmt.executeQuery();
                    int rowCount = 0;
                    if (rs.last()) {
                        rowCount = rs.getRow();
                        rs.beforeFirst();
                    }

                    if (rowCount == 0) {
                        final TextComponent messageText = new TextComponent("Player not verified!");
                        messageText.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                        sender.spigot().sendMessage(messageText);
                    } else {
                        rs.next();
                        final String username = rs.getString("username");

                        final TextComponent messageText = new TextComponent("Player ");
                        messageText.setColor(net.md_5.bungee.api.ChatColor.AQUA);

                        final TextComponent playerNameText = new TextComponent(userToCheck);
                        playerNameText.setColor(net.md_5.bungee.api.ChatColor.LIGHT_PURPLE);

                        final TextComponent messageText2 = new TextComponent(" is ");
                        messageText2.setColor(net.md_5.bungee.api.ChatColor.AQUA);

                        final TextComponent usernameText = new TextComponent(username);
                        usernameText.setColor(net.md_5.bungee.api.ChatColor.LIGHT_PURPLE);

                        messageText.addExtra(playerNameText);
                        messageText.addExtra(messageText2);
                        messageText.addExtra(usernameText);

                        sender.spigot().sendMessage(messageText);
                    }

                } catch (final SQLException e) {
                    e.printStackTrace();
                    final TextComponent messageText = new TextComponent(
                            "An error occured, please contact adminstrators");
                    messageText.setColor(net.md_5.bungee.api.ChatColor.RED);
                    sender.spigot().sendMessage(messageText);
                }
            }
        });

        return true;
    }
}