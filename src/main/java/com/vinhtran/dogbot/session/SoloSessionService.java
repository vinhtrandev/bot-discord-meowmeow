package com.vinhtran.dogbot.session;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SoloSessionService {

    private final Map<String, SoloSession> sessions = new ConcurrentHashMap<>();

    public void save(SoloSession session) {
        sessions.put(session.getChallengerId(), session);
    }

    public SoloSession getByChallenger(String challengerId) {
        return sessions.get(challengerId);
    }

    public SoloSession getByTarget(String targetId) {
        return sessions.values().stream()
                .filter(s -> s.getTargetId().equals(targetId))
                .findFirst().orElse(null);
    }

    public SoloSession getByUser(String userId) {
        SoloSession s = getByChallenger(userId);
        return s != null ? s : getByTarget(userId);
    }

    public void remove(String challengerId) {
        sessions.remove(challengerId);
    }
}