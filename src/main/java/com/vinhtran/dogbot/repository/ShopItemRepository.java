package com.vinhtran.dogbot.repository;

import com.vinhtran.dogbot.entity.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {

    // Tìm item global (serverId = null)
    Optional<ShopItem> findByItemIdAndServerIdIsNull(String itemId);

    // Tìm item theo server hoặc global
    @Query("SELECT s FROM ShopItem s WHERE s.itemId = :itemId " +
            "AND (s.serverId = :sid OR s.serverId IS NULL) " +
            "ORDER BY s.serverId NULLS LAST")
    Optional<ShopItem> findByItemId(@Param("itemId") String itemId,
                                    @Param("sid") String serverId);

    // Lấy tất cả item của server + global
    @Query("SELECT s FROM ShopItem s WHERE s.serverId = :sid OR s.serverId IS NULL " +
            "ORDER BY s.type, s.price")
    List<ShopItem> findAllForServer(@Param("sid") String serverId);

    // Lấy theo type cho server + global
    @Query("SELECT s FROM ShopItem s WHERE s.type = :type " +
            "AND (s.serverId = :sid OR s.serverId IS NULL)")
    List<ShopItem> findByTypeForServer(@Param("type") String type,
                                       @Param("sid") String serverId);
}