package com.vinhtran.dogbot.util;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class BotDeployer {

    private static final String ANNOUNCE_CHANNEL_ID = "1491539644515483668";

    private final DeployManager deployManager;

    public BotDeployer(DeployManager deployManager) {
        this.deployManager = deployManager;
    }

    /**
     * Gọi từ ReadyListener khi bot đã sẵn sàng.
     * Chỉ thực sự làm gì nếu tồn tại file maintenance.flag.
     */
    public void onBotReady(JDA jda) {
        List<String> changes = List.of(
                "Cập nhật Blackjack, bài cào logic game chuẩn bài truyền thống Việt Nam",
                "Cập nhật giao diện người dùng độc quyền Meow Meow",
                "Fix lỗi hiển thị sai tên người dùng"
        );

        deployManager.onDeployComplete(jda, ANNOUNCE_CHANNEL_ID, changes);
    }
}