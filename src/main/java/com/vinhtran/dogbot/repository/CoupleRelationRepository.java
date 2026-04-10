package com.vinhtran.dogbot.repository;

import com.vinhtran.dogbot.entity.CoupleRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface CoupleRelationRepository extends JpaRepository<CoupleRelation, Long> {

    Optional<CoupleRelation> findByUserAIdAndUserBIdAndServerId(String aId, String bId, String serverId);

    @Query("SELECT c FROM CoupleRelation c WHERE c.serverId = :sid " +
            "AND (c.userAId = :uid OR c.userBId = :uid)")
    Stream<CoupleRelation> streamByUserAndServer(@Param("uid") String userId,
                                                 @Param("sid") String serverId);

    boolean existsByUserAIdAndServerId(String userAId, String serverId);
    boolean existsByUserBIdAndServerId(String userBId, String serverId);

    @Query("SELECT c FROM CoupleRelation c WHERE c.serverId = :sid " +
            "AND (c.userAId = :uid OR c.userBId = :uid) AND c.status = 'ACTIVE'")
    Optional<CoupleRelation> findActiveCouple(@Param("uid") String userId,
                                              @Param("sid") String serverId);
}