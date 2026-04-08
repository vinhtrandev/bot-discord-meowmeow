package com.vinhtran.dogbot.util;

import com.vinhtran.dogbot.service.MaintenanceService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DeployManager {

    private final MaintenanceService maintenanceService;

    public DeployManager(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    /**
     * Gọi sau khi bot restart xong (trong ReadyEvent).
     * Lúc này maintenance.flag vẫn còn → bot vẫn đang báo bảo trì.
     * Method này sẽ: bump version → announce → tắt bảo trì → dọn changes.
     */
    public void onDeployComplete(JDA jda, String channelId, List<String> changes) {
        if (!maintenanceService.isMaintenance()) {
            log.info("ℹ️  Không có file bảo trì — bỏ qua thông báo deploy.");
            return;
        }

        log.info("🚀 Đang xử lý deploy...");

        maintenanceService.nextPatchVersion();
        changes.forEach(maintenanceService::addChange);

        // Thông báo trước khi tắt bảo trì
        maintenanceService.announceDeploy(jda, channelId);

        // Tắt bảo trì sau khi bot đã sẵn sàng hoàn toàn
        maintenanceService.disableMaintenance();
        maintenanceService.clearChanges();
    }
}