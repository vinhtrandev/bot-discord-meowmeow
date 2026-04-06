package com.vinhtran.dogbot.game;

import com.vinhtran.dogbot.util.CardImageGenerator;
import com.vinhtran.dogbot.game.BlackjackGame.Card; // Import class Card từ Blackjack
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BaicaoGame {

    // Record lưu trữ thông tin bộ bài
    public record Hand(List<Integer> cards, int score, String rank) {}

    private final Random random = new Random();

    public Hand dealHand() {
        List<Integer> cards = new ArrayList<>();
        // Lấy 3 lá ngẫu nhiên từ 1-10
        for (int i = 0; i < 3; i++) cards.add(random.nextInt(10) + 1);

        int score = cards.stream().mapToInt(Integer::intValue).sum() % 10;
        String rank = getRank(score);
        return new Hand(cards, score, rank);
    }

    private String getRank(int score) {
        if (score == 9) return "Cào 9 🔥";
        if (score == 0) return "Bù (0)";
        return "Điểm " + score;
    }

    public String determineResult(Hand player, Hand bot) {
        if (player.score() > bot.score()) return "WIN";
        if (player.score() < bot.score()) return "LOSE";
        return "DRAW";
    }

    /**
     * Hàm vẽ ảnh: Sử dụng trực tiếp drawTable của CardImageGenerator
     */
    public InputStream getTableImage(Hand pHand, Hand bHand) throws Exception {
        // Convert List<Integer> sang List<Card> để Generator hiểu được
        // Chúng ta mặc định Suit = 0 (Bích) vì bài cào cũ của bạn không chia chất
        List<Card> pCards = pHand.cards().stream()
                .map(val -> new Card(val, 0))
                .toList();
        List<Card> bCards = bHand.cards().stream()
                .map(val -> new Card(val, 0))
                .toList();

        // Gọi hàm drawTable (Tham số cuối là false vì bài cào không giấu bài Bot)
        return CardImageGenerator.drawTable(
                pCards, pHand.score(),
                bCards, bHand.score(),
                false
        );
    }
}