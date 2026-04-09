package com.vinhtran.dogbot.bot;

import com.vinhtran.dogbot.service.MaintenanceService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ShutdownHandler {

    private static final String CHANNEL_ID = "1491539644515483668";

    private final MaintenanceService maintenanceService;
    private final JDA jda;

    public ShutdownHandler(MaintenanceService maintenanceService, JDA jda) {
        this.maintenanceService = maintenanceService;
        this.jda = jda;
    }

    @PreDestroy
    public void onShutdown() {
        log.info("🔧 Bot đang tắt — bật bảo trì...");

        // Lưu vào Redis trước
        maintenanceService.enableMaintenance();

        // Thông báo Discord (.complete() vì process sắp tắt)
        var channel = jda.getTextChannelById(CHANNEL_ID);
        if (channel != null) {
            channel.sendMessage("🔧 **Bot đang bảo trì, vui lòng chờ ít phút...**").complete();
        }

        log.info("✅ Hoàn tất shutdown.");
    }
}