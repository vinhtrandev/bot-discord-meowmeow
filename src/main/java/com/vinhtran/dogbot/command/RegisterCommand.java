package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.service.UserService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent; // ✅ QUAN TRỌNG
import org.springframework.stereotype.Component;

import java.awt.*;

@Component
@RequiredArgsConstructor
public class RegisterCommand implements Command {

    private final UserService userService;

    @Override
    public String getName() {
        return "!register";
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {

        String id = event.getAuthor().getId();
        String username = event.getAuthor().getName();

        try {
            userService.register(id, username);

            event.getChannel().sendMessageEmbeds(
                    new EmbedBuilder()
                            .setTitle("Dang ky thanh cong!")
                            .setDescription("Chao **" + username + "**!\nBan nhan duoc **1000 coin**")
                            .setColor(Color.GREEN)
                            .build()
            ).queue();

        } catch (Exception e) {
            event.getChannel().sendMessage("Loi: " + e.getMessage()).queue();
        }
    }
}