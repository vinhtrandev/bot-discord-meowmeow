package com.vinhtran.dogbot.game;

import com.vinhtran.dogbot.util.CardImageGenerator;
import lombok.Getter;
import java.io.InputStream;
import java.util.*;

public class BlackjackGame {

    public record Card(int rank, int suitIndex) {}

    @Getter // Tự động tạo hàm getPlayerHand()
    private final List<Card> playerHand = new ArrayList<>();

    @Getter // Tự động tạo hàm getDealerHand()
    private final List<Card> dealerHand = new ArrayList<>();

    private final Random random = new Random();

    public BlackjackGame() {
        playerHand.add(drawCard());
        playerHand.add(drawCard());
        dealerHand.add(drawCard());
        dealerHand.add(drawCard());
    }

    public Card drawCard() {
        return new Card(random.nextInt(13) + 1, random.nextInt(4));
    }

    public void playerHit() { playerHand.add(drawCard()); }

    // =====================================================================
    // TÍNH ĐIỂM & TRẠNG THÁI
    // =====================================================================
    public int calcScore(List<Card> hand) {
        int score = 0, aceCount = 0;
        for (Card c : hand) {
            if (c.rank() == 1)       { aceCount++; score += 11; }
            else if (c.rank() >= 10) { score += 10; }
            else                     { score += c.rank(); }
        }
        while (score > 21 && aceCount > 0) { score -= 10; aceCount--; }
        return score;
    }

    public int getPlayerScore() { return calcScore(playerHand); }
    public int getDealerScore() { return calcScore(dealerHand); }

    public boolean playerBust() { return getPlayerScore() > 21; }
    public boolean dealerBust() { return getDealerScore() > 21; }

    // Logic bộ đặc biệt
    public boolean isXiBang(List<Card> hand)  { return hand.size() == 2 && hand.get(0).rank() == 1 && hand.get(1).rank() == 1; }
    public boolean isXiDach(List<Card> hand)  {
        if (hand.size() != 2) return false;
        int a = hand.get(0).rank(), b = hand.get(1).rank();
        return (a == 1 && b >= 10) || (b == 1 && a >= 10);
    }
    public boolean isNguLinh(List<Card> hand) { return hand.size() >= 5 && calcScore(hand) <= 21; }

    public boolean isPlayerXiBang()  { return isXiBang(playerHand); }
    public boolean isDealerXiBang()  { return isXiBang(dealerHand); }
    public boolean isPlayerXiDach()  { return isXiDach(playerHand); }
    public boolean isDealerXiDach()  { return isXiDach(dealerHand); }
    public boolean isPlayerNguLinh() { return isNguLinh(playerHand); }
    public boolean isDealerNguLinh() { return isNguLinh(dealerHand); }

    public boolean canPlayerStand()  { return getPlayerScore() >= 16 || isPlayerNguLinh(); }

    public void dealerPlay() {
        while (getDealerScore() < 15 && !isDealerNguLinh() && dealerHand.size() < 5) {
            dealerHand.add(drawCard());
        }
    }

    // =====================================================================
    // HIỂN THỊ TRÊN ẢNH (Đã sửa lỗi int -> String)
    // =====================================================================

    public String getRankLabel(List<Card> hand, boolean isHidden) {
        if (isHidden) {
            Card first = hand.get(0);
            int val = (first.rank() == 1) ? 11 : Math.min(first.rank(), 10);
            return val + " + ?";
        }
        if (isXiBang(hand))  return "Xì Bàn 🃏🃏";
        if (isXiDach(hand))  return "Xì Dách 🃏";
        if (isNguLinh(hand)) return "Ngũ Linh ✨";
        int score = calcScore(hand);
        if (score > 21) return "Quắc (" + score + ") 💀";
        return score + " điểm";
    }

    public InputStream getTableImagePlaying() throws Exception {
        return CardImageGenerator.drawTable(
                playerHand, getRankLabel(playerHand, false),
                dealerHand, getRankLabel(dealerHand, true),
                true
        );
    }

    public InputStream getTableImageFinal() throws Exception {
        return CardImageGenerator.drawTable(
                playerHand, getRankLabel(playerHand, false),
                dealerHand, getRankLabel(dealerHand, false),
                false
        );
    }
}