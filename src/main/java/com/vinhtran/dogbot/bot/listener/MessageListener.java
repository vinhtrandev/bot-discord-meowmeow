package com.vinhtran.dogbot.bot.listener;

import com.vinhtran.dogbot.command.Command;
import com.vinhtran.dogbot.command.CommandHandler;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageListener extends ListenerAdapter {

    private final CommandHandler commandHandler;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String raw = event.getMessage().getContentRaw().trim();
        if (!raw.startsWith("!")) return;

        String[] parts = raw.split("\\s+");
        String cmdName = parts[0].toLowerCase();

        Command cmd = commandHandler.getCommand(cmdName);

        if (cmd != null) {
            cmd.execute(event, parts);
        }
    }
}