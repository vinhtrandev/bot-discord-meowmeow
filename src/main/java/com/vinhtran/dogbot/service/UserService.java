package com.vinhtran.dogbot.service;

import com.vinhtran.dogbot.entity.User;
import com.vinhtran.dogbot.entity.UserCoin;
import com.vinhtran.dogbot.repository.UserCoinRepository;
import com.vinhtran.dogbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserCoinRepository userCoinRepository;

    /**
     * Lấy user theo server — tạo mới nếu chưa tồn tại.
     * Overload không có username: fallback dùng discordId làm username.
     */
    public User getOrCreate(String discordId, String serverId) {
        return getOrCreate(discordId, serverId, discordId);
    }

    /**
     * Lấy user theo server — tạo mới nếu chưa tồn tại.
     * Mỗi (discordId + serverId) là 1 profile độc lập.
     */
    public User getOrCreate(String discordId, String serverId, String username) {
        return userRepository.findByDiscordIdAndServerId(discordId, serverId)
                .orElseGet(() -> {
                    User user = userRepository.save(User.builder()
                            .discordId(discordId)
                            .serverId(serverId)
                            .username(username)
                            .createdAt(LocalDateTime.now())
                            .build());
                    userCoinRepository.save(UserCoin.builder()
                            .user(user)
                            .balance(1000L)
                            .totalEarned(0L)
                            .build());
                    return user;
                });
    }

    @Transactional(readOnly = true)
    public User getUser(String discordId, String serverId) {
        return userRepository.findByDiscordIdAndServerId(discordId, serverId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));
    }

    @Transactional(readOnly = true)
    public long getBalance(String discordId, String serverId) {
        User user = getUser(discordId, serverId);
        return userCoinRepository.findByUserId(user.getId())
                .map(UserCoin::getBalance)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví!"));
    }

    public void updateBalance(String discordId, String serverId, long delta) {
        User user = getUser(discordId, serverId);
        UserCoin coin = userCoinRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví!"));

        if (coin.getBalance() + delta < 0)
            throw new RuntimeException("Số dư không đủ!");

        coin.setBalance(coin.getBalance() + delta);
        if (delta > 0)
            coin.setTotalEarned(coin.getTotalEarned() + delta);

        userCoinRepository.save(coin);
    }

    /**
     * Leaderboard: query thẳng từ user_coins, không cần table riêng.
     */
    @Transactional(readOnly = true)
    public List<UserCoin> getLeaderboard(String serverId, int limit) {
        return userCoinRepository.findTopByServerId(serverId, PageRequest.of(0, limit));
    }
}