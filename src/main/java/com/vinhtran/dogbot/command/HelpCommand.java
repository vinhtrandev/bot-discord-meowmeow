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
                // Game
                .addField("!bj <coin/all>",  "Chơi Xì Dách | 5p không chơi tự hủy",       false)
                .addField("!bc <coin/all>",     "Chơi Bài Cào",                               false)
                // Tài khoản
                .addField("!balance",               "Xem số dư ví + ngân hàng",                   false)
                // Kinh tế
                .addField("!steal @user",           "Trộm coin ví người khác (10p cooldown)",     false)
                .addField("!bank",                  "Menu ngân hàng (mở/gửi/rút/xem)",            false)
                // Shop
                .addField("!shop",                  "Cửa hàng vật phẩm (skin/frame/nhẫn)",        false)
                .addField("!shop mua <item_id>",    "Mua vật phẩm",                               false)
                .addField("!shop trang_bi <id>",    "Trang bị vật phẩm",                          false)
                .addField("!shop tui_do",           "Xem túi đồ",                                 false)
                // Cặp đôi
                .addField("!couple tang @user <id>","Tặng nhẫn cầu hôn",                        false)
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