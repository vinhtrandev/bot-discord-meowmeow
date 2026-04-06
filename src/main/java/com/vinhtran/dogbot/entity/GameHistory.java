package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity @Table(name = "game_history")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GameHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "game_type") private String gameType;
    @Column(name = "bet_amount") private Long betAmount;
    private String result; // WIN / LOSE / DRAW
    @Column(name = "profit_loss") private Long profitLoss;

    @Column(name = "played_at")
    private LocalDateTime playedAt;

    @PrePersist
    protected void onCreate() { playedAt = LocalDateTime.now(); }
}
