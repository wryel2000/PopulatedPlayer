package com.populatedplayer.command;

import com.populatedplayer.fakeplayer.FakePlayerManager;
import com.populatedplayer.message.AutomaticMessageTask;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class ZeroCommand implements CommandExecutor {
    private final FakePlayerManager fakePlayerManager;
    private final AutomaticMessageTask automaticMessageTask;

    public ZeroCommand(FakePlayerManager fakePlayerManager, AutomaticMessageTask automaticMessageTask) {
        this.fakePlayerManager = fakePlayerManager;
        this.automaticMessageTask = automaticMessageTask;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        fakePlayerManager.setTargetAmount(0);
        automaticMessageTask.stop();
        sender.sendMessage(ChatColor.GREEN + "Todos os fake players foram removidos do TAB e o chat automatico foi pausado.");
        return true;
    }
}
