package com.vinhtran.dogbot.repository;

import com.vinhtran.dogbot.entity.UserCoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCoinRepository extends JpaRepository<UserCoin, Long> {
    Optional<UserCoin> findByUserDiscordId(String discordId);
    List<UserCoin> findTop10ByOrderByBalanceDesc();
}
