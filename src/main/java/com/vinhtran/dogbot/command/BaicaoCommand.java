package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.game.BaicaoGame;
import com.vinhtran.dogbot.service.GameService;
import com.vinhtran.dogbot.service.UserService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;

import java.awt.Color;

@Component
@RequiredArgsConstructor
public class BaicaoCommand {

    private final UserService userService;
    private final GameService gameService;
    private final BaicaoGame baicaoGame = new BaicaoGame();

    public void handle(SlashCommandInteractionEvent event) {
        String discordId = event.getUser().getId();
        long bet = event.getOption("bet").getAsLong();

        try {
            long balance = userService.getBalance(discordId);
            if (bet > balance || bet <= 0) {
                event.reply("❌ Cược không hợp lệ! Số dư: " + balance + " 🪙").setEphemeral(true).queue();
                return;
            }

            BaicaoGame.Hand playerHand = baicaoGame.dealHand();
            BaicaoGame.Hand botHand    = baicaoGame.dealHand();
            String result = baicaoGame.determineResult(playerHand, botHand);

            gameService.recordResult(discordId, "BAI_CAO", bet, result);

            String msg = result.equals("WIN") ? "🎉 Thắng " + bet + " 🪙!"
                    : result.equals("LOSE") ? "😢 Thua " + bet + " 🪙"
                    : "🤝 Hòa!";
            Color color = result.equals("WIN") ? Color.GREEN
                    : result.equals("LOSE") ? Color.RED : Color.YELLOW;

            event.replyEmbeds(new EmbedBuilder()
                    .setTitle("🀄 Bài Cào")
                    .addField("Bài của bạn", playerHand.cards() + " → " + playerHand.rank(), false)
                    .addField("Bài bot", botHand.cards() + " → " + botHand.rank(), false)
                    .setDescription(msg).setColor(color).build()
            ).queue();

        } catch (Exception e) {
            event.reply("❌ " + e.getMessage()).setEphemeral(true).queue();
        }
    }
}