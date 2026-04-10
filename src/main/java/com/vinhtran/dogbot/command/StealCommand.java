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
    public String getName() { return "!steal"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (event.getMessage().getMentions().getMembers().isEmpty()) {
            event.getChannel().sendMessage("❌ Bạn cần tag người muốn trộm! HD: `!steal @username`").queue();
            return;
        }

        Member target   = event.getMessage().getMentions().getMembers().get(0);
        String thiefId  = event.getAuthor().getId();
        String targetId = target.getId();
        String serverId = event.getGuild().getId();

        if (target.getUser().isBot()) {
            event.getChannel().sendMessage("🤖 Bạn không thể trộm tiền của Bot!").queue();
            return;
        }

        try {
            StealService.StealResult result = stealService.steal(thiefId, targetId, serverId);

            EmbedBuilder embed = new EmbedBuilder()
                    .setAuthor(event.getAuthor().getName(), null, event.getAuthor().getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now());

            if (result.success()) {
                embed.setTitle("💰 PHI VỤ THÀNH CÔNG!")
                        .setDescription(String.format("Bạn đã lẻn vào nhà **%s** và lấy đi **%d** 🪙!",
                                target.getEffectiveName(), result.amount()))
                        .setColor(Color.GREEN);
            } else {
                embed.setTitle("🚔 PHI VỤ THẤT BẠI!")
                        .setDescription(String.format("Cảnh sát đã tóm gọn bạn tại nhà **%s**!\nBạn bị phạt **%d** 🪙.",
                                target.getEffectiveName(), result.amount()))
                        .setColor(Color.RED);
            }

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (RuntimeException e) {
            event.getChannel().sendMessage("⚠️ " + e.getMessage()).queue();
        } catch (Exception e) {
            log.error("Lỗi lệnh steal: ", e);
            event.getChannel().sendMessage("❌ Đã xảy ra lỗi hệ thống!").queue();
        }
    }
}