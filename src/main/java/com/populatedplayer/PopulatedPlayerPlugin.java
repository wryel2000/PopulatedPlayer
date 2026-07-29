package com.populatedplayer;

import com.populatedplayer.command.PeriodCommand;
import com.populatedplayer.config.PopulatedConfig;
import com.populatedplayer.fakeplayer.FakePlayerJoinListener;
import com.populatedplayer.fakeplayer.FakePlayerManager;
import com.populatedplayer.message.AutomaticMessageTask;
import com.populatedplayer.ping.FakePlayerPingListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PopulatedPlayerPlugin extends JavaPlugin {
    private PopulatedConfig populatedConfig;
    private FakePlayerManager fakePlayerManager;
    private AutomaticMessageTask automaticMessageTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocalConfig();

        fakePlayerManager = new FakePlayerManager(this, populatedConfig);
        automaticMessageTask = new AutomaticMessageTask(this, fakePlayerManager, () -> populatedConfig);

        registerPeriodCommand("manha", PopulatedConfig::morningAmount);
        registerPeriodCommand("tarde", PopulatedConfig::afternoonAmount);
        registerPeriodCommand("noite", PopulatedConfig::nightAmount);
        getServer().getPluginManager().registerEvents(new FakePlayerJoinListener(fakePlayerManager), this);
        getServer().getPluginManager().registerEvents(new FakePlayerPingListener(fakePlayerManager), this);

        automaticMessageTask.restart();
        getLogger().info("PopulatedPlayer habilitado com injeção de fake players apenas no TAB.");
    }

    @Override
    public void onDisable() {
        if (automaticMessageTask != null) {
            automaticMessageTask.stop();
        }
    }

    public void reloadLocalConfig() {
        reloadConfig();
        populatedConfig = PopulatedConfig.from(getConfig());
        if (fakePlayerManager != null) {
            fakePlayerManager.updateConfig(populatedConfig);
        }
        if (automaticMessageTask != null) {
            automaticMessageTask.restart();
        }
    }

    private void registerPeriodCommand(String name, java.util.function.ToIntFunction<PopulatedConfig> amountResolver) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Comando não registrado no plugin.yml: " + name);
        }
        command.setExecutor(new PeriodCommand(fakePlayerManager, () -> populatedConfig, amountResolver));
    }
}
