package com.vinhtran.dogbot.command;

import com.vinhtran.dogbot.entity.ShopItem;
import com.vinhtran.dogbot.entity.UserInventory;
import com.vinhtran.dogbot.service.ShopService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ShopCommand implements Command {

    private final ShopService shopService;

    @Override
    public String getName() { return "!shop"; }

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        String userId = event.getAuthor().getId();

        if (args.length < 2) {
            showShop(event);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "mua"      -> handleBuy(event, userId, args);
            case "trang_bi" -> handleEquip(event, userId, args);
            case "tui_do"   -> handleInventory(event, userId);
            default         -> showShop(event);
        }
    }

    private void showShop(MessageReceivedEvent event) {
        List<ShopItem> items = shopService.getAllItems();
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("🏪 Cửa Hàng Vật Phẩm")
                .setColor(Color.MAGENTA);

        // Skin bài
        StringBuilder skinSb = new StringBuilder();
        items.stream().filter(i -> i.getType().equals("SKIN_BAI")).forEach(i ->
                skinSb.append(i.getEmoji()).append(" **").append(i.getName()).append("**")
                        .append(" — ").append(i.getPrice() == 0 ? "Miễn phí" : i.getPrice() + " coin")
                        .append(" `").append(i.getItemId()).append("`\n"));
        eb.addField("🃏 Skin Bài", skinSb.toString(), false);

        // Frame
        StringBuilder frameSb = new StringBuilder();
        items.stream().filter(i -> i.getType().equals("FRAME")).forEach(i ->
                frameSb.append(i.getEmoji()).append(" **").append(i.getName()).append("**")
                        .append(" — ").append(i.getPrice()).append(" coin")
                        .append(" `").append(i.getItemId()).append("`\n"));
        eb.addField("🖼️ Khung Profile", frameSb.toString(), false);

        // Nhẫn cặp đôi
        StringBuilder ringSb = new StringBuilder();
        items.stream().filter(i -> i.getType().equals("COUPLE_RING")).forEach(i ->
                ringSb.append(i.getEmoji()).append(" **").append(i.getName()).append("**")
                        .append(" — ").append(i.getPrice()).append(" coin")
                        .append(" `").append(i.getItemId()).append("`\n"));
        eb.addField("💍 Nhẫn Cặp Đôi", ringSb.toString(), false);

        eb.addField("Lệnh",
                "`!shop mua <item_id>` — Mua vật phẩm\n"
                        + "`!shop trang_bi <item_id>` — Trang bị\n"
                        + "`!shop tui_do` — Xem túi đồ", false);

        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }

    private void handleBuy(MessageReceivedEvent event, String userId, String[] args) {
        if (args.length < 3) { event.getChannel().sendMessage("Dùng: `!shop mua <item_id>`").queue(); return; }
        try {
            UserInventory inv = shopService.buyItem(userId, args[2]);
            event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("✅ Mua thành công!")
                    .setDescription("Đã mua **" + args[2] + "**!\nDùng `!shop trang_bi " + args[2] + "` để trang bị.")
                    .setColor(Color.GREEN).build()).queue();
        } catch (Exception e) {
            event.getChannel().sendMessage("❌ " + e.getMessage()).queue();
        }
    }

    private void handleEquip(MessageReceivedEvent event, String userId, String[] args) {
        if (args.length < 3) { event.getChannel().sendMessage("Dùng: `!shop trang_bi <item_id>`").queue(); return; }
        try {
            shopService.equipItem(userId, args[2]);
            event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("✅ Trang bị thành công!")
                    .setDescription("Đã trang bị **" + args[2] + "**!")
                    .setColor(Color.GREEN).build()).queue();
        } catch (Exception e) {
            event.getChannel().sendMessage("❌ " + e.getMessage()).queue();
        }
    }

    private void handleInventory(MessageReceivedEvent event, String userId) {
        List<UserInventory> inv = shopService.getInventory(userId);
        if (inv.isEmpty()) {
            event.getChannel().sendMessage("Túi đồ trống! Mua vật phẩm tại `!shop`").queue();
            return;
        }
        StringBuilder sb = new StringBuilder();
        inv.forEach(i -> sb.append(i.getIsEquipped() ? "✅ " : "⬜ ")
                .append("**").append(i.getItemId()).append("**\n"));

        event.getChannel().sendMessageEmbeds(new EmbedBuilder()
                .setTitle("🎒 Túi Đồ")
                .setDescription(sb.toString())
                .setFooter("✅ = Đang trang bị")
                .setColor(Color.CYAN).build()).queue();
    }
}