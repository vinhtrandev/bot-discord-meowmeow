package com.vinhtran.dogbot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Lưu cấu hình bot theo từng Discord server (guild).
 *
 * Table: server_config
 * Mỗi server có đúng 1 row, identified by server_id (Discord guild ID).
 */
@Entity
@Table(name = "server_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerConfigEntity {

    /** Discord Guild ID — primary key, không tự sinh */
    @Id
    @Column(name = "server_id", nullable = false, updatable = false)
    private String serverId;

    /** Prefix lệnh, mặc định "!" */
    @Column(name = "prefix", nullable = false, length = 5)
    @Builder.Default
    private String prefix = "!";

    /** ID kênh Discord mà bot chỉ phản hồi trong đó (null = tất cả kênh) */
    @Column(name = "bot_channel_id")
    private String botChannelId;

    /** ID role Discord được dùng lệnh /admin (null = chỉ owner + ADMINISTRATOR) */
    @Column(name = "admin_role_id")
    private String adminRoleId;

    /**
     * Alias tùy chỉnh: alias → tên lệnh gốc.
     * Lưu dạng JSON trong PostgreSQL (jsonb).
     * VD: {"xidach": "blackjack", "bal": "balance"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aliases", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> aliases = new HashMap<>();

    /** Thời điểm tạo row */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Thời điểm cập nhật gần nhất */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}