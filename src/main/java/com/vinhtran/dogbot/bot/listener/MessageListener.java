package com.vinhtran.dogbot.bot.listener;

import com.vinhtran.dogbot.command.AdminConfigCommand;
import com.vinhtran.dogbot.command.Command;
import com.vinhtran.dogbot.command.CommandHandler;
import com.vinhtran.dogbot.service.MaintenanceService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Slf4j
@Component
public class MessageListener extends ListenerAdapter {

    private final CommandHandler     commandHandler;
    private final Executor           executor;
    private final MaintenanceService maintenanceService;
    private final AdminConfigCommand adminConfig;

    public MessageListener(CommandHandler commandHandler,
                           @Qualifier("commandExecutor") Executor executor,
                           MaintenanceService maintenanceService,
                           AdminConfigCommand adminConfig) {
        this.commandHandler     = commandHandler;
        this.executor           = executor;
        this.maintenanceService = maintenanceService;
        this.adminConfig        = adminConfig;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getGuild() == null)  return;

        String serverId = event.getGuild().getId();
        AdminConfigCommand.ServerConfig cfg = adminConfig.getConfig(serverId);

        // ── Lọc kênh ──────────────────────────────────────────────────────
        String botChannelId = cfg.botChannelId();
        if (botChannelId != null && !event.getChannel().getId().equals(botChannelId)) return;

        // ── Lấy prefix động ───────────────────────────────────────────────
        String prefix = cfg.prefix();
        String raw    = event.getMessage().getContentRaw().trim();
        if (!raw.startsWith(prefix)) return;

        // ── Chặn khi bảo trì ──────────────────────────────────────────────
        String maintenanceCmd = prefix + "maintenance";
        if (maintenanceService.isMaintenance() && !raw.startsWith(maintenanceCmd)) {
            event.getChannel().sendMessage("⚠️ **Bot đang bảo trì!** Vui lòng thử lại sau.").queue();
            return;
        }

        String[] parts   = raw.split("\\s+");
        String   cmdName = parts[0].toLowerCase();

        // ── Lookup: truyền serverId để hỗ trợ dynamic alias ───────────────
        Command cmd = commandHandler.getCommand(cmdName, serverId);
        if (cmd == null) return;

        event.getChannel().sendTyping().queue();

        executor.execute(() -> {
            try {
                cmd.execute(event, parts);
            } catch (Exception e) {
                log.error("Lỗi khi thực thi command {}", cmdName, e);
                event.getChannel().sendMessage("❌ Có lỗi xảy ra khi xử lý lệnh.").queue();
            }
        });
    }
}