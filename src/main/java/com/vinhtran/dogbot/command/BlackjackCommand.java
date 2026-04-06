package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.game.BlackjackGame;
import com.vinhtran.dogbot.service.CoupleService;
import com.vinhtran.dogbot.service.GameService;
import com.vinhtran.dogbot.service.ShopService;
import com.vinhtran.dogbot.service.UserService;
import com.vinhtran.dogbot.session.BlackjackSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlackjackCommand implements Command {

    private final UserService             userService;
    private final GameService             gameService;
    private final BlackjackSessionService bjService;
    private final ShopService             shopService;
    private final CoupleService           coupleService;

    @Override
    public String getName() { return "!blackjack"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String userId   = event.getAuthor().getId();
        String username = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();

        // 1. Kiểm tra đăng ký
        try {
            userService.getUser(userId);
        } catch (RuntimeException e) {
            event.getChannel().sendMessage("❌ Bạn chưa đăng ký! Dùng `!register` trước nhé.").queue();
            return;
        }

        // 2. Kiểm tra cược
        if (args.length < 2) {
            event.getChannel().sendMessage("Dùng: `!blackjack <số coin hoặc all>`").queue();
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

            if (bjService.hasGame(userId)) {
                event.getChannel().sendMessage("⚠️ Bạn đang có ván chưa xong!").queue();
                return;
            }

            // 3. Khởi tạo Game
            BlackjackGame game = new BlackjackGame();
            bjService.saveGame(userId, game, bet);
            String skinEmoji = getSkinEmoji(userId);

            // Xử lý Cặp đôi
            coupleService.getPartnerId(userId).ifPresent(partnerId -> {
                try {
                    String partnerName = userService.getUser(partnerId).getUsername();
                    String coupleEmoji = coupleService.getCoupleEmoji(userId);
                    event.getChannel().sendMessage("💖 " + coupleEmoji + " **" + username + "** và **" + partnerName + "** đang chơi Xì Dách! 💕").queue();
                } catch (Exception ignored) {}
            });

            // 4. Kiểm tra bài đặc biệt ngay khi chia (Xì Bàn / Xì Dách)
            if (game.isPlayerXiBang() || game.isDealerXiBang() || game.isPlayerXiDach() || game.isDealerXiDach()) {
                handleInstantWinLose(event, game, userId, username, bet, skinEmoji);
                return;
            }

            // 5. Nếu bài bình thường -> Bắt đầu lượt rút
            sendPlaying(event, game, skinEmoji, userId, bet, false);

        } catch (Exception e) {
            log.error("Lỗi BlackjackCommand", e);
            event.getChannel().sendMessage("Lỗi: " + e.getMessage()).queue();
        }
    }

    private void handleInstantWinLose(MessageReceivedEvent event, BlackjackGame game, String userId, String username, long bet, String skinEmoji) {
        String res = "DRAW";
        String msg = "";
        Color col = Color.YELLOW;

        if (game.isPlayerXiBang() && game.isDealerXiBang()) { msg = "🤝 Cả hai đều Xì Bàn! Hòa"; }
        else if (game.isPlayerXiBang()) { msg = "🃏🃏 XÌ BÀN! **" + username + "** thắng **" + (bet * 2) + "** (x2)"; res = "WIN"; }
        else if (game.isDealerXiBang()) { msg = "😢 Bot có Xì Bàn! Bạn thua"; res = "LOSE"; col = Color.RED; }
        else if (game.isPlayerXiDach() && game.isDealerXiDach()) { msg = "🤝 Cả hai đều Xì Dách! Hòa"; }
        else if (game.isPlayerXiDach()) { msg = "🃏 XÌ DÁCH! **" + username + "** thắng **" + bet + "**"; res = "WIN"; }
        else if (game.isDealerXiDach()) { msg = "😢 Bot có Xì Dách! Bạn thua"; res = "LOSE"; col = Color.RED; }

        gameService.recordResult(userId, "BLACKJACK", (res.equals("WIN") && game.isPlayerXiBang() ? bet * 2 : bet), res);
        bjService.clear(userId);
        sendFinal(event, game, skinEmoji, msg, col);
    }

    public void sendPlaying(MessageReceivedEvent event, BlackjackGame game, String skinEmoji, String userId, long bet, boolean doubled) {
        try (InputStream img = game.getTableImagePlaying()) {
            event.getChannel()
                    .sendMessageEmbeds(buildPlayingEmbed(game, skinEmoji, bet, doubled))
                    .addFiles(FileUpload.fromData(img, "table.png"))
                    .setActionRow(
                            Button.success("bj_hit:"    + userId + ":" + bet, "🃏 Rút"),
                            Button.danger ("bj_stand:"  + userId + ":" + bet, "✋ Dừng"),
                            Button.primary("bj_double:" + userId + ":" + bet, "🔥 Gấp đôi")
                    ).queue();
        } catch (Exception e) { log.error("Lỗi sendPlaying", e); }
    }

    public void sendFinal(MessageReceivedEvent event, BlackjackGame game, String skinEmoji, String msg, Color color) {
        try (InputStream img = game.getTableImageFinal()) {
            event.getChannel()
                    .sendMessageEmbeds(buildFinalEmbed(skinEmoji, msg, color))
                    .addFiles(FileUpload.fromData(img, "table.png"))
                    .queue();
        } catch (Exception e) { log.error("Lỗi sendFinal", e); }
    }

    private MessageEmbed buildPlayingEmbed(BlackjackGame game, String skinEmoji, long bet, boolean doubled) {
        String betLine = "💰 Cược: **" + bet + " coin**" + (doubled ? " 🔥" : "");
        String hint = game.canPlayerStand() ? "" : "\n⚠️ Chưa đủ 16 điểm (Dằn non)";

        return new EmbedBuilder()
                .setTitle(skinEmoji + " Xì Dách")
                .setDescription(betLine + hint)
                .setImage("attachment://table.png")
                .setFooter("Xì Bàn > Xì Dách > Ngũ Linh > 21 | Dealer dừng ≥ 15đ")
                .setColor(Color.BLUE).build();
    }

    private MessageEmbed buildFinalEmbed(String skinEmoji, String msg, Color color) {
        return new EmbedBuilder()
                .setTitle(skinEmoji + " Xì Dách — Kết quả")
                .setDescription(msg)
                .setImage("attachment://table.png")
                .setColor(color).build();
    }

    public String getSkinEmoji(String userId) {
        String emoji = coupleService.getCoupleEmoji(userId);
        return emoji.isEmpty() ? shopService.getEquippedSkinEmoji(userId) : emoji;
    }
}