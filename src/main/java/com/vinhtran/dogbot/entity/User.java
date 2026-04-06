package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discord_id", unique = true, nullable = false)
    private String discordId;

    @Column(nullable = false)
    private String username;

    @Column(name = "is_banned")
    private Boolean isBanned;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserCoin userCoin;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<GameHistory> gameHistories;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}