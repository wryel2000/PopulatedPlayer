package com.populatedplayer.message;

import com.populatedplayer.config.PopulatedConfig;
import com.populatedplayer.fakeplayer.FakePlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class AutomaticMessageTask {
    private final Plugin plugin;
    private final FakePlayerManager fakePlayerManager;
    private final Supplier<PopulatedConfig> configSupplier;
    private BukkitTask task;

    public AutomaticMessageTask(Plugin plugin, FakePlayerManager fakePlayerManager, Supplier<PopulatedConfig> configSupplier) {
        this.plugin = plugin;
        this.fakePlayerManager = fakePlayerManager;
        this.configSupplier = configSupplier;
    }

    public void restart() {
        stop();
        long intervalTicks = configSupplier.get().automaticMessageIntervalSeconds() * 20L;
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::sendAutomaticMessage, intervalTicks, intervalTicks);
    }

    public boolean isRunning() {
        return task != null;
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void sendAutomaticMessage() {
        PopulatedConfig config = configSupplier.get();
        List<String> messages = config.automaticMessages();
        Optional<String> sender = fakePlayerManager.chooseMessageSender();
        if (messages.isEmpty() || sender.isEmpty()) {
            return;
        }
        String text = messages.get(ThreadLocalRandom.current().nextInt(messages.size())).stripLeading();
        String formatted = "<" + sender.get() + "> " + ChatColor.translateAlternateColorCodes('&', text);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(formatted));
    }
}
