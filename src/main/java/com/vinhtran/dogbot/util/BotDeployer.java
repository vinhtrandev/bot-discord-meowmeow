package com.vinhtran.dogbot.util;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class BotDeployer {

    @Value("${bot.announce-channel-id}")
    private String announceChannelId;

    private final DeployManager deployManager;

    public BotDeployer(DeployManager deployManager) {
        this.deployManager = deployManager;
    }

    public void onBotReady(JDA jda) {
        List<String> changes = List.of(
                "Cập nhật Blackjack, bài cào logic game chuẩn bài truyền thống Việt Nam",
                "Cập nhật giao diện người dùng độc quyền Meow Meow",
                "Fix lỗi hiển thị sai tên người dùng",
                "Test tính năng bảo trì tự động"
        );

        deployManager.onDeployComplete(jda, announceChannelId, changes);
    }
}