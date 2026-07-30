package com.populatedplayer.command;

import com.populatedplayer.PopulatedPlayerPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class PopulatedReloadCommand implements CommandExecutor {
    private final PopulatedPlayerPlugin plugin;

    public PopulatedReloadCommand(PopulatedPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        plugin.reloadLocalConfig();
        int validNameCount = plugin.fakePlayerManager().validNameCount();
        String message = "Configuracao recarregada. Total de nomes validos carregados: " + validNameCount + ".";
        plugin.getLogger().info(message);
        sender.sendMessage(ChatColor.GREEN + message);
        return true;
    }
}
