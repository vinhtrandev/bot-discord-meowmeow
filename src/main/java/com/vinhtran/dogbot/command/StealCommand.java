package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.service.StealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class StealCommand implements Command {

    private final StealService stealService;

    @Override
    public String getName() {
        return "!steal";
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        // 1. Kiểm tra xem có tag ai không
        if (event.getMessage().getMentions().getMembers().isEmpty()) {
            event.getChannel().sendMessage("❌ Bạn cần tag người muốn trộm! HD: `!steal @username`").queue();
            return;
        }

        Member target = event.getMessage().getMentions().getMembers().get(0);
        String thiefId = event.getAuthor().getId();
        String targetId = target.getId();

        // Không cho phép trộm Bot
        if (target.getUser().isBot()) {
            event.getChannel().sendMessage("🤖 Bạn không thể trộm tiền của Bot đâu, nó không có ví đâu!").queue();
            return;
        }

        try {
            // 2. Gọi Service xử lý logic
            StealService.StealResult result = stealService.steal(thiefId, targetId);

            // 3. Xây dựng thông báo kết quả bằng Embed
            EmbedBuilder embed = new EmbedBuilder()
                    .setAuthor(event.getAuthor().getName(), null, event.getAuthor().getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now());

            if (result.success()) {
                embed.setTitle("💰 PHI VỤ THÀNH CÔNG!")
                        .setDescription(String.format("Bạn đã lẻn vào nhà **%s** và lấy đi **%d** 🪙!",
                                target.getEffectiveName(), result.amount()))
                        .setColor(Color.GREEN)
                        .setThumbnail("https://cdn-icons-png.flaticon.com/512/1039/1039328.png"); // Icon túi tiền
            } else {
                embed.setTitle("🚔 PHI VỤ THẤT BẠI!")
                        .setDescription(String.format("Cảnh sát đã tóm gọn bạn tại nhà **%s**!\nBạn bị phạt **%d** 🪙 vì tội trộm cắp.",
                                target.getEffectiveName(), result.amount()))
                        .setColor(Color.RED)
                        .setThumbnail("https://cdn-icons-png.flaticon.com/512/1022/1022333.png"); // Icon cảnh sát
            }

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (RuntimeException e) {
            // Xử lý các lỗi như: Trùng ID, Hết tiền, Đang trong cooldown
            event.getChannel().sendMessage("⚠️ " + e.getMessage()).queue();
        } catch (Exception e) {
            log.error("Lỗi lệnh steal: ", e);
            event.getChannel().sendMessage("❌ Đã xảy ra lỗi hệ thống khi đi trộm!").queue();
        }
    }
}