package com.vinhtran.dogbot.service;

import com.vinhtran.dogbot.entity.User;
import com.vinhtran.dogbot.entity.UserCoin;
import com.vinhtran.dogbot.repository.UserCoinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional
public class StealService {

    private final UserCoinRepository userCoinRepository;
    private final UserService userService;
    private final Random random = new Random();

    public static final int COOLDOWN_MINUTES = 10;
    private static final long COOLDOWN_MS = COOLDOWN_MINUTES * 60 * 1000L;

    // Key: "discordId:serverId" → timestamp lần trộm cuối (in-memory, không cần DB)
    private final Map<String, Long> cooldownCache = new ConcurrentHashMap<>();

    public record StealResult(boolean success, long amount, String message) {}

    public StealResult steal(String thiefDiscordId, String targetDiscordId, String serverId) {
        if (thiefDiscordId.equals(targetDiscordId))
            throw new RuntimeException("Bạn không thể tự trộm chính mình!");

        // Kiểm tra cooldown từ cache
        String cacheKey = thiefDiscordId + ":" + serverId;
        Long lastSteal = cooldownCache.get(cacheKey);
        if (lastSteal != null) {
            long remainingMs = COOLDOWN_MS - (Instant.now().toEpochMilli() - lastSteal);
            if (remainingMs > 0) {
                long remaining = (remainingMs / 60000) + 1;
                throw new RuntimeException("⏳ Bạn cần chờ thêm **" + remaining + " phút** nữa mới có thể trộm!");
            }
        }

        // Lấy user nạn nhân theo server
        User targetUser = userService.getOrCreate(targetDiscordId, serverId);
        UserCoin targetCoin = userCoinRepository.findByUserId(targetUser.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng này!"));

        if (targetCoin.getBalance() <= 0)
            throw new RuntimeException("Nạn nhân không có coin trong ví để trộm!");

        // Ghi cooldown vào cache
        cooldownCache.put(cacheKey, Instant.now().toEpochMilli());

        // 50% thành công
        boolean success = random.nextInt(100) < 50;

        if (success) {
            long stealPercent = 5 + random.nextInt(11); // 5% đến 15%
            long amount = Math.max(1, targetCoin.getBalance() * stealPercent / 100);

            targetCoin.setBalance(targetCoin.getBalance() - amount);
            userCoinRepository.save(targetCoin);
            userService.updateBalance(thiefDiscordId, serverId, amount);

            return new StealResult(true, amount,
                    "🦹 Trộm thành công! Bạn lấy được **" + amount + " 🪙** (" + stealPercent + "% ví)");
        } else {
            long thiefBalance = userService.getBalance(thiefDiscordId, serverId);
            long penalty = Math.max(1, thiefBalance * (5 + random.nextInt(11)) / 100); // 5 - 15%
            try { userService.updateBalance(thiefDiscordId, serverId, -penalty); } catch (Exception ignored) {}

            return new StealResult(false, penalty,
                    "🚔 Trộm thất bại! Bị bắt và phạt **" + penalty + " 🪙**");
        }
    }

    public long getCooldownRemaining(String discordId, String serverId) {
        Long lastSteal = cooldownCache.get(discordId + ":" + serverId);
        if (lastSteal == null) return 0;
        long remainingMs = COOLDOWN_MS - (Instant.now().toEpochMilli() - lastSteal);
        return Math.max(0, remainingMs / 60000);
    }
}