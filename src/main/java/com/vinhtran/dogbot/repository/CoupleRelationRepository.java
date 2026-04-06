package com.vinhtran.dogbot.repository;

import com.vinhtran.dogbot.entity.CoupleRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoupleRelationRepository extends JpaRepository<CoupleRelation, Long> {
    Optional<CoupleRelation> findByUserAIdAndUserBId(String aId, String bId);
    Optional<CoupleRelation> findByUserAIdOrUserBId(String aId, String bId);
    boolean existsByUserAIdOrUserBId(String aId, String bId);

    @Query("SELECT c FROM CoupleRelation c WHERE " +
            "(c.userAId = :uid OR c.userBId = :uid) AND c.status = 'ACTIVE'")
    Optional<CoupleRelation> findActiveCouple(@Param("uid") String userId);
}
