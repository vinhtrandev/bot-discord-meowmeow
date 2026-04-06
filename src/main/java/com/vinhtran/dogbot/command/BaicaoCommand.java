package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.game.BaicaoGame;
import com.vinhtran.dogbot.service.CoupleService;
import com.vinhtran.dogbot.service.GameService;
import com.vinhtran.dogbot.service.ShopService;
import com.vinhtran.dogbot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
    public String getName() { return "!baicao"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String userId = event.getAuthor().getId();
        String username = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();

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
            if (args[1].equalsIgnoreCase("all")) bet = balance;
            else bet = Long.parseLong(args[1]);

            if (bet <= 0 || bet > balance) {
                event.getChannel().sendMessage("Cược không hợp lệ! Số dư: **" + balance + " coin**").queue();
                return;
            }

            // Tạo game và lấy kết quả
            BaicaoGame baicaoGame = new BaicaoGame();
            BaicaoGame.Hand playerHand = baicaoGame.dealHand();
            BaicaoGame.Hand botHand = baicaoGame.dealHand();
            String result = baicaoGame.determineResult(playerHand, botHand);

            gameService.recordResult(userId, "BAI_CAO", bet, result);
            String skinEmoji = getSkinEmoji(userId);

            // Gửi ảnh bàn bài (Truyền rank String vào để vẽ chữ đẹp)
            try (InputStream img = com.vinhtran.dogbot.util.CardImageGenerator.drawBaicaoTable(
                    playerHand.cards(), playerHand.rank(),
                    botHand.cards(), botHand.rank())) {

                event.getChannel().sendMessageEmbeds(buildFinalEmbed(username, bet, result, skinEmoji))
                        .addFiles(FileUpload.fromData(img, "baicao.png"))
                        .queue();
            }

        } catch (Exception e) {
            log.error("Lỗi BaicaoCommand", e);
            event.getChannel().sendMessage("Lỗi: " + e.getMessage()).queue();
        }
    }

    private MessageEmbed buildFinalEmbed(String username, long bet, String result, String skinEmoji) {
        String msg;
        Color color;
        switch (result) {
            case "WIN"  -> { msg = "🎉 **" + username + "** thắng **" + bet + " coin**!"; color = Color.GREEN; }
            case "LOSE" -> { msg = "😢 **" + username + "** thua **" + bet + " coin**"; color = Color.RED; }
            default     -> { msg = "🤝 Hòa — hoàn lại **" + bet + " coin**"; color = Color.YELLOW; }
        }

        return new EmbedBuilder()
                .setTitle(skinEmoji + " Bài Cào — Kết quả")
                .setDescription(msg)
                .setImage("attachment://baicao.png")
                .setFooter("Thùng Phá Sảnh > Ba Cô > Thùng > Sảnh > Đôi > Điểm")
                .setColor(color)
                .build();
    }

    private String getSkinEmoji(String userId) {
        String emoji = coupleService.getCoupleEmoji(userId);
        return emoji.isEmpty() ? shopService.getEquippedSkinEmoji(userId) : emoji;
    }
}