package com.vinhtran.dogbot.repository;

import com.vinhtran.dogbot.entity.UserCoin;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserCoinRepository extends JpaRepository<UserCoin, Long> {

    Optional<UserCoin> findByUserId(Long userId);

    @Query("SELECT uc FROM UserCoin uc JOIN uc.user u " +
            "WHERE u.serverId = :serverId " +
            "ORDER BY uc.balance DESC")
    List<UserCoin> findTopByServerId(@Param("serverId") String serverId, Pageable pageable);
}