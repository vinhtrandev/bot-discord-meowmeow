package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "card_skins")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CardSkin {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne @JoinColumn(name = "user_id", unique = true)
    private User user;

    // 0=Mặc định, 1=Gỗ, 2=Bạc, 3=Vàng, 4=Kim Cương, 5=Huyền Thoại
    @Column(name = "skin_level")
    private Integer skinLevel = 0;
}