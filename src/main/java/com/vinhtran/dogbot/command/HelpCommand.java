package com.vinhtran.dogbot.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

import java.awt.*;

@Component
public class HelpCommand implements Command {

    @Override
    public String getName() { return "!help"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("📖 Danh Sách Lệnh")
                .setColor(Color.BLUE)
                // Tài khoản
                .addField("!register",              "Đăng ký, nhận 1000 coin",                    false)
                .addField("!balance",               "Xem số dư ví + ngân hàng",                   false)
                // Game
                .addField("!blackjack <coin/all>",  "Chơi Xì Dách | 5p không chơi tự hủy",       false)
                .addField("!baicao <coin/all>",     "Chơi Bài Cào",                               false)
                // Kinh tế
                .addField("!steal @user",           "Trộm coin ví người khác (10p cooldown)",     false)
                .addField("!bank",                  "Menu ngân hàng (mở/gửi/rút/xem)",            false)
                .addField("!transfer @user <coin>", "Chuyển khoản từ ngân hàng",                  false)
                // Shop
                .addField("!shop",                  "Cửa hàng vật phẩm (skin/frame/nhẫn)",        false)
                .addField("!shop mua <item_id>",    "Mua vật phẩm",                               false)
                .addField("!shop trang_bi <id>",    "Trang bị vật phẩm",                          false)
                .addField("!shop tui_do",           "Xem túi đồ",                                 false)
                // Cặp đôi
                .addField("!couple tang @user <ring>","Tặng nhẫn cầu hôn",                        false)
                .addField("!couple nhan",            "Chấp nhận lời mời cặp đôi",                 false)
                .addField("!couple huy",             "Hủy cặp đôi",                               false)
                .addField("!couple xem",             "Xem thông tin cặp đôi",                     false)
                // Misc
                .addField("!leaderboard",           "Bảng xếp hạng top 10",                       false)
                .addField("!help",                  "Xem danh sách lệnh này",                      false)
                .build()
        ).queue();
    }
}