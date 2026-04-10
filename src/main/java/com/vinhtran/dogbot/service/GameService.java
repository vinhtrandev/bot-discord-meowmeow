package com.vinhtran.dogbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GameService {

    private final UserService userService;

    /**
     * Ghi nhận kết quả game và cập nhật balance.
     * Không lưu GameHistory (tốn DB).
     * Leaderboard lấy từ user_coins trực tiếp qua UserService.getLeaderboard().
     */
    public void recordResult(String discordId, String serverId, String gameType, long bet, String result) {
        long delta = switch (result) {
            case "WIN"  ->  bet;
            case "LOSE" -> -bet;
            default     ->  0L;  // TIE hoặc các trường hợp khác
        };

        if (delta != 0) {
            userService.updateBalance(discordId, serverId, delta);
        }
    }
}