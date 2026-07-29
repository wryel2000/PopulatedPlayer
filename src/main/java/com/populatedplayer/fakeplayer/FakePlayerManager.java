package com.populatedplayer.fakeplayer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.populatedplayer.config.PopulatedConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
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
    private static final int FAKE_PLAYER_LATENCY = 0;

    private final ProtocolManager protocolManager;
    private final Map<String, FakeTabPlayer> onlineFakePlayers = new LinkedHashMap<>();
    private volatile PopulatedConfig config;
    private int targetAmount;

    public FakePlayerManager(Plugin plugin, PopulatedConfig config) {
        // Keep the plugin parameter for the existing construction API; packets are handled entirely by ProtocolLib.
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.config = config;
    }

    public synchronized void updateConfig(PopulatedConfig config) {
        this.config = config;
        this.targetAmount = Math.min(targetAmount, validAvailableNames().size());
    }

    public synchronized void refreshCurrentTarget() {
        removePlayersNoLongerAllowed();
        setTargetAmount(targetAmount);
    }

    public synchronized void rotateOnePlayer() {
        if (targetAmount <= 0 || onlineFakePlayers.isEmpty()) {
            return;
        }
        if (validAvailableNames().stream().noneMatch(name -> !onlineFakePlayers.containsKey(name))) {
            return;
        }
        removeRandomPlayers(1);
        addRandomPlayers(1);
    }

    public synchronized void setTargetAmount(int targetAmount) {
        int safeTarget = Math.min(Math.max(0, targetAmount), validAvailableNames().size());
        this.targetAmount = safeTarget;
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
        List<String> candidates = validAvailableNames().stream()
                .filter(name -> !onlineFakePlayers.containsKey(name))
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
        return new FakeTabPlayer(uuid, name, new WrappedGameProfile(uuid, name));
    }

    private List<String> validAvailableNames() {
        return config.allowedNames().stream()
                .filter(this::isValidPlayerName)
                .filter(name -> Bukkit.getPlayerExact(name) == null)
                .distinct()
                .toList();
    }

    private void removePlayersNoLongerAllowed() {
        List<String> validNames = validAvailableNames();
        List<FakeTabPlayer> removed = onlineFakePlayers.values().stream()
                .filter(fake -> !validNames.contains(fake.name()))
                .toList();
        removed.forEach(fake -> onlineFakePlayers.remove(fake.name()));
        broadcastRemovePacket(removed.stream().map(FakeTabPlayer::uuid).toList());
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
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        packet.getUUIDLists().write(0, uuids);
        Bukkit.getOnlinePlayers().forEach(player -> sendPacket(player, packet));
    }

    private void sendAddPacket(Player viewer, List<FakeTabPlayer> fakePlayers) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoActions().write(0, EnumSet.of(
                EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                EnumWrappers.PlayerInfoAction.UPDATE_LISTED
        ));
        writePlayerInfoData(packet, fakePlayers.stream()
                .map(this::toPlayerInfoData)
                .toList());
        sendPacket(viewer, packet);
    }

    private PlayerInfoData toPlayerInfoData(FakeTabPlayer fakePlayer) {
        return createPlayerInfoData(fakePlayer.uuid(), fakePlayer.profile(), WrappedChatComponent.fromText(fakePlayer.name()));
    }

    private PlayerInfoData createPlayerInfoData(UUID uuid, WrappedGameProfile profile, WrappedChatComponent displayName) {
        try {
            Constructor<PlayerInfoData> constructor = PlayerInfoData.class.getConstructor(
                    UUID.class,
                    int.class,
                    boolean.class,
                    EnumWrappers.NativeGameMode.class,
                    WrappedGameProfile.class,
                    WrappedChatComponent.class
            );
            return constructor.newInstance(
                    uuid,
                    FAKE_PLAYER_LATENCY,
                    true,
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    profile,
                    displayName
            );
        } catch (NoSuchMethodException ignored) {
            return createLegacyPlayerInfoData(profile, displayName);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create ProtocolLib player info data for " + profile.getName(), exception);
        }
    }

    private PlayerInfoData createLegacyPlayerInfoData(WrappedGameProfile profile, WrappedChatComponent displayName) {
        try {
            Constructor<PlayerInfoData> constructor = PlayerInfoData.class.getConstructor(
                    WrappedGameProfile.class,
                    int.class,
                    EnumWrappers.NativeGameMode.class,
                    WrappedChatComponent.class
            );
            return constructor.newInstance(profile, FAKE_PLAYER_LATENCY, EnumWrappers.NativeGameMode.SURVIVAL, displayName);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create ProtocolLib player info data for " + profile.getName(), exception);
        }
    }

    private void writePlayerInfoData(PacketContainer packet, List<PlayerInfoData> playerInfoData) {
        StructureModifier<List<PlayerInfoData>> lists = packet.getPlayerInfoDataLists();
        int index = lists.size() > 1 ? 1 : 0;
        lists.write(index, playerInfoData);
    }

    private void sendPacket(Player player, PacketContainer packet) {
        try {
            protocolManager.sendServerPacket(player, packet);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not send fake TAB packet to " + player.getName(), exception);
        }
    }
}