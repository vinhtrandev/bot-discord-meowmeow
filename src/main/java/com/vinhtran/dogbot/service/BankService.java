package com.vinhtran.dogbot.service;

import com.vinhtran.dogbot.entity.BankAccount;
import com.vinhtran.dogbot.entity.User;
import com.vinhtran.dogbot.repository.BankAccountRepository;
import com.vinhtran.dogbot.repository.UserCoinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class BankService {

    private final BankAccountRepository bankAccountRepository;
    private final UserCoinRepository userCoinRepository;
    private final UserService userService;

    // Tier info
    public static final long[] TIER_COST    = {0, 500, 2000, 5000, 15000};
    public static final long[] TIER_MAX     = {0, 5000, 20000, 100000, 999999999};
    public static final String[] TIER_NAME  = {"Chưa mở", "🥉 Đồng", "🥈 Bạc", "🥇 Vàng", "💎 Kim Cương"};

    public BankAccount openBank(String discordId, int tier) {
        User user = userService.getUser(discordId);

        // Kiểm tra đã có tài khoản chưa
        BankAccount existing = bankAccountRepository.findByUserDiscordId(discordId).orElse(null);
        if (existing != null && existing.getTier() >= tier) {
            throw new RuntimeException("Bạn đã có tài khoản **" + TIER_NAME[existing.getTier()] + "** rồi!");
        }

        long cost = TIER_COST[tier];
        long balance = userService.getBalance(discordId);
        if (balance < cost) {
            throw new RuntimeException("Không đủ coin! Cần **" + cost + " 🪙**, bạn có **" + balance + " 🪙**");
        }

        userService.updateBalance(discordId, -cost);

        if (existing != null) {
            existing.setTier(tier);
            existing.setMaxBalance(TIER_MAX[tier]);
            return bankAccountRepository.save(existing);
        }

        BankAccount bank = BankAccount.builder()
                .user(user).balance(0L)
                .tier(tier).maxBalance(TIER_MAX[tier])
                .openedAt(java.time.LocalDateTime.now())
                .build();
        return bankAccountRepository.save(bank);
    }

    public void deposit(String discordId, long amount) {
        BankAccount bank = bankAccountRepository.findByUserDiscordId(discordId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa mở tài khoản ngân hàng! Dùng `!bank mo <tier>`"));

        if (bank.getTier() == 0)
            throw new RuntimeException("Tài khoản chưa được kích hoạt!");

        long wallet = userService.getBalance(discordId);
        if (amount > wallet)
            throw new RuntimeException("Ví không đủ! Ví có **" + wallet + " 🪙**");
        if (bank.getBalance() + amount > bank.getMaxBalance())
            throw new RuntimeException("Vượt giới hạn tài khoản **" + TIER_NAME[bank.getTier()]
                    + "**! Tối đa: **" + bank.getMaxBalance() + " 🪙**");

        userService.updateBalance(discordId, -amount);
        bank.setBalance(bank.getBalance() + amount);
        bankAccountRepository.save(bank);
    }

    public void withdraw(String discordId, long amount) {
        BankAccount bank = bankAccountRepository.findByUserDiscordId(discordId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa mở tài khoản ngân hàng!"));

        if (amount > bank.getBalance())
            throw new RuntimeException("Ngân hàng không đủ! Ngân hàng có **" + bank.getBalance() + " 🪙**");

        bank.setBalance(bank.getBalance() - amount);
        bankAccountRepository.save(bank);
        userService.updateBalance(discordId, amount);
    }

    public void transfer(String fromDiscordId, String toDiscordId, long amount) {
        BankAccount from = bankAccountRepository.findByUserDiscordId(fromDiscordId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa mở tài khoản ngân hàng!"));
        BankAccount to = bankAccountRepository.findByUserDiscordId(toDiscordId)
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

    public BankAccount getBank(String discordId) {
        return bankAccountRepository.findByUserDiscordId(discordId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa mở tài khoản ngân hàng!"));
    }
}