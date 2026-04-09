package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.service.UserService;
import com.vinhtran.dogbot.session.SoloSession;
import com.vinhtran.dogbot.session.SoloSessionService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;

import java.awt.*;

@Component
@RequiredArgsConstructor
public class SoloCommand implements Command {

    private final UserService userService;
    private final SoloSessionService soloService;

    @Override
    public String getName() { return "!solo"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String challengerId   = event.getAuthor().getId();
        String challengerName = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getAuthor().getName();

        userService.getUser(challengerId, challengerName);

        if (args.length < 3) {
            event.getChannel().sendMessage("Dùng: `!solo @user <số coin>`").queue();
            return;
        }

        if (event.getMessage().getMentions().getMembers().isEmpty()) {
            event.getChannel().sendMessage("❌ Hãy tag người muốn solo!").queue();
            return;
        }

        var target        = event.getMessage().getMentions().getMembers().get(0);
        String targetId   = target.getId();
        String targetName = target.getEffectiveName();

        if (targetId.equals(challengerId)) {
            event.getChannel().sendMessage("❌ Không thể solo với chính mình!").queue();
            return;
        }

        if (target.getUser().isBot()) {
            event.getChannel().sendMessage("❌ Không thể solo với bot!").queue();
            return;
        }

        long bet;
        try {
            String rawBet = args[args.length - 1];
            long balance = userService.getBalance(challengerId);
            bet = rawBet.equalsIgnoreCase("all") ? balance : Long.parseLong(rawBet);
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Số coin không hợp lệ!").queue();
            return;
        }

        if (bet <= 0) {
            event.getChannel().sendMessage("❌ Cược phải lớn hơn 0!").queue();
            return;
        }

        long challengerBalance = userService.getBalance(challengerId);
        if (challengerBalance < bet) {
            event.getChannel().sendMessage("❌ Bạn không đủ coin! Số dư: **" + challengerBalance + " coin**").queue();
            return;
        }

        if (soloService.getByUser(challengerId) != null) {
            event.getChannel().sendMessage("❌ Bạn đang có ván solo chưa xong!").queue();
            return;
        }

        if (soloService.getByUser(targetId) != null) {
            event.getChannel().sendMessage("❌ **" + targetName + "** đang có ván solo chưa xong!").queue();
            return;
        }

        userService.getUser(targetId, targetName);
        long targetBalance = userService.getBalance(targetId);
        if (targetBalance < bet) {
            event.getChannel().sendMessage("❌ **" + targetName + "** không đủ coin! Số dư: **" + targetBalance + " coin**").queue();
            return;
        }

        SoloSession session = new SoloSession(challengerId, challengerName, targetId, targetName, bet);
        soloService.save(session);

        // Lưu message ID ngay khi gửi để dùng cho editMessageEmbedsById sau này
        event.getChannel().sendMessageEmbeds(
                new EmbedBuilder()
                        .setTitle("⚔️ Thách đấu Solo!")
                        .setDescription("**" + challengerName + "** thách **" + targetName + "** solo!\n"
                                + "💰 Cược: **" + bet + " coin**\n\n"
                                + "<@" + targetId + "> bạn có chấp nhận không?")
                        .setColor(Color.ORANGE)
                        .setFooter("Hết hạn sau 60 giây")
                        .build()
        ).setActionRow(
                Button.success("solo_accept:" + challengerId, "✅ Chấp nhận"),
                Button.danger("solo_decline:" + challengerId, "❌ Từ chối")
        ).queue(msg -> session.setPublicMessageId(msg.getId())); // ← lưu ID tại đây
    }
}