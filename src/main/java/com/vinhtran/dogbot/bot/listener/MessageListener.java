package com.vinhtran.dogbot.bot.listener;

import com.vinhtran.dogbot.command.AdminConfigCommand;
import com.vinhtran.dogbot.command.Command;
import com.vinhtran.dogbot.command.CommandHandler;
import com.vinhtran.dogbot.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageListener extends ListenerAdapter {

    private final CommandHandler     commandHandler;
    private final AdminConfigCommand adminConfigCommand;
    private final MaintenanceService maintenanceService;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild())       return;

        String serverId = event.getGuild().getId();
        AdminConfigCommand.ServerConfig cfg = adminConfigCommand.getConfig(serverId);

        // ── Channel filter ────────────────────────────────────────────────
        String botChannelId = cfg.botChannelId();
        if (botChannelId != null && !event.getChannel().getId().equals(botChannelId)) {
            return;
        }

        // ── Maintenance check ─────────────────────────────────────────────
        if (maintenanceService.isMaintenance()) {
            return;
        }

        // ── Prefix check ──────────────────────────────────────────────────
        String prefix  = cfg.prefix();
        String content = event.getMessage().getContentRaw().trim();
        if (!content.startsWith(prefix)) return;

        String withoutPrefix = content.substring(prefix.length()).trim();
        if (withoutPrefix.isBlank()) return;

        String[] parts   = withoutPrefix.split("\\s+");
        String   rawName = parts[0].toLowerCase();

        // ── Lookup command ────────────────────────────────────────────────
        Command cmd = commandHandler.getCommand(rawName, serverId);
        if (cmd == null) return;

        // ── Hiệu ứng "Bot đang nhập..." ───────────────────────────────────
        event.getChannel().sendTyping().queue();

        try {
            cmd.execute(event, parts);
        } catch (Exception e) {
            log.error("Lỗi execute command '{}' server={}", rawName, serverId, e);
            event.getChannel().sendMessage("❌ Có lỗi xảy ra khi thực hiện lệnh!").queue();
        }
    }
}