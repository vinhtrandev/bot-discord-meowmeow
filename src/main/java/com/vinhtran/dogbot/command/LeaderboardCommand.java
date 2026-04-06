package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.entity.Leaderboard;
import com.vinhtran.dogbot.entity.User;
import com.vinhtran.dogbot.repository.LeaderboardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardCommand implements Command {

    private final LeaderboardRepository leaderboardRepository;

    @Override
    public String getName() {
        return "!leaderboard";
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        try {
            // 1. Lấy Top 10 từ Database
            List<Leaderboard> top10 = leaderboardRepository.findTop10ByOrderByTotalWinningsDesc();

            if (top10.isEmpty()) {
                event.getChannel().sendMessage("Hiện tại chưa có dữ liệu trên bảng xếp hạng! 📭").queue();
                return;
            }

            StringBuilder sb = new StringBuilder();
            String[] medals = {"🥇", "🥈", "🥉"};

            // 2. Duyệt danh sách
            for (int i = 0; i < top10.size(); i++) {
                Leaderboard lb = top10.get(i);
                User userEntity = lb.getUser(); // Lấy Object User từ quan hệ @OneToOne

                String rankDisplay = (i < 3) ? medals[i] : (i + 1) + ".";
                String displayName = "Người dùng ẩn";

                if (userEntity != null) {
                    // CỐ GẮNG LẤY USERNAME THAY VÌ ID
                    // Bước 1: Thử lấy Member trực tiếp từ Server (Guild) để lấy Nickname mới nhất
                    Member member = event.getGuild().getMemberById(userEntity.getDiscordId());

                    if (member != null) {
                        displayName = member.getEffectiveName(); // Tên hiển thị trong server
                    } else {
                        // Bước 2: Nếu người đó rời server, lấy Username lưu trong DB của bạn
                        displayName = userEntity.getUsername();
                    }
                }

                // Định dạng tiền tệ và tỉ lệ thắng
                String formattedMoney = String.format("%,d", lb.getTotalWinnings());
                double winRate = (lb.getWinRate() != null) ? lb.getWinRate() : 0.0;

                sb.append(String.format("%s **%s** — `%s` 🪙 *(Win: %.1f%%)*\n",
                        rankDisplay,
                        displayName,
                        formattedMoney,
                        winRate));
            }

            // 3. Xây dựng giao diện Embed
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🏆 BẢNG XẾP HẠNG BẠC THỦ")
                    .setDescription(sb.toString())
                    .setColor(new Color(255, 215, 0)) // Màu vàng Gold
                    .setThumbnail("https://cdn-icons-png.flaticon.com/512/3112/3112946.png")
                    .setFooter("Yêu cầu bởi: " + event.getAuthor().getName(), event.getAuthor().getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (Exception e) {
            log.error("Lỗi thực thi lệnh leaderboard: ", e);
            event.getChannel().sendMessage("❌ Đã có lỗi xảy ra khi xử lý bảng xếp hạng!").queue();
        }
    }
}