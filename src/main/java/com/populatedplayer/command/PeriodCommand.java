package com.populatedplayer.command;

import com.populatedplayer.config.PopulatedConfig;
import com.populatedplayer.fakeplayer.FakePlayerManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.function.ToIntFunction;

public final class PeriodCommand implements CommandExecutor {
    private final FakePlayerManager fakePlayerManager;
    private final ToIntFunction<PopulatedConfig> amountResolver;
    private final ConfigSupplier configSupplier;

    public PeriodCommand(FakePlayerManager fakePlayerManager, ConfigSupplier configSupplier, ToIntFunction<PopulatedConfig> amountResolver) {
        this.fakePlayerManager = fakePlayerManager;
        this.configSupplier = configSupplier;
        this.amountResolver = amountResolver;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        int targetAmount = amountResolver.applyAsInt(configSupplier.get());
        fakePlayerManager.setTargetAmount(targetAmount);
        sender.sendMessage(ChatColor.GREEN + "Fake players ajustados para " + targetAmount + " no período " + label + ".");
        return true;
    }

    @FunctionalInterface
    public interface ConfigSupplier {
        PopulatedConfig get();
    }
}
