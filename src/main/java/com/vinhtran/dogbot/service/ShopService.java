package com.vinhtran.dogbot.service;

import com.vinhtran.dogbot.entity.ShopItem;
import com.vinhtran.dogbot.entity.User;
import com.vinhtran.dogbot.entity.UserInventory;
import com.vinhtran.dogbot.repository.ShopItemRepository;
import com.vinhtran.dogbot.repository.UserInventoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopService {

    private final ShopItemRepository      shopItemRepository;
    private final UserInventoryRepository inventoryRepository;
    private final UserService             userService;

    @PostConstruct
    public void initShopItems() {
        if (shopItemRepository.count() > 0) return;

        List<ShopItem> items = List.of(
                // --- SKIN BÀI ---
                ShopItem.builder().itemId("skin_default").name("Mặc định")
                        .description("Giao diện bộ bài Tây truyền thống").price(0L)
                        .type("SKIN_BAI").emoji("🃏").isCouple(false).build(),

                ShopItem.builder().itemId("skin_emerald").name("Lục Bảo")
                        .description("Vẻ đẹp tinh xảo, mang lại may mắn từ thiên nhiên").price(10000L)
                        .type("SKIN_BAI").emoji("🎴").isCouple(false).build(),

                ShopItem.builder().itemId("skin_ruby").name("Hồng Ngọc")
                        .description("Sắc đỏ quyền lực, biểu tượng của sự giàu sang").price(50000L)
                        .type("SKIN_BAI").emoji("🧧").isCouple(false).build(),

                ShopItem.builder().itemId("skin_star").name("Tinh Tú")
                        .description("Tuyệt phẩm từ vũ trụ với hào quang lấp lánh").price(200000L)
                        .type("SKIN_BAI").emoji("🔮").isCouple(false).build(),

                // --- NHẪN CẶP ĐÔI ---
                ShopItem.builder().itemId("ring_silver").name("Nhẫn Bạc Đôi")
                        .description("Nhẫn cặp đôi màu bạc – món quà ý nghĩa cho người ấy!")
                        .price(10000L).type("COUPLE_RING").emoji("💍").isCouple(true).build(),

                ShopItem.builder().itemId("ring_gold").name("Nhẫn Vàng Đôi")
                        .description("Nhẫn cặp đôi bằng vàng 24K sang trọng")
                        .price(50000L).type("COUPLE_RING").emoji("💛").isCouple(true).build(),

                ShopItem.builder().itemId("ring_diamond").name("Nhẫn Kim Cương")
                        .description("Biểu tượng của tình yêu vĩnh cửu – hiếm nhất server!")
                        .price(200000L).type("COUPLE_RING").emoji("💎💍").isCouple(true).build()
        );
        shopItemRepository.saveAll(items);
    }

    @Transactional(readOnly = true)
    public List<ShopItem> getAllItems(String serverId) {
        return shopItemRepository.findAllForServer(serverId);
    }

    @Transactional(readOnly = true)
    public List<ShopItem> getItemsByType(String type, String serverId) {
        return shopItemRepository.findByTypeForServer(type, serverId);
    }

    public UserInventory buyItem(String discordId, String serverId, String itemId) {
        ShopItem item = shopItemRepository.findByItemId(itemId, serverId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại!"));

        User user = userService.getOrCreate(discordId, serverId);

        if (inventoryRepository.findByUserIdAndItemId(user.getId(), itemId).isPresent())
            throw new RuntimeException("Bạn đã sở hữu vật phẩm này rồi!");

        long balance = userService.getBalance(discordId, serverId);
        if (balance < item.getPrice())
            throw new RuntimeException("Không đủ coin! Cần **" + item.getPrice()
                    + " 🪙**, bạn có **" + balance + " 🪙**");

        userService.updateBalance(discordId, serverId, -item.getPrice());

        return inventoryRepository.save(UserInventory.builder()
                .user(user).itemId(itemId).isEquipped(false).build());
    }

    public void equipItem(String discordId, String serverId, String itemId) {
        User user = userService.getUser(discordId, serverId);

        UserInventory inv = inventoryRepository.findByUserIdAndItemId(user.getId(), itemId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa sở hữu vật phẩm này!"));

        ShopItem item = shopItemRepository.findByItemId(itemId, serverId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại!"));

        // Bỏ trang bị tất cả item cùng type trước
        List<UserInventory> sameType = inventoryRepository.findByUserId(user.getId()).stream()
                .filter(i -> shopItemRepository.findByItemId(i.getItemId(), serverId)
                        .map(si -> si.getType().equals(item.getType()))
                        .orElse(false))
                .toList();

        sameType.forEach(i -> i.setIsEquipped(false));
        inventoryRepository.saveAll(sameType);

        inv.setIsEquipped(true);
        inventoryRepository.save(inv);
    }

    @Transactional(readOnly = true)
    public String getEquippedSkinEmoji(String discordId, String serverId) {
        User user = userService.getOrCreate(discordId, serverId);
        return inventoryRepository.findByUserIdAndIsEquipped(user.getId(), true).stream()
                .map(inv -> shopItemRepository.findByItemId(inv.getItemId(), serverId)
                        .filter(i -> i.getType().equals("SKIN_BAI"))
                        .map(ShopItem::getEmoji).orElse(null))
                .filter(e -> e != null)
                .findFirst()
                .orElse("🃏");
    }

    @Transactional(readOnly = true)
    public List<UserInventory> getInventory(String discordId, String serverId) {
        User user = userService.getOrCreate(discordId, serverId);
        return inventoryRepository.findByUserId(user.getId());
    }
}