package com.vinhtran.dogbot.repository;

import com.vinhtran.dogbot.entity.StealCooldown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StealCooldownRepository extends JpaRepository<StealCooldown, Long> {
    Optional<StealCooldown> findByThiefDiscordId(String discordId);
}