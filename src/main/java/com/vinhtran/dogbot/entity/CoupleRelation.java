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

    @Column(name = "server_id", nullable = false)
    private String serverId;

    @Column(name = "user_a_id", nullable = false)
    private String userAId;

    @Column(name = "user_b_id", nullable = false)
    private String userBId;

    @Column(name = "ring_item_id")
    private String ringItemId;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}