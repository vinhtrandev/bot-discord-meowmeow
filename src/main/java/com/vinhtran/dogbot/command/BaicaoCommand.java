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
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaicaoCommand implements Command {

    private final UserService userService;
    private final GameService gameService;
    private final ShopService shopService;
    private final CoupleService coupleService;

    // Lưu session tạm: userId -> [game, playerHand, botHand, bet, username]
    private final Map<String, BaicaoSession> sessions = new ConcurrentHashMap<>();

    record BaicaoSession(BaicaoGame game, BaicaoGame.Hand playerHand,
                         BaicaoGame.Hand botHand, long bet, String username) {}

    @Override
    public String getName() {
        return "!baicao";
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String userId   = event.getAuthor().getId();
        String username = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getAuthor().getName();

        try {
            userService.getUser(userId);
        } catch (RuntimeException e) {
            event.getChannel().sendMessage(
                    "❌ Bạn chưa đăng ký! Dùng `!register` trước nhé.").queue();
            return;
        }

        if (args.length < 2) {
            event.getChannel().sendMessage("Dùng: `!baicao <số coin hoặc all>`").queue();
            return;
        }

        if (sessions.containsKey(userId)) {
            event.getChannel().sendMessage(
                    "⚠️ Bạn đang có ván chưa xong! Dùng nút **Mở** hoặc **Gấp Đôi**.").queue();
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
                        "Cược không hợp lệ! Số dư: **" + balance + " coin**").queue();
                return;
            }

            // Khởi tạo game & chia bài
            BaicaoGame game       = new BaicaoGame();
            BaicaoGame.Hand pHand = game.dealHand();
            BaicaoGame.Hand bHand = game.dealHand();

            // Lưu session
            sessions.put(userId, new BaicaoSession(game, pHand, bHand, bet, username));

            String skinEmoji = getSkinEmoji(userId);

            // Thông báo cặp đôi
            coupleService.getPartnerId(userId).ifPresent(partnerId -> {
                try {
                    String partnerName = userService.getUser(partnerId).getUsername();
                    String coupleEmoji = coupleService.getCoupleEmoji(userId);
                    event.getChannel().sendMessage(
                            "💖 " + coupleEmoji + " **" + username + "** và **" + partnerName
                                    + "** đang chơi Bài Cào! 💕").queue();
                } catch (Exception ignored) {}
            });

            // Gửi ảnh 2 lá ngửa + lá 3 úp, kèm nút MỞ / GẤP ĐÔI
            try (InputStream img = game.getTableImageHidden(pHand, bHand, username)) {
                event.getChannel().sendMessageEmbeds(
                                new EmbedBuilder()
                                        .setTitle(skinEmoji + " Bài Cào")
                                        .setDescription("💰 Cược: **" + bet + " coin**\n"
                                                + "🂠 Lá thứ 3 đang úp — Chọn hành động của bạn!")
                                        .setImage("attachment://baicao.png")
                                        .setFooter("Sáp(x5) > Liêng(x3) > Ba Tây(x2) > Điểm thường(x1)")
                                        .setColor(Color.BLUE)
                                        .build()
                        ).addFiles(FileUpload.fromData(img, "baicao.png"))
                        .setActionRow(
                                Button.success("bc_open:"   + userId + ":" + bet, "👁 Mở"),
                                Button.primary("bc_double:" + userId + ":" + bet, "🔥 Gấp Đôi (x2)")
                        ).queue();
            }

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Số coin không hợp lệ!").queue();
        } catch (Exception e) {
            log.error("Lỗi BaicaoCommand", e);
            event.getChannel().sendMessage("Lỗi: " + e.getMessage()).queue();
        }
    }

    // =====================================================================
    // XỬ LÝ KẾT QUẢ — gọi từ ButtonListener
    // =====================================================================
    public void handleOpen(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event,
                           String userId, long bet) {
        BaicaoSession session = sessions.remove(userId);
        if (session == null) {
            event.reply("Không tìm thấy ván game!").setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();
        resolveGame(event, session, bet, false);
    }

    public void handleDouble(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event,
                             String userId, long bet) {
        BaicaoSession session = sessions.get(userId);
        if (session == null) {
            event.reply("Không tìm thấy ván game!").setEphemeral(true).queue();
            return;
        }

        long balance = userService.getBalance(userId);
        if (balance < bet) {
            event.reply("❌ Không đủ coin để gấp đôi!").setEphemeral(true).queue();
            return;
        }

        sessions.remove(userId);
        event.deferEdit().queue();
        resolveGame(event, session, bet * 2, true);
    }

    private void resolveGame(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event,
                             BaicaoSession session, long finalBet, boolean doubled) {
        BaicaoGame      game   = session.game();
        BaicaoGame.Hand pHand  = session.playerHand();
        BaicaoGame.Hand bHand  = session.botHand();
        String          userId = session.game().toString(); // không dùng — lấy từ button
        String          uname  = session.username();
        String          skinEmoji = getSkinEmoji(event.getUser().getId());

        String result = game.determineResult(pHand, bHand);
        gameService.recordResult(event.getUser().getId(), "BAI_CAO", finalBet, result);

        String msg;
        Color  color;
        switch (result) {
            case "WIN" -> {
                long prize = game.calcPrize(pHand, finalBet);
                msg   = "🎉 **" + uname + "** thắng **" + prize + " coin**!"
                        + (doubled ? " 🔥 *(Gấp đôi)*" : "")
                        + "\nBot: " + bHand.rank();
                color = Color.GREEN;
            }
            case "LOSE" -> {
                msg   = "😢 **" + uname + "** thua **" + finalBet + " coin**"
                        + (doubled ? " 🔥 *(Gấp đôi)*" : "")
                        + "\nBot: " + bHand.rank();
                color = Color.RED;
            }
            default -> {
                msg   = "🤝 Hòa — hoàn lại **" + finalBet + " coin**\nBot: " + bHand.rank();
                color = Color.YELLOW;
            }
        }

        try (InputStream img = game.getTableImageFinal(pHand, bHand, uname)) {
            event.getHook()
                    .editOriginalEmbeds(new EmbedBuilder()
                            .setTitle(skinEmoji + " Bài Cào — Kết quả")
                            .setDescription(msg)
                            .setImage("attachment://baicao.png")
                            .setFooter("Sáp(x5) > Liêng(x3) > Ba Tây(x2) > Điểm thường(x1)")
                            .setColor(color)
                            .build())
                    .setFiles(FileUpload.fromData(img, "baicao.png"))
                    .setComponents()
                    .queue();
        } catch (Exception e) {
            log.error("Lỗi render kết quả BaicaoCommand", e);
        }
    }

    public void clearSession(String userId) {
        sessions.remove(userId);
    }

    private String getSkinEmoji(String userId) {
        String coupleEmoji = coupleService.getCoupleEmoji(userId);
        return !coupleEmoji.isEmpty() ? coupleEmoji : shopService.getEquippedSkinEmoji(userId);
    }
}