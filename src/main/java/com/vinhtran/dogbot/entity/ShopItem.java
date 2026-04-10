package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shop_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"item_id", "server_id"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ShopItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    // NULL = global (tất cả server dùng chung), có giá trị = riêng server đó
    @Column(name = "server_id")
    private String serverId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Long price;

    // SKIN_BAI, FRAME, COUPLE_RING
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String emoji;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCouple = false;
}