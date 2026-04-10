package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.entity.BankAccount;
import com.vinhtran.dogbot.service.BankService;
import com.vinhtran.dogbot.service.UserService;
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
public class BankCommand implements Command {

    private final BankService bankService;
    private final UserService userService;

    @Override
    public String getName() { return "!bank"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) { sendHelp(event); return; }

        String discordId = event.getAuthor().getId();
        String serverId  = event.getGuild().getId();

        try {
            switch (args[1].toLowerCase()) {
                case "mo"              -> handleOpen(event, args, discordId, serverId);
                case "gui"             -> handleDeposit(event, args, discordId, serverId);
                case "rut"             -> handleWithdraw(event, args, discordId, serverId);
                case "chuyen"          -> handleTransfer(event, args, discordId, serverId);
                case "info", "acc"     -> handleInfo(event, discordId, serverId);
                default                -> sendHelp(event);
            }
        } catch (Exception e) {
            event.getChannel().sendMessage("⚠️ " + e.getMessage()).queue();
        }
    }

    private void handleInfo(MessageReceivedEvent event, String discordId, String serverId) {
        BankAccount ba = bankService.getBank(discordId, serverId);
        event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("🏦 THÔNG TIN NGÂN HÀNG")
                .setColor(Color.CYAN)
                .addField("Hạng thẻ", BankService.TIER_NAME[ba.getTier()], true)
                .addField("Số dư",    String.format("**%,d 🪙**", ba.getBalance()), true)
                .addField("Hạn mức",  String.format("%,d 🪙", ba.getMaxBalance()), true)
                .setTimestamp(Instant.now())
                .build()).queue();
    }

    private void handleOpen(MessageReceivedEvent event, String[] args,
                            String discordId, String serverId) {
        if (args.length < 3) {
            StringBuilder sb = new StringBuilder("**Danh sách hạng thẻ:**\n");
            for (int i = 1; i < BankService.TIER_NAME.length; i++) {
                sb.append(String.format("`%d`. %s - Phí: **%,d** 🪙 (Sức chứa: **%,d**)\n",
                        i, BankService.TIER_NAME[i], BankService.TIER_COST[i], BankService.TIER_MAX[i]));
            }
            sb.append("\nSử dụng: `!bank mo <số>` (VD: `!bank mo 1`)");
            event.getChannel().sendMessage(sb.toString()).queue();
            return;
        }
        int tier = Integer.parseInt(args[2]);
        bankService.openBank(discordId, serverId, tier);
        event.getChannel().sendMessage("✅ Đã mở thẻ **" + BankService.TIER_NAME[tier] + "** thành công!").queue();
    }

    private void handleDeposit(MessageReceivedEvent event, String[] args,
                               String discordId, String serverId) {
        if (args.length < 3) throw new RuntimeException("Cú pháp: `!bank gui <số tiền/all>`");
        long amount = args[2].equalsIgnoreCase("all")
                ? userService.getBalance(discordId, serverId)
                : Long.parseLong(args[2]);
        bankService.deposit(discordId, serverId, amount);
        event.getChannel().sendMessage(String.format("✅ Đã gửi **%,d 🪙** vào ngân hàng.", amount)).queue();
    }

    private void handleWithdraw(MessageReceivedEvent event, String[] args,
                                String discordId, String serverId) {
        if (args.length < 3) throw new RuntimeException("Cú pháp: `!bank rut <số tiền/all>`");
        long amount = args[2].equalsIgnoreCase("all")
                ? bankService.getBank(discordId, serverId).getBalance()
                : Long.parseLong(args[2]);
        bankService.withdraw(discordId, serverId, amount);
        event.getChannel().sendMessage(String.format("✅ Đã rút **%,d 🪙** về ví.", amount)).queue();
    }

    private void handleTransfer(MessageReceivedEvent event, String[] args,
                                String discordId, String serverId) {
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 4)
            throw new RuntimeException("Cú pháp: `!bank chuyen @user <số tiền>`");
        Member target = event.getMessage().getMentions().getMembers().get(0);
        long amount   = Long.parseLong(args[3]);
        bankService.transfer(discordId, target.getId(), serverId, amount);
        event.getChannel().sendMessage(String.format("💸 Đã chuyển **%,d 🪙** cho **%s**.",
                amount, target.getEffectiveName())).queue();
    }

    private void sendHelp(MessageReceivedEvent event) {
        event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("🏦 HỆ THỐNG NGÂN HÀNG Meow Meow")
                .setColor(Color.YELLOW)
                .setDescription(
                        "### 💳 CÁC HẠNG THẺ HIỆN CÓ\n" +
                                "🥉 **Cấp 1: Thẻ Đồng**\n┕ Phí: 500 | Max: 5,000 🪙\n" +
                                "🥈 **Cấp 2: Thẻ Bạc**\n┕ Phí: 5,000 | Max: 50,000 🪙\n" +
                                "🥇 **Cấp 3: Thẻ Vàng**\n┕ Phí: 20,000 | Max: 250,000 🪙\n" +
                                "💎 **Cấp 4: Thẻ Kim Cương**\n┕ Phí: 100,000 | Max: 1 Tỷ 🪙\n\n" +

                                "### 🛠️ LỆNH TƯƠNG TÁC\n" +
                                "🔹 `!bank info` — Kiểm tra số dư & hạng thẻ\n" +
                                "🔹 `!bank mo <1-4>` — Mở hoặc nâng cấp thẻ\n" +
                                "🔹 `!bank gui <số/all>` — Gửi tiền vào ngân hàng\n" +
                                "🔹 `!bank rut <số/all>` — Rút tiền về ví chính\n" +
                                "🔹 `!bank chuyen @user <số>` — Chuyển tiền từ **Ngân hàng** của bạn sang **Ngân hàng** người khác\n" +
                                "⚠️ *Lưu ý: Cả 2 phải đã mở thẻ và có đủ tiền trong tài khoản.*\n" +

                                "💡 *Mẹo: Gửi tiền vào ngân hàng để không bị người khác trộm (`!steal`)!*")
                .setFooter("Meow Meow Banking System")
                .build()).queue();
    }
}