package com.populatedplayer.command;

import com.populatedplayer.fakeplayer.FakePlayerManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class GirarCommand implements CommandExecutor {
    private final FakePlayerManager fakePlayerManager;

    public GirarCommand(FakePlayerManager fakePlayerManager) {
        this.fakePlayerManager = fakePlayerManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        int online = fakePlayerManager.rotateAllPlayers();
        sender.sendMessage(ChatColor.GREEN + "Nomes dos fake players rotacionados no TAB. Total online: " + online + ".");
        return true;
    }
}
