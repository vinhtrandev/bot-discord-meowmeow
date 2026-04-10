package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
@Builder @NoArgsConstructor @AllArgsConstructor @Getter @Setter
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Builder.Default
    private Long balance = 0L;

    private Long maxBalance;
    private Integer tier;
    private LocalDateTime openedAt;
}