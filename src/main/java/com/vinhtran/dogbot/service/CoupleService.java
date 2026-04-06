package com.vinhtran.dogbot.service;

import com.vinhtran.dogbot.entity.CoupleRelation;
import com.vinhtran.dogbot.entity.ShopItem;
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

    // Gửi lời mời cặp đôi (cần có nhẫn)
    public CoupleRelation propose(String fromId, String toId, String ringItemId) {
        if (fromId.equals(toId))
            throw new RuntimeException("Không thể tự ghép đôi với chính mình!");

        // Kiểm tra đã có couple chưa
        if (coupleRepository.existsByUserAIdOrUserBId(fromId, fromId) ||
                coupleRepository.existsByUserAIdOrUserBId(toId, toId)) {
            throw new RuntimeException("Một trong hai người đã có cặp đôi rồi!");
        }

        // Kiểm tra có nhẫn không
        inventoryRepository.findByUserDiscordIdAndItemId(fromId, ringItemId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa có nhẫn này! Mua tại `!shop`"));

        return coupleRepository.save(CoupleRelation.builder()
                .userAId(fromId).userBId(toId)
                .ringItemId(ringItemId).status("PENDING").build());
    }

    // Chấp nhận lời mời
    public CoupleRelation accept(String userId) {
        CoupleRelation relation = coupleRepository
                .findByUserAIdAndUserBId(userId, userId)
                .orElseGet(() -> coupleRepository
                        .findByUserAIdOrUserBId(userId, userId)
                        .filter(r -> r.getUserBId().equals(userId) && r.getStatus().equals("PENDING"))
                        .orElseThrow(() -> new RuntimeException("Không có lời mời cặp đôi nào!")));

        if (!relation.getUserBId().equals(userId))
            throw new RuntimeException("Không có lời mời nào dành cho bạn!");
        if (!relation.getStatus().equals("PENDING"))
            throw new RuntimeException("Lời mời đã được xử lý rồi!");

        relation.setStatus("ACTIVE");
        return coupleRepository.save(relation);
    }

    // Từ chối
    public void decline(String userId) {
        CoupleRelation relation = coupleRepository
                .findByUserAIdOrUserBId(userId, userId)
                .filter(r -> r.getUserBId().equals(userId) && r.getStatus().equals("PENDING"))
                .orElseThrow(() -> new RuntimeException("Không có lời mời nào!"));
        coupleRepository.delete(relation);
    }

    // Hủy cặp đôi
    public void breakUp(String userId) {
        CoupleRelation relation = coupleRepository.findActiveCouple(userId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa có cặp đôi!"));
        coupleRepository.delete(relation);
    }

    // Lấy thông tin cặp đôi
    public Optional<CoupleRelation> getCouple(String userId) {
        return coupleRepository.findActiveCouple(userId);
    }

    // Kiểm tra 2 người có phải cặp đôi không
    public boolean isCouple(String userAId, String userBId) {
        return coupleRepository.findActiveCouple(userAId)
                .map(r -> (r.getUserAId().equals(userBId) || r.getUserBId().equals(userBId)))
                .orElse(false);
    }

    // Lấy emoji nhẫn của cặp đôi
    public String getCoupleEmoji(String userId) {
        return getCouple(userId)
                .map(r -> shopItemRepository.findByItemId(r.getRingItemId())
                        .map(ShopItem::getEmoji).orElse("💍"))
                .orElse("");
    }

    // Lấy partner id
    public Optional<String> getPartnerId(String userId) {
        return getCouple(userId).map(r ->
                r.getUserAId().equals(userId) ? r.getUserBId() : r.getUserAId());
    }
}