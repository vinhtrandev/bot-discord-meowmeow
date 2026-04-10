package com.vinhtran.dogbot.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * Mixin interface — implement thêm vào Command nếu muốn hỗ trợ slash.
 *
 * Ví dụ:
 *   public class BalanceCommand implements Command, SlashCommand { ... }
 *
 * Khi đó command tự động được đăng ký slash VÀ vẫn dùng được bằng prefix.
 */
public interface SlashCommand {

    /**
     * Định nghĩa slash command data để đăng ký với Discord.
     * VD: Commands.slash("balance", "Xem số dư của bạn")
     */
    SlashCommandData buildSlashCommand();

    /**
     * Xử lý khi user dùng slash command.
     */
    void executeSlash(SlashCommandInteractionEvent event);
}