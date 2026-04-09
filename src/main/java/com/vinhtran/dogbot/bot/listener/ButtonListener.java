package com.vinhtran.dogbot.bot.listener;

import com.vinhtran.dogbot.command.BaicaoCommand;
import com.vinhtran.dogbot.command.BlackjackCommand;
import com.vinhtran.dogbot.game.BaicaoGame;
import com.vinhtran.dogbot.game.BlackjackGame;
import com.vinhtran.dogbot.game.GameResult;
import com.vinhtran.dogbot.service.*;
import com.vinhtran.dogbot.session.BlackjackSessionService;
import com.vinhtran.dogbot.session.SoloSession;
import com.vinhtran.dogbot.session.SoloSessionService;
import com.vinhtran.dogbot.session.TransferSessionService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.stereotype.Component;

import java.awt.*;

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
    private final BaicaoCommand           baicaoCommand;
    private final SoloSessionService      soloService;

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
            } else if (parts[0].startsWith("solo_")) {
                handleSolo(event, parts);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!event.isAcknowledged())
                event.reply("Đã xảy ra lỗi!").setEphemeral(true).queue();
        }
    }

    // =========================================================
    // BLACKJACK THƯỜNG
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
            event.reply("Không tìm thấy ván game hoặc đã hết giờ (5 phút)!").setEphemeral(true).queue();
            return;
        }

        String username  = event.getMember() != null ? event.getMember().getEffectiveName() : event.getUser().getName();
        String skinEmoji = blackjackCommand.getSkinEmoji(userId);

        event.deferEdit().queue();

        switch (action) {
            case "hit" -> {
                game.playerHit();
                if (game.playerBust()) {
                    gameService.recordResult(userId, "BLACKJACK", bet, "LOSE");
                    bjService.clear(userId);
                    editFinal(event, game, skinEmoji, username,
                            "💥 Quắc! **" + username + "** thua **" + bet + " coin**", Color.RED);
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
                            "🔴 Dằn non! **" + username + "** chưa đủ 16 điểm → Thua **" + bet + " coin**", Color.RED);
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
                    event.getHook().sendMessage("Không đủ coin để gấp đôi!").setEphemeral(true).queue();
                    return;
                }
                long newBet = bet * 2;
                bjService.saveGame(userId, game, newBet);
                game.playerHit();
                if (game.playerBust()) {
                    gameService.recordResult(userId, "BLACKJACK", newBet, "LOSE");
                    bjService.clear(userId);
                    editFinal(event, game, skinEmoji, username,
                            "💥 Quắc! **" + username + "** thua **" + newBet + " coin** (Gấp đôi)", Color.RED);
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

    private void editPlaying(ButtonInteractionEvent event, BlackjackGame game,
                             String skinEmoji, String userId, String username, long bet, boolean doubled) {
        try {
            byte[] img = game.getTableImagePlaying(username).readAllBytes();
            event.getHook()
                    .editOriginalEmbeds(blackjackCommand.buildPlaying(game, skinEmoji, bet, doubled))
                    .setFiles(FileUpload.fromData(img, "table.png"))
                    .setActionRow(
                            Button.success("bj_hit:"    + userId + ":" + bet, "🃏 Rút bài"),
                            Button.danger ("bj_stand:"  + userId + ":" + bet, "✋ Dừng"),
                            Button.primary("bj_double:" + userId + ":" + bet, "🔥 Gấp Đôi").asDisabled()
                    ).queue();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void editFinal(ButtonInteractionEvent event, BlackjackGame game,
                           String skinEmoji, String username, String msg, Color color) {
        try {
            byte[] img = game.getTableImageFinal(username).readAllBytes();
            event.getHook()
                    .editOriginalEmbeds(blackjackCommand.buildFinal(game, skinEmoji, msg, color))
                    .setFiles(FileUpload.fromData(img, "table.png"))
                    .setComponents().queue();
        } catch (Exception e) {
            e.printStackTrace();
            event.getHook()
                    .editOriginalEmbeds(blackjackCommand.buildFinal(game, skinEmoji, msg, color))
                    .setComponents().queue();
        }
    }

    // =========================================================
    // BÀI CÀO THƯỜNG
    // =========================================================
    private void handleBaicao(ButtonInteractionEvent event, String[] parts) {
        String action = parts[0];
        String userId = parts[1];
        long   bet    = Long.parseLong(parts[2]);

        if (!event.getUser().getId().equals(userId)) {
            event.reply("Đây không phải ván của bạn!").setEphemeral(true).queue();
            return;
        }

        if (action.equals("bc_open"))        baicaoCommand.handleOpen(event, userId, bet);
        else if (action.equals("bc_double")) baicaoCommand.handleDouble(event, userId, bet);
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
                            .setTitle("Hết hạn phiên chuyển khoản!").setColor(Color.RED).build())
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
                        .setDescription("Đã chuyển **" + data.amount() + " coin** cho **" + receiverName + "**")
                        .setColor(Color.GREEN).build()).setComponents().queue();
            } catch (Exception e) {
                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                        .setTitle("❌ Lỗi chuyển khoản!").setDescription(e.getMessage())
                        .setColor(Color.RED).build()).setComponents().queue();
            }
        } else {
            event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setTitle("Đã hủy chuyển khoản").setColor(Color.RED).build())
                    .setComponents().queue();
        }

        transferService.clear(ownerId);
    }

    // =========================================================
    // SOLO
    // =========================================================
    private void handleSolo(ButtonInteractionEvent event, String[] parts) {
        String action       = parts[0];
        String challengerId = parts[1];

        SoloSession session = soloService.getByChallenger(challengerId);
        if (session == null) {
            event.reply("❌ Phòng solo không tồn tại hoặc đã hết hạn!").setEphemeral(true).queue();
            return;
        }

        // ── Accept ───────────────────────────────────────────────────────
        if (action.equals("solo_accept")) {
            if (!event.getUser().getId().equals(session.getTargetId())) {
                event.reply("❌ Đây không phải lời mời của bạn!").setEphemeral(true).queue();
                return;
            }
            session.setState(SoloSession.State.CHOOSING_GAME);
            event.editMessageEmbeds(new EmbedBuilder()
                    .setTitle("⚔️ Chọn game!")
                    .setDescription("**" + session.getChallengerName() + "** vs **" + session.getTargetName() + "**\n"
                            + "💰 Cược: **" + session.getBet() + " coin**\n\nChọn game muốn chơi:")
                    .setColor(Color.CYAN).build()
            ).setActionRow(
                    Button.primary("solo_game_baicao:" + challengerId, "🃏 Bài Cào"),
                    Button.primary("solo_game_bj:" + challengerId, "🂡 Xì Dách")
            ).queue();
            return;
        }

        // ── Decline ──────────────────────────────────────────────────────
        if (action.equals("solo_decline")) {
            if (!event.getUser().getId().equals(session.getTargetId())) {
                event.reply("❌ Đây không phải lời mời của bạn!").setEphemeral(true).queue();
                return;
            }
            soloService.remove(challengerId);
            event.editMessageEmbeds(new EmbedBuilder()
                    .setTitle("❌ Từ chối thách đấu")
                    .setDescription("**" + session.getTargetName() + "** đã từ chối!")
                    .setColor(Color.RED).build()
            ).setComponents().queue();
            return;
        }

        // ── Chọn Bài Cào ─────────────────────────────────────────────────
        if (action.equals("solo_game_baicao")) {
            if (!event.getUser().getId().equals(session.getTargetId())
                    && !event.getUser().getId().equals(session.getChallengerId())) {
                event.reply("Không phải ván của bạn!").setEphemeral(true).queue();
                return;
            }

            BaicaoGame game = new BaicaoGame();
            session.setBaicaoGame(game);
            session.setChallengerHand(game.dealHand());
            session.setTargetHand(game.dealHand());
            session.setState(SoloSession.State.PLAYING_BAICAO);

            event.deferEdit().queue();

            try {
                byte[] img = game.getTableImageHidden(
                        session.getChallengerHand(), session.getTargetHand(),
                        session.getChallengerName() + " vs " + session.getTargetName()
                ).readAllBytes();

                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                                .setTitle("🃏 Solo Bài Cào")
                                .setDescription("**" + session.getChallengerName() + "** vs **" + session.getTargetName() + "**\n"
                                        + "💰 Cược: **" + session.getBet() + " coin**\n"
                                        + "🂠 Lá thứ 3 đang úp — Chọn **Mở** để xem bài nhau!")
                                .setImage("attachment://baicao.png")
                                .setColor(Color.BLUE).build()
                        ).setFiles(FileUpload.fromData(img, "baicao.png"))
                        .setActionRow(
                                Button.success("solo_bc_open:" + challengerId, "👁 Mở"),
                                Button.primary("solo_bc_double:" + challengerId, "🔥 Gấp Đôi (x2)")
                        ).queue(msg -> session.setPublicMessageId(msg.getId()));

            } catch (Exception e) {
                e.printStackTrace();
                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                        .setTitle("🃏 Solo Bài Cào").setDescription("❌ Lỗi render bài!")
                        .setColor(Color.RED).build()).setComponents().queue();
            }
            return;
        }

        // ── Chọn Xì Dách ─────────────────────────────────────────────────
        if (action.equals("solo_game_bj")) {
            if (!event.getUser().getId().equals(session.getTargetId())
                    && !event.getUser().getId().equals(session.getChallengerId())) {
                event.reply("Không phải ván của bạn!").setEphemeral(true).queue();
                return;
            }

            BlackjackGame cBj = new BlackjackGame();
            BlackjackGame tBj = new BlackjackGame();
            session.setChallengerBj(cBj);
            session.setTargetBj(tBj);
            session.setState(SoloSession.State.PLAYING_BLACKJACK);

            event.deferEdit().queue();

            // ── Kiểm tra kết thúc ngay nếu ai có Xì Bàn hoặc Xì Dách ───
            boolean cSpecial = cBj.isPlayerXiBang() || cBj.isPlayerXiDach();
            boolean tSpecial = tBj.isPlayerXiBang() || tBj.isPlayerXiDach();

            if (cSpecial || tSpecial) {
                resolveInstantSoloBj(event, session, cBj, tBj);
                return;
            }

            // ── Không có bài đặc biệt → Challenger chơi trước ────────────
            try {
                byte[] publicImg = BlackjackGame.getSoloImagePublic(
                        session.getChallengerName(), cBj,
                        session.getTargetName(),     tBj,
                        true,
                        "⏳ Đang lượt " + session.getChallengerName() + "..."
                ).readAllBytes();

                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                                .setTitle("🂡 Solo Xì Dách")
                                .setDescription("**" + session.getChallengerName() + "** vs **" + session.getTargetName() + "**\n"
                                        + "💰 Cược: **" + session.getBet() + " coin**\n\n"
                                        + "🎯 Đến lượt <@" + session.getChallengerId() + "> — nhấn nút bên dưới để xem bài và chọn hành động!")
                                .setImage("attachment://table.png")
                                .setColor(Color.BLUE).build()
                        ).setFiles(FileUpload.fromData(publicImg, "table.png"))
                        .setActionRow(
                                Button.primary("solo_bj_view:" + challengerId + ":challenger", "👁 Xem bài & hành động")
                        ).queue(msg -> session.setPublicMessageId(msg.getId()));

            } catch (Exception e) {
                e.printStackTrace();
                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                        .setTitle("🂡 Solo Xì Dách").setDescription("❌ Lỗi render bài!")
                        .setColor(Color.RED).build()).setComponents().queue();
            }
            return;
        }

        // ── Solo Bài Cào: Mở / Gấp Đôi ──────────────────────────────────
        if (action.equals("solo_bc_open") || action.equals("solo_bc_double")) {
            if (!event.getUser().getId().equals(session.getChallengerId())
                    && !event.getUser().getId().equals(session.getTargetId())) {
                event.reply("Không phải ván của bạn!").setEphemeral(true).queue();
                return;
            }

            long finalBet = action.equals("solo_bc_double") ? session.getBet() * 2 : session.getBet();

            if (action.equals("solo_bc_double")) {
                long cBal = userService.getBalance(session.getChallengerId());
                long tBal = userService.getBalance(session.getTargetId());
                if (cBal < finalBet || tBal < finalBet) {
                    event.reply("❌ Một trong hai người không đủ coin để gấp đôi!").setEphemeral(true).queue();
                    return;
                }
            }

            event.deferEdit().queue();

            BaicaoGame game   = session.getBaicaoGame();
            String     result = game.determineResult(session.getChallengerHand(), session.getTargetHand());

            String msg;
            Color  color;
            if (result.equals("WIN")) {
                long prize = game.calcPrize(session.getChallengerHand(), finalBet);
                userService.updateBalance(session.getChallengerId(),  prize);
                userService.updateBalance(session.getTargetId(),     -finalBet);
                msg   = "🎉 **" + session.getChallengerName() + "** thắng **" + prize + " coin**!";
                color = Color.GREEN;
            } else if (result.equals("LOSE")) {
                long prize = game.calcPrize(session.getTargetHand(), finalBet);
                userService.updateBalance(session.getTargetId(),      prize);
                userService.updateBalance(session.getChallengerId(), -finalBet);
                msg   = "🎉 **" + session.getTargetName() + "** thắng **" + prize + " coin**!";
                color = Color.GREEN;
            } else {
                msg   = "🤝 Hòa — hoàn lại **" + finalBet + " coin**";
                color = Color.YELLOW;
            }

            soloService.remove(session.getChallengerId());

            try {
                byte[] img = game.getTableImageFinal(
                        session.getChallengerHand(), session.getTargetHand(),
                        session.getChallengerName() + " vs " + session.getTargetName()
                ).readAllBytes();

                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                                .setTitle("🃏 Solo Bài Cào — Kết quả")
                                .setDescription(msg + "\n"
                                        + session.getChallengerName() + ": **" + session.getChallengerHand().rank() + "**\n"
                                        + session.getTargetName() + ": **" + session.getTargetHand().rank() + "**")
                                .setImage("attachment://baicao.png")
                                .setColor(color).build()
                        ).setFiles(FileUpload.fromData(img, "baicao.png"))
                        .setComponents().queue();

            } catch (Exception e) {
                e.printStackTrace();
                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                        .setTitle("🃏 Solo Bài Cào — Kết quả")
                        .setDescription(msg).setColor(color).build()
                ).setComponents().queue();
            }
            return;
        }

        // ── Solo Xì Dách: Xem bài (ephemeral) ────────────────────────────
        if (action.equals("solo_bj_view")) {
            String  role         = parts[2];
            boolean isChallenger = role.equals("challenger");
            String  expectedId   = isChallenger ? session.getChallengerId() : session.getTargetId();

            // Chỉ đúng người mới được xem
            if (!event.getUser().getId().equals(expectedId)) {
                event.reply("❌ Đây không phải bài của bạn!").setEphemeral(true).queue();
                return;
            }

            // Kiểm tra đúng lượt
            if (isChallenger && session.isChallengerDone()) {
                event.reply("❌ Bạn đã dừng rồi, chờ đối thủ!").setEphemeral(true).queue();
                return;
            }
            if (!isChallenger && !session.isChallengerDone()) {
                event.reply("❌ Chưa đến lượt bạn! Chờ **" + session.getChallengerName() + "** chơi xong.").setEphemeral(true).queue();
                return;
            }

            BlackjackGame myGame = isChallenger ? session.getChallengerBj() : session.getTargetBj();

            try {
                byte[] privateImg = isChallenger
                        ? BlackjackGame.getSoloImageForChallenger(
                        session.getChallengerName(), session.getChallengerBj(),
                        session.getTargetName(),     session.getTargetBj()).readAllBytes()
                        : BlackjackGame.getSoloImageForTarget(
                        session.getChallengerName(), session.getChallengerBj(),
                        session.getTargetName(),     session.getTargetBj()).readAllBytes();

                String canStandNote = myGame.canPlayerStand()
                        ? ""
                        : "\n⚠️ Chưa đủ 16 điểm — Dừng sẽ bị tính **Dằn non**!";

                event.reply(
                                "🔒 **Bài của bạn** _(chỉ mình bạn thấy)_\n"
                                        + "Điểm hiện tại: **" + myGame.getPlayerScore() + " điểm**"
                                        + canStandNote + "\n"
                                        + "👇 Chọn hành động:"
                        )
                        .addFiles(FileUpload.fromData(privateImg, "myhand.png"))
                        .addActionRow(
                                Button.success("solo_bj_hit:"   + challengerId + ":" + role, "🃏 Rút bài"),
                                Button.danger ("solo_bj_stand:" + challengerId + ":" + role, "✋ Dừng")
                        )
                        .setEphemeral(true)
                        .queue();

            } catch (Exception e) {
                e.printStackTrace();
                event.reply("❌ Lỗi khi xem bài!").setEphemeral(true).queue();
            }
            return;
        }

        // ── Solo Xì Dách: Hit / Stand ──────────────────────────────────────
        if (action.equals("solo_bj_hit") || action.equals("solo_bj_stand")) {
            String  role         = parts[2];
            boolean isChallenger = role.equals("challenger");
            String  expectedId   = isChallenger ? session.getChallengerId() : session.getTargetId();
            String  playerName   = isChallenger ? session.getChallengerName() : session.getTargetName();

            if (!event.getUser().getId().equals(expectedId)) {
                event.reply("❌ Chưa đến lượt bạn!").setEphemeral(true).queue();
                return;
            }

            // Kiểm tra đúng lượt (target chưa được chơi nếu challenger chưa xong)
            if (!isChallenger && !session.isChallengerDone()) {
                event.reply("❌ Chờ **" + session.getChallengerName() + "** chơi xong trước!").setEphemeral(true).queue();
                return;
            }

            event.deferEdit().queue();

            BlackjackGame myGame = isChallenger ? session.getChallengerBj() : session.getTargetBj();

            // Rút bài nếu là hit
            if (action.equals("solo_bj_hit")) myGame.playerHit();

            boolean turnDone = action.equals("solo_bj_stand")
                    || myGame.playerBust()
                    || myGame.isPlayerNguLinh();

            if (!turnDone) {
                // ── Vẫn còn lượt — cập nhật ephemeral ──────────────────
                try {
                    byte[] privateImg = isChallenger
                            ? BlackjackGame.getSoloImageForChallenger(
                            session.getChallengerName(), session.getChallengerBj(),
                            session.getTargetName(),     session.getTargetBj()).readAllBytes()
                            : BlackjackGame.getSoloImageForTarget(
                            session.getChallengerName(), session.getChallengerBj(),
                            session.getTargetName(),     session.getTargetBj()).readAllBytes();

                    String canStandNote = myGame.canPlayerStand()
                            ? ""
                            : "\n⚠️ Chưa đủ 16 điểm — Dừng sẽ bị tính **Dằn non**!";

                    event.getHook().editOriginal(
                                    "🔒 **Bài của bạn** _(chỉ mình bạn thấy)_\n"
                                            + "Điểm hiện tại: **" + myGame.getPlayerScore() + " điểm**"
                                            + canStandNote + "\n"
                                            + "👇 Chọn hành động:"
                            )
                            .setFiles(FileUpload.fromData(privateImg, "myhand.png"))
                            .setActionRow(
                                    Button.success("solo_bj_hit:"   + challengerId + ":" + role, "🃏 Rút bài"),
                                    Button.danger ("solo_bj_stand:" + challengerId + ":" + role, "✋ Dừng")
                            ).queue();

                    // Cập nhật public message (bài vẫn úp)
                    updatePublicMessage(event, session, challengerId, playerName, role);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }

            // ── Lượt người này đã xong ────────────────────────────────────
            String doneNote;
            if (myGame.playerBust()) {
                doneNote = "💥 **" + playerName + "** bị quắc!";
            } else if (myGame.isPlayerNguLinh()) {
                doneNote = "🖐️ **" + playerName + "** NGŨ LINH! (" + myGame.getPlayerScore() + " điểm)";
            } else if (!myGame.canPlayerStand() && action.equals("solo_bj_stand")) {
                // Dằn non — tính thua ngay
                doneNote = "🔴 **" + playerName + "** dằn non! (chưa đủ 16 điểm)";
            } else {
                doneNote = "✅ **" + playerName + "** dừng (" + myGame.getPlayerScore() + " điểm)";
            }

            // Ẩn nút trên ephemeral
            event.getHook().editOriginal(
                    "✅ **Bạn đã " + (action.equals("solo_bj_stand") ? "dừng" : "xong lượt") + "** với **"
                            + myGame.getPlayerScore() + " điểm**.\n"
                            + (isChallenger
                            ? "Chờ **" + session.getTargetName() + "** đánh xong để xem kết quả!"
                            : "Đang tính kết quả...")
            ).setComponents().queue();

            if (isChallenger) {
                // ── Challenger xong → chuyển lượt sang target ────────────
                session.setChallengerDone(true);

                try {
                    byte[] publicImg = BlackjackGame.getSoloImagePublic(
                            session.getChallengerName(), session.getChallengerBj(),
                            session.getTargetName(),     session.getTargetBj(),
                            false,
                            "⏳ Đang lượt " + session.getTargetName() + "..."
                    ).readAllBytes();

                    sendPublicTurnMessage(event, session, challengerId,
                            doneNote + "\n\n🎯 Đến lượt <@" + session.getTargetId() + ">!\n"
                                    + "💰 Cược: **" + session.getBet() + " coin**\n"
                                    + "_(Nhấn nút bên dưới để xem bài và chọn hành động)_",
                            "target", Color.ORANGE, publicImg);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                // ── Target xong → tính kết quả ────────────────────────────
                session.setTargetDone(true);
                resolveSoloBj(event, session);
            }
        }
    }

    // =========================================================
    // SOLO XÌ DÁCH — KẾT THÚC NGAY (Xì Bàn / Xì Dách)
    // =========================================================
    private void resolveInstantSoloBj(ButtonInteractionEvent event, SoloSession session,
                                      BlackjackGame cBj, BlackjackGame tBj) {
        long   bet        = session.getBet();
        int    cmp        = BlackjackGame.compareSolo(cBj, tBj);
        String resultLine = session.getChallengerName() + ": **" + cBj.getSoloResultDesc() + "**"
                + " | " + session.getTargetName() + ": **" + tBj.getSoloResultDesc() + "**";
        String msg;
        Color  color;

        if (cmp > 0) {
            userService.updateBalance(session.getChallengerId(),  bet);
            userService.updateBalance(session.getTargetId(),     -bet);
            msg   = "🎉 **" + session.getChallengerName() + "** thắng **" + bet + " coin**!";
            color = Color.GREEN;
        } else if (cmp < 0) {
            userService.updateBalance(session.getTargetId(),      bet);
            userService.updateBalance(session.getChallengerId(), -bet);
            msg   = "🎉 **" + session.getTargetName() + "** thắng **" + bet + " coin**!";
            color = Color.GREEN;
        } else {
            msg   = "🤝 Hòa — hoàn lại **" + bet + " coin**";
            color = Color.YELLOW;
        }

        soloService.remove(session.getChallengerId());

        try {
            byte[] img = BlackjackGame.getSoloImageFinal(
                    session.getChallengerName(), cBj,
                    session.getTargetName(),     tBj,
                    resultLine
            ).readAllBytes();

            event.getHook().editOriginalEmbeds(new EmbedBuilder()
                            .setTitle("🂡 Solo Xì Dách — Kết quả ngay!")
                            .setDescription(msg + "\n" + resultLine)
                            .setImage("attachment://table.png")
                            .setColor(color).build()
                    ).setFiles(FileUpload.fromData(img, "table.png"))
                    .setComponents().queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().editOriginalEmbeds(new EmbedBuilder()
                    .setTitle("🂡 Solo Xì Dách — Kết quả ngay!")
                    .setDescription(msg + "\n" + resultLine)
                    .setColor(color).build()
            ).setComponents().queue();
        }
    }

    // =========================================================
    // SOLO XÌ DÁCH — KẾT THÚC SAU KHI CẢ 2 CHƠI XONG
    // =========================================================
    private void resolveSoloBj(ButtonInteractionEvent event, SoloSession session) {
        BlackjackGame cGame = session.getChallengerBj();
        BlackjackGame tGame = session.getTargetBj();
        long bet = session.getBet();

        // ── Xử lý dằn non trước khi so bài ──────────────────────────────
        // Nếu challenger dằn non (dừng < 16 điểm và không phải ngũ linh/bust)
        boolean cDanNon = !cGame.playerBust() && !cGame.isPlayerNguLinh()
                && !cGame.isPlayerXiBang() && !cGame.isPlayerXiDach()
                && cGame.getPlayerScore() < 16;
        boolean tDanNon = !tGame.playerBust() && !tGame.isPlayerNguLinh()
                && !tGame.isPlayerXiBang() && !tGame.isPlayerXiDach()
                && tGame.getPlayerScore() < 16;

        String msg;
        Color  color;
        String resultLine = session.getChallengerName() + ": **" + cGame.getSoloResultDesc() + "**"
                + " | " + session.getTargetName() + ": **" + tGame.getSoloResultDesc() + "**";

        if (cDanNon && tDanNon) {
            // Cả 2 dằn non → hòa
            msg   = "🤝 Cả hai dằn non — hoàn lại **" + bet + " coin**";
            color = Color.YELLOW;
        } else if (cDanNon) {
            userService.updateBalance(session.getTargetId(),      bet);
            userService.updateBalance(session.getChallengerId(), -bet);
            msg   = "🔴 **" + session.getChallengerName() + "** dằn non! **" + session.getTargetName() + "** thắng **" + bet + " coin**!";
            color = Color.RED;
        } else if (tDanNon) {
            userService.updateBalance(session.getChallengerId(),  bet);
            userService.updateBalance(session.getTargetId(),     -bet);
            msg   = "🔴 **" + session.getTargetName() + "** dằn non! **" + session.getChallengerName() + "** thắng **" + bet + " coin**!";
            color = Color.RED;
        } else {
            // ── So bài đúng luật VN: Xì Bàn > Xì Dách > Ngũ Linh > Điểm > Bust ──
            int cmp = BlackjackGame.compareSolo(cGame, tGame);

            if (cmp > 0) {
                userService.updateBalance(session.getChallengerId(),  bet);
                userService.updateBalance(session.getTargetId(),     -bet);
                msg   = "🎉 **" + session.getChallengerName() + "** thắng **" + bet + " coin**!";
                color = Color.GREEN;
            } else if (cmp < 0) {
                userService.updateBalance(session.getTargetId(),      bet);
                userService.updateBalance(session.getChallengerId(), -bet);
                msg   = "🎉 **" + session.getTargetName() + "** thắng **" + bet + " coin**!";
                color = Color.GREEN;
            } else {
                msg   = "🤝 Hòa — hoàn lại **" + bet + " coin**";
                color = Color.YELLOW;
            }
        }

        soloService.remove(session.getChallengerId());

        if (session.getPublicMessageId() == null) {
            event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("🂡 Solo Xì Dách — Kết quả")
                    .setDescription(msg + "\n" + resultLine)
                    .setColor(color).build()
            ).queue();
            return;
        }

        try {
            byte[] img = BlackjackGame.getSoloImageFinal(
                    session.getChallengerName(), cGame,
                    session.getTargetName(),     tGame,
                    resultLine
            ).readAllBytes();

            event.getChannel().editMessageEmbedsById(session.getPublicMessageId(),
                            new EmbedBuilder()
                                    .setTitle("🂡 Solo Xì Dách — Kết quả")
                                    .setDescription(msg + "\n" + resultLine)
                                    .setImage("attachment://table.png")
                                    .setColor(color).build()
                    ).setFiles(FileUpload.fromData(img, "table.png"))
                    .setComponents().queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.getChannel().editMessageEmbedsById(session.getPublicMessageId(),
                    new EmbedBuilder()
                            .setTitle("🂡 Solo Xì Dách — Kết quả")
                            .setDescription(msg + "\n" + resultLine)
                            .setColor(color).build()
            ).setComponents().queue();
        }
    }

    // =========================================================
    // HELPER — Cập nhật public message trong lúc đang rút bài
    // =========================================================
    private void updatePublicMessage(ButtonInteractionEvent event, SoloSession session,
                                     String challengerId, String playerName, String role) {
        if (session.getPublicMessageId() == null) return;
        try {
            boolean isChallengerTurn = role.equals("challenger");
            byte[] publicImg = BlackjackGame.getSoloImagePublic(
                    session.getChallengerName(), session.getChallengerBj(),
                    session.getTargetName(),     session.getTargetBj(),
                    isChallengerTurn,
                    "⏳ Đang lượt " + playerName + "..."
            ).readAllBytes();

            event.getChannel().editMessageEmbedsById(session.getPublicMessageId(),
                            new EmbedBuilder()
                                    .setTitle("🂡 Solo Xì Dách — Lượt " + playerName)
                                    .setDescription("💰 Cược: **" + session.getBet() + " coin**\n"
                                            + "🎯 <@" + (isChallengerTurn ? session.getChallengerId() : session.getTargetId())
                                            + "> đang chọn bài...")
                                    .setImage("attachment://table.png")
                                    .setColor(Color.BLUE).build()
                    ).setFiles(FileUpload.fromData(publicImg, "table.png"))
                    .setActionRow(
                            Button.primary("solo_bj_view:" + challengerId + ":" + role, "👁 Xem bài & hành động")
                    ).queue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // HELPER — Gửi public message khi chuyển lượt
    // =========================================================
    private void sendPublicTurnMessage(ButtonInteractionEvent event, SoloSession session,
                                       String challengerId, String description,
                                       String nextRole, Color color, byte[] publicImg) {
        if (session.getPublicMessageId() == null) return;
        try {
            String nextName = nextRole.equals("challenger")
                    ? session.getChallengerName() : session.getTargetName();

            event.getChannel().editMessageEmbedsById(session.getPublicMessageId(),
                            new EmbedBuilder()
                                    .setTitle("🂡 Solo Xì Dách — Lượt " + nextName)
                                    .setDescription(description)
                                    .setImage("attachment://table.png")
                                    .setColor(color).build()
                    ).setFiles(FileUpload.fromData(publicImg, "table.png"))
                    .setActionRow(
                            Button.primary("solo_bj_view:" + challengerId + ":" + nextRole, "👁 Xem bài & hành động")
                    ).queue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // HELPER BLACKJACK THƯỜNG
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

    private String buildMsg(GameResult result, String username, long bet, long prize, boolean doubled) {
        return switch (result) {
            case XI_BANG_WIN  -> "🃏🃏 XÌ BÀN! **" + username + "** thắng **" + prize + " coin**! (x2)";
            case XI_DACH_WIN  -> "🃏 XÌ DÁCH! **" + username + "** thắng **" + prize + " coin**!";
            case NGU_LINH_WIN -> "🖐️ NGŨ LINH! **" + username + "** thắng **" + prize + " coin**!" + (doubled ? " 🔥 (x4)" : " (x2)");
            case WIN          -> "🎉 **" + username + "** thắng **" + prize + " coin**!" + (doubled ? " 🔥 Gấp đôi!" : "");
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