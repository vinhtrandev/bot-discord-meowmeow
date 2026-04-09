package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.service.MaintenanceService;
import com.vinhtran.dogbot.service.UserService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.awt.*;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class WorkCommand implements Command {

    private final UserService userService;
    private final JedisPool jedisPool;

    private static final int COOLDOWN_SECONDS = 600; // 10 phút

    private static final List<String[]> MISSIONS = List.of(
            new String[]{"🐶 Trộm chó hàng xóm", "30"},
            new String[]{"🐟 Đi câu cá", "25"},
            new String[]{"🚗 Rửa xe thuê", "40"},
            new String[]{"📦 Giao hàng online", "35"},
            new String[]{"🌿 Nhổ cỏ vườn", "20"},
            new String[]{"🍜 Bán tô bún bò", "45"},
            new String[]{"🎮 Chơi game kiếm tiền", "15"},
            new String[]{"📱 Bán điện thoại cũ", "50"}
    );

    @Override
    public String getName() {
        return "!work";
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String userId = event.getAuthor().getId();
        String username = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getAuthor().getName();

        userService.getUser(userId, username);

        String cooldownKey = "work:cooldown:" + userId;

        try (Jedis jedis = jedisPool.getResource()) {
            long ttl = jedis.ttl(cooldownKey);

            if (ttl > 0) {
                long minutes = ttl / 60;
                long seconds = ttl % 60;
                event.getChannel().sendMessageEmbeds(
                        new EmbedBuilder()
                                .setTitle("⏳ Chưa đến giờ làm việc!")
                                .setDescription("Bạn cần chờ **" + minutes + " phút " + seconds + " giây** nữa.")
                                .setColor(Color.ORANGE)
                                .build()
                ).queue();
                return;
            }

            // Random nhiệm vụ
            String[] mission = MISSIONS.get(new Random().nextInt(MISSIONS.size()));
            String missionName = mission[0];
            long reward = Long.parseLong(mission[1]);

            // Cộng tiền
            userService.updateBalance(userId, reward);

            // Set cooldown
            jedis.setex(cooldownKey, COOLDOWN_SECONDS, "1");

            event.getChannel().sendMessageEmbeds(
                    new EmbedBuilder()
                            .setTitle("💼 Làm việc thành công!")
                            .setDescription("**" + username + "** đã thực hiện: " + missionName
                                    + "\n💰 Nhận được: **+" + reward + " coin**")
                            .setFooter("Cooldown: 10 phút")
                            .setColor(Color.GREEN)
                            .build()
            ).queue();

        } catch (Exception e) {
            event.getChannel().sendMessage("❌ Lỗi: " + e.getMessage()).queue();
        }
    }
}