package com.vinhtran.dogbot.service;

import com.vinhtran.dogbot.entity.CoupleRelation;
import com.vinhtran.dogbot.entity.User;
import com.vinhtran.dogbot.repository.CoupleRelationRepository;
import com.vinhtran.dogbot.repository.ShopItemRepository;
import com.vinhtran.dogbot.repository.UserInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CoupleService {

    private final CoupleRelationRepository coupleRepository;
    private final UserInventoryRepository inventoryRepository;
    private final ShopItemRepository shopItemRepository;
    private final UserService userService;

    public CoupleRelation propose(String fromId, String toId, String serverId, String ringItemId) {
        if (fromId.equals(toId))
            throw new RuntimeException("Không thể tự ghép đôi với chính mình!");

        // Kiểm tra đã có couple trên server này chưa
        if (coupleRepository.existsByUserAIdAndServerId(fromId, serverId) ||
                coupleRepository.existsByUserBIdAndServerId(fromId, serverId))
            throw new RuntimeException("Bạn đã có cặp đôi trên server này rồi!");

        if (coupleRepository.existsByUserAIdAndServerId(toId, serverId) ||
                coupleRepository.existsByUserBIdAndServerId(toId, serverId))
            throw new RuntimeException("Người kia đã có cặp đôi trên server này rồi!");

        // Kiểm tra có nhẫn không
        User fromUser = userService.getOrCreate(fromId, serverId);
        inventoryRepository.findByUserIdAndItemId(fromUser.getId(), ringItemId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa có nhẫn này! Mua tại `!shop`"));

        return coupleRepository.save(CoupleRelation.builder()
                .serverId(serverId)
                .userAId(fromId)
                .userBId(toId)
                .ringItemId(ringItemId)
                .status("PENDING")
                .build());
    }

    public CoupleRelation accept(String userId, String serverId) {
        CoupleRelation relation = coupleRepository
                .streamByUserAndServer(userId, serverId)
                .filter(r -> r.getUserBId().equals(userId) && r.getStatus().equals("PENDING"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không có lời mời cặp đôi nào!"));

        relation.setStatus("ACTIVE");
        return coupleRepository.save(relation);
    }

    public void decline(String userId, String serverId) {
        CoupleRelation relation = coupleRepository
                .streamByUserAndServer(userId, serverId)
                .filter(r -> r.getUserBId().equals(userId) && r.getStatus().equals("PENDING"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không có lời mời nào!"));
        coupleRepository.delete(relation);
    }

    public void breakUp(String userId, String serverId) {
        CoupleRelation relation = coupleRepository.findActiveCouple(userId, serverId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa có cặp đôi!"));
        coupleRepository.delete(relation);
    }

    @Transactional(readOnly = true)
    public Optional<CoupleRelation> getCouple(String userId, String serverId) {
        return coupleRepository.findActiveCouple(userId, serverId);
    }

    @Transactional(readOnly = true)
    public boolean isCouple(String userAId, String userBId, String serverId) {
        return coupleRepository.findActiveCouple(userAId, serverId)
                .map(r -> r.getUserAId().equals(userBId) || r.getUserBId().equals(userBId))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public String getCoupleEmoji(String userId, String serverId) {
        return getCouple(userId, serverId)
                .map(r -> shopItemRepository.findByItemId(r.getRingItemId(), serverId)
                        .map(com.vinhtran.dogbot.entity.ShopItem::getEmoji).orElse("💍"))
                .orElse("");
    }

    @Transactional(readOnly = true)
    public Optional<String> getPartnerId(String userId, String serverId) {
        return getCouple(userId, serverId).map(r ->
                r.getUserAId().equals(userId) ? r.getUserBId() : r.getUserAId());
    }
}