package com.populatedplayer;

import com.populatedplayer.command.PeriodCommand;
import com.populatedplayer.command.ChatMuteCommand;
import com.populatedplayer.command.GirarCommand;
import com.populatedplayer.command.PopulatedReloadCommand;
import com.populatedplayer.command.ZeroCommand;
import com.populatedplayer.config.PopulatedConfig;
import com.populatedplayer.fakeplayer.FakePlayerJoinListener;
import com.populatedplayer.fakeplayer.FakePlayerManager;
import com.populatedplayer.message.AutomaticMessageTask;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class PopulatedPlayerPlugin extends JavaPlugin {
    private PopulatedConfig populatedConfig;
    private FakePlayerManager fakePlayerManager;
    private AutomaticMessageTask automaticMessageTask;
    private BukkitTask tabRotationTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocalConfig();

        fakePlayerManager = new FakePlayerManager(this, populatedConfig);
        automaticMessageTask = new AutomaticMessageTask(this, fakePlayerManager, () -> populatedConfig);

        registerPeriodCommand("manha", PopulatedConfig::morningAmount);
        registerPeriodCommand("tarde", PopulatedConfig::afternoonAmount);
        registerPeriodCommand("noite", PopulatedConfig::nightAmount);
        registerSimpleCommand("zero", new ZeroCommand(fakePlayerManager, automaticMessageTask));
        registerSimpleCommand("reload", new PopulatedReloadCommand(this));
        registerSimpleCommand("girar", new GirarCommand(fakePlayerManager));
        registerSimpleCommand("mutarchat", new ChatMuteCommand(automaticMessageTask));
        
        getServer().getPluginManager().registerEvents(new FakePlayerJoinListener(fakePlayerManager), this);

        automaticMessageTask.restart();
        startTabRotationTask();
        getLogger().info("PopulatedPlayer habilitado com injeção de fake players apenas no TAB.");
    }

    @Override
    public void onDisable() {
        if (automaticMessageTask != null) {
            automaticMessageTask.stop();
        }
        if (tabRotationTask != null) {
            tabRotationTask.cancel();
            tabRotationTask = null;
        }
    }

    public FakePlayerManager fakePlayerManager() {
        return fakePlayerManager;
    }

    public void reloadLocalConfig() {
        reloadConfig();
        PopulatedConfig newConfig = PopulatedConfig.from(getConfig());
        populatedConfig = newConfig;

        if (fakePlayerManager != null) {
            fakePlayerManager.onConfigReload(newConfig);
        }
        if (automaticMessageTask != null) {
            automaticMessageTask.restart();
            startTabRotationTask();
        }
    }

    private void startTabRotationTask() {
        if (tabRotationTask != null) {
            tabRotationTask.cancel();
        }
        // Rotaciona 1 jogador a cada 5 minutos
        long fiveMinutesTicks = 5L * 60L * 20L;
        tabRotationTask = getServer().getScheduler().runTaskTimer(this, fakePlayerManager::rotateOnePlayer, fiveMinutesTicks, fiveMinutesTicks);
    }

    private void registerPeriodCommand(String name, java.util.function.ToIntFunction<PopulatedConfig> amountResolver) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Comando não registrado no plugin.yml: " + name);
        }
        command.setExecutor(new PeriodCommand(fakePlayerManager, () -> populatedConfig, amountResolver, automaticMessageTask));
    }

    private void registerSimpleCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Comando nao registrado no plugin.yml: " + name);
        }
        command.setExecutor(executor);
    }
}