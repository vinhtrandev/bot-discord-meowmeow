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
 * FIX: canonicalNames chỉ chứa tên CHÍNH (getName()) của từng command.
 *      commandMap chứa tên chính + alias mặc định để lookup nhanh.
 *      addAlias() validate dựa vào canonicalNames — không cấm alias mặc định (bj, bc...).
 */
@Slf4j
@Component
public class CommandHandler {

    // Tên thuần → Command (tên chính + alias mặc định, dùng để lookup)
    private final Map<String, Command>      commandMap;

    // Chỉ tên CHÍNH của từng command — dùng để validate alias
    // VD: {"blackjack", "baicao", "balance", ...}
    private final Set<String>               canonicalNames;

    private final Map<String, SlashCommand> slashMap;

    // Dynamic alias: serverId → (alias → tên thuần command)
    private final Map<String, Map<String, String>> dynamicAliases = new ConcurrentHashMap<>();

    public CommandHandler(List<Command> commands) {
        // Build commandMap từ tên chính
        this.commandMap = new ConcurrentHashMap<>(commands.stream()
                .collect(Collectors.toMap(
                        c -> stripPrefix(c.getName()),
                        Function.identity()
                )));

        // Lưu riêng tập tên CHÍNH để validate — không gồm alias
        this.canonicalNames = Collections.unmodifiableSet(
                commands.stream()
                        .map(c -> stripPrefix(c.getName()))
                        .collect(Collectors.toSet())
        );

        // Đăng ký alias mặc định vào commandMap để lookup được
        for (Command cmd : commands) {
            String canonicalName = stripPrefix(cmd.getName());
            for (String alias : cmd.getAliases()) {
                String aliasKey = stripPrefix(alias);
                if (!aliasKey.equals(canonicalName)) {
                    commandMap.putIfAbsent(aliasKey, cmd);
                    log.debug("Alias mặc định: {} → {}", aliasKey, canonicalName);
                }
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
        log.info("📌 Tên lệnh gốc: {}", canonicalNames);
    }

    // ── Lookup ────────────────────────────────────────────────────────────

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
     *
     * Validate:
     *  - commandName phải là tên lệnh hợp lệ (có trong commandMap — gồm cả alias mặc định)
     *  - alias KHÔNG được trùng tên CHÍNH (canonicalNames) — alias mặc định như "bj" vẫn OK
     */
    public void addAlias(String serverId, String alias, String commandName) {
        String aliasKey  = stripPrefix(alias);
        String targetKey = stripPrefix(commandName);

        // commandName phải resolve được (tên chính hoặc alias mặc định)
        if (!commandMap.containsKey(targetKey)) {
            throw new IllegalArgumentException("Lệnh `" + commandName + "` không tồn tại!");
        }

        // Alias không được trùng với tên lệnh CHÍNH (blackjack, baicao, balance...)
        if (canonicalNames.contains(aliasKey)) {
            throw new IllegalArgumentException(
                    "`" + alias + "` đã là tên lệnh gốc, không thể dùng làm alias!"
            );
        }

        dynamicAliases
                .computeIfAbsent(serverId, id -> new ConcurrentHashMap<>())
                .put(aliasKey, targetKey);

        log.info("Dynamic alias added: server={} {} → {}", serverId, aliasKey, targetKey);
    }

    public void removeAlias(String serverId, String alias) {
        String aliasKey = stripPrefix(alias);
        Map<String, String> serverAliases = dynamicAliases.get(serverId);
        if (serverAliases == null || !serverAliases.containsKey(aliasKey)) {
            throw new IllegalArgumentException("Alias `" + alias + "` không tồn tại!");
        }
        serverAliases.remove(aliasKey);
        log.info("Dynamic alias removed: server={} {}", serverId, aliasKey);
    }

    public void clearDynamicAliases(String serverId) {
        dynamicAliases.remove(serverId);
        log.info("Dynamic aliases cleared: server={}", serverId);
    }

    public Map<String, String> getDynamicAliases(String serverId) {
        return Collections.unmodifiableMap(
                dynamicAliases.getOrDefault(serverId, Collections.emptyMap())
        );
    }

    public Set<String> getAllCommandNames() {
        return Collections.unmodifiableSet(commandMap.keySet());
    }

    /** Chỉ trả về tên lệnh CHÍNH (không gồm alias mặc định) */
    public Set<String> getCanonicalCommandNames() {
        return canonicalNames;
    }

    // ── Helper ────────────────────────────────────────────────────────────

    public static String stripPrefix(String name) {
        return name.replaceFirst("^[^a-zA-Z0-9]+", "").toLowerCase();
    }
}