package com.vinhtran.dogbot.bot;

import com.vinhtran.dogbot.bot.listener.ButtonListener;
import com.vinhtran.dogbot.bot.listener.MessageListener;
import com.vinhtran.dogbot.bot.listener.ReadyListener;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DiscordBot {

    @Value("${discord.bot.token}")
    private String token;

    private final MessageListener messageListener;
    private final ButtonListener buttonListener;
    private final ReadyListener readyListener;

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
                        readyListener
                )
                .build()
                .awaitReady();

        jda.updateCommands().queue();

        System.out.println("Bot online: " + jda.getSelfUser().getName());
        return jda; // ← trả về để Spring quản lý như 1 bean
    }
}