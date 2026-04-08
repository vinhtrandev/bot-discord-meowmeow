package com.vinhtran.dogbot.game;

import com.vinhtran.dogbot.util.CardImageGenerator;
import com.vinhtran.dogbot.game.BlackjackGame.Card;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BaicaoGame {

    public record Hand(List<Integer> cards, int score, String rank, boolean isSap,
                       boolean isLieng, boolean isBaTay) {}

    private final Random random = new Random();

    // =====================================================================
    // CHIA BÀI — 3 lá (rank 1-13)
    // =====================================================================
    public Hand dealHand() {
        List<Integer> cards = new ArrayList<>();
        for (int i = 0; i < 3; i++) cards.add(random.nextInt(13) + 1);
        return buildHand(cards);
    }

    private Hand buildHand(List<Integer> cards) {
        boolean isSap   = checkSap(cards);
        boolean isLieng = checkLieng(cards);
        boolean isBaTay = checkBaTay(cards);
        int score = calcScore(cards);
        String rank = getRank(score, isSap, isLieng, isBaTay);
        return new Hand(cards, score, rank, isSap, isLieng, isBaTay);
    }

    // =====================================================================
    // TÍNH ĐIỂM
    // A=1, 2-10=mặt bài, J/Q/K=0
    // =====================================================================
    public int calcScore(List<Integer> cards) {
        int total = 0;
        for (int c : cards) {
            if (c >= 11) total += 0;   // J, Q, K = 0 (Tiên)
            else         total += c;   // A=1, 2-10
        }
        return total % 10;
    }

    // =====================================================================
    // CÁC BỘ ĐẶC BIỆT
    // =====================================================================

    /** Sáp: 3 lá cùng rank */
    private boolean checkSap(List<Integer> cards) {
        return cards.get(0).equals(cards.get(1)) && cards.get(1).equals(cards.get(2));
    }

    /** Liêng: 3 lá liên tiếp (A-2-3 đến J-Q-K) */
    private boolean checkLieng(List<Integer> cards) {
        List<Integer> sorted = new ArrayList<>(cards);
        sorted.sort(Integer::compareTo);
        return sorted.get(1) == sorted.get(0) + 1 && sorted.get(2) == sorted.get(1) + 1;
    }

    /** Ba Tây: 3 lá toàn J, Q, K */
    private boolean checkBaTay(List<Integer> cards) {
        return cards.stream().allMatch(c -> c >= 11);
    }

    // =====================================================================
    // TÊN BỘ BÀI
    // =====================================================================
    private String getRank(int score, boolean isSap, boolean isLieng, boolean isBaTay) {
        if (isSap)   return "Sáp 🀄 (x5)";
        if (isLieng) return "Liêng 🔗 (x3)";
        if (isBaTay) return "Ba Tây 👑 (x2)";
        if (score == 9) return "9 nút 🔥";
        if (score == 0) return "Bù (0)";
        return score + " nút";
    }

    // =====================================================================
    // XÁC ĐỊNH KẾT QUẢ
    // Thứ tự ưu tiên: Sáp > Liêng > Ba Tây > điểm thường
    // =====================================================================
    public String determineResult(Hand player, Hand bot) {
        int pRank = specialRank(player);
        int bRank = specialRank(bot);

        if (pRank > bRank) return "WIN";
        if (pRank < bRank) return "LOSE";

        // Cùng loại bộ đặc biệt hoặc cùng điểm thường
        if (player.score() > bot.score()) return "WIN";
        if (player.score() < bot.score()) return "LOSE";
        return "DRAW";
    }

    /** Sáp=3, Liêng=2, Ba Tây=1, thường=0 */
    private int specialRank(Hand hand) {
        if (hand.isSap())   return 3;
        if (hand.isLieng()) return 2;
        if (hand.isBaTay()) return 1;
        return 0;
    }

    // =====================================================================
    // TÍNH TIỀN THẮNG
    // =====================================================================
    public long calcPrize(Hand winner, long bet) {
        if (winner.isSap())   return bet * 5;
        if (winner.isLieng()) return bet * 3;
        if (winner.isBaTay()) return bet * 2;
        return bet;
    }

    // =====================================================================
    // VẼ ẢNH — 2 lá ngửa, lá 3 úp (đang chọn MỞ/GẤP ĐÔI)
    // =====================================================================
    public InputStream getTableImageHidden(Hand pHand, Hand bHand, String username) throws Exception {
        List<Card> pCards = toCardList(pHand.cards());
        List<Card> bCards = toCardList(bHand.cards());

        // hideDealerLast=false vì bot cũng úp lá 3 — ta dùng hideLast cho cả player
        // Trick: truyền hideDealerLast=true để úp lá cuối của bot,
        // còn player ta tự cắt danh sách chỉ lấy 2 lá + 1 lá úp
        List<Card> pVisible = new ArrayList<>(pCards.subList(0, 2));
        pVisible.add(new Card(0, 0)); // placeholder — sẽ bị hideLast úp

        return CardImageGenerator.drawTable(
                pVisible, calcScore(pHand.cards().subList(0, 2)),
                bCards,   bHand.score(),
                true,     // hideDealerLast = úp lá cuối bot
                username
        );
    }

    /**
     * Vẽ ảnh kết quả — lật hết tất cả bài.
     */
    public InputStream getTableImageFinal(Hand pHand, Hand bHand, String username) throws Exception {
        List<Card> pCards = toCardList(pHand.cards());
        List<Card> bCards = toCardList(bHand.cards());

        return CardImageGenerator.drawTable(
                pCards, pHand.score(),
                bCards, bHand.score(),
                false,
                username
        );
    }

    // =====================================================================
    // HELPER
    // =====================================================================
    private List<Card> toCardList(List<Integer> cards) {
        Random rand = new Random();
        return cards.stream()
                .map(val -> new Card(val, rand.nextInt(4))) // suitIndex=0 (♠) vì bài cào không chia chất(Nhưng mà nên để như này cho đẹp)
                .toList();
    }
}