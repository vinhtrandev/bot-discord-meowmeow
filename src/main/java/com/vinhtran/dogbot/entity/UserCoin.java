package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "user_coins")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserCoin {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false)
    private Long balance;

    @Column(name = "total_earned")
    private Long totalEarned = 0L;
}
