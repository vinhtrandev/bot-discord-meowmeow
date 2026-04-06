package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "bank_accounts")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BankAccount {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "balance")
    private Long balance = 0L;

    // Tier 0=chưa mở, 1=Đồng, 2=Bạc, 3=Vàng, 4=Kim Cương
    @Column(name = "tier")
    private Integer tier = 0;

    @Column(name = "max_balance")
    private Long maxBalance = 0L;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;
}