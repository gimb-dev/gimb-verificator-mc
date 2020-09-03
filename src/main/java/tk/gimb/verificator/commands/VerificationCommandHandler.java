package tk.gimb.verificator.commands;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;

import tk.gimb.verificator.managers.ObjectManager;

public class VerificationCommandHandler implements CommandExecutor {
    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        final String url = ObjectManager.ConfigurationObject.getString("url");

        final TextComponent messageText = new TextComponent("Verify your account here: ");
        messageText.setColor(net.md_5.bungee.api.ChatColor.AQUA);

        final TextComponent linkText = new TextComponent(url);
        linkText.setColor(net.md_5.bungee.api.ChatColor.LIGHT_PURPLE);
        linkText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));

        messageText.addExtra(linkText);

        sender.spigot().sendMessage(messageText);
        return true;
    }
}
