package com.vinhtran.dogbot.service;

import com.vinhtran.dogbot.entity.CardSkin;
import com.vinhtran.dogbot.entity.User;
import com.vinhtran.dogbot.repository.CardSkinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class SkinService {

    private final CardSkinRepository cardSkinRepository;
    private final UserService userService;

    public static final String[] SKIN_NAME  = {"🃏 Mặc định", "🌿 Gỗ", "⚙️ Sắt", "🥈 Bạc", "🥇 Vàng", "💎 Huyền Thoại"};
    public static final long[]   SKIN_PRICE = {0, 500, 2000, 8000, 25000, 100000};
    public static final String[] SKIN_EMOJI = {"🃏", "🌿", "⚙️", "🥈", "🥇", "💎"};

    public CardSkin buySkin(String discordId, int level) {
        if (level < 1 || level >= SKIN_NAME.length)
            throw new RuntimeException("Skin không hợp lệ!");

        User user = userService.getUser(discordId);
        CardSkin skin = cardSkinRepository.findByUserDiscordId(discordId)
                .orElse(CardSkin.builder().user(user).skinLevel(0).build());

        if (skin.getSkinLevel() >= level)
            throw new RuntimeException("Bạn đã sở hữu skin này hoặc cao hơn rồi!");

        long price = SKIN_PRICE[level];
        long balance = userService.getBalance(discordId);
        if (balance < price)
            throw new RuntimeException("Không đủ coin! Cần **" + price + " 🪙**, bạn có **" + balance + " 🪙**");

        userService.updateBalance(discordId, -price);
        skin.setSkinLevel(level);
        return cardSkinRepository.save(skin);
    }

    public int getSkinLevel(String discordId) {
        return cardSkinRepository.findByUserDiscordId(discordId)
                .map(CardSkin::getSkinLevel).orElse(0);
    }

    public String getSkinEmoji(String discordId) {
        return SKIN_EMOJI[getSkinLevel(discordId)];
    }
}