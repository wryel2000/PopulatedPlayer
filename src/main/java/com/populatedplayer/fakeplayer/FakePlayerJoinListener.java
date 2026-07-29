package com.populatedplayer.fakeplayer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class FakePlayerJoinListener implements Listener {
    private final FakePlayerManager fakePlayerManager;

    public FakePlayerJoinListener(FakePlayerManager fakePlayerManager) {
        this.fakePlayerManager = fakePlayerManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        fakePlayerManager.sendCurrentTabTo(event.getPlayer());
    }
}
