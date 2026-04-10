package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.service.BankService;
import com.vinhtran.dogbot.service.UserService;
import com.vinhtran.dogbot.entity.BankAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceCommand implements Command {

    private final UserService userService;
    private final BankService bankService;

    @Override
    public String getName() { return "!balance"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String discordId  = event.getAuthor().getId();
        String serverId   = event.getGuild().getId();
        String authorName = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getAuthor().getName();
        String avatarUrl  = event.getAuthor().getEffectiveAvatarUrl();

        try {
            userService.getOrCreate(discordId, serverId, authorName);
            event.getChannel()
                    .sendMessageEmbeds(buildEmbed(discordId, serverId, authorName, avatarUrl))
                    .queue();
        } catch (Exception e) {
            log.error("Lỗi BalanceCommand userId={}", discordId, e);
            event.getChannel().sendMessage("❌ Không thể tải thông tin tài chính!").queue();
        }
    }

    private MessageEmbed buildEmbed(String discordId, String serverId,
                                    String authorName, String avatarUrl) {
        long wallet = userService.getBalance(discordId, serverId);

        long   bank     = 0;
        String bankInfo = "❌ *Chưa mở tài khoản*";

        try {
            BankAccount ba = bankService.getBank(discordId, serverId);
            if (ba != null) {
                bank     = ba.getBalance();
                bankInfo = String.format("`%s` | **%,d 🪙**",
                        BankService.TIER_NAME[ba.getTier()], bank);
            }
        } catch (Exception ignored) {}

        long total = wallet + bank;

        return new EmbedBuilder()
                .setAuthor("Tài chính của " + authorName, null, avatarUrl)
                .setTitle("🏦 THÔNG TIN TÀI KHOẢN")
                .setColor(new Color(52, 152, 219))
                .addField("👛 Ví tiền",      String.format("**%,d** 🪙", wallet), true)
                .addField("💳 Ngân hàng",    bankInfo,                             true)
                .addBlankField(false)
                .addField("💰 Tổng tài sản", String.format("**%,d** 🪙", total),  false)
                .setThumbnail("https://cdn-icons-png.flaticon.com/512/2489/2489756.png")
                .setFooter("Yêu cầu bởi " + authorName, avatarUrl)
                .setTimestamp(Instant.now())
                .build();
    }
}