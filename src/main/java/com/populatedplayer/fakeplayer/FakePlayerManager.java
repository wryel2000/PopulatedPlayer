package com.populatedplayer.fakeplayer;

import com.mojang.authlib.GameProfile;
import com.populatedplayer.config.PopulatedConfig;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class FakePlayerManager {
    private static final String OFFLINE_UUID_PREFIX = "OfflinePlayer:";

    private final Plugin plugin;
    private final Map<String, FakeTabPlayer> onlineFakePlayers = new LinkedHashMap<>();
    private volatile PopulatedConfig config;

    public FakePlayerManager(Plugin plugin, PopulatedConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public synchronized void updateConfig(PopulatedConfig config) {
        this.config = config;
    }

    public synchronized void setTargetAmount(int targetAmount) {
        int safeTarget = Math.min(Math.max(0, targetAmount), config.allowedNames().size());
        if (onlineFakePlayers.size() < safeTarget) {
            addRandomPlayers(safeTarget - onlineFakePlayers.size());
            return;
        }
        if (onlineFakePlayers.size() > safeTarget) {
            removeRandomPlayers(onlineFakePlayers.size() - safeTarget);
        }
    }

    public synchronized int onlineCount() {
        return onlineFakePlayers.size();
    }

    public synchronized List<String> onlineNames() {
        return List.copyOf(onlineFakePlayers.keySet());
    }

    public synchronized Optional<String> chooseMessageSender() {
        if (onlineFakePlayers.isEmpty()) {
            return Optional.empty();
        }
        String specific = config.specificMessageSender();
        if (!config.randomMessageSender() && specific != null && onlineFakePlayers.containsKey(specific)) {
            return Optional.of(specific);
        }
        List<String> names = new ArrayList<>(onlineFakePlayers.keySet());
        return Optional.of(names.get(ThreadLocalRandom.current().nextInt(names.size())));
    }

    public void sendCurrentTabTo(Player player) {
        List<FakeTabPlayer> snapshot;
        synchronized (this) {
            snapshot = List.copyOf(onlineFakePlayers.values());
        }
        if (!snapshot.isEmpty()) {
            sendAddPacket(player, snapshot);
        }
    }

    private void addRandomPlayers(int amount) {
        List<String> candidates = config.allowedNames().stream()
                .filter(this::isValidPlayerName)
                .filter(name -> !onlineFakePlayers.containsKey(name))
                .filter(name -> Bukkit.getPlayerExact(name) == null)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(candidates, new Random());

        List<FakeTabPlayer> added = candidates.stream()
                .limit(amount)
                .map(this::createFakePlayer)
                .toList();
        added.forEach(fake -> onlineFakePlayers.put(fake.name(), fake));
        broadcastAddPacket(added);
    }

    private void removeRandomPlayers(int amount) {
        List<FakeTabPlayer> current = new ArrayList<>(onlineFakePlayers.values());
        Collections.shuffle(current, new Random());
        List<FakeTabPlayer> removed = current.stream().limit(amount).toList();
        removed.forEach(fake -> onlineFakePlayers.remove(fake.name()));
        broadcastRemovePacket(removed.stream().map(FakeTabPlayer::uuid).toList());
    }

    private FakeTabPlayer createFakePlayer(String name) {
        UUID uuid = UUID.nameUUIDFromBytes((OFFLINE_UUID_PREFIX + name).getBytes(StandardCharsets.UTF_8));
        return new FakeTabPlayer(uuid, name, new GameProfile(uuid, name));
    }

    private boolean isValidPlayerName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{3,16}");
    }

    private void broadcastAddPacket(List<FakeTabPlayer> fakePlayers) {
        if (fakePlayers.isEmpty()) {
            return;
        }
        Bukkit.getOnlinePlayers().forEach(player -> sendAddPacket(player, fakePlayers));
    }

    private void broadcastRemovePacket(List<UUID> uuids) {
        if (uuids.isEmpty()) {
            return;
        }
        ClientboundPlayerInfoRemovePacket packet = new ClientboundPlayerInfoRemovePacket(uuids);
        Bukkit.getOnlinePlayers().forEach(player -> sendPacket(player, packet));
    }

    private void sendAddPacket(Player viewer, List<FakeTabPlayer> fakePlayers) {
        CraftServer craftServer = (CraftServer) plugin.getServer();
        List<ServerPlayer> nmsPlayers = fakePlayers.stream()
                .map(fake -> new ServerPlayer(craftServer.getServer(), ((CraftPlayer) viewer).getHandle().serverLevel(), fake.profile()))
                .toList();
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED),
                nmsPlayers
        );
        sendPacket(viewer, packet);
    }

    private void sendPacket(Player player, Object packet) {
        ((CraftPlayer) player).getHandle().connection.send((net.minecraft.network.protocol.Packet<?>) packet);
    }
}
