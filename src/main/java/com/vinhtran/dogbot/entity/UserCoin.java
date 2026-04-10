package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_coins")
@Builder @NoArgsConstructor @AllArgsConstructor @Getter @Setter
public class UserCoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Long balance = 0L;

    @Builder.Default
    private Long totalEarned = 0L;
}