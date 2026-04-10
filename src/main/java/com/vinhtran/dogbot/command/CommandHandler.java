package com.vinhtran.dogbot.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CommandHandler quản lý prefix command, slash command, và alias.
 *
 * Alias có 2 nguồn:
 *   1. Hardcode trong Command.getAliases() — alias mặc định
 *   2. Dynamic alias thêm bởi admin qua /admin alias — lưu per-server
 *
 * Key trong commandMap luôn là tên thuần (không prefix).
 * VD: "!blackjack" → "blackjack", alias "bj" → cũng trỏ về BlackjackCommand
 */
@Slf4j
@Component
public class CommandHandler {

    // ── Tên thuần → Command ───────────────────────────────────────────────
    private final Map<String, Command>      commandMap;
    private final Map<String, SlashCommand> slashMap;

    // ── Dynamic alias: serverId → (alias → tên thuần command) ─────────────
    private final Map<String, Map<String, String>> dynamicAliases = new ConcurrentHashMap<>();

    public CommandHandler(List<Command> commands) {
        // Build commandMap từ tên chính
        this.commandMap = commands.stream()
                .collect(Collectors.toMap(
                        c -> stripPrefix(c.getName()),
                        Function.identity()
                ));

        // Đăng ký alias mặc định (hardcode trong từng command)
        for (Command cmd : commands) {
            String canonicalName = stripPrefix(cmd.getName());
            for (String alias : cmd.getAliases()) {
                String aliasKey = stripPrefix(alias);
                commandMap.putIfAbsent(aliasKey, cmd);
                log.debug("Alias mặc định: {} → {}", aliasKey, canonicalName);
            }
        }

        // Build slashMap
        this.slashMap = commands.stream()
                .filter(c -> c instanceof SlashCommand)
                .map(c -> (SlashCommand) c)
                .collect(Collectors.toMap(
                        s -> stripPrefix(((Command) s).getName()),
                        Function.identity()
                ));

        log.info("✅ Đã load {} command(s) (bao gồm alias), {} slash command(s)",
                commandMap.size(), slashMap.size());
    }

    // ── Lookup ────────────────────────────────────────────────────────────

    /**
     * Lấy command theo tên hoặc alias.
     * Ưu tiên: dynamic alias của server → commandMap (tên chính + alias mặc định)
     */
    public Command getCommand(String name, String serverId) {
        String key = stripPrefix(name);

        // 1. Kiểm tra dynamic alias của server này
        Map<String, String> serverAliases = dynamicAliases.get(serverId);
        if (serverAliases != null) {
            String canonical = serverAliases.get(key);
            if (canonical != null) {
                Command cmd = commandMap.get(canonical);
                if (cmd != null) return cmd;
            }
        }

        // 2. Tra commandMap (tên chính + alias mặc định)
        return commandMap.get(key);
    }

    /** Overload không cần serverId — dùng cho slash (không cần alias) */
    public Command getCommand(String name) {
        return commandMap.get(stripPrefix(name));
    }

    public SlashCommand getSlashCommand(String name) {
        return slashMap.get(stripPrefix(name));
    }

    public List<net.dv8tion.jda.api.interactions.commands.build.SlashCommandData> getAllSlashCommandData() {
        return slashMap.values().stream()
                .map(SlashCommand::buildSlashCommand)
                .collect(Collectors.toList());
    }

    // ── Dynamic alias management ──────────────────────────────────────────

    /**
     * Thêm alias động cho 1 server.
     * VD: addAlias("1234", "xidach", "blackjack")
     *
     * @throws IllegalArgumentException nếu commandName không tồn tại
     */
    public void addAlias(String serverId, String alias, String commandName) {
        String aliasKey   = stripPrefix(alias);
        String canonical  = stripPrefix(commandName);

        if (!commandMap.containsKey(canonical)) {
            throw new IllegalArgumentException("Lệnh `" + commandName + "` không tồn tại!");
        }
        if (commandMap.containsKey(aliasKey)) {
            throw new IllegalArgumentException("`" + alias + "` đã là tên lệnh gốc, không thể dùng làm alias!");
        }

        dynamicAliases
                .computeIfAbsent(serverId, id -> new ConcurrentHashMap<>())
                .put(aliasKey, canonical);

        log.info("Dynamic alias added: server={} {} → {}", serverId, aliasKey, canonical);
    }

    /**
     * Xóa alias động của 1 server.
     */
    public void removeAlias(String serverId, String alias) {
        String aliasKey = stripPrefix(alias);
        Map<String, String> serverAliases = dynamicAliases.get(serverId);
        if (serverAliases == null || !serverAliases.containsKey(aliasKey)) {
            throw new IllegalArgumentException("Alias `" + alias + "` không tồn tại!");
        }
        serverAliases.remove(aliasKey);
        log.info("Dynamic alias removed: server={} {}", serverId, aliasKey);
    }

    /**
     * Xóa toàn bộ alias động của 1 server (dùng khi reset về mặc định).
     */
    public void clearDynamicAliases(String serverId) {
        dynamicAliases.remove(serverId);
        log.info("Dynamic aliases cleared: server={}", serverId);
    }

    /**
     * Lấy toàn bộ alias động của 1 server (để hiển thị).
     */
    public Map<String, String> getDynamicAliases(String serverId) {
        return Collections.unmodifiableMap(
                dynamicAliases.getOrDefault(serverId, Collections.emptyMap())
        );
    }

    /**
     * Lấy tất cả tên lệnh chính (để validate khi admin đặt alias).
     */
    public Set<String> getAllCommandNames() {
        return Collections.unmodifiableSet(commandMap.keySet());
    }

    // ── Helper ────────────────────────────────────────────────────────────

    /** Bỏ ký tự prefix đặc biệt ở đầu: "!blackjack" → "blackjack" */
    public static String stripPrefix(String name) {
        return name.replaceFirst("^[^a-zA-Z0-9]+", "").toLowerCase();
    }
}