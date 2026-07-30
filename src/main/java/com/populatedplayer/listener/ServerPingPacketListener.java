package com.populatedplayer.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import com.populatedplayer.fakeplayer.FakePlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class ServerPingPacketListener {
    private final Plugin plugin;
    private final FakePlayerManager fakePlayerManager;

    public ServerPingPacketListener(Plugin plugin, FakePlayerManager fakePlayerManager) {
        this.plugin = plugin;
        this.fakePlayerManager = fakePlayerManager;
    }

    public void register() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Status.Server.SERVER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPacket().getServerPings().size() == 0) {
                    return;
                }
                WrappedServerPing ping = event.getPacket().getServerPings().read(0);
                if (ping == null) {
                    return;
                }
                int total = Bukkit.getOnlinePlayers().size() + fakePlayerManager.onlineCount();
                ping.setPlayersOnline(total);
                event.getPacket().getServerPings().write(0, ping);
            }
        });
    }
}
