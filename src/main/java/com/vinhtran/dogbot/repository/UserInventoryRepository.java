package com.vinhtran.dogbot.repository;

import com.vinhtran.dogbot.entity.UserInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInventoryRepository extends JpaRepository<UserInventory, Long> {

    List<UserInventory> findByUserId(Long userId);

    Optional<UserInventory> findByUserIdAndItemId(Long userId, String itemId);

    List<UserInventory> findByUserIdAndIsEquipped(Long userId, boolean equipped);
}