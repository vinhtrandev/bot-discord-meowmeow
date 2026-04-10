package com.vinhtran.dogbot.bot;

import com.vinhtran.dogbot.bot.listener.ButtonListener;
import com.vinhtran.dogbot.bot.listener.MessageListener;
import com.vinhtran.dogbot.bot.listener.ReadyListener;
import com.vinhtran.dogbot.bot.listener.SlashCommandListener;
import com.vinhtran.dogbot.command.AdminConfigCommand;
import com.vinhtran.dogbot.command.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DiscordBot {

    @Value("${discord.bot.token}")
    private String token;

    private final MessageListener      messageListener;
    private final ButtonListener       buttonListener;
    private final ReadyListener        readyListener;
    private final SlashCommandListener slashCommandListener;
    private final AdminConfigCommand   adminConfigCommand;
    private final CommandHandler       commandHandler;

    @Bean
    public JDA jda() throws Exception {
        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MESSAGES
                )
                .addEventListeners(
                        messageListener,
                        buttonListener,
                        readyListener,
                        slashCommandListener,  // nhận slash command events
                        adminConfigCommand     // nhận button interaction events
                )
                .build()
                .awaitReady();

        // Gom tất cả slash commands:
        // 1. Lệnh game/utility (implement SlashCommand interface) — lấy từ CommandHandler
        // 2. Lệnh /admin — build riêng vì không phải Command thông thường
        List<SlashCommandData> allCommands = new ArrayList<>();
        allCommands.addAll(commandHandler.getAllSlashCommandData());  // balance, blackjack, v.v.
        allCommands.add(AdminConfigCommand.buildCommandData());        // /admin

        // Đăng ký với Discord — chạy 1 lần khi bot start
        // Global update: mất 1–5 phút để có hiệu lực
        jda.updateCommands()
                .addCommands(allCommands)
                .queue(
                        cmds -> log.info("✅ Đã đăng ký {} slash command(s) với Discord: {}",
                                cmds.size(),
                                cmds.stream().map(c -> "/" + c.getName()).toList()),
                        err  -> log.error("❌ Lỗi đăng ký slash commands", err)
                );

        log.info("🐶 Bot online: {}", jda.getSelfUser().getName());
        return jda;
    }
}