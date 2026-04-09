package com.vinhtran.dogbot.service;

import com.vinhtran.dogbot.entity.StealCooldown;
import com.vinhtran.dogbot.entity.UserCoin;
import com.vinhtran.dogbot.repository.StealCooldownRepository;
import com.vinhtran.dogbot.repository.UserCoinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class StealService {

    private final StealCooldownRepository stealCooldownRepository;
    private final UserCoinRepository userCoinRepository;
    private final UserService userService;
    private final Random random = new Random();

    public static final int COOLDOWN_MINUTES = 10;

    public record StealResult(boolean success, long amount, String message) {}

    public StealResult steal(String thiefDiscordId, String targetDiscordId) {
        if (thiefDiscordId.equals(targetDiscordId))
            throw new RuntimeException("Bạn không thể tự trộm chính mình!");

        // Kiểm tra cooldown
        StealCooldown cooldown = stealCooldownRepository
                .findByThiefDiscordId(thiefDiscordId).orElse(null);

        if (cooldown != null) {
            long minutesPassed = ChronoUnit.MINUTES.between(cooldown.getLastStealAt(), LocalDateTime.now());
            if (minutesPassed < COOLDOWN_MINUTES) {
                long remaining = COOLDOWN_MINUTES - minutesPassed;
                throw new RuntimeException("⏳ Bạn cần chờ thêm **" + remaining + " phút** nữa mới có thể trộm!");
            }
        }

        // Lấy coin trong ví của nạn nhân
        UserCoin targetCoin = userCoinRepository.findByUserDiscordId(targetDiscordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng này!"));

        if (targetCoin.getBalance() <= 0)
            throw new RuntimeException("Nạn nhân không có coin trong ví để trộm!");

        // Cập nhật cooldown
        if (cooldown == null) {
            cooldown = StealCooldown.builder()
                    .thiefDiscordId(thiefDiscordId)
                    .lastStealAt(LocalDateTime.now())
                    .build();
        } else {
            cooldown.setLastStealAt(LocalDateTime.now());
        }
        stealCooldownRepository.save(cooldown);

        // 50% thành công, 50% thất bại
        boolean success = random.nextInt(100) < 50;

        if (success) {
            // Trộm 10%-25% tiền trong ví nạn nhân
            long stealPercent = 10 + random.nextInt(16); // 10 + [0-15] => 10%-25%
            long amount = Math.max(1, targetCoin.getBalance() * stealPercent / 100);

            targetCoin.setBalance(targetCoin.getBalance() - amount);
            userCoinRepository.save(targetCoin);
            userService.updateBalance(thiefDiscordId, amount);

            return new StealResult(true, amount,
                    "🦹 Trộm thành công! Bạn lấy được **" + amount + " 🪙** (" + stealPercent + "% ví)");
        } else {
            // Thất bại → mất 10%-15% tiền ví của kẻ trộm
            long thiefBalance = userService.getBalance(thiefDiscordId);
            long penalty = Math.max(1, thiefBalance * (10 + random.nextInt(6)) / 100); // 10% - 15%
            try { userService.updateBalance(thiefDiscordId, -penalty); } catch (Exception ignored) {}

            return new StealResult(false, penalty,
                    "🚔 Trộm thất bại! Bị bắt và phạt **" + penalty + " 🪙**");
        }
    }

    public long getCooldownRemaining(String discordId) {
        StealCooldown cooldown = stealCooldownRepository
                .findByThiefDiscordId(discordId).orElse(null);
        if (cooldown == null) return 0;
        long passed = ChronoUnit.MINUTES.between(cooldown.getLastStealAt(), LocalDateTime.now());
        return Math.max(0, COOLDOWN_MINUTES - passed);
    }
}