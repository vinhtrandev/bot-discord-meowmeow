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
        // args[0] = "!bank", nên kiểm tra args[1]
        if (args.length < 2) {
            sendHelp(event);
            return;
        }

        String subCommand = args[1].toLowerCase();
        String discordId = event.getAuthor().getId();

        try {
            switch (subCommand) {
                case "mo" -> handleOpen(event, args, discordId);
                case "gui" -> handleDeposit(event, args, discordId);
                case "rut" -> handleWithdraw(event, args, discordId);
                case "chuyen" -> handleTransfer(event, args, discordId);
                case "info", "acc" -> handleInfo(event, discordId);
                default -> sendHelp(event);
            }
        } catch (Exception e) {
            event.getChannel().sendMessage("⚠️ " + e.getMessage()).queue();
        }
    }

    private void handleInfo(MessageReceivedEvent event, String discordId) {
        BankAccount ba = bankService.getBank(discordId);
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("🏦 THÔNG TIN NGÂN HÀNG")
                .setColor(Color.CYAN)
                .addField("Hạng thẻ", BankService.TIER_NAME[ba.getTier()], true)
                .addField("Số dư", String.format("**%,d 🪙**", ba.getBalance()), true)
                .addField("Hạn mức", String.format("%,d 🪙", ba.getMaxBalance()), true)
                .setTimestamp(Instant.now());
        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }

    private void handleOpen(MessageReceivedEvent event, String[] args, String discordId) {
        // !bank mo 1 -> args[0]="!bank", args[1]="mo", args[2]="1"
        if (args.length < 3) {
            StringBuilder sb = new StringBuilder("**Danh sách hạng thẻ:**\n");
            for (int i = 1; i < BankService.TIER_NAME.length; i++) {
                sb.append(String.format("`%d`. %s - Phí: **%,d** 🪙 (Sức chứa: **%,d**)\n",
                        i, BankService.TIER_NAME[i], BankService.TIER_COST[i], BankService.TIER_MAX[i]));
            }
            sb.append("\nSử dụng: `!bank mo <số>` (VD: `!bank mo 1`) hay");
            event.getChannel().sendMessage(sb.toString()).queue();
            return;
        }
        int tier = Integer.parseInt(args[2]);
        bankService.openBank(discordId, tier);
        event.getChannel().sendMessage("✅ Đã mở thẻ **" + BankService.TIER_NAME[tier] + "** thành công!").queue();
    }

    private void handleDeposit(MessageReceivedEvent event, String[] args, String discordId) {
        if (args.length < 3) throw new RuntimeException("Cú pháp: `!bank gui <số tiền/all>`");

        long amount = args[2].equalsIgnoreCase("all")
                ? userService.getBalance(discordId)
                : Long.parseLong(args[2]);

        bankService.deposit(discordId, amount);
        event.getChannel().sendMessage(String.format("✅ Đã gửi **%,d 🪙** vào ngân hàng.", amount)).queue();
    }

    private void handleWithdraw(MessageReceivedEvent event, String[] args, String discordId) {
        if (args.length < 3) throw new RuntimeException("Cú pháp: `!bank rut <số tiền/all>`");

        long amount = args[2].equalsIgnoreCase("all")
                ? bankService.getBank(discordId).getBalance()
                : Long.parseLong(args[2]);

        bankService.withdraw(discordId, amount);
        event.getChannel().sendMessage(String.format("✅ Đã rút **%,d 🪙** về ví.", amount)).queue();
    }

    private void handleTransfer(MessageReceivedEvent event, String[] args, String discordId) {
        // !bank chuyen @user 1000 -> args[2] là @user, args[3] là tiền
        if (event.getMessage().getMentions().getMembers().isEmpty() || args.length < 4) {
            throw new RuntimeException("Cú pháp: `!bank chuyen @user <số tiền>`");
        }
        Member target = event.getMessage().getMentions().getMembers().get(0);
        long amount = Long.parseLong(args[3]);

        bankService.transfer(discordId, target.getId(), amount);
        event.getChannel().sendMessage(String.format("💸 Đã chuyển **%,d 🪙** cho **%s**.", amount, target.getEffectiveName())).queue();
    }

    private void sendHelp(MessageReceivedEvent event) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("🏦 HỆ THỐNG NGÂN HÀNG")
                .setColor(Color.YELLOW)
                .setDescription("`!bank info` - Xem tài khoản\n" +
                        "`!bank mo <1-4>` - Mở/Nâng cấp thẻ\n" +
                        "`!bank gui <tiền/all>` - Gửi tiền vào\n" +
                        "`!bank rut <tiền/all>` - Rút tiền ra\n" +
                        "`!bank chuyen @user <tiền>` - Chuyển tiền")
                .setFooter("DogBot Banking System");
        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }
}