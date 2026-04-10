package com.vinhtran.dogbot.bot.listener;

import com.vinhtran.dogbot.command.AdminConfigCommand;
import com.vinhtran.dogbot.command.CommandHandler;
import com.vinhtran.dogbot.command.SlashCommand;
import com.vinhtran.dogbot.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

/**
 * Nhận slash command interaction và route về đúng SlashCommand handler.
 * Admin command (/admin) được xử lý trực tiếp tại đây qua AdminConfigCommand.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlashCommandListener extends ListenerAdapter {

    private final CommandHandler     commandHandler;
    private final MaintenanceService maintenanceService;
    private final AdminConfigCommand adminConfigCommand;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String name = event.getName().toLowerCase();

        // /admin route thẳng sang AdminConfigCommand
        if (name.equals("admin")) {
            try {
                adminConfigCommand.handleAdmin(event);
            } catch (Exception e) {
                log.error("Lỗi slash command /admin", e);
                if (event.isAcknowledged()) {
                    event.getHook().sendMessage("❌ Có lỗi xảy ra: " + e.getMessage()).setEphemeral(true).queue();
                } else {
                    event.reply("❌ Có lỗi xảy ra: " + e.getMessage()).setEphemeral(true).queue();
                }
            }
            return;
        }

        // Chặn khi bảo trì
        if (maintenanceService.isMaintenance()) {
            event.reply("⚠️ **Bot đang bảo trì!** Vui lòng thử lại sau.")
                    .setEphemeral(true).queue();
            return;
        }

        SlashCommand cmd = commandHandler.getSlashCommand(name);
        if (cmd == null) {
            event.reply("❌ Lệnh không tồn tại!").setEphemeral(true).queue();
            return;
        }

        try {
            cmd.executeSlash(event);
        } catch (Exception e) {
            log.error("Lỗi slash command /{}", name, e);
            if (event.isAcknowledged()) {
                event.getHook().sendMessage("❌ Có lỗi xảy ra!").setEphemeral(true).queue();
            } else {
                event.reply("❌ Có lỗi xảy ra!").setEphemeral(true).queue();
            }
        }
    }
}