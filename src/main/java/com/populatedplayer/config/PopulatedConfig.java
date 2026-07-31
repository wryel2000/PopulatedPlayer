package com.populatedplayer.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public record PopulatedConfig(
        List<String> allowedNames,
        int morningAmount,
        int afternoonAmount,
        int nightAmount,
        long automaticMessageIntervalSeconds,
        boolean randomMessageSender,
        String specificMessageSender,
        String messagePrefix,
        List<String> automaticMessages
) {
    public static PopulatedConfig from(FileConfiguration config) {
        return new PopulatedConfig(
                List.copyOf(config.getStringList("nomes_permitidos")),
                Math.max(0, config.getInt("quantidades.manha", 5)),
                Math.max(0, config.getInt("quantidades.tarde", 20)),
                Math.max(0, config.getInt("quantidades.noite", 50)),
                Math.max(1L, config.getLong("tempo_intervalo_mensagens", 120L)),
                config.getBoolean("mensagens_automaticas.aleatorio", true),
                config.getString("mensagens_automaticas.jogador_especifico", ""),
                config.getString("mensagens_automaticas.prefixo", "[Membro]"),
                List.copyOf(config.getStringList("mensagens_automaticas.textos"))
        );
    }
}
