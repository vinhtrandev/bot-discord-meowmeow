package com.vinhtran.dogbot.service;

import com.vinhtran.dogbot.entity.BankAccount;
import com.vinhtran.dogbot.entity.User;
import com.vinhtran.dogbot.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class BankService {

    private final BankAccountRepository bankAccountRepository;
    private final UserService userService;

    public static final long[] TIER_COST = {0, 500, 5000, 20000, 100000};
    public static final long[] TIER_MAX  = {0, 5000, 50000, 250000, 1000000000}; // 1 Tỷ
    public static final String[] TIER_NAME = {"Chưa mở", "🥉 Đồng", "🥈 Bạc", "🥇 Vàng", "💎 Kim Cương"};

    public BankAccount openBank(String discordId, String serverId, int tier) {
        User user = userService.getOrCreate(discordId, serverId);

        BankAccount existing = bankAccountRepository.findByUserId(user.getId()).orElse(null);
        if (existing != null && existing.getTier() >= tier)
            throw new RuntimeException("Bạn đã có tài khoản **" + TIER_NAME[existing.getTier()] + "** rồi!");

        long balance = userService.getBalance(discordId, serverId);
        if (balance < TIER_COST[tier])
            throw new RuntimeException("Không đủ coin! Cần **" + TIER_COST[tier] + " 🪙**, bạn có **" + balance + " 🪙**");

        userService.updateBalance(discordId, serverId, -TIER_COST[tier]);

        if (existing != null) {
            existing.setTier(tier);
            existing.setMaxBalance(TIER_MAX[tier]);
            return bankAccountRepository.save(existing);
        }

        return bankAccountRepository.save(BankAccount.builder()
                .user(user)
                .balance(0L)
                .tier(tier)
                .maxBalance(TIER_MAX[tier])
                .openedAt(LocalDateTime.now())
                .build());
    }

    public void deposit(String discordId, String serverId, long amount) {
        User user = userService.getOrCreate(discordId, serverId);
        BankAccount bank = bankAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa mở tài khoản ngân hàng! Dùng `!bank mo <tier>`"));

        long wallet = userService.getBalance(discordId, serverId);
        if (amount > wallet)
            throw new RuntimeException("Ví không đủ! Ví có **" + wallet + " 🪙**");
        if (bank.getBalance() + amount > bank.getMaxBalance())
            throw new RuntimeException("Vượt giới hạn tài khoản **" + TIER_NAME[bank.getTier()]
                    + "**! Tối đa: **" + bank.getMaxBalance() + " 🪙**");

        userService.updateBalance(discordId, serverId, -amount);
        bank.setBalance(bank.getBalance() + amount);
        bankAccountRepository.save(bank);
    }

    public void withdraw(String discordId, String serverId, long amount) {
        User user = userService.getUser(discordId, serverId);
        BankAccount bank = bankAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa mở tài khoản ngân hàng!"));

        if (amount > bank.getBalance())
            throw new RuntimeException("Ngân hàng không đủ! Ngân hàng có **" + bank.getBalance() + " 🪙**");

        bank.setBalance(bank.getBalance() - amount);
        bankAccountRepository.save(bank);
        userService.updateBalance(discordId, serverId, amount);
    }

    public void transfer(String fromDiscordId, String toDiscordId, String serverId, long amount) {
        User fromUser = userService.getUser(fromDiscordId, serverId);
        User toUser   = userService.getUser(toDiscordId, serverId);

        BankAccount from = bankAccountRepository.findByUserId(fromUser.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa mở tài khoản ngân hàng!"));
        BankAccount to   = bankAccountRepository.findByUserId(toUser.getId())
                .orElseThrow(() -> new RuntimeException("Người nhận chưa mở tài khoản ngân hàng!"));

        if (amount <= 0)
            throw new RuntimeException("Số tiền không hợp lệ!");
        if (from.getBalance() < amount)
            throw new RuntimeException("Tài khoản không đủ! Ngân hàng có **" + from.getBalance() + " 🪙**");
        if (to.getBalance() + amount > to.getMaxBalance())
            throw new RuntimeException("Tài khoản người nhận đã đầy!");

        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);
        bankAccountRepository.save(from);
        bankAccountRepository.save(to);
    }

    public BankAccount getBank(String discordId, String serverId) {
        User user = userService.getUser(discordId, serverId);
        return bankAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa mở tài khoản ngân hàng!"));
    }
}