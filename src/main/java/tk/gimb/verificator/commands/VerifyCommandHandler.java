package tk.gimb.verificator.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import tk.gimb.verificator.Main;
import tk.gimb.verificator.managers.ObjectManager;
import tk.gimb.verificator.storage.ConnectionPool;

public class VerifyCommandHandler implements CommandExecutor {
    private final LuckPerms luckPerms;
    private final Main plugin;

    public VerifyCommandHandler(final Main plugin, final LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    @Override
    public boolean onCommand(final CommandSender finalSender, final Command cmd, final String label,
            final String[] args) {

        if (!(finalSender instanceof Player)) {
            final TextComponent messageText = new TextComponent("The command can only be executed by players.");
            messageText.setColor(net.md_5.bungee.api.ChatColor.RED);
            finalSender.spigot().sendMessage(messageText);
            return true;
        }

        if (args.length == 0) {
            return false;
        }

        final String providedCode = args[0];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                Connection conn;
                PreparedStatement stmt;
                try {
                    // Connect to DB
                    conn = ConnectionPool.getConnection();

                    // Check if user is already verified
                    stmt = conn.prepareStatement(
                            "SELECT COUNT(*) FROM " + ObjectManager.SQL_TABLE_NAME + " WHERE ign = ?");
                    stmt.setString(1, finalSender.getName());

                    ResultSet rs = stmt.executeQuery();
                    rs.next();

                    // Check if account is already verified
                    if (rs.getInt(1) > 0) {
                        final TextComponent messageText = new TextComponent("Account has already been verified");
                        messageText.setColor(net.md_5.bungee.api.ChatColor.RED);
                        finalSender.spigot().sendMessage(messageText);
                        return;
                    }

                    // Get code & expiration
                    stmt = conn.prepareStatement(
                            "SELECT username, ign, expiration FROM " + ObjectManager.SQL_TABLE_NAME + " WHERE vk = ?");
                    stmt.setString(1, providedCode);
                    rs = stmt.executeQuery();

                    int rowCount = 0;
                    if (rs.last()) {
                        rowCount = rs.getRow();
                        rs.beforeFirst();
                    }

                    // Check if code is valid
                    if (rowCount == 0) {
                        final String url = ObjectManager.ConfigurationObject.getString("url");
                        final TextComponent messageText = new TextComponent("Invalid code. Get a valid one one on ");
                        messageText.setColor(net.md_5.bungee.api.ChatColor.LIGHT_PURPLE);

                        final TextComponent linkText = new TextComponent(url);
                        linkText.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                        linkText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));

                        messageText.addExtra(linkText);
                        finalSender.spigot().sendMessage(messageText);
                        return;

                    } else if (rowCount != 1) {
                        final TextComponent messageText = new TextComponent(
                                "Unexpected state, please contact administrators");
                        messageText.setColor(net.md_5.bungee.api.ChatColor.RED);
                        finalSender.spigot().sendMessage(messageText);
                        return;
                    }

                    rs.next();
                    // Get the name of the account stored in DB (to be de-verified)
                    final String registeredIgn = rs.getString("ign");

                    // Get student's username
                    final String username = rs.getString("username");

                    // Get expiration timestamp
                    final Timestamp expirationTimestamp = rs.getTimestamp("expiration");
                    final Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

                    // Check time validity of the code
                    if (expirationTimestamp.before(currentTimestamp)) {
                        final String url = ObjectManager.ConfigurationObject.getString("url");

                        final TextComponent messageText = new TextComponent("Code expired! Get a new one on ");
                        messageText.setColor(net.md_5.bungee.api.ChatColor.LIGHT_PURPLE);

                        final TextComponent linkText = new TextComponent(url);
                        linkText.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                        linkText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));

                        messageText.addExtra(linkText);
                        finalSender.spigot().sendMessage(messageText);
                        return;
                    }

                    // Get the name of verified group
                    final String verGroupName = ObjectManager.ConfigurationObject.getString("verified-group");
                    // Get the group object
                    final Group group = luckPerms.getGroupManager().getGroup(verGroupName);

                    // Get instance of the user invoking the command
                    User user = luckPerms.getUserManager().getUser(((Player) finalSender).getName());
                    // Get the node of the verified group
                    Optional<InheritanceNode> currentNode = user.resolveInheritedNodes(QueryOptions.nonContextual())
                            .stream().filter(NodeType.INHERITANCE::matches).map(NodeType.INHERITANCE::cast)
                            .filter(n -> n.getGroupName() == verGroupName).findFirst();
                    InheritanceNode verifiedNode = currentNode.orElse(null);

                    // Create an empty DataMutateResult object
                    DataMutateResult result;

                    // If node exists, remove it
                    if (verifiedNode != null) {
                        result = user.data().remove(verifiedNode);
                    }
                    // Create a new one and set it to true
                    InheritanceNode node = InheritanceNode.builder(group).value(true).build();
                    result = user.data().add(node);
                    // Save changes to the user
                    luckPerms.getUserManager().saveUser(user);

                    // Get instance of the user invoking the command
                    if (registeredIgn != null) {
                        user = luckPerms.getUserManager().getUser(registeredIgn);
                        // Get the node of the verified group
                        currentNode = user.resolveInheritedNodes(QueryOptions.nonContextual()).stream()
                                .filter(NodeType.INHERITANCE::matches).map(NodeType.INHERITANCE::cast)
                                .filter(n -> n.getGroupName() == verGroupName).findFirst();
                        verifiedNode = currentNode.orElse(null);

                        // If node exists, remove it
                        if (verifiedNode != null) {
                            result = user.data().remove(verifiedNode);
                        }
                        // Create a new one and set it to true
                        node = InheritanceNode.builder(group).value(true).build();
                        result = user.data().add(node);
                        // Save changes to the user
                        luckPerms.getUserManager().saveUser(user);
                    }

                    // Update user's IGN and deactivate code
                    stmt = conn.prepareStatement(
                            "UPDATE " + ObjectManager.SQL_TABLE_NAME + " SET ign = ?, vk = NULL WHERE username = ?");
                    stmt.setString(1, finalSender.getName());
                    stmt.setString(2, username);
                    stmt.executeUpdate();

                    final TextComponent messageText = new TextComponent("Account verified!");
                    messageText.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                    finalSender.spigot().sendMessage(messageText);

                } catch (final SQLException e) {
                    e.printStackTrace();
                    final TextComponent messageText = new TextComponent(
                            "An error occured, please contact adminstrators");
                    messageText.setColor(net.md_5.bungee.api.ChatColor.RED);
                    finalSender.spigot().sendMessage(messageText);
                }
            }
        });        

        return true;
    }
}
