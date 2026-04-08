package com.vinhtran.dogbot.bot;

import com.vinhtran.dogbot.service.MaintenanceService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupHandler extends ListenerAdapter {

    private static final String CHANNEL_ID = "1491539644515483668";

    private final MaintenanceService maintenanceService;

    public StartupHandler(MaintenanceService maintenanceService, JDA jda) {
        this.maintenanceService = maintenanceService;
        jda.addEventListener(this);
    }

    @Override
    public void onReady(ReadyEvent event) {
        maintenanceService.disableMaintenance();

        var channel = event.getJDA().getTextChannelById(CHANNEL_ID);
        if (channel != null) {
            channel.sendMessage("✅ **Bot đã hoạt động trở lại!**").queue(
                    success -> log.info("✅ Gửi thông báo thành công"),
                    error   -> log.error("❌ Gửi thất bại: {}", error.getMessage())
            );
        } else {
            log.warn("❌ Không tìm thấy channel: {}", CHANNEL_ID);
        }
    }
}