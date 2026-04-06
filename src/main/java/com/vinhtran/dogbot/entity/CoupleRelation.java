package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "couple_relations")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CoupleRelation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // discordId của người gửi
    @Column(name = "user_a_id", nullable = false)
    private String userAId;

    // discordId của người nhận
    @Column(name = "user_b_id", nullable = false)
    private String userBId;

    // itemId của nhẫn/skin cặp đôi
    @Column(name = "ring_item_id")
    private String ringItemId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // PENDING (chờ chấp nhận), ACTIVE (đã ghép đôi)
    @Column(nullable = false)
    private String status = "PENDING";

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}