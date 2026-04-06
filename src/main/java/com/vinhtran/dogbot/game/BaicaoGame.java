package com.vinhtran.dogbot.game;

import com.vinhtran.dogbot.util.CardImageGenerator;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BaicaoGame {

    // Giữ nguyên record cũ dùng List<Integer>
    public record Hand(List<Integer> cards, int score, String rank) {}

    private final Random random = new Random();

    public Hand dealHand() {
        List<Integer> cards = new ArrayList<>();
        // Chỉ lấy số từ 1-10 như cũ
        for (int i = 0; i < 3; i++) cards.add(random.nextInt(10) + 1);

        int score = cards.stream().mapToInt(Integer::intValue).sum() % 10;
        String rank = getRank(score);
        return new Hand(cards, score, rank);
    }

    private String getRank(int score) {
        return switch (score) {
            case 9 -> "Cào 9 🔥";
            case 8 -> "Cào 8";
            case 0 -> "Bù (0)";
            default -> "Điểm " + score;
        };
    }

    public String determineResult(Hand player, Hand bot) {
        if (player.score() > bot.score()) return "WIN";
        if (player.score() < bot.score()) return "LOSE";
        return "DRAW";
    }

    /**
     * Hàm vẽ ảnh cho bài cào (phiên bản dùng Card mặc định)
     */
    public InputStream getTableImage(Hand pHand, Hand bHand) throws Exception {
        // Vì CardImageGenerator yêu cầu List<Card>, ta convert nhanh từ Integer sang Card
        List<com.vinhtran.dogbot.game.BlackjackGame.Card> pCards = pHand.cards().stream()
                .map(rank -> new com.vinhtran.dogbot.game.BlackjackGame.Card(rank, 0)) // Mặc định chất bích (0)
                .toList();
        List<com.vinhtran.dogbot.game.BlackjackGame.Card> bCards = bHand.cards().stream()
                .map(rank -> new com.vinhtran.dogbot.game.BlackjackGame.Card(rank, 0))
                .toList();

        // Gọi hàm vẽ ảnh (Nếu CardImageGenerator của bạn nhận String pRank thì truyền pHand.rank())
        return CardImageGenerator.drawBaicaoTableSimple(pCards, pHand.score(), bCards, bHand.score());
    }
}