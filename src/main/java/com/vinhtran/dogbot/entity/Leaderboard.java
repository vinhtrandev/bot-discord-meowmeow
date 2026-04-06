package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "leaderboard")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Leaderboard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "total_winnings") private Long totalWinnings = 0L;
    @Column(name = "games_played")  private Integer gamesPlayed = 0;
    @Column(name = "games_won")     private Integer gamesWon = 0;
    @Column(name = "win_rate")      private Double winRate = 0.0;
}
