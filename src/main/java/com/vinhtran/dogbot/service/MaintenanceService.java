package com.vinhtran.dogbot.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MaintenanceService {

    private static final String MAINTENANCE_KEY = "maintenance";

    private final JedisPool jedisPool;

    @Value("${bot.maintenance.enabled:false}")
    private boolean maintenanceEnabled;

    public MaintenanceService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        log.info("✅ Đã kết nối Redis");
    }

    private String version = "1.0.0";
    private final List<String> changes = new ArrayList<>();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @PostConstruct
    public void init() {
        try {
            if (isMaintenance()) {
                log.info("⚠️  Bot đang ở chế độ bảo trì (đọc từ Redis).");
            } else {
                log.info("✅ Bot khởi động bình thường, không có bảo trì.");
            }
        } catch (Exception e) {
            log.warn("⚠️  Không thể kết nối Redis khi khởi động: {}", e.getMessage());
        }
    }

    public void enableMaintenance() {
        if (!maintenanceEnabled) {
            log.info("ℹ️  Bỏ qua enableMaintenance (local mode).");
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(MAINTENANCE_KEY, "true");
            log.info("🔧 Đã bật bảo trì.");
        } catch (Exception e) {
            log.warn("⚠️ Redis không khả dụng, không thể bật bảo trì");
        }
    }

    public void disableMaintenance() {
        if (!maintenanceEnabled) {
            log.info("ℹ️  Bỏ qua disableMaintenance (local mode).");
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(MAINTENANCE_KEY);
            log.info("✅ Đã tắt bảo trì.");
        } catch (Exception e) {
            log.warn("⚠️ Redis không khả dụng, không thể tắt bảo trì");
        }
    }

    public boolean isMaintenance() {
        if (!maintenanceEnabled) return false;
        try (Jedis jedis = jedisPool.getResource()) {
            return "true".equals(jedis.get(MAINTENANCE_KEY));
        } catch (Exception e) {
            log.warn("⚠️ Redis không khả dụng, bỏ qua kiểm tra bảo trì");
            return false;
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void nextPatchVersion() {
        String[] parts = version.split("\\.");
        int patch = Integer.parseInt(parts[2]) + 1;
        this.version = parts[0] + "." + parts[1] + "." + patch;
        log.info("📌 Version mới: {}", this.version);
    }

    public void addChange(String change) {
        changes.add(change);
    }

    public List<String> getChanges() {
        return new ArrayList<>(changes);
    }

    public void clearChanges() {
        changes.clear();
    }

    public void announceDeploy(JDA jda, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            log.warn("⚠️  Không tìm thấy channel: {}", channelId);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🚀 **Bot đã deploy xong!**\n");
        sb.append("✅ **Bot đã hoạt động trở lại!**\n");
        sb.append("📅 Ngày: ").append(LocalDate.now().format(dateFormatter)).append("\n");
        sb.append("📌 Phiên bản: **").append(version).append("**\n");
        sb.append("✨ Thay đổi:\n");
        for (String c : changes) {
            sb.append("• ").append(c).append("\n");
        }

        channel.sendMessage(sb.toString()).queue(
                ok -> log.info("✅ Đã thông báo deploy."),
                err -> log.error("❌ Lỗi gửi thông báo", err)
        );
    }
}