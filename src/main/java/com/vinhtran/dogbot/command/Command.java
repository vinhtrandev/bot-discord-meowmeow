package com.vinhtran.dogbot.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public interface Command {

    /** Tên chính của command, VD: "!blackjack" */
    String getName();

    /**
     * Alias mặc định hardcode trong command.
     * VD: BlackjackCommand trả về ["bj"]
     * Admin có thể thêm alias động qua /admin alias
     */
    default List<String> getAliases() {
        return List.of();
    }

    void execute(MessageReceivedEvent event, String[] args);
}