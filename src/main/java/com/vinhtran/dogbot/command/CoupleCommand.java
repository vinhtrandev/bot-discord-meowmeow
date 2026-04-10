package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.entity.CoupleRelation;
import com.vinhtran.dogbot.service.CoupleService;
import com.vinhtran.dogbot.service.UserService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class CoupleCommand implements Command {

    private final CoupleService coupleService;
    private final UserService   userService;

    // Lưu pending breakup: userId -> serverId (chờ xác nhận)
    private final Map<String, String> pendingBreakUp = new ConcurrentHashMap<>();

    @Override
    public String getName() { return "!couple"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String userId   = event.getAuthor().getId();
        String serverId = event.getGuild().getId();

        if (args.length < 2) { showHelp(event); return; }

        switch (args[1].toLowerCase()) {
            case "tang"    -> handlePropose(event, userId, serverId, args);
            case "nhan"    -> handleAccept(event, userId, serverId);
            case "tu_choi" -> handleDecline(event, userId, serverId);
            case "huy"     -> handleBreakUp(event, userId, serverId);
            case "xem"     -> handleView(event, userId, serverId);
            default        -> showHelp(event);
        }
    }

    private void handlePropose(MessageReceivedEvent event, String userId,
                               String serverId, String[] args) {
        if (event.getMessage().getMentions().getUsers().isEmpty() || args.length < 4) {
            event.getChannel().sendMessage("Dùng: `!couple tang @nguoi <item_id>`").queue();
            return;
        }
        try {
            String targetId   = event.getMessage().getMentions().getUsers().get(0).getId();
            String targetName = event.getMessage().getMentions().getUsers().get(0).getName();
            String ringItemId = args[3];

            coupleService.propose(userId, targetId, serverId, ringItemId);

            event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("💍 Lời Mời Cặp Đôi!")
                    .setDescription("**" + event.getAuthor().getName() + "** đã gửi nhẫn đến **" + targetName + "**!\n\n"
                            + targetName + " hãy dùng `!couple nhan` để chấp nhận\n"
                            + "hoặc `!couple tu_choi` để từ chối.")
                    .setColor(Color.PINK).build()).queue();
        } catch (Exception e) {
            event.getChannel().sendMessage("❌ " + e.getMessage()).queue();
        }
    }

    private void handleAccept(MessageReceivedEvent event, String userId, String serverId) {
        try {
            CoupleRelation relation   = coupleService.accept(userId, serverId);
            String         partnerId  = relation.getUserAId();
            String         emoji      = coupleService.getCoupleEmoji(userId, serverId);

            String partnerName;
            try {
                var member = event.getGuild().getMemberById(partnerId);
                partnerName = member != null ? member.getEffectiveName() : partnerId;
            } catch (Exception ignored) { partnerName = partnerId; }

            event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("💖 Ghép Đôi Thành Công!")
                    .setDescription(emoji + " **" + event.getAuthor().getName()
                            + "** và **" + partnerName + "** đã trở thành cặp đôi!\n\n"
                            + "Khi cùng chơi sẽ có skin cặp đặc biệt 💕")
                    .setColor(Color.PINK).build()).queue();
        } catch (Exception e) {
            event.getChannel().sendMessage("❌ " + e.getMessage()).queue();
        }
    }

    private void handleDecline(MessageReceivedEvent event, String userId, String serverId) {
        try {
            coupleService.decline(userId, serverId);
            event.getChannel().sendMessage("Đã từ chối lời mời cặp đôi.").queue();
        } catch (Exception e) {
            event.getChannel().sendMessage("❌ " + e.getMessage()).queue();
        }
    }

    private void handleBreakUp(MessageReceivedEvent event, String userId, String serverId) {
        try {
            Optional<CoupleRelation> couple = coupleService.getCouple(userId, serverId);
            if (couple.isEmpty()) { event.getChannel().sendMessage("Bạn chưa có cặp đôi!").queue(); return; }

            // FIX: resolve partnerId từ relation thay vì dùng getPartnerId()
            CoupleRelation r = couple.get();
            String partnerId = r.getUserAId().equals(userId) ? r.getUserBId() : r.getUserAId();

            // Lưu pending: userId -> serverId
            pendingBreakUp.put(userId, serverId);

            final String finalPartnerId = partnerId;
            event.getGuild().retrieveMemberById(partnerId).queue(
                    member -> {
                        String partnerName = member != null ? member.getEffectiveName() : finalPartnerId;
                        sendBreakUpConfirm(event, userId, partnerName);
                    },
                    err -> sendBreakUpConfirm(event, userId, finalPartnerId)
            );

        } catch (Exception e) {
            event.getChannel().sendMessage("❌ " + e.getMessage()).queue();
        }
    }

    private void sendBreakUpConfirm(MessageReceivedEvent event, String userId, String partnerName) {
        event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setTitle("💔 Xác Nhận Chia Tay")
                        .setDescription("Bạn có chắc muốn chia tay **" + partnerName + "** không?\n\n"
                                + "⚠️ Hành động này không thể hoàn tác!")
                        .setColor(Color.RED).build())
                .setActionRow(
                        Button.danger  ("couple_breakup_confirm:" + userId, "💔 Xác nhận chia tay"),
                        Button.secondary("couple_breakup_cancel:"  + userId, "❌ Hủy bỏ")
                ).queue();
    }

    // ── Xử lý nút xác nhận / hủy ─────────────────────────────────────────

    public void handleBreakUpConfirm(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event,
                                     String userId) {
        String serverId = pendingBreakUp.remove(userId);
        if (serverId == null) {
            event.reply("❌ Không tìm thấy yêu cầu chia tay!").setEphemeral(true).queue();
            return;
        }

        if (!event.getUser().getId().equals(userId)) {
            event.reply("❌ Bạn không thể xác nhận thay người khác!").setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();

        try {
            Optional<CoupleRelation> couple = coupleService.getCouple(userId, serverId);
            String partnerId = couple.map(r -> r.getUserAId().equals(userId) ? r.getUserBId() : r.getUserAId())
                    .orElse(userId);

            coupleService.breakUp(userId, serverId);

            final String callerName = event.getUser().getName();
            event.getGuild().retrieveMemberById(partnerId).queue(
                    member -> {
                        String partnerName = member != null ? member.getEffectiveName() : partnerId;
                        event.getHook().editOriginalEmbeds(new EmbedBuilder()
                                        .setTitle("💔 Chia Tay")
                                        .setDescription("**" + callerName + "** và **" + partnerName
                                                + "** đã chia tay.\nRất tiếc...")
                                        .setColor(Color.GRAY).build())
                                .setComponents().queue();
                    },
                    err -> event.getHook().editOriginalEmbeds(new EmbedBuilder()
                                    .setTitle("💔 Chia Tay")
                                    .setDescription("**" + callerName + "** đã chia tay.\nRất tiếc...")
                                    .setColor(Color.GRAY).build())
                            .setComponents().queue()
            );
        } catch (Exception e) {
            event.getHook().sendMessage("❌ " + e.getMessage()).setEphemeral(true).queue();
        }
    }

    public void handleBreakUpCancel(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event,
                                    String userId) {
        // Chỉ cho phép chính user đó bấm
        if (!event.getUser().getId().equals(userId)) {
            event.reply("❌ Bạn không thể hủy thay người khác!").setEphemeral(true).queue();
            return;
        }

        pendingBreakUp.remove(userId);

        event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("✅ Đã Hủy")
                        .setDescription("Bạn đã hủy yêu cầu chia tay. Cặp đôi vẫn còn! 💕")
                        .setColor(Color.GREEN).build())
                .setComponents()
                .queue();
    }

    private void handleView(MessageReceivedEvent event, String userId, String serverId) {
        coupleService.getCouple(userId, serverId).ifPresentOrElse(r -> {
            String partnerId = r.getUserAId().equals(userId) ? r.getUserBId() : r.getUserAId();
            String emoji     = coupleService.getCoupleEmoji(userId, serverId);

            event.getGuild().retrieveMemberById(partnerId).queue(
                    member -> {
                        String partnerName = member != null ? member.getEffectiveName() : partnerId;
                        event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                                .setTitle("💕 Thông Tin Cặp Đôi")
                                .addField("Đối tác", emoji + " **" + partnerName + "**", true)
                                .addField("Nhẫn",    r.getRingItemId(),                  true)
                                .setColor(Color.PINK).build()).queue();
                    },
                    err -> event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                            .setTitle("💕 Thông Tin Cặp Đôi")
                            .addField("Đối tác", emoji + " **" + partnerId + "**", true)
                            .addField("Nhẫn",    r.getRingItemId(),                true)
                            .setColor(Color.PINK).build()).queue()
            );
        }, () -> event.getChannel().sendMessage("Bạn chưa có cặp đôi! Dùng `!couple tang @ai_đó <ring_id>`").queue());
    }

    private void showHelp(MessageReceivedEvent event) {
        event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("💍 Hệ Thống Cặp Đôi")
                .setColor(Color.PINK)
                .addField("!couple tang @user <ring_id>", "Tặng nhẫn cầu hôn",    false)
                .addField("!couple nhan",                  "Chấp nhận lời mời",    false)
                .addField("!couple tu_choi",               "Từ chối lời mời",      false)
                .addField("!couple huy",                   "Hủy cặp đôi",          false)
                .addField("!couple xem",                   "Xem thông tin cặp đôi",false)
                .build()).queue();
    }
}