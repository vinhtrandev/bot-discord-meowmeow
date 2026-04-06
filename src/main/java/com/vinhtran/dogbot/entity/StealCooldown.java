package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "steal_cooldown")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StealCooldown {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thief_discord_id")
    private String thiefDiscordId;

    @Column(name = "last_steal_at")
    private LocalDateTime lastStealAt;
}