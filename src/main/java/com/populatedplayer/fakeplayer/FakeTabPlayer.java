package com.populatedplayer.fakeplayer;

import com.comphenix.protocol.wrappers.WrappedGameProfile;

import java.util.UUID;

public record FakeTabPlayer(UUID uuid, String name, WrappedGameProfile profile) {
}
