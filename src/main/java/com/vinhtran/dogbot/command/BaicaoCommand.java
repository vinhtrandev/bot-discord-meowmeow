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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaicaoCommand implements Command {

    private final UserService   userService;
    private final GameService   gameService;
    private final ShopService   shopService;
    private final CoupleService coupleService;

    private final Map<String, BaicaoSession> sessions = new ConcurrentHashMap<>();

    record BaicaoSession(BaicaoGame game, BaicaoGame.Hand playerHand,
                         BaicaoGame.Hand botHand, long bet, String username, String serverId) {}

    @Override
    public String getName() { return "!baicao"; }

    @Override
    public List<String> getAliases() { return List.of("bc", "baicao"); }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String userId   = event.getAuthor().getId();
        String username = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
        String serverId = event.getGuild().getId();

        userService.getOrCreate(userId, serverId, username);

        if (args.length < 2) {
            event.getChannel().sendMessage("Dùng: `!baicao <số coin hoặc all>`").queue();
            return;
        }
        if (sessions.containsKey(userId)) {
            event.getChannel().sendMessage("⚠️ Bạn đang có ván chưa xong! Dùng nút **Mở** hoặc **Gấp Đôi**.").queue();
            return;
        }

        try {
            long balance = userService.getBalance(userId, serverId);
            long bet;

            if (args[1].equalsIgnoreCase("all")) {
                if (balance <= 0) { event.getChannel().sendMessage("Bạn không có coin nào!").queue(); return; }
                bet = balance;
            } else {
                bet = Long.parseLong(args[1]);
            }

            if (bet <= 0 || bet > balance) {
                event.getChannel().sendMessage("Cược không hợp lệ! Số dư: **" + balance + " coin**").queue();
                return;
            }

            BaicaoGame      game  = new BaicaoGame();

            // ── Giống Blackjack: lấy skinEmoji TRƯỚC khi put session ──────
            // → người vào trước chưa có partner session → dùng skin thường
            // → người vào sau partner đã có session → dùng couple emoji
            String skinEmoji = getSkinEmoji(userId, serverId);

            BaicaoGame.Hand pHand = game.dealHand();
            BaicaoGame.Hand bHand = game.dealHand();
            sessions.put(userId, new BaicaoSession(game, pHand, bHand, bet, username, serverId));

            // ── Thông báo couple đang chơi cùng ──────────────────────────
            sendCoupleNotificationIfPlaying(event, userId, username, serverId);

            byte[] imgBytes = game.getTableImageHidden(pHand, bHand, username).readAllBytes();

            event.getChannel().sendMessageEmbeds(
                            new EmbedBuilder()
                                    .setTitle(skinEmoji + " Bài Cào")
                                    .setDescription("💰 Cược: **" + bet + " coin**\n🂠 Lá thứ 3 đang úp — Chọn hành động!")
                                    .setImage("attachment://baicao.png")
                                    .setFooter("Sáp(x5) > Liêng(x3) > Ba Tây(x2) > Điểm thường(x1)")
                                    .setColor(Color.BLUE).build())
                    .addFiles(FileUpload.fromData(imgBytes, "baicao.png"))
                    .setActionRow(
                            Button.success("bc_open:"   + userId + ":" + bet, "👁 Mở"),
                            Button.primary("bc_double:" + userId + ":" + bet, "🔥 Gấp Đôi (x2)")
                    ).queue();

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Số coin không hợp lệ!").queue();
        } catch (Exception e) {
            log.error("Lỗi BaicaoCommand", e);
            event.getChannel().sendMessage("Lỗi: " + e.getMessage()).queue();
        }
    }

    // ── Couple notification ───────────────────────────────────────────────

    /**
     * Gửi thông báo "Cặp Đôi Cùng Chiến" chỉ khi partner CŨNG đang có session.
     * Tức là chỉ người vào SAU mới trigger — giống Blackjack dùng bjService.hasGame.
     */
    private void sendCoupleNotificationIfPlaying(MessageReceivedEvent event,
                                                 String userId,
                                                 String username,
                                                 String serverId) {
        try {
            coupleService.getPartnerId(userId, serverId).ifPresent(partnerId -> {
                if (!sessions.containsKey(partnerId)) return;

                String coupleEmoji = coupleService.getCoupleEmoji(userId, serverId);
                String partnerName;
                try {
                    var member = event.getGuild().getMemberById(partnerId);
                    partnerName = member != null ? member.getEffectiveName() : "người ấy";
                } catch (Exception ignored) {
                    partnerName = "người ấy";
                }

                event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                        .setTitle(coupleEmoji + " Cặp Đôi Cùng Chiến!")
                        .setDescription(
                                "💕 **" + username + "** và **" + partnerName + "** đang cùng chơi Bài Cào!\n\n"
                                        + "✨ Chúc cặp đôi may mắn và thắng lớn! " + coupleEmoji
                        )
                        .setColor(Color.PINK)
                        .build()
                ).queue();
            });
        } catch (Exception ignored) { }
    }

    // ── Button handlers ───────────────────────────────────────────────────

    public void handleOpen(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event,
                           String userId, long bet) {
        BaicaoSession session = sessions.remove(userId);
        if (session == null) { event.reply("Không tìm thấy ván game!").setEphemeral(true).queue(); return; }
        event.deferEdit().queue();
        resolveGame(event, session, bet, false);
    }

    public void handleDouble(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event,
                             String userId, long bet) {
        BaicaoSession session = sessions.get(userId);
        if (session == null) { event.reply("Không tìm thấy ván game!").setEphemeral(true).queue(); return; }

        long balance = userService.getBalance(userId, session.serverId());
        if (balance < bet) { event.reply("❌ Không đủ coin để gấp đôi!").setEphemeral(true).queue(); return; }

        sessions.remove(userId);
        event.deferEdit().queue();
        resolveGame(event, session, bet * 2, true);
    }

    private void resolveGame(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event,
                             BaicaoSession session, long finalBet, boolean doubled) {
        BaicaoGame      game      = session.game();
        BaicaoGame.Hand pHand     = session.playerHand();
        BaicaoGame.Hand bHand     = session.botHand();
        String          uname     = session.username();
        String          serverId  = session.serverId();
        String          userId    = event.getUser().getId();
        String          skinEmoji = getSkinEmoji(userId, serverId);

        String result = game.determineResult(pHand, bHand);
        gameService.recordResult(userId, serverId, "BAI_CAO", finalBet, result);

        String msg;
        Color  color;
        switch (result) {
            case "WIN" -> {
                long prize = game.calcPrize(pHand, finalBet);
                msg   = "🎉 **" + uname + "** thắng **" + prize + " coin**!" + (doubled ? " 🔥 *(Gấp đôi)*" : "") + "\nBot: " + bHand.rank();
                color = Color.GREEN;
            }
            case "LOSE" -> {
                msg   = "😢 **" + uname + "** thua **" + finalBet + " coin**" + (doubled ? " 🔥 *(Gấp đôi)*" : "") + "\nBot: " + bHand.rank();
                color = Color.RED;
            }
            default -> {
                msg   = "🤝 Hòa — hoàn lại **" + finalBet + " coin**\nBot: " + bHand.rank();
                color = Color.YELLOW;
            }
        }

        try {
            byte[] imgBytes = game.getTableImageFinal(pHand, bHand, uname).readAllBytes();
            event.getHook()
                    .editOriginalEmbeds(new EmbedBuilder()
                            .setTitle(skinEmoji + " Bài Cào — Kết quả")
                            .setDescription(msg)
                            .setImage("attachment://baicao.png")
                            .setFooter("Sáp(x5) > Liêng(x3) > Ba Tây(x2) > Điểm thường(x1)")
                            .setColor(color).build())
                    .setFiles(FileUpload.fromData(imgBytes, "baicao.png"))
                    .setComponents().queue();
        } catch (Exception e) {
            log.error("Lỗi render kết quả BaicaoCommand", e);
            event.getHook()
                    .editOriginalEmbeds(new EmbedBuilder()
                            .setTitle(skinEmoji + " Bài Cào — Kết quả")
                            .setDescription(msg).setColor(color).build())
                    .setComponents().queue();
        }
    }

    public void clearSession(String userId) { sessions.remove(userId); }

    // ── Skin / emoji helper ───────────────────────────────────────────────

    /**
     * Giống BlackjackCommand.getSkinEmoji:
     * Chỉ dùng couple emoji khi partner ĐANG có session active (sessions.containsKey).
     * Người vào trước → partner chưa có session → skin thường.
     * Người vào sau → partner đã có session → couple emoji.
     */
    public String getSkinEmoji(String userId, String serverId) {
        String skinEmoji = shopService.getEquippedSkinEmoji(userId, serverId);
        return coupleService.getPartnerId(userId, serverId)
                .filter(sessions::containsKey)
                .map(partnerId -> coupleService.getCoupleEmoji(userId, serverId))
                .filter(e -> !e.isEmpty())
                .orElse(skinEmoji);
    }
}