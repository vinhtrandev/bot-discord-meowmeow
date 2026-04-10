package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.service.UserService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WorkCommand implements Command {

    private final UserService userService;

    private static final int  COOLDOWN_SECONDS = 600; // 10 phút
    private static final long COOLDOWN_MS      = COOLDOWN_SECONDS * 1000L;

    // Key: "discordId:serverId" → timestamp hết cooldown
    private final Map<String, Long> cooldownCache = new ConcurrentHashMap<>();

    private static final List<String[]> MISSIONS = List.of(
            new String[]{"🐶 Trộm chó hàng xóm",   "30"},
            new String[]{"🐟 Đi câu cá",             "25"},
            new String[]{"🚗 Rửa xe thuê",           "40"},
            new String[]{"📦 Giao hàng online",      "35"},
            new String[]{"🌿 Nhổ cỏ vườn",           "20"},
            new String[]{"🍜 Bán tô bún bò",         "45"},
            new String[]{"🎮 Chơi game kiếm tiền",   "15"},
            new String[]{"📱 Bán điện thoại cũ",     "50"}
    );

    @Override
    public String getName() { return "!work"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String userId   = event.getAuthor().getId();
        String username = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getAuthor().getName();
        String serverId = event.getGuild().getId();

        userService.getOrCreate(userId, serverId);

        String cacheKey = userId + ":" + serverId;
        long   now      = Instant.now().toEpochMilli();
        Long   expiry   = cooldownCache.get(cacheKey);

        if (expiry != null && now < expiry) {
            long remaining = (expiry - now) / 1000;
            long minutes   = remaining / 60;
            long seconds   = remaining % 60;
            event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("⏳ Chưa đến giờ làm việc!")
                    .setDescription("Bạn cần chờ **" + minutes + " phút " + seconds + " giây** nữa.")
                    .setColor(Color.ORANGE)
                    .build()).queue();
            return;
        }

        String[] mission    = MISSIONS.get(new Random().nextInt(MISSIONS.size()));
        String   missionName = mission[0];
        long     reward     = Long.parseLong(mission[1]);

        userService.updateBalance(userId, serverId, reward);
        cooldownCache.put(cacheKey, now + COOLDOWN_MS);

        event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("💼 Làm việc thành công!")
                .setDescription("**" + username + "** đã thực hiện: " + missionName
                        + "\n💰 Nhận được: **+" + reward + " coin**")
                .setFooter("Cooldown: 10 phút")
                .setColor(Color.GREEN)
                .build()).queue();
    }
}