package com.vinhtran.dogbot.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class MaintenanceService {

    private boolean maintenance = false;
    private String version = "1.0.0";
    private final List<String> changes = new ArrayList<>();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ----------------------------------------
    // Bật / tắt bảo trì
    // ----------------------------------------
    public void enableMaintenance() {
        this.maintenance = true;
    }

    public void disableMaintenance() {
        this.maintenance = false;
    }

    public boolean isMaintenance() {
        return maintenance;
    }

    // ----------------------------------------
    // Version
    // ----------------------------------------
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void nextPatchVersion() {
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);
        patch++;
        this.version = major + "." + minor + "." + patch;
    }

    // ----------------------------------------
    // Change log
    // ----------------------------------------
    public void addChange(String change) {
        changes.add(change);
    }

    public List<String> getChanges() {
        return new ArrayList<>(changes);
    }

    public void clearChanges() {
        changes.clear();
    }

    // ----------------------------------------
    // Thông báo deploy lên channel Discord
    // ----------------------------------------
    public void announceDeploy(JDA jda, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("🚀 **Bot đã deploy xong!**\n");
        sb.append("📅 Ngày: ").append(LocalDate.now().format(dateFormatter)).append("\n");
        sb.append("📌 Phiên bản: ").append(version).append("\n");
        sb.append("✨ Thay đổi:\n");
        for (String c : changes) {
            sb.append("• ").append(c).append("\n");
        }

        channel.sendMessage(sb.toString()).queue();
    }
}