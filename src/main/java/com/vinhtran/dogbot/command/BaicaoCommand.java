package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.game.BaicaoGame;
import com.vinhtran.dogbot.service.CoupleService;
import com.vinhtran.dogbot.service.GameService;
import com.vinhtran.dogbot.service.ShopService;
import com.vinhtran.dogbot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaicaoCommand implements Command {

    private final UserService userService;
    private final GameService gameService;
    private final ShopService shopService;
    private final CoupleService coupleService;

    @Override
    public String getName() {
        return "!baicao";
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String userId = event.getAuthor().getId();
        String username = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getAuthor().getName();

        try {
            userService.getUser(userId);
        } catch (RuntimeException e) {
            event.getChannel().sendMessage(
                    "❌ Bạn chưa đăng ký! Dùng `!register` trước nhé."
            ).queue();
            return;
        }

        if (args.length < 2) {
            event.getChannel().sendMessage("Dùng: `!baicao <số coin hoặc all>`").queue();
            return;
        }

        try {
            long balance = userService.getBalance(userId);
            long bet;

            if (args[1].equalsIgnoreCase("all")) {
                if (balance <= 0) {
                    event.getChannel().sendMessage("Bạn không có coin nào!").queue();
                    return;
                }
                bet = balance;
            } else {
                bet = Long.parseLong(args[1]);
            }

            if (bet <= 0 || bet > balance) {
                event.getChannel().sendMessage(
                        "Cược không hợp lệ! Số dư: **" + balance + " coin**"
                ).queue();
                return;
            }

            // Tạo game
            BaicaoGame baicaoGame = new BaicaoGame();
            BaicaoGame.Hand playerHand = baicaoGame.dealHand();
            BaicaoGame.Hand botHand = baicaoGame.dealHand();
            String result = baicaoGame.determineResult(playerHand, botHand);

            // Ghi kết quả
            gameService.recordResult(userId, "BAI_CAO", bet, result);

            String skinEmoji = getSkinEmoji(userId);

            // Thông báo cặp đôi nếu có
            coupleService.getPartnerId(userId).ifPresent(partnerId -> {
                try {
                    String partnerName = userService.getUser(partnerId).getUsername();
                    String coupleEmoji = coupleService.getCoupleEmoji(userId);
                    event.getChannel().sendMessage(
                            "💖 " + coupleEmoji + " **" + username + "** và **" + partnerName
                                    + "** đang trải nghiệm Bài Cào cùng nhau! 💕"
                    ).queue();
                } catch (Exception ignored) {}
            });

            // Tin nhắn kết quả
            String msg;
            Color color;

            switch (result) {
                case "WIN" -> {
                    msg = "🎉 **" + username + "** thắng **" + bet + " coin**!";
                    color = Color.GREEN;
                }
                case "LOSE" -> {
                    msg = "😢 **" + username + "** thua **" + bet + " coin**";
                    color = Color.RED;
                }
                default -> {
                    msg = "🤝 Hòa — hoàn lại **" + bet + " coin**";
                    color = Color.YELLOW;
                }
            }

            // Gửi embed kèm hình bàn bài
            try (InputStream img = baicaoGame.getTableImage(playerHand, botHand)) {
                event.getChannel().sendMessageEmbeds(
                                new EmbedBuilder()
                                        .setTitle(skinEmoji + " Bài Cào — Kết quả")
                                        .addField("🃏 Bài của bạn",
                                                playerHand.cardsText() + "\n**" + playerHand.rank() + "**", true)
                                        .addField("🤖 Bài Bot",
                                                botHand.cardsText() + "\n**" + botHand.rank() + "**", true)
                                        .setDescription(msg)
                                        .setImage("attachment://baicao.png")
                                        .setFooter("Thùng Phá Sảnh > Ba Cô > Thùng > Sảnh > Đôi > Điểm")
                                        .setColor(color)
                                        .build()
                        ).addFiles(FileUpload.fromData(img, "baicao.png"))
                        .queue();
            }

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Số coin không hợp lệ!").queue();
        } catch (Exception e) {
            log.error("Lỗi BaicaoCommand userId={}", userId, e);
            event.getChannel().sendMessage("Lỗi: " + e.getMessage()).queue();
        }
    }

    private String getSkinEmoji(String userId) {
        String coupleEmoji = coupleService.getCoupleEmoji(userId);
        if (!coupleEmoji.isEmpty()) return coupleEmoji;
        return shopService.getEquippedSkinEmoji(userId);
    }
}