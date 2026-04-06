package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.game.BaicaoGame;
import com.vinhtran.dogbot.service.CoupleService;
import com.vinhtran.dogbot.service.GameService;
import com.vinhtran.dogbot.service.ShopService;
import com.vinhtran.dogbot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.InputStream;
import java.util.stream.Collectors;

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
            event.getChannel().sendMessage("❌ Bạn chưa đăng ký! Dùng `!register` trước nhé.").queue();
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
                event.getChannel().sendMessage("Cược không hợp lệ! Số dư: **" + balance + " coin**").queue();
                return;
            }

            // Khởi tạo Game (Bản mặc định)
            BaicaoGame baicaoGame = new BaicaoGame();
            BaicaoGame.Hand playerHand = baicaoGame.dealHand();
            BaicaoGame.Hand botHand = baicaoGame.dealHand();
            String result = baicaoGame.determineResult(playerHand, botHand);

            // Ghi kết quả
            gameService.recordResult(userId, "BAI_CAO", bet, result);

            String skinEmoji = getSkinEmoji(userId);

            // Thông báo cặp đôi
            coupleService.getPartnerId(userId).ifPresent(partnerId -> {
                try {
                    String partnerName = userService.getUser(partnerId).getUsername();
                    String coupleEmoji = coupleService.getCoupleEmoji(userId);
                    event.getChannel().sendMessage("💖 " + coupleEmoji + " **" + username + "** và **" + partnerName + "** đang chơi Bài Cào! 💕").queue();
                } catch (Exception ignored) {}
            });

            // Xử lý thông báo Text
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

            // Chuyển List<Integer> thành String để hiển thị trong Embed (Fallback nếu ảnh lỗi)
            String playerCardsText = playerHand.cards().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(" "));
            String botCardsText = botHand.cards().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(" "));

            // Gửi Embed kết quả kèm hình ảnh từ Generator
            try (InputStream img = baicaoGame.getTableImage(playerHand, botHand)) {
                event.getChannel().sendMessageEmbeds(
                        new EmbedBuilder()
                                .setTitle(skinEmoji + " Bài Cào — Kết quả")
                                .setDescription(msg) // msg là chuỗi "Thắng/Thua/Hòa"
                                .setImage("attachment://baicao.png")
                                .setFooter("Bot Casino | Dealer: Random")
                                .setColor(color)
                                .build()
                ).addFiles(FileUpload.fromData(img, "baicao.png")).queue();
            }

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Số coin không hợp lệ!").queue();
        } catch (Exception e) {
            log.error("Lỗi BaicaoCommand", e);
            event.getChannel().sendMessage("Lỗi: " + e.getMessage()).queue();
        }
    }

    private String getSkinEmoji(String userId) {
        String coupleEmoji = coupleService.getCoupleEmoji(userId);
        return !coupleEmoji.isEmpty() ? coupleEmoji : shopService.getEquippedSkinEmoji(userId);
    }
}