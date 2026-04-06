package com.vinhtran.dogbot.repository;

import com.vinhtran.dogbot.entity.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {
    Optional<ShopItem> findByItemId(String itemId);
    List<ShopItem> findByType(String type);
    List<ShopItem> findAll();
}
