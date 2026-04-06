package com.vinhtran.dogbot.command;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CommandHandler {

    private final Map<String, Command> commandMap;

    public CommandHandler(List<Command> commands) {
        this.commandMap = commands.stream()
                .collect(Collectors.toMap(
                        Command::getName,
                        Function.identity()
                ));
    }

    public Command getCommand(String name) {
        return commandMap.get(name.toLowerCase());
    }
}