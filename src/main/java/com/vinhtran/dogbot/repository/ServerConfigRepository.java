package com.vinhtran.dogbot.repository;

import com.vinhtran.dogbot.entity.ServerConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository cho ServerConfigEntity.
 * Hibernate tự tạo table "server_config" khi bot start (ddl-auto=update).
 */
@Repository
public interface ServerConfigRepository extends JpaRepository<ServerConfigEntity, String> {
    // findById(serverId) có sẵn từ JpaRepository — đủ dùng
}