package com.vinhtran.dogbot.util;

import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BotDeployer {

    private final DeployManager deployManager;

    public BotDeployer(DeployManager deployManager) {
        this.deployManager = deployManager;
    }

    public void deployBot(JDA jda) {
        List<String> changes = List.of(
                "Cập nhật Blackjack, bài cào logic game chuẩn bài truyền thống Việt Nam",
                "Cập nhật giao diện người dùng độc quyền Meow Meow",
                "Fix lỗi hiển thị sai tên người dùng"
        );

        deployManager.deploy(jda, "1491539644515483668", changes);
    }
}