package com.vinhtran.dogbot.bot;

import com.vinhtran.dogbot.service.MaintenanceService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ShutdownHandler {

    @Value("${bot.status-channel-id}")
    private String channelId;

    private final MaintenanceService maintenanceService;
    private final JDA jda;

    public ShutdownHandler(MaintenanceService maintenanceService, JDA jda) {
        this.maintenanceService = maintenanceService;
        this.jda = jda;
    }

    @PreDestroy
    public void onShutdown() {
        log.info("🔧 Bot đang tắt — bật bảo trì...");

        maintenanceService.enableMaintenance();

        try {
            var channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage("🔧 **Bot đang bảo trì, vui lòng chờ ít phút...**").complete();
            }
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo shutdown: {}", e.getMessage());
        }

        log.info("✅ Hoàn tất shutdown.");
    }
}