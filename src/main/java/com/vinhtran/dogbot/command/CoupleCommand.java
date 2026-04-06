package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.entity.CoupleRelation;
import com.vinhtran.dogbot.service.CoupleService;
import com.vinhtran.dogbot.service.UserService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CoupleCommand implements Command {

    private final CoupleService coupleService;
    private final UserService   userService;

    @Override
    public String getName() { return "!couple"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String userId   = event.getAuthor().getId();
        String username = event.getAuthor().getName();

        if (args.length < 2) {
            showHelp(event);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "tang"   -> handlePropose(event, userId, args);
            case "nhan"   -> handleAccept(event, userId);
            case "tu_choi"-> handleDecline(event, userId);
            case "huy"    -> handleBreakUp(event, userId, username);
            case "xem"    -> handleView(event, userId);
            default       -> showHelp(event);
        }
    }

    private void handlePropose(MessageReceivedEvent event, String userId, String[] args) {
        if (event.getMessage().getMentions().getUsers().isEmpty() || args.length < 4) {
            event.getChannel().sendMessage("Dùng: `!couple tang @nguoi <item_id>`\nVí dụ: `!couple tang @Bạn ring_silver`").queue();
            return;
        }
        try {
            String targetId   = event.getMessage().getMentions().getUsers().get(0).getId();
            String targetName = event.getMessage().getMentions().getUsers().get(0).getName();
            String ringItemId = args[3];

            coupleService.propose(userId, targetId, ringItemId);

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

    private void handleAccept(MessageReceivedEvent event, String userId) {
        try {
            CoupleRelation relation = coupleService.accept(userId);
            String partnerId   = relation.getUserAId();
            String partnerName = userService.getUser(partnerId).getUsername();
            String emoji       = coupleService.getCoupleEmoji(userId);

            event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("💖 Ghép Đôi Thành Công!")
                    .setDescription(emoji + " **" + event.getAuthor().getName()
                            + "** và **" + partnerName + "** đã trở thành cặp đôi!\n\n"
                            + "Khi cùng chơi Blackjack sẽ có skin cặp đặc biệt 💕")
                    .setColor(Color.PINK).build()).queue();
        } catch (Exception e) {
            event.getChannel().sendMessage("❌ " + e.getMessage()).queue();
        }
    }

    private void handleDecline(MessageReceivedEvent event, String userId) {
        try {
            coupleService.decline(userId);
            event.getChannel().sendMessage("Đã từ chối lời mời cặp đôi.").queue();
        } catch (Exception e) {
            event.getChannel().sendMessage("❌ " + e.getMessage()).queue();
        }
    }

    private void handleBreakUp(MessageReceivedEvent event, String userId, String username) {
        try {
            Optional<CoupleRelation> couple = coupleService.getCouple(userId);
            if (couple.isEmpty()) { event.getChannel().sendMessage("Bạn chưa có cặp đôi!").queue(); return; }

            String partnerId   = coupleService.getPartnerId(userId).orElse("");
            String partnerName = userService.getUser(partnerId).getUsername();

            coupleService.breakUp(userId);

            event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("💔 Chia Tay")
                    .setDescription("**" + username + "** và **" + partnerName + "** đã chia tay.\nRất tiếc...")
                    .setColor(Color.GRAY).build()).queue();
        } catch (Exception e) {
            event.getChannel().sendMessage("❌ " + e.getMessage()).queue();
        }
    }

    private void handleView(MessageReceivedEvent event, String userId) {
        coupleService.getCouple(userId).ifPresentOrElse(r -> {
            String partnerId   = r.getUserAId().equals(userId) ? r.getUserBId() : r.getUserAId();
            String partnerName = userService.getUser(partnerId).getUsername();
            String emoji       = coupleService.getCoupleEmoji(userId);

            event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("💕 Thông Tin Cặp Đôi")
                    .addField("Đối tác", emoji + " **" + partnerName + "**", true)
                    .addField("Nhẫn",    r.getRingItemId(),                   true)
                    .setColor(Color.PINK).build()).queue();
        }, () -> event.getChannel().sendMessage("Bạn chưa có cặp đôi! Dùng `!couple tang @ai đó <ring_id>`").queue());
    }

    private void showHelp(MessageReceivedEvent event) {
        event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("💍 Hệ Thống Cặp Đôi")
                .setColor(Color.PINK)
                .addField("!couple tang @user <ring_id>", "Tặng nhẫn cầu hôn",         false)
                .addField("!couple nhan",                  "Chấp nhận lời mời",          false)
                .addField("!couple tu_choi",               "Từ chối lời mời",            false)
                .addField("!couple huy",                   "Hủy cặp đôi",                false)
                .addField("!couple xem",                   "Xem thông tin cặp đôi",      false)
                .build()).queue();
    }
}