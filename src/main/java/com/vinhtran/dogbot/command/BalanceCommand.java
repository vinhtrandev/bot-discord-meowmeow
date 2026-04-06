package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.entity.BankAccount;
import com.vinhtran.dogbot.service.BankService;
import com.vinhtran.dogbot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class BalanceCommand implements Command {

    private final UserService userService;
    private final BankService bankService;

    @Override
    public String getName() {
        return "!balance";
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String discordId = event.getAuthor().getId();
        String authorName = event.getAuthor().getName();
        String avatarUrl = event.getAuthor().getEffectiveAvatarUrl();

        try {
            // 1. Lấy số dư ví (Wallet)
            long wallet = userService.getBalance(discordId);

            // 2. Lấy thông tin ngân hàng (Bank)
            long bank = 0;
            String bankInfo = "❌ *Chưa mở tài khoản*";
            String tierDisplay = "N/A";

            try {
                BankAccount ba = bankService.getBank(discordId);
                if (ba != null) {
                    bank = ba.getBalance();
                    tierDisplay = BankService.TIER_NAME[ba.getTier()];
                    bankInfo = String.format("`%s` | **%,d 🪙**", tierDisplay, bank);
                }
            } catch (Exception ignored) {
                // Nếu chưa có bank thì vẫn giữ giá trị mặc định
            }

            // 3. Tính tổng tài sản
            long total = wallet + bank;

            // 4. Xây dựng giao diện Embed đẹp hơn
            EmbedBuilder embed = new EmbedBuilder()
                    .setAuthor("Tài chính của " + authorName, null, avatarUrl)
                    .setTitle("🏦 THÔNG TIN TÀI KHOẢN")
                    .setColor(new Color(52, 152, 219)) // Màu xanh dương chuyên nghiệp

                    // Sử dụng định dạng %,d để hiển thị dấu phân cách hàng nghìn
                    .addField("👛 Ví tiền", String.format("**%,d** 🪙", wallet), true)
                    .addField("💳 Ngân hàng", bankInfo, true)

                    // Dòng ngăn cách hoặc khoảng trống
                    .addBlankField(false)

                    .addField("💰 Tổng tài sản", String.format("**%,d** 🪙", total), false)

                    .setThumbnail("https://cdn-icons-png.flaticon.com/512/2489/2489756.png") // Icon ví/tiền
                    .setFooter("Yêu cầu bởi " + authorName, avatarUrl)
                    .setTimestamp(Instant.now());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra số dư của {}: ", discordId, e);
            event.getChannel().sendMessage("❌ **Lỗi:** Không thể tải thông tin tài chính của bạn!").queue();
        }
    }
}