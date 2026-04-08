package com.vinhtran.dogbot.bot.listener;

import com.vinhtran.dogbot.command.BaicaoCommand;
import com.vinhtran.dogbot.command.BlackjackCommand;
import com.vinhtran.dogbot.game.BlackjackGame;
import com.vinhtran.dogbot.game.GameResult;
import com.vinhtran.dogbot.service.*;
import com.vinhtran.dogbot.session.BlackjackSessionService;
import com.vinhtran.dogbot.session.TransferSessionService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class ButtonListener extends ListenerAdapter {

    private final BlackjackSessionService bjService;
    private final TransferSessionService  transferService;
    private final GameService             gameService;
    private final BankService             bankService;
    private final ShopService             shopService;
    private final CoupleService           coupleService;
    private final UserService             userService;
    private final BlackjackCommand        blackjackCommand;
    private final BaicaoCommand           baicaoCommand;  // ← thêm

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        try {
            String[] parts = event.getComponentId().split(":");
            if (parts[0].startsWith("bj_")) {
                handleBlackjack(event, parts);
            } else if (parts[0].startsWith("bc_")) {
                handleBaicao(event, parts);
            } else if (parts[0].equals("transfer_confirm") || parts[0].equals("transfer_cancel")) {
                handleTransfer(event, parts);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!event.isAcknowledged())
                event.reply("Đã xảy ra lỗi!").setEphemeral(true).queue();
        }
    }

    // =========================================================
    // BLACKJACK
    // =========================================================
    private void handleBlackjack(ButtonInteractionEvent event, String[] parts) {
        String action = parts[0].replace("bj_", "");
        String userId = parts[1];
        long   bet    = Long.parseLong(parts[2]);

        if (!event.getUser().getId().equals(userId)) {
            event.reply("Đây không phải ván của bạn!").setEphemeral(true).queue();
            return;
        }

        BlackjackGame game = bjService.getGame(userId);
        if (game == null) {
            event.reply("Không tìm thấy ván game hoặc đã hết giờ (5 phút)!")
                    .setEphemeral(true).queue();
            return;
        }

        String username  = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getUser().getName();
        String skinEmoji = blackjackCommand.getSkinEmoji(userId);

        event.deferEdit().queue();

        switch (action) {

            case "hit" -> {
                game.playerHit();
                if (game.playerBust()) {
                    gameService.recordResult(userId, "BLACKJACK", bet, "LOSE");
                    bjService.clear(userId);
                    editFinal(event, game, skinEmoji, username,
                            "💥 Quắc! **" + username + "** thua **" + bet + " coin**",
                            Color.RED);
                } else if (game.isPlayerNguLinh()) {
                    GameResult result = game.determineResultAfterDealer();
                    long   prize = calcPrize(result, bet, false);
                    String msg   = buildMsg(result, username, bet, prize, false);
                    gameService.recordResult(userId, "BLACKJACK", bet, toSimple(result));
                    bjService.clear(userId);
                    editFinal(event, game, skinEmoji, username, msg, resultColor(result));
                } else {
                    editPlaying(event, game, skinEmoji, userId, username, bet, false);
                }
            }

            case "stand" -> {
                if (!game.canPlayerStand()) {
                    gameService.recordResult(userId, "BLACKJACK", bet, "LOSE");
                    bjService.clear(userId);
                    editFinal(event, game, skinEmoji, username,
                            "🔴 Dằn non! **" + username + "** chưa đủ 16 điểm → Thua **" + bet + " coin**",
                            Color.RED);
                    return;
                }
                GameResult result = game.determineResultAfterDealer();
                long   prize = calcPrize(result, bet, false);
                String msg   = buildMsg(result, username, bet, prize, false);
                gameService.recordResult(userId, "BLACKJACK", bet, toSimple(result));
                bjService.clear(userId);
                editFinal(event, game, skinEmoji, username, msg, resultColor(result));
            }

            case "double" -> {
                long balance = userService.getBalance(userId);
                if (balance < bet) {
                    event.getHook().sendMessage("Không đủ coin để gấp đôi!")
                            .setEphemeral(true).queue();
                    return;
                }
                long newBet = bet * 2;
                bjService.saveGame(userId, game, newBet);
                game.playerHit();

                if (game.playerBust()) {
                    gameService.recordResult(userId, "BLACKJACK", newBet, "LOSE");
                    bjService.clear(userId);
                    editFinal(event, game, skinEmoji, username,
                            "💥 Quắc! **" + username + "** thua **" + newBet + " coin** (Gấp đôi)",
                            Color.RED);
                } else {
                    GameResult result = game.determineResultAfterDealer();
                    long   prize = calcPrize(result, newBet, true);
                    String msg   = buildMsg(result, username, newBet, prize, true);
                    gameService.recordResult(userId, "BLACKJACK", newBet, toSimple(result));
                    bjService.clear(userId);
                    editFinal(event, game, skinEmoji, username, msg, resultColor(result));
                }
            }
        }
    }

    // ── Edit embed đang chơi ─────────────────────────────────────────────
    private void editPlaying(ButtonInteractionEvent event, BlackjackGame game,
                             String skinEmoji, String userId, String username,
                             long bet, boolean doubled) {
        try {
            InputStream img = game.getTableImagePlaying(username);
            event.getHook()
                    .editOriginalEmbeds(blackjackCommand.buildPlaying(game, skinEmoji, bet, doubled))
                    .setFiles(FileUpload.fromData(img, "table.png"))
                    .setActionRow(
                            Button.success("bj_hit:"    + userId + ":" + bet, "🃏 Rút bài"),
                            Button.danger ("bj_stand:"  + userId + ":" + bet, "✋ Dừng"),
                            Button.primary("bj_double:" + userId + ":" + bet, "🔥 Gấp Đôi").asDisabled()
                    ).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Edit embed kết quả blackjack ─────────────────────────────────────
    private void editFinal(ButtonInteractionEvent event, BlackjackGame game,
                           String skinEmoji, String username,
                           String msg, Color color) {
        try {
            InputStream img = game.getTableImageFinal(username);
            event.getHook()
                    .editOriginalEmbeds(blackjackCommand.buildFinal(game, skinEmoji, msg, color))
                    .setFiles(FileUpload.fromData(img, "table.png"))
                    .setComponents()
                    .queue();
        } catch (Exception e) {
            e.printStackTrace();
            event.getHook()
                    .editOriginalEmbeds(blackjackCommand.buildFinal(game, skinEmoji, msg, color))
                    .setComponents()
                    .queue();
        }
    }

    // =========================================================
    // BÀI CÀO
    // =========================================================
    private void handleBaicao(ButtonInteractionEvent event, String[] parts) {
        String action = parts[0];
        String userId = parts[1];
        long   bet    = Long.parseLong(parts[2]);

        if (!event.getUser().getId().equals(userId)) {
            event.reply("Đây không phải ván của bạn!").setEphemeral(true).queue();
            return;
        }

        if (action.equals("bc_open")) {
            baicaoCommand.handleOpen(event, userId, bet);
        } else if (action.equals("bc_double")) {
            baicaoCommand.handleDouble(event, userId, bet);
        }
    }

    // =========================================================
    // TRANSFER
    // =========================================================
    private void handleTransfer(ButtonInteractionEvent event, String[] parts) {
        String ownerId = parts[1];

        if (!event.getUser().getId().equals(ownerId)) {
            event.reply("Đây không phải lệnh của bạn!").setEphemeral(true).queue();
            return;
        }

        TransferSessionService.TransferData data = transferService.get(ownerId);
        if (data == null) {
            event.deferEdit().queue();
            event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setTitle("Hết hạn phiên chuyển khoản!")
                            .setColor(Color.RED).build())
                    .setComponents().queue();
            return;
        }

        event.deferEdit().queue();

        if (parts[0].equals("transfer_confirm")) {
            try {
                bankService.transfer(ownerId, data.target(), data.amount());
                String receiverName;
                try { receiverName = userService.getUser(data.target()).getUsername(); }
                catch (Exception e) { receiverName = data.target(); }

                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                                .setTitle("✅ Chuyển khoản thành công!")
                                .setDescription("Đã chuyển **" + data.amount() + " coin** cho **"
                                        + receiverName + "**")
                                .setColor(Color.GREEN).build())
                        .setComponents().queue();
            } catch (Exception e) {
                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                                .setTitle("❌ Lỗi chuyển khoản!")
                                .setDescription(e.getMessage())
                                .setColor(Color.RED).build())
                        .setComponents().queue();
            }
        } else {
            event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setTitle("Đã hủy chuyển khoản")
                            .setColor(Color.RED).build())
                    .setComponents().queue();
        }

        transferService.clear(ownerId);
    }

    // =========================================================
    // HELPER BLACKJACK
    // =========================================================
    private long calcPrize(GameResult result, long bet, boolean doubled) {
        return switch (result) {
            case XI_BANG_WIN  -> bet * 2;
            case XI_DACH_WIN  -> bet;
            case NGU_LINH_WIN -> doubled ? bet * 4 : bet * 2;
            case WIN          -> bet;
            default           -> 0;
        };
    }

    private String buildMsg(GameResult result, String username,
                            long bet, long prize, boolean doubled) {
        return switch (result) {
            case XI_BANG_WIN  -> "🃏🃏 XÌ BÀN! **" + username + "** thắng **" + prize + " coin**! (x2)";
            case XI_DACH_WIN  -> "🃏 XÌ DÁCH! **" + username + "** thắng **" + prize + " coin**!";
            case NGU_LINH_WIN -> "🖐️ NGŨ LINH! **" + username + "** thắng **" + prize + " coin**!"
                    + (doubled ? " 🔥 (x4)" : " (x2)");
            case WIN          -> "🎉 **" + username + "** thắng **" + prize + " coin**!"
                    + (doubled ? " 🔥 Gấp đôi!" : "");
            case DRAW         -> "🤝 Hòa! Hoàn lại **" + bet + " coin**";
            case DAN_NON      -> "🔴 Dằn non! **" + username + "** thua **" + bet + " coin**";
            case LOSE         -> "😢 **" + username + "** thua **" + bet + " coin**";
        };
    }

    private Color resultColor(GameResult result) {
        return switch (result) {
            case XI_BANG_WIN, XI_DACH_WIN, NGU_LINH_WIN, WIN -> Color.GREEN;
            case DRAW -> Color.YELLOW;
            default   -> Color.RED;
        };
    }

    private String toSimple(GameResult result) {
        return switch (result) {
            case XI_BANG_WIN, XI_DACH_WIN, NGU_LINH_WIN, WIN -> "WIN";
            case DRAW -> "DRAW";
            default   -> "LOSE";
        };
    }
}