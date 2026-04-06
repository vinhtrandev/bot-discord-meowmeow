package com.vinhtran.dogbot.service;

import com.vinhtran.dogbot.entity.ShopItem;
import com.vinhtran.dogbot.entity.UserInventory;
import com.vinhtran.dogbot.repository.ShopItemRepository;
import com.vinhtran.dogbot.repository.UserInventoryRepository;
import com.vinhtran.dogbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopService {

    private final ShopItemRepository shopItemRepository;
    private final UserInventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    // Khởi tạo shop items mặc định khi app start
    @jakarta.annotation.PostConstruct
    public void initShopItems() {
        if (shopItemRepository.count() > 0) return;

        List<ShopItem> items = List.of(
                // Skin bài
                ShopItem.builder().itemId("skin_default").name("Skin Mặc Định")
                        .description("Skin bài mặc định").price(0L)
                        .type("SKIN_BAI").emoji("🃏").isCouple(false).build(),

                ShopItem.builder().itemId("skin_cute").name("Skin Cute")
                        .description("Bài hình cute dễ thương").price(500L)
                        .type("SKIN_BAI").emoji("🌸").isCouple(false).build(),

                ShopItem.builder().itemId("skin_anime").name("Skin Anime")
                        .description("Bài phong cách anime").price(1000L)
                        .type("SKIN_BAI").emoji("⚔️").isCouple(false).build(),

                ShopItem.builder().itemId("skin_galaxy").name("Skin Galaxy")
                        .description("Bài thiên hà lung linh").price(2000L)
                        .type("SKIN_BAI").emoji("🌌").isCouple(false).build(),

                // Frame / Avatar
                ShopItem.builder().itemId("frame_silver").name("Khung Bạc")
                        .description("Khung profile màu bạc").price(300L)
                        .type("FRAME").emoji("🥈").isCouple(false).build(),

                ShopItem.builder().itemId("frame_gold").name("Khung Vàng")
                        .description("Khung profile màu vàng lấp lánh").price(1000L)
                        .type("FRAME").emoji("✨").isCouple(false).build(),

                ShopItem.builder().itemId("frame_diamond").name("Khung Kim Cương")
                        .description("Khung profile kim cương nhấp nháy").price(5000L)
                        .type("FRAME").emoji("💎").isCouple(false).build(),

                // Couple Ring
                ShopItem.builder().itemId("ring_silver").name("Nhẫn Bạc Đôi")
                        .description("Nhẫn cặp đôi màu bạc – tặng cho người ấy!").price(1000L)
                        .type("COUPLE_RING").emoji("💍").isCouple(true).build(),

                ShopItem.builder().itemId("ring_gold").name("Nhẫn Vàng Đôi")
                        .description("Nhẫn cặp đôi màu vàng sang trọng").price(3000L)
                        .type("COUPLE_RING").emoji("💛").isCouple(true).build(),

                ShopItem.builder().itemId("ring_diamond").name("Nhẫn Kim Cương Đôi")
                        .description("Nhẫn cặp đôi kim cương – hiếm nhất server!").price(10000L)
                        .type("COUPLE_RING").emoji("💎💍").isCouple(true).build()
        );
        shopItemRepository.saveAll(items);
    }

    public List<ShopItem> getAllItems() {
        return shopItemRepository.findAll();
    }

    public List<ShopItem> getItemsByType(String type) {
        return shopItemRepository.findByType(type);
    }

    public UserInventory buyItem(String discordId, String itemId) {
        ShopItem item = shopItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại!"));

        // Kiểm tra đã có chưa
        if (inventoryRepository.findByUserDiscordIdAndItemId(discordId, itemId).isPresent()) {
            throw new RuntimeException("Bạn đã sở hữu vật phẩm này rồi!");
        }

        long balance = userService.getBalance(discordId);
        if (balance < item.getPrice()) {
            throw new RuntimeException("Không đủ coin! Cần **" + item.getPrice()
                    + " coin**, bạn có **" + balance + " coin**");
        }

        userService.updateBalance(discordId, -item.getPrice());

        var user = userRepository.findByDiscordId(discordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

        return inventoryRepository.save(UserInventory.builder()
                .user(user).itemId(itemId).isEquipped(false).build());
    }

    public void equipItem(String discordId, String itemId) {
        UserInventory inv = inventoryRepository
                .findByUserDiscordIdAndItemId(discordId, itemId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa sở hữu vật phẩm này!"));

        ShopItem item = shopItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Vật phẩm không tồn tại!"));

        // Bỏ trang bị tất cả item cùng type
        List<UserInventory> sameType = inventoryRepository
                .findByUserDiscordId(discordId).stream()
                .filter(i -> {
                    var si = shopItemRepository.findByItemId(i.getItemId()).orElse(null);
                    return si != null && si.getType().equals(item.getType());
                }).toList();

        sameType.forEach(i -> i.setIsEquipped(false));
        inventoryRepository.saveAll(sameType);

        inv.setIsEquipped(true);
        inventoryRepository.save(inv);
    }

    public String getEquippedSkinEmoji(String discordId) {
        return inventoryRepository.findByUserDiscordId(discordId).stream()
                .filter(UserInventory::getIsEquipped)
                .map(inv -> shopItemRepository.findByItemId(inv.getItemId())
                        .filter(i -> i.getType().equals("SKIN_BAI"))
                        .map(ShopItem::getEmoji).orElse(null))
                .filter(e -> e != null)
                .findFirst()
                .orElse("🃏");
    }

    public String getEquippedFrameEmoji(String discordId) {
        return inventoryRepository.findByUserDiscordId(discordId).stream()
                .filter(UserInventory::getIsEquipped)
                .map(inv -> shopItemRepository.findByItemId(inv.getItemId())
                        .filter(i -> i.getType().equals("FRAME"))
                        .map(ShopItem::getEmoji).orElse(null))
                .filter(e -> e != null)
                .findFirst()
                .orElse("");
    }

    public List<UserInventory> getInventory(String discordId) {
        return inventoryRepository.findByUserDiscordId(discordId);
    }
}