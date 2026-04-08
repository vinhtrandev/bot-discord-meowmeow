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

    private final UserService userService;
    private final GameService gameService;
    private final BlackjackSessionService bjService;
    private final ShopService shopService;
    private final CoupleService coupleService;

    @Override
    public String getName() {
        return "!blackjack";
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
                    "❌ Bạn chưa đăng ký! Dùng `!register` trước nhé.").queue();
            return;
        }

        if (args.length < 2) {
            event.getChannel().sendMessage("Dùng: `!blackjack <số coin hoặc all>`").queue();
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

            if (bjService.hasGame(userId)) {
                event.getChannel().sendMessage(
                        "⚠️ Bạn đang có ván chưa xong! (Tự hủy sau 5 phút)").queue();
                return;
            }

            BlackjackGame game = new BlackjackGame();
            bjService.saveGame(userId, game, bet);

            String skinEmoji = getSkinEmoji(userId);

            coupleService.getPartnerId(userId).ifPresent(partnerId -> {
                try {
                    String partnerName = userService.getUser(partnerId).getUsername();
                    String coupleEmoji = coupleService.getCoupleEmoji(userId);
                    event.getChannel().sendMessage(
                            "💖 " + coupleEmoji + " **" + username + "** và **" + partnerName
                                    + "** đang trải nghiệm Xì Dách cùng nhau! 💕").queue();
                } catch (Exception ignored) {
                }
            });

            // Kiểm tra các bộ bài đặc biệt ngay khi chia
            if (game.isPlayerXiBang() && game.isDealerXiBang()) {
                gameService.recordResult(userId, "BLACKJACK", bet, "DRAW");
                bjService.clear(userId);
                sendFinal(event, game, skinEmoji, username,
                        "🤝 Cả hai đều Xì Bàn! Hòa — hoàn lại **" + bet + " coin**",
                        Color.YELLOW);
                return;
            }

            if (game.isPlayerXiBang()) {
                gameService.recordResult(userId, "BLACKJACK", bet * 2, "WIN");
                bjService.clear(userId);
                sendFinal(event, game, skinEmoji, username,
                        "🃏🃏 XÌ BÀN! **" + username + "** thắng **" + (bet * 2) + " coin**! (x2)",
                        Color.YELLOW);
                return;
            }

            if (game.isDealerXiBang()) {
                gameService.recordResult(userId, "BLACKJACK", bet, "LOSE");
                bjService.clear(userId);
                sendFinal(event, game, skinEmoji, username,
                        "😢 Bot có Xì Bàn! **" + username + "** thua **" + bet + " coin**",
                        Color.RED);
                return;
            }

            if (game.isPlayerXiDach() && game.isDealerXiDach()) {
                gameService.recordResult(userId, "BLACKJACK", bet, "DRAW");
                bjService.clear(userId);
                sendFinal(event, game, skinEmoji, username,
                        "🤝 Cả hai đều Xì Dách! Hòa — hoàn lại **" + bet + " coin**",
                        Color.YELLOW);
                return;
            }

            if (game.isPlayerXiDach()) {
                gameService.recordResult(userId, "BLACKJACK", bet, "WIN");
                bjService.clear(userId);
                sendFinal(event, game, skinEmoji, username,
                        "🃏 XÌ DÁCH! **" + username + "** thắng **" + bet + " coin**!",
                        Color.YELLOW);
                return;
            }

            if (game.isDealerXiDach()) {
                gameService.recordResult(userId, "BLACKJACK", bet, "LOSE");
                bjService.clear(userId);
                sendFinal(event, game, skinEmoji, username,
                        "😢 Bot có Xì Dách! **" + username + "** thua **" + bet + " coin**",
                        Color.RED);
                return;
            }

            // Nếu không có bộ đặc biệt thì bắt đầu lượt rút bài
            sendPlaying(event, game, skinEmoji, userId, username, bet, false);

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Số coin không hợp lệ!").queue();
        } catch (Exception e) {
            log.error("Lỗi BlackjackCommand userId={}", userId, e);
            event.getChannel().sendMessage("Lỗi: " + e.getMessage()).queue();
        }
    }

    // username thêm vào để truyền cho ảnh, không ảnh hưởng logic
    public void sendPlaying(MessageReceivedEvent event, BlackjackGame game,
                            String skinEmoji, String userId, String username,
                            long bet, boolean doubled) {
        try {
            InputStream img = game.getTableImagePlaying(username);
            event.getChannel()
                    .sendMessageEmbeds(buildPlayingEmbed(game, skinEmoji, bet, doubled))
                    .addFiles(FileUpload.fromData(img, "table.png"))
                    .setActionRow(
                            Button.success("bj_hit:" + userId + ":" + bet, "🃏 Rút bài"),
                            Button.danger("bj_stand:" + userId + ":" + bet, "✋ Dừng"),
                            Button.primary("bj_double:" + userId + ":" + bet, "🔥 Gấp Đôi Cược")
                    ).queue();
        } catch (Exception e) {
            log.error("Lỗi sendPlaying userId={}", userId, e);
            event.getChannel().sendMessage("Lỗi render bài!").queue();
        }
    }

    public void sendFinal(MessageReceivedEvent event, BlackjackGame game,
                          String skinEmoji, String username, String msg, Color color) {
        try {
            InputStream img = game.getTableImageFinal(username);
            event.getChannel()
                    .sendMessageEmbeds(buildFinalEmbed(skinEmoji, msg, color))
                    .addFiles(FileUpload.fromData(img, "table.png"))
                    .queue();
        } catch (Exception e) {
            log.error("Lỗi sendFinal", e);
            event.getChannel()
                    .sendMessageEmbeds(buildFinalEmbed(skinEmoji, msg, color))
                    .queue();
        }
    }

    public MessageEmbed buildPlaying(BlackjackGame game, String skinEmoji,
                                     long bet, boolean doubled) {
        return buildPlayingEmbed(game, skinEmoji, bet, doubled);
    }

    public MessageEmbed buildFinal(BlackjackGame game, String skinEmoji,
                                   String msg, Color color) {
        return buildFinalEmbed(skinEmoji, msg, color);
    }

    private MessageEmbed buildPlayingEmbed(BlackjackGame game, String skinEmoji,
                                           long bet, boolean doubled) {
        String betLine = "💰 Cược: **" + bet + " coin**" + (doubled ? " 🔥 *(Gấp đôi)*" : "");
        String hint = game.canPlayerStand() ? ""
                : "\n⚠️ Chưa đủ 16 điểm — Dừng sẽ bị tính **Dằn non**!";

        return new EmbedBuilder()
                .setTitle(skinEmoji + " Xì Dách")
                .setDescription(betLine + hint)
                .setImage("attachment://table.png")
                .setFooter("Xì Bàn > Xì Dách > Ngũ Linh > 21 > Thường | Dealer dừng ≥ 15đ")
                .setColor(Color.BLUE)
                .build();
    }

    private MessageEmbed buildFinalEmbed(String skinEmoji, String msg, Color color) {
        return new EmbedBuilder()
                .setTitle(skinEmoji + " Xì Dách — Kết quả")
                .setDescription(msg)
                .setImage("attachment://table.png")
                .setColor(color)
                .build();
    }

    public String getSkinEmoji(String userId) {
        String coupleEmoji = coupleService.getCoupleEmoji(userId);
        if (!coupleEmoji.isEmpty()) return coupleEmoji;
        return shopService.getEquippedSkinEmoji(userId);
    }
}