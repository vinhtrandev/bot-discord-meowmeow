package com.vinhtran.dogbot.service;

import com.vinhtran.dogbot.entity.GameHistory;
import com.vinhtran.dogbot.entity.Leaderboard;
import com.vinhtran.dogbot.entity.User;
import com.vinhtran.dogbot.repository.GameHistoryRepository;
import com.vinhtran.dogbot.repository.LeaderboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GameService {

    private final GameHistoryRepository gameHistoryRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final UserService userService;

    public void recordResult(String discordId, String gameType, long bet, String result) {
        User user = userService.getUser(discordId);

        long profitLoss = result.equals("WIN") ? bet : result.equals("LOSE") ? -bet : 0;
        userService.updateBalance(discordId, profitLoss);

        gameHistoryRepository.save(GameHistory.builder()
                .user(user).gameType(gameType)
                .betAmount(bet).result(result).profitLoss(profitLoss).build());

        Leaderboard lb = leaderboardRepository.findByUserDiscordId(discordId).orElseThrow();
        lb.setGamesPlayed(lb.getGamesPlayed() + 1);
        if (result.equals("WIN")) {
            lb.setGamesWon(lb.getGamesWon() + 1);
            lb.setTotalWinnings(lb.getTotalWinnings() + profitLoss);
        }
        lb.setWinRate((double) lb.getGamesWon() / lb.getGamesPlayed() * 100);
        leaderboardRepository.save(lb);
    }
}