package com.vinhtran.dogbot.bot;

import com.vinhtran.dogbot.bot.listener.ButtonListener;
import com.vinhtran.dogbot.bot.listener.MessageListener;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class DiscordBot {

    @Value("${discord.bot.token}")
    private String token;

    private final MessageListener messageListener;
    private final ButtonListener buttonListener; // ← THÊM CÁI NÀY

    @PostConstruct
    public void start() throws Exception {
        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MESSAGES
                )
                .addEventListeners(
                        messageListener,
                        buttonListener  // ← ĐĂNG KÝ CẢ HAI
                )
                .build()
                .awaitReady();

        jda.updateCommands().queue(); // xoa slash commands cu

        System.out.println("Bot online: " + jda.getSelfUser().getName());
    }
}