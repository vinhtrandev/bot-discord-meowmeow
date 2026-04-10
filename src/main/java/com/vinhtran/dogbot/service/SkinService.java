package com.vinhtran.dogbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SkinService giờ delegate sang ShopService.
 * CardSkin table đã bị xoá — skin được quản lý qua user_inventory với type = SKIN_BAI.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SkinService {

    private final ShopService shopService;

    // Map skin level cũ → itemId mới trong shop
    public static final String[] SKIN_ITEM_IDS = {
            "skin_default", "skin_emerald", "skin_ruby", "skin_star"
    };

    public static final String[] SKIN_NAME = {
            "🃏 Mặc định",   // Icon lá bài Joker truyền thống
            "🎴 Lục Bảo",   // Icon bài hoa (Hanafuda) cực kỳ nghệ thuật
            "🧧 Hồng Ngọc",  // Icon thẻ đỏ (Ruby) nhìn như mặt sau bài quý tộc
            "🔮 Tinh Tú"    // Icon tinh thể ma thuật đại diện cho vũ trụ
    };

    // Đã cập nhật giá cao để tương xứng với tên gọi xa xỉ
    public static final long[] SKIN_PRICE = {0, 10000, 50000, 200000};

    public void buySkin(String discordId, String serverId, int level) {
        if (level < 1 || level >= SKIN_ITEM_IDS.length)
            throw new RuntimeException("Skin không hợp lệ!");
        shopService.buyItem(discordId, serverId, SKIN_ITEM_IDS[level]);
        shopService.equipItem(discordId, serverId, SKIN_ITEM_IDS[level]);
    }

    public String getSkinEmoji(String discordId, String serverId) {
        return shopService.getEquippedSkinEmoji(discordId, serverId);
    }
}