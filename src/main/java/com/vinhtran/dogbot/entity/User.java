package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"discord_id", "server_id"}))
@Builder @NoArgsConstructor @AllArgsConstructor @Getter @Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discord_id", nullable = false)
    private String discordId;

    @Column(name = "server_id", nullable = false)
    private String serverId;

    @Column(name = "username", nullable = false)
    private String username;

    @Builder.Default
    private Boolean isBanned = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}