package com.vinhtran.dogbot.session;

import com.vinhtran.dogbot.game.BlackjackGame;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BlackjackSessionService {

    private record Session(BlackjackGame game, long bet, LocalDateTime createdAt) {}

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public void saveGame(String userId, BlackjackGame game, long bet) {
        sessions.put(userId, new Session(game, bet, LocalDateTime.now()));
    }

    public BlackjackGame getGame(String userId) {
        Session s = sessions.get(userId);
        if (s == null) return null;
        // Kiểm tra timeout 5 phút
        if (ChronoUnit.MINUTES.between(s.createdAt(), LocalDateTime.now()) >= 5) {
            sessions.remove(userId);
            return null;
        }
        return s.game();
    }

    public long getBet(String userId) {
        Session s = sessions.get(userId);
        return s != null ? s.bet() : 0L;
    }

    public boolean hasGame(String userId) {
        return getGame(userId) != null; // getGame đã check timeout
    }

    public void clear(String userId) {
        sessions.remove(userId);
    }

    // Tự động dọn session hết hạn mỗi 1 phút
    @Scheduled(fixedDelay = 60000)
    public void cleanExpired() {
        sessions.entrySet().removeIf(entry ->
                ChronoUnit.MINUTES.between(
                        entry.getValue().createdAt(), LocalDateTime.now()) >= 5);
    }
}