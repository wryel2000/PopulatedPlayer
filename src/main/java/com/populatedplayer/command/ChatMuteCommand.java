package com.populatedplayer.command;

import com.populatedplayer.message.AutomaticMessageTask;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class ChatMuteCommand implements CommandExecutor {
    private final AutomaticMessageTask automaticMessageTask;

    public ChatMuteCommand(AutomaticMessageTask automaticMessageTask) {
        this.automaticMessageTask = automaticMessageTask;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (automaticMessageTask.isRunning()) {
            automaticMessageTask.stop();
            sender.sendMessage(ChatColor.YELLOW + "Chat automatico dos fake players mutado.");
            return true;
        }
        automaticMessageTask.restart();
        sender.sendMessage(ChatColor.GREEN + "Chat automatico dos fake players ativado.");
        return true;
    }
}
