package com.vinhtran.dogbot.service;

import com.vinhtran.dogbot.entity.Leaderboard;
import com.vinhtran.dogbot.entity.User;
import com.vinhtran.dogbot.entity.UserCoin;
import com.vinhtran.dogbot.repository.LeaderboardRepository;
import com.vinhtran.dogbot.repository.UserCoinRepository;
import com.vinhtran.dogbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserCoinRepository userCoinRepository;
    private final LeaderboardRepository leaderboardRepository;

    @Transactional
    public User getUser(String discordId, String username) {
        return userRepository.findByDiscordId(discordId)
                .orElseGet(() -> {
                    User user = userRepository.save(User.builder()
                            .discordId(discordId).username(username).build());
                    userCoinRepository.save(UserCoin.builder()
                            .user(user).balance(1000L).totalEarned(0L).build());
                    leaderboardRepository.save(Leaderboard.builder()
                            .user(user).totalWinnings(0L).gamesPlayed(0)
                            .gamesWon(0).winRate(0.0).build());
                    return user;
                });
    }

    @Transactional(readOnly = true)
    public User getUser(String discordId) {
        return userRepository.findByDiscordId(discordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));
    }

    @Transactional(readOnly = true)
    public long getBalance(String discordId) {
        return userCoinRepository.findByUserDiscordId(discordId)
                .map(UserCoin::getBalance)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví!"));
    }

    public void updateBalance(String discordId, long delta) {
        UserCoin coin = userCoinRepository.findByUserDiscordId(discordId)
                .orElseThrow();
        if (coin.getBalance() + delta < 0)
            throw new RuntimeException("Số dư không đủ!");
        coin.setBalance(coin.getBalance() + delta);
        if (delta > 0) coin.setTotalEarned(coin.getTotalEarned() + delta);
        userCoinRepository.save(coin);
    }
}