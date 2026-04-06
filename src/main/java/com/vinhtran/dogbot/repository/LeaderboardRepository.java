package com.vinhtran.dogbot.repository;

import com.vinhtran.dogbot.entity.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, Long> {
    Optional<Leaderboard> findByUserDiscordId(String discordId);
    List<Leaderboard> findTop10ByOrderByTotalWinningsDesc();
}