package com.vinhtran.dogbot.session;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransferSessionService {

    public record TransferData(String target, long amount, long timestamp) {}

    private final Map<String, TransferData> sessions = new ConcurrentHashMap<>();

    public void save(String userId, String target, long amount) {
        sessions.put(userId, new TransferData(target, amount, System.currentTimeMillis()));
    }

    public TransferData get(String userId) {
        TransferData data = sessions.get(userId);
        if (data == null) return null;
        // Het han sau 60 giay
        if (System.currentTimeMillis() - data.timestamp() > 60000) {
            sessions.remove(userId);
            return null;
        }
        return data;
    }

    public void clear(String userId) {
        sessions.remove(userId);
    }
}