package com.vinhtran.dogbot.bot;

import com.vinhtran.dogbot.bot.listener.ButtonListener;
import com.vinhtran.dogbot.bot.listener.MessageListener;
import com.vinhtran.dogbot.bot.listener.ReadyListener;
import com.vinhtran.dogbot.bot.listener.SlashCommandListener;
import com.vinhtran.dogbot.command.AdminConfigCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Khởi tạo JDA và đăng ký tất cả listeners.
 *
 * FIX: Bỏ đăng ký slash command tại đây.
 *      Việc đăng ký slash command CHỈ làm trong ReadyListener.onReady()
 *      để tránh bị gọi 2 lần gây xung đột / ghi đè nhau.
 */
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
                        slashCommandListener,  // xử lý slash command
                        adminConfigCommand     // xử lý button interaction (reset default)
                )
                .build()
                .awaitReady();

        // KHÔNG đăng ký slash command ở đây nữa.
        // ReadyListener.onReady() đã lo việc này.

        log.info("🐶 Bot online: {}", jda.getSelfUser().getName());
        return jda;
    }
}