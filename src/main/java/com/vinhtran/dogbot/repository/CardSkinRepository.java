package com.vinhtran.dogbot.repository;

import com.vinhtran.dogbot.entity.CardSkin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardSkinRepository extends JpaRepository<CardSkin, Long> {
    Optional<CardSkin> findByUserDiscordId(String discordId);
}