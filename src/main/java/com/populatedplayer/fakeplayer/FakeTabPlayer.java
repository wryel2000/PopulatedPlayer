package com.populatedplayer.fakeplayer;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

public record FakeTabPlayer(UUID uuid, String name, GameProfile profile) {
}
