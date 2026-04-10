package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.entity.ServerConfigEntity;
import com.vinhtran.dogbot.repository.ServerConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý cấu hình bot theo từng server (guild).
 *
 * Config được lưu vào PostgreSQL (Supabase) — tồn tại vĩnh viễn dù redeploy.
 * In-memory cache để tránh query DB mỗi lần nhận tin nhắn.
 *
 * FIX: Mọi thao tác thêm/xóa alias đều sync sang CommandHandler (in-memory)
 *      để MessageListener dùng getCommand(name, serverId) hoạt động đúng.
 *
 * Lấy config từ class khác:
 *   AdminConfigCommand.ServerConfig cfg = adminConfigCommand.getConfig(serverId);
 *   String prefix = cfg.prefix();
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminConfigCommand extends ListenerAdapter {

    private final CommandHandler           commandHandler;
    private final ServerConfigRepository   repo;

    public static final String DEFAULT_PREFIX = "!";

    // ── ServerConfig record ───────────────────────────────────────────────

    public record ServerConfig(
            String              prefix,
            String              botChannelId,
            String              adminRoleId,
            Map<String, String> aliases
    ) {
        @Override
        public String prefix() {
            return prefix != null ? prefix : DEFAULT_PREFIX;
        }

        public ServerConfig withPrefix(String p) {
            return new ServerConfig(p, botChannelId, adminRoleId, aliases);
        }

        public ServerConfig withChannel(String c) {
            return new ServerConfig(prefix, c, adminRoleId, aliases);
        }

        public ServerConfig withAdminRole(String r) {
            return new ServerConfig(prefix, botChannelId, r, aliases);
        }

        public ServerConfig withAliases(Map<String, String> a) {
            return new ServerConfig(prefix, botChannelId, adminRoleId, a);
        }
    }

    public static ServerConfig defaultConfig() {
        return new ServerConfig(DEFAULT_PREFIX, null, null, new HashMap<>());
    }

    // ── In-memory cache ───────────────────────────────────────────────────
    private final Map<String, ServerConfig> cache = new ConcurrentHashMap<>();

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Lấy config server. Ưu tiên cache, nếu miss thì load từ DB.
     * Khi load từ DB, sync toàn bộ alias vào CommandHandler luôn.
     */
    public ServerConfig getConfig(String serverId) {
        return cache.computeIfAbsent(serverId, id -> {
            ServerConfig cfg = loadFromDb(id);
            syncAliasesToCommandHandler(id, cfg.aliases());
            return cfg;
        });
    }

    /**
     * Lưu config: cập nhật cache + upsert vào DB.
     */
    public void saveConfig(String serverId, ServerConfig config) {
        cache.put(serverId, config);
        persistToDb(serverId, config);
    }

    /**
     * Lấy dynamic aliases của server (từ cache/DB).
     */
    public Map<String, String> getDynamicAliases(String serverId) {
        Map<String, String> aliases = getConfig(serverId).aliases();
        return aliases != null ? aliases : new HashMap<>();
    }

    /**
     * Thêm alias: lưu DB + sync CommandHandler.
     */
    public void addDynamicAlias(String serverId, String alias, String command) {
        ServerConfig cfg     = getConfig(serverId);
        Map<String, String> updated = new HashMap<>(cfg.aliases() != null ? cfg.aliases() : Map.of());
        updated.put(alias, command);
        saveConfig(serverId, cfg.withAliases(updated));

        // Sync sang CommandHandler để MessageListener dùng được ngay
        try {
            commandHandler.addAlias(serverId, alias, command);
        } catch (Exception e) {
            // CommandHandler.addAlias() đã validate — nếu lỗi ở đây thì chỉ log
            log.warn("Sync alias to CommandHandler failed: {}", e.getMessage());
        }
    }

    /**
     * Xóa alias: lưu DB + sync CommandHandler.
     */
    public void removeDynamicAlias(String serverId, String alias) {
        ServerConfig cfg     = getConfig(serverId);
        Map<String, String> updated = new HashMap<>(cfg.aliases() != null ? cfg.aliases() : Map.of());
        updated.remove(alias);
        saveConfig(serverId, cfg.withAliases(updated));

        // Sync sang CommandHandler
        try {
            commandHandler.removeAlias(serverId, alias);
        } catch (Exception e) {
            log.warn("Sync remove alias to CommandHandler failed: {}", e.getMessage());
        }
    }

    /**
     * Xóa toàn bộ alias: lưu DB + sync CommandHandler.
     */
    public void clearDynamicAliases(String serverId) {
        saveConfig(serverId, getConfig(serverId).withAliases(new HashMap<>()));
        commandHandler.clearDynamicAliases(serverId);
    }

    // ── DB I/O ────────────────────────────────────────────────────────────

    private ServerConfig loadFromDb(String serverId) {
        return repo.findById(serverId)
                .map(e -> new ServerConfig(
                        e.getPrefix()       != null ? e.getPrefix()       : DEFAULT_PREFIX,
                        e.getBotChannelId(),
                        e.getAdminRoleId(),
                        e.getAliases()      != null ? e.getAliases()      : new HashMap<>()
                ))
                .orElseGet(() -> {
                    log.debug("Chưa có config cho server {}, dùng mặc định", serverId);
                    return defaultConfig();
                });
    }

    private void persistToDb(String serverId, ServerConfig cfg) {
        try {
            ServerConfigEntity entity = repo.findById(serverId)
                    .orElseGet(() -> ServerConfigEntity.builder()
                            .serverId(serverId)
                            .build());

            entity.setPrefix(cfg.prefix());
            entity.setBotChannelId(cfg.botChannelId());
            entity.setAdminRoleId(cfg.adminRoleId());
            entity.setAliases(cfg.aliases() != null ? cfg.aliases() : new HashMap<>());

            repo.save(entity);
            log.debug("Đã lưu config server {} vào DB", serverId);
        } catch (Exception e) {
            log.error("Lỗi lưu config server {} vào DB: {}", serverId, e.getMessage());
        }
    }

    /**
     * Khi load config từ DB, sync toàn bộ alias sang CommandHandler.
     * Gọi 1 lần duy nhất mỗi khi cache miss (bot restart hoặc server mới).
     */
    private void syncAliasesToCommandHandler(String serverId, Map<String, String> aliases) {
        if (aliases == null || aliases.isEmpty()) return;
        // Clear trước để tránh duplicate khi reload
        commandHandler.clearDynamicAliases(serverId);
        aliases.forEach((alias, command) -> {
            try {
                commandHandler.addAlias(serverId, alias, command);
            } catch (Exception e) {
                log.warn("Bỏ qua alias lỗi khi sync: server={} alias={} → {}: {}",
                        serverId, alias, command, e.getMessage());
            }
        });
        log.debug("Đã sync {} alias(es) vào CommandHandler cho server {}", aliases.size(), serverId);
    }

    // ── Build slash command data ──────────────────────────────────────────

    public static SlashCommandData buildCommandData() {
        return Commands.slash("admin", "⚙️ Cấu hình bot cho server (chỉ admin)")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                        new SubcommandData("setprefix", "Đổi prefix lệnh bot")
                                .addOption(OptionType.STRING, "prefix", "Prefix mới (VD: ! hoặc .)", true),
                        new SubcommandData("setchannel", "Bot chỉ hoạt động trong kênh này")
                                .addOption(OptionType.CHANNEL, "channel", "Kênh chat cho bot", true),
                        new SubcommandData("setadminrole", "Đặt role được dùng lệnh /admin")
                                .addOption(OptionType.ROLE, "role", "Role admin", true),
                        new SubcommandData("info", "Xem cấu hình hiện tại của server")
                )
                .addSubcommandGroups(
                        new SubcommandGroupData("alias", "Quản lý alias lệnh")
                                .addSubcommands(
                                        new SubcommandData("add", "Thêm alias cho lệnh")
                                                .addOption(OptionType.STRING, "alias",   "Tên alias mới (VD: xidach)", true)
                                                .addOption(OptionType.STRING, "command", "Tên lệnh gốc (VD: blackjack)", true),
                                        new SubcommandData("remove", "Xóa alias")
                                                .addOption(OptionType.STRING, "alias", "Alias cần xóa", true),
                                        new SubcommandData("list", "Xem tất cả alias của server")
                                )
                );
    }

    // ── Entry point ───────────────────────────────────────────────────────

    public void handleAdmin(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue(v -> {
            try {
                if (event.getGuild() == null) {
                    event.getHook().sendMessage("❌ Lệnh này chỉ dùng được trong server!").queue();
                    return;
                }
                if (!isAuthorized(event)) {
                    event.getHook().sendMessage(
                            "❌ Bạn không có quyền dùng lệnh này!\n"
                                    + "Yêu cầu: Server Owner, quyền Administrator, hoặc role admin đã cấu hình."
                    ).queue();
                    return;
                }

                String serverId = event.getGuild().getId();
                String sub      = event.getSubcommandName();
                String group    = event.getSubcommandGroup();
                if (sub == null) return;

                if ("alias".equals(group)) {
                    switch (sub) {
                        case "add"    -> handleAliasAdd(event, serverId);
                        case "remove" -> handleAliasRemove(event, serverId);
                        case "list"   -> handleAliasList(event, serverId);
                    }
                } else {
                    switch (sub) {
                        case "setprefix"    -> handleSetPrefix(event, serverId);
                        case "setchannel"   -> handleSetChannel(event, serverId);
                        case "setadminrole" -> handleSetAdminRole(event, serverId);
                        case "info"         -> handleInfo(event, serverId);
                    }
                }
            } catch (Exception e) {
                log.error("Lỗi handleAdmin sub={}", event.getSubcommandName(), e);
                event.getHook().sendMessage("❌ Lỗi nội bộ: " + e.getMessage()).queue();
            }
        });
    }

    // ── Button: reset về mặc định ─────────────────────────────────────────

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith("admin_reset_default:")) return;

        String serverId = componentId.split(":")[1];
        if (event.getGuild() == null || !event.getGuild().getId().equals(serverId)) return;

        Member member = event.getMember();
        if (member == null
                || (!event.getGuild().getOwnerId().equals(member.getId())
                && !member.hasPermission(Permission.ADMINISTRATOR))) {
            event.reply("❌ Bạn không có quyền reset cấu hình!").setEphemeral(true).queue();
            return;
        }

        // Xóa DB + cache + CommandHandler aliases
        repo.deleteById(serverId);
        cache.remove(serverId);
        commandHandler.clearDynamicAliases(serverId); // FIX: sync CommandHandler luôn

        event.replyEmbeds(new EmbedBuilder()
                .setTitle("🔄 Đã reset về mặc định")
                .setDescription(
                        "✅ Prefix: **`" + DEFAULT_PREFIX + "`**\n"
                                + "✅ Kênh bot: **tất cả kênh**\n"
                                + "✅ Role admin: **chưa đặt**\n"
                                + "✅ Alias tùy chỉnh: **đã xóa hết**"
                )
                .setColor(Color.ORANGE)
                .build()
        ).setEphemeral(true).queue();
    }

    // ── Kiểm tra quyền ───────────────────────────────────────────────────

    private boolean isAuthorized(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null) return false;

        if (event.getGuild().getOwnerId().equals(member.getId())) return true;
        if (member.hasPermission(Permission.ADMINISTRATOR)) return true;

        String adminRoleId = getConfig(event.getGuild().getId()).adminRoleId();
        if (adminRoleId != null) {
            return member.getRoles().stream()
                    .map(Role::getId)
                    .anyMatch(adminRoleId::equals);
        }
        return false;
    }

    // ── /admin setprefix ──────────────────────────────────────────────────

    private void handleSetPrefix(SlashCommandInteractionEvent event, String serverId) {
        String newPrefix = event.getOption("prefix").getAsString();
        if (newPrefix.isBlank() || newPrefix.length() > 5) {
            event.getHook().sendMessage("❌ Prefix phải từ 1–5 ký tự!").queue();
            return;
        }
        saveConfig(serverId, getConfig(serverId).withPrefix(newPrefix));
        event.getHook().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("✅ Đã cập nhật prefix")
                .setDescription(
                        "Prefix mới: **`" + newPrefix + "`**\n"
                                + "Ví dụ: `" + newPrefix + "balance`, `" + newPrefix + "bj 100`"
                )
                .setColor(Color.GREEN)
                .build()
        ).queue();
    }

    // ── /admin setchannel ─────────────────────────────────────────────────

    private void handleSetChannel(SlashCommandInteractionEvent event, String serverId) {
        var    channel = event.getOption("channel").getAsChannel();
        String chanId  = channel.getId();
        saveConfig(serverId, getConfig(serverId).withChannel(chanId));
        event.getHook().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("✅ Đã đặt kênh bot")
                .setDescription("Bot sẽ chỉ phản hồi trong <#" + chanId + ">")
                .setColor(Color.GREEN)
                .build()
        ).queue();
    }

    // ── /admin setadminrole ───────────────────────────────────────────────

    private void handleSetAdminRole(SlashCommandInteractionEvent event, String serverId) {
        Role   role   = event.getOption("role").getAsRole();
        String roleId = role.getId();
        saveConfig(serverId, getConfig(serverId).withAdminRole(roleId));
        event.getHook().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("✅ Đã đặt role admin")
                .setDescription("Thành viên có role **" + role.getName() + "** có thể dùng `/admin`")
                .setColor(Color.GREEN)
                .build()
        ).queue();
    }

    // ── /admin info ───────────────────────────────────────────────────────

    private void handleInfo(SlashCommandInteractionEvent event, String serverId) {
        ServerConfig cfg       = getConfig(serverId);
        String       channel   = cfg.botChannelId() != null ? "<#"  + cfg.botChannelId() + ">"  : "_(tất cả kênh)_";
        String       adminRole = cfg.adminRoleId()  != null ? "<@&" + cfg.adminRoleId()  + ">"  : "_(chưa đặt)_";

        Map<String, String> aliases = getDynamicAliases(serverId);
        StringBuilder aliasSb = new StringBuilder();
        if (aliases.isEmpty()) {
            aliasSb.append("_(chưa có alias tùy chỉnh)_");
        } else {
            aliases.forEach((alias, cmd) ->
                    aliasSb.append("`").append(alias).append("` → `").append(cmd).append("`\n"));
        }

        event.getHook().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("⚙️ Cấu hình Bot — " + event.getGuild().getName())
                .setColor(Color.CYAN)
                .addField("🔤 Prefix",          "`" + cfg.prefix() + "`",  true)
                .addField("📢 Kênh bot",         channel,                    true)
                .addField("🛡️ Role admin",       adminRole,                  true)
                .addField("🔀 Alias tùy chỉnh", aliasSb.toString(),         false)
                .setFooter("Config lưu trong PostgreSQL — tồn tại vĩnh viễn dù redeploy")
                .build()
        ).addActionRow(
                Button.danger("admin_reset_default:" + serverId, "🔄 Reset về mặc định")
        ).queue();
    }

    // ── /admin alias add ──────────────────────────────────────────────────

    private void handleAliasAdd(SlashCommandInteractionEvent event, String serverId) {
        String alias   = event.getOption("alias").getAsString().toLowerCase().trim();
        String command = event.getOption("command").getAsString().toLowerCase().trim();

        // Validate qua CommandHandler (đã có đủ kiểm tra)
        try {
            commandHandler.addAlias(serverId, alias, command);
        } catch (IllegalArgumentException e) {
            event.getHook().sendMessage("❌ " + e.getMessage()).queue();
            return;
        }

        // Lưu vào DB/cache
        ServerConfig cfg     = getConfig(serverId);
        Map<String, String> updated = new HashMap<>(cfg.aliases() != null ? cfg.aliases() : Map.of());
        updated.put(CommandHandler.stripPrefix(alias), CommandHandler.stripPrefix(command));
        // Lưu thẳng vào cache+DB mà không gọi addDynamicAlias() để tránh addAlias() bị gọi 2 lần
        cache.put(serverId, cfg.withAliases(updated));
        persistToDb(serverId, cfg.withAliases(updated));

        event.getHook().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("✅ Đã thêm alias")
                .setDescription(
                        "`" + alias + "` → `" + command + "`\n"
                                + "Giờ dùng được: `" + getConfig(serverId).prefix() + alias + "`"
                )
                .setColor(Color.GREEN)
                .build()
        ).queue();
    }

    // ── /admin alias remove ───────────────────────────────────────────────

    private void handleAliasRemove(SlashCommandInteractionEvent event, String serverId) {
        String alias = event.getOption("alias").getAsString().toLowerCase().trim();

        if (!getDynamicAliases(serverId).containsKey(alias)) {
            event.getHook().sendMessage(
                    "❌ Alias `" + alias + "` không tồn tại!\n"
                            + "Dùng `/admin alias list` để xem danh sách alias hiện tại."
            ).queue();
            return;
        }

        // Xóa khỏi CommandHandler
        try {
            commandHandler.removeAlias(serverId, alias);
        } catch (Exception e) {
            log.warn("removeAlias từ CommandHandler lỗi: {}", e.getMessage());
        }

        // Xóa khỏi DB/cache
        ServerConfig cfg     = getConfig(serverId);
        Map<String, String> updated = new HashMap<>(cfg.aliases() != null ? cfg.aliases() : Map.of());
        updated.remove(CommandHandler.stripPrefix(alias));
        cache.put(serverId, cfg.withAliases(updated));
        persistToDb(serverId, cfg.withAliases(updated));

        event.getHook().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("✅ Đã xóa alias")
                .setDescription("Alias `" + alias + "` đã bị xóa.")
                .setColor(Color.ORANGE)
                .build()
        ).queue();
    }

    // ── /admin alias list ─────────────────────────────────────────────────

    private void handleAliasList(SlashCommandInteractionEvent event, String serverId) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("🔀 Alias của server: " + event.getGuild().getName())
                .setColor(Color.CYAN);

        // Alias mặc định (built-in từ từng Command class)
        StringBuilder defaultSb = new StringBuilder();
        commandHandler.getAllCommandNames().stream().sorted().forEach(name -> {
            Command cmd = commandHandler.getCommand(name);
            if (cmd != null && !cmd.getAliases().isEmpty()) {
                cmd.getAliases().stream()
                        .map(CommandHandler::stripPrefix)
                        .filter(a -> !a.equals(name))
                        .forEach(a -> defaultSb
                                .append("`").append(a).append("` → `").append(name).append("`\n"));
            }
        });
        if (!defaultSb.isEmpty()) {
            eb.addField("📌 Alias mặc định (built-in)", defaultSb.toString(), false);
        }

        // Alias tùy chỉnh (lưu trong PostgreSQL)
        Map<String, String> dynamicAliases = getDynamicAliases(serverId);
        if (dynamicAliases.isEmpty()) {
            eb.addField("🔧 Alias tùy chỉnh",
                    "_(chưa có)_\nDùng `/admin alias add <alias> <command>` để thêm", false);
        } else {
            StringBuilder dynSb = new StringBuilder();
            dynamicAliases.forEach((alias, cmd) ->
                    dynSb.append("`").append(alias).append("` → `").append(cmd).append("`\n"));
            eb.addField("🔧 Alias tùy chỉnh (" + dynamicAliases.size() + ")", dynSb.toString(), false);
        }

        eb.setFooter("Alias tùy chỉnh lưu trong PostgreSQL — tồn tại vĩnh viễn");
        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }
}