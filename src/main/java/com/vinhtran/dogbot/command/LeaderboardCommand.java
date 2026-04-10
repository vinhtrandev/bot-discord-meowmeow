package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.entity.UserCoin;
import com.vinhtran.dogbot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardCommand implements Command {

    private final UserService userService;

    @Override
    public String getName() { return "!leaderboard"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String serverId = event.getGuild().getId();

        try {
            List<UserCoin> top10 = userService.getLeaderboard(serverId, 10);

            if (top10.isEmpty()) {
                event.getChannel().sendMessage("Hiện tại chưa có dữ liệu trên bảng xếp hạng! 📭").queue();
                return;
            }

            String[] medals   = {"🥇", "🥈", "🥉"};
            String[] names    = new String[top10.size()];
            AtomicInteger done = new AtomicInteger(0);

            for (int i = 0; i < top10.size(); i++) {
                final int idx       = i;
                String    discordId = top10.get(i).getUser().getDiscordId();

                event.getGuild().retrieveMemberById(discordId).queue(
                        member -> {
                            names[idx] = member != null ? member.getEffectiveName() : discordId;
                            if (done.incrementAndGet() == top10.size()) sendEmbed(event, top10, names, medals);
                        },
                        err -> {
                            names[idx] = discordId;
                            if (done.incrementAndGet() == top10.size()) sendEmbed(event, top10, names, medals);
                        }
                );
            }

        } catch (Exception e) {
            log.error("Lỗi leaderboard: ", e);
            event.getChannel().sendMessage("❌ Đã có lỗi xảy ra khi xử lý bảng xếp hạng!").queue();
        }
    }

    private void sendEmbed(MessageReceivedEvent event, List<UserCoin> top10,
                           String[] names, String[] medals) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < top10.size(); i++) {
            String rankDisplay = (i < 3) ? medals[i] : (i + 1) + ".";
            sb.append(String.format("%s **%s** — `%,d` 🪙\n",
                    rankDisplay, names[i], top10.get(i).getBalance()));
        }

        event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("🏆 BẢNG XẾP HẠNG")
                .setDescription(sb.toString())
                .setColor(new Color(255, 215, 0))
                .setFooter("Yêu cầu bởi: " + event.getAuthor().getName(),
                        event.getAuthor().getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build()).queue();
    }
}