package com.vinhtran.dogbot.util;

import com.vinhtran.dogbot.service.MaintenanceService;
import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeployManager {

    private final MaintenanceService maintenanceService;

    public DeployManager(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    public void deploy(JDA jda, String channelId, List<String> changes) {
        // Bật bảo trì
        maintenanceService.enableMaintenance();

        // Tăng patch version
        maintenanceService.nextPatchVersion();

        // Thêm các thay đổi
        changes.forEach(maintenanceService::addChange);

        // Tắt bảo trì
        maintenanceService.disableMaintenance();

        // Thông báo lên Discord
        maintenanceService.announceDeploy(jda, channelId);

        // Dọn dẹp change list
        maintenanceService.clearChanges();
    }
}