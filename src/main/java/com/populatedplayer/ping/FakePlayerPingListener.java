package com.populatedplayer.ping;

import com.populatedplayer.fakeplayer.FakePlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public final class FakePlayerPingListener implements Listener {
    private final FakePlayerManager fakePlayerManager;

    public FakePlayerPingListener(FakePlayerManager fakePlayerManager) {
        this.fakePlayerManager = fakePlayerManager;
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        event.setNumPlayers(event.getNumPlayers() + fakePlayerManager.onlineCount());
    }
}
