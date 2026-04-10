package com.vinhtran.dogbot.bot.listener;

import com.vinhtran.dogbot.command.AdminConfigCommand;
import com.vinhtran.dogbot.command.CommandHandler;
import com.vinhtran.dogbot.util.BotDeployer;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ReadyListener extends ListenerAdapter {

    @Value("${bot.status-channel-id}")
    private String channelId;

    private final BotDeployer    botDeployer;
    private final CommandHandler commandHandler;

    public ReadyListener(BotDeployer botDeployer, CommandHandler commandHandler) {
        this.botDeployer    = botDeployer;
        this.commandHandler = commandHandler;
    }

    @Override
    public void onReady(ReadyEvent event) {
        log.info("✅ Bot đã sẵn sàng: {}", event.getJDA().getSelfUser().getName());

        // ── Gom tất cả slash command data ─────────────────────────────────
        List<SlashCommandData> allSlash = new ArrayList<>();

        // 1. /admin (AdminConfigCommand tự build)
        allSlash.add(AdminConfigCommand.buildCommandData());

        // 2. Tất cả command implement SlashCommand (bj, baicao, balance, ...)
        allSlash.addAll(commandHandler.getAllSlashCommandData());

        // ── Xóa toàn bộ commands cũ trước, sau đó đăng ký lại ────────────
        event.getJDA().updateCommands().queue(
                cleared -> {
                    log.info("🗑️ Đã xóa slash commands cũ, đang đăng ký lại {} command(s)...", allSlash.size());
                    event.getJDA().updateCommands()
                            .addCommands(allSlash)
                            .queue(
                                    ok  -> log.info("✅ Đã đăng ký {} slash command(s)", allSlash.size()),
                                    err -> log.error("❌ Lỗi đăng ký slash command: {}", err.getMessage())
                            );
                },
                err -> log.error("❌ Lỗi xóa slash commands cũ: {}", err.getMessage())
        );

        // ── Gửi thông báo bot online ───────────────────────────────────────
        try {
            var channel = event.getJDA().getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage("✅ **Bot đã hoạt động trở lại!**").queue();
            }
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo ready: {}", e.getMessage());
        }

        botDeployer.onBotReady(event.getJDA());
    }
}