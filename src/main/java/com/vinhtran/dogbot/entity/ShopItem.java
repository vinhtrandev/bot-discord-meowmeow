package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shop_items")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ShopItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String itemId; // "skin_cute", "skin_anime", "frame_gold"...

    @Column(nullable = false)
    private String name; // "Skin Cute"

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Long price;

    // SKIN_BAI, FRAME, TITLE, COUPLE_RING
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String emoji; // emoji hiển thị

    @Column(nullable = false)
    private Boolean isCouple = false; // vật phẩm cặp đôi không
}