package com.vinhtran.dogbot.game;

import com.vinhtran.dogbot.util.CardImageGenerator;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BaicaoGame {

    // Dùng chung cấu trúc Card để CardImageGenerator có thể vẽ được
    public record Card(int rank, int suitIndex) {}
    public record Hand(List<Card> cards, int score, String rank) {}

    private final Random random = new Random();

    public Hand dealHand() {
        List<Card> cards = new ArrayList<>();
        int totalScore = 0;

        for (int i = 0; i < 3; i++) {
            int rank = random.nextInt(13) + 1; // 1-13
            int suit = random.nextInt(4);      // 0-3
            cards.add(new Card(rank, suit));

            // Tính điểm Bài Cào (J, Q, K tính là 10 hoặc 0 tùy luật, ở đây tính sum % 10)
            totalScore += Math.min(rank, 10);
        }

        int finalScore = totalScore % 10;
        return new Hand(cards, finalScore, getRankLabel(finalScore, cards));
    }

    private String getRankLabel(int score, List<Card> cards) {
        // Kiểm tra Ba Tây (3 lá J, Q, K)
        boolean isBaTay = cards.stream().allMatch(c -> c.rank() > 10);
        if (isBaTay) return "Ba Tây 👑";

        return switch (score) {
            case 9  -> "Cào 9 🔥";
            case 0  -> "Bù (0)";
            default -> "Điểm " + score;
        };
    }

    public String determineResult(Hand player, Hand bot) {
        if (player.score() > bot.score()) return "WIN";
        if (player.score() < bot.score()) return "LOSE";
        return "DRAW";
    }

    // Hàm này sẽ gọi CardImageGenerator đã sửa
    public InputStream getTableImage(Hand pHand, Hand bHand) throws Exception {
        return CardImageGenerator.drawBaicaoTable(
                pHand.cards(), pHand.rank(), //
                bHand.cards(), bHand.rank()  //
        );
    }
}