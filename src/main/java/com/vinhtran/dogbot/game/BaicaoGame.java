package com.vinhtran.dogbot.game;

import com.vinhtran.dogbot.util.CardImageGenerator;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class BaicaoGame {

    public record Card(int rank, int suitIndex) {}

    private static final String[] SUIT_SYMBOLS = {"♠", "♥", "♦", "♣"};
    private static final String[] RANK_NAME    = {
            "", "A","2","3","4","5","6","7","8","9","10","J","Q","K"
    };

    private final Random random = new Random();

    // Hand giờ chứa List<Card> thật
    public record Hand(List<Card> cards, int score, String rank, String cardsText) {}

    public Hand dealHand() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            cards.add(new Card(random.nextInt(13) + 1, random.nextInt(4)));

        int score = calcScore(cards);
        String rank = getRank(cards, score);
        String text = cards.stream()
                .map(c -> RANK_NAME[c.rank()] + SUIT_SYMBOLS[c.suitIndex()])
                .collect(Collectors.joining("  "));
        return new Hand(cards, score, rank, text);
    }

    private int calcScore(List<Card> cards) {
        int total = 0;
        for (Card c : cards) {
            int v = c.rank();
            if (v >= 10) v = 0; // J, Q, K, 10 = 0 điểm
            else if (v == 1) v = 1; // Ace = 1
            total += v;
        }
        return total % 10;
    }

    private String getRank(List<Card> cards, int score) {
        if (isThung(cards) && isSanh(cards)) return "Thùng Phá Sảnh 🔥";
        if (isThung(cards))                  return "Thùng ♠";
        if (isSanh(cards))                   return "Sảnh →";
        if (isBaCo(cards))                   return "Ba Cô 👑";
        if (isDoi(cards))                    return "Đôi ✌";
        if (score == 9)                      return "Cào 9 🔥";
        if (score == 8)                      return "Cào 8";
        return "Điểm " + score;
    }

    private boolean isThung(List<Card> cards) {
        return cards.get(0).suitIndex() == cards.get(1).suitIndex()
                && cards.get(1).suitIndex() == cards.get(2).suitIndex();
    }

    private boolean isSanh(List<Card> cards) {
        List<Integer> ranks = cards.stream()
                .map(Card::rank).sorted().collect(Collectors.toList());
        return ranks.get(1) == ranks.get(0) + 1 && ranks.get(2) == ranks.get(1) + 1;
    }

    private boolean isBaCo(List<Card> cards) {
        return cards.get(0).rank() == cards.get(1).rank()
                && cards.get(1).rank() == cards.get(2).rank();
    }

    private boolean isDoi(List<Card> cards) {
        int a = cards.get(0).rank(), b = cards.get(1).rank(), c = cards.get(2).rank();
        return a == b || b == c || a == c;
    }

    // So sánh 2 tay bài — trả về 1 nếu a > b, -1 nếu a < b, 0 nếu bằng
    private int handPriority(Hand h) {
        List<Card> c = h.cards();
        if (isThung(c) && isSanh(c)) return 5;
        if (isBaCo(c))               return 4;
        if (isThung(c))              return 3;
        if (isSanh(c))               return 2;
        if (isDoi(c))                return 1;
        return 0;
    }

    public String determineResult(Hand player, Hand bot) {
        int pp = handPriority(player), bp = handPriority(bot);
        if (pp != bp) return pp > bp ? "WIN" : "LOSE";
        // cùng loại → so điểm
        if (player.score() > bot.score()) return "WIN";
        if (player.score() < bot.score()) return "LOSE";
        return "DRAW";
    }

    // ── Ảnh ──────────────────────────────────────────────────────────────
    public InputStream getTableImage(Hand playerHand, Hand botHand) throws Exception {
        return CardImageGenerator.drawBaicaoTable(
                playerHand.cards(), playerHand.score(),
                botHand.cards(),    botHand.score()
        );
    }
}