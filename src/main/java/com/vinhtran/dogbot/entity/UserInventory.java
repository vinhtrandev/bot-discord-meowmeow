package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_inventory")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserInventory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(name = "is_equipped")
    private Boolean isEquipped = false;

    @Column(name = "bought_at")
    private LocalDateTime boughtAt;

    @PrePersist
    protected void onCreate() { boughtAt = LocalDateTime.now(); }
}