package com.vinhtran.dogbot.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;

public class EmbedUtil {

    public static MessageEmbed error(String message) {
        return new EmbedBuilder()
                .setTitle("❌ Lỗi")
                .setDescription(message)
                .setColor(Color.RED)
                .build();
    }

    public static MessageEmbed success(String message) {
        return new EmbedBuilder()
                .setTitle("✅ Thành công")
                .setDescription(message)
                .setColor(Color.GREEN)
                .build();
    }
}