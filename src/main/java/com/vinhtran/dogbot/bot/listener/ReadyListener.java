package com.vinhtran.dogbot.bot.listener;

import com.vinhtran.dogbot.util.BotDeployer;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReadyListener extends ListenerAdapter {

    @Value("${bot.status-channel-id}")
    private String channelId;

    private final BotDeployer botDeployer;

    public ReadyListener(BotDeployer botDeployer) {
        this.botDeployer = botDeployer;
    }

    @Override
    public void onReady(ReadyEvent event) {
        log.info("✅ Bot đã sẵn sàng: {}", event.getJDA().getSelfUser().getName());

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