package com.vinhtran.dogbot.util;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class BotDeployer {

    @Value("${bot.announce-channel-id:}")
    private String announceChannelId;

    private final DeployManager deployManager;

    public BotDeployer(DeployManager deployManager) {
        this.deployManager = deployManager;
    }

    public void onBotReady(JDA jda) {
        // Chỉ announce nếu channel được cấu hình
        if (announceChannelId == null || announceChannelId.isBlank()) {
            log.info("bot.announce-channel-id chưa được cấu hình, bỏ qua announce.");
            return;
        }

        List<String> changes = List.of(
                "🎰 SÒNG BÀI TRUYỀN THỐNG VIỆT NAM!",
                "GAME HIỆN TẠI CHỈ CÓ 2 TỰA GAME DUY NHẤT LÀ BLACKJACK (XÌ DÁCH) VÀ BÀI CÀO",
                "🃏 Blackjack (Xì Dách), Logic chuẩn bài tính điểm thông minh.",
                "🃏 Bài Cào (Ba Cây) đếm nút so trình nhân phẩm.",
                "🚀 Sắp tới sẽ cập nhật thêm nhiều game khác hấp dẫn hơn!"
        );

        deployManager.onDeployComplete(jda, announceChannelId, changes);
    }
}