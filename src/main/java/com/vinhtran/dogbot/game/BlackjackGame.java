package com.vinhtran.dogbot.game;

import com.vinhtran.dogbot.util.CardImageGenerator;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class BlackjackGame {

    public record Card(int rank, int suitIndex) {}

    private static final String[] SUIT_SYMBOLS = {"\u2660", "\u2665", "\u2666", "\u2663"};

    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> dealerHand = new ArrayList<>();
    private final Random     random     = new Random();

    public BlackjackGame() {
        playerHand.add(drawCard());
        playerHand.add(drawCard());
        dealerHand.add(drawCard());
        dealerHand.add(drawCard());
    }

    // =====================================================================
    // RÚT BÀI
    // =====================================================================
    public Card drawCard() {
        return new Card(random.nextInt(13) + 1, random.nextInt(4));
    }

    public void playerHit() { playerHand.add(drawCard()); }

    // =====================================================================
    // TÍNH ĐIỂM
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

    public int     getPlayerScore()    { return calcScore(playerHand); }
    public int     getDealerScore()    { return calcScore(dealerHand); }
    public boolean playerBust()        { return getPlayerScore() > 21; }
    public boolean dealerBust()        { return getDealerScore() > 21; }
    public int     getPlayerHandSize() { return playerHand.size(); }
    public int     getDealerHandSize() { return dealerHand.size(); }

    /** Trả về bản sao danh sách bài của player */
    public List<Card> getPlayerHand() { return Collections.unmodifiableList(playerHand); }

    // =====================================================================
    // CÁC BỘ ĐẶC BIỆT
    // =====================================================================
    public boolean isXiBang(List<Card> hand) {
        return hand.size() == 2 && hand.get(0).rank() == 1 && hand.get(1).rank() == 1;
    }

    public boolean isXiDach(List<Card> hand) {
        if (hand.size() != 2) return false;
        int a = hand.get(0).rank(), b = hand.get(1).rank();
        return (a == 1 && b >= 10) || (b == 1 && a >= 10);
    }

    public boolean isNguLinh(List<Card> hand) {
        return hand.size() >= 5 && calcScore(hand) <= 21;
    }

    public boolean isPlayerXiBang()  { return isXiBang(playerHand); }
    public boolean isDealerXiBang()  { return isXiBang(dealerHand); }
    public boolean isPlayerXiDach()  { return isXiDach(playerHand); }
    public boolean isDealerXiDach()  { return isXiDach(dealerHand); }
    public boolean isPlayerNguLinh() { return isNguLinh(playerHand); }
    public boolean isDealerNguLinh() { return isNguLinh(dealerHand); }

    public boolean canPlayerStand() {
        return getPlayerScore() >= 16 || isPlayerNguLinh();
    }

    // =====================================================================
    // DEALER TỰ CHƠI
    // =====================================================================
    public void dealerPlay() {
        while (getDealerScore() < 15 && !isDealerNguLinh()) {
            dealerHand.add(drawCard());
        }
    }

    // =====================================================================
    // XÁC ĐỊNH KẾT QUẢ (BLACKJACK THƯỜNG vs BOT)
    // =====================================================================
    public GameResult determineResultAfterDealer() {
        dealerPlay();
        return determineResult();
    }

    public GameResult determineResult() {
        int pScore = getPlayerScore();
        int dScore = getDealerScore();

        if (pScore < 16 && !isPlayerNguLinh()) return GameResult.DAN_NON;

        if (isPlayerXiBang() && isDealerXiBang()) return GameResult.DRAW;
        if (isPlayerXiBang()) return GameResult.XI_BANG_WIN;
        if (isDealerXiBang()) return GameResult.LOSE;

        if (isPlayerXiDach() && isDealerXiDach()) return GameResult.DRAW;
        if (isPlayerXiDach()) return GameResult.XI_DACH_WIN;
        if (isDealerXiDach()) return GameResult.LOSE;

        if (isPlayerNguLinh() && isDealerNguLinh()) {
            if (pScore < dScore) return GameResult.NGU_LINH_WIN;
            if (pScore > dScore) return GameResult.LOSE;
            return GameResult.DRAW;
        }
        if (isPlayerNguLinh()) return GameResult.NGU_LINH_WIN;
        if (isDealerNguLinh()) return GameResult.LOSE;

        if (playerBust() && dealerBust()) return GameResult.LOSE;
        if (playerBust())  return GameResult.LOSE;
        if (dealerBust())  return GameResult.WIN;

        if (pScore > dScore) return GameResult.WIN;
        if (pScore < dScore) return GameResult.LOSE;
        return GameResult.DRAW;
    }

    // =====================================================================
    // SO BÀI SOLO 1V1 — Xì Bàn > Xì Dách > Ngũ Linh > Điểm > Bust
    // Trả về: 1 = gameA thắng, -1 = gameB thắng, 0 = Hòa
    // =====================================================================
    public static int compareSolo(BlackjackGame gameA, BlackjackGame gameB) {
        boolean aBang  = gameA.isPlayerXiBang(),  bBang  = gameB.isPlayerXiBang();
        boolean aDach  = gameA.isPlayerXiDach(),  bDach  = gameB.isPlayerXiDach();
        boolean aLinh  = gameA.isPlayerNguLinh(), bLinh  = gameB.isPlayerNguLinh();
        boolean aBust  = gameA.playerBust(),      bBust  = gameB.playerBust();
        int     aScore = gameA.getPlayerScore(),  bScore = gameB.getPlayerScore();

        // 1. Xì Bàn (2 con Át)
        if (aBang && bBang) return 0;
        if (aBang) return 1;
        if (bBang) return -1;

        // 2. Xì Dách (A + 10/J/Q/K)
        if (aDach && bDach) return 0;
        if (aDach) return 1;
        if (bDach) return -1;

        // 3. Ngũ Linh (5 lá không quắc) — ít điểm hơn thắng theo luật VN
        if (aLinh && bLinh) {
            if (aScore < bScore) return 1;
            if (aScore > bScore) return -1;
            return 0;
        }
        if (aLinh) return 1;
        if (bLinh) return -1;

        // 4. Cả 2 quắc → hòa
        if (aBust && bBust) return 0;

        // 5. 1 người quắc
        if (aBust) return -1;
        if (bBust) return 1;

        // 6. So điểm thường
        if (aScore > bScore) return 1;
        if (aScore < bScore) return -1;
        return 0;
    }

    // =====================================================================
    // MÔ TẢ KẾT QUẢ BÀI — dùng cho dòng resultLine
    // =====================================================================
    public String getSoloResultDesc() {
        if (isPlayerXiBang())  return "Xì Bàn 🃏🃏";
        if (isPlayerXiDach())  return "Xì Dách 🃏";
        if (isPlayerNguLinh()) return "Ngũ Linh 🖐️ (" + getPlayerScore() + " điểm)";
        if (playerBust())      return "Quắc 💥";
        return getPlayerScore() + " điểm";
    }

    // =====================================================================
    // ẢNH — BLACKJACK THƯỜNG
    // =====================================================================
    public InputStream getTableImagePlaying(String username) throws Exception {
        return CardImageGenerator.drawTable(
                playerHand, getPlayerScore(),
                dealerHand, getDealerScore(),
                true,
                username
        );
    }

    public InputStream getTableImageFinal(String username) throws Exception {
        return CardImageGenerator.drawTable(
                playerHand, getPlayerScore(),
                dealerHand, getDealerScore(),
                false,
                username
        );
    }

    // =====================================================================
    // ẢNH — SOLO XÌ DÁCH
    // =====================================================================

    /**
     * [PUBLIC] Cả 2 bài đều úp — đăng công khai trong channel khi đang chơi.
     */
    public static InputStream getSoloImagePublic(
            String challengerName, BlackjackGame challengerGame,
            String targetName,     BlackjackGame targetGame,
            boolean challengerTurn,
            String statusLine) throws Exception {
        return CardImageGenerator.drawSolo(
                challengerName, challengerGame.getPlayerHand(), challengerGame.getPlayerScore(), true,
                targetName,     targetGame.getPlayerHand(),     targetGame.getPlayerScore(),     true,
                challengerTurn,
                statusLine
        );
    }

    /**
     * [EPHEMERAL - CHALLENGER] Challenger thấy bài MÌNH (hàng dưới), target bị úp.
     */
    public static InputStream getSoloImageForChallenger(
            String challengerName, BlackjackGame challengerGame,
            String targetName,     BlackjackGame targetGame) throws Exception {
        String scoreNote = challengerGame.playerBust()
                ? "\uD83D\uDCA5 Quắc!"
                : "Bài của bạn: " + challengerGame.getPlayerScore() + " điểm";
        return CardImageGenerator.drawSolo(
                targetName,     targetGame.getPlayerHand(),     targetGame.getPlayerScore(),     true,
                challengerName, challengerGame.getPlayerHand(), challengerGame.getPlayerScore(), false,
                false,
                scoreNote
        );
    }

    /**
     * [EPHEMERAL - TARGET] Target thấy bài MÌNH (hàng dưới), challenger bị úp.
     */
    public static InputStream getSoloImageForTarget(
            String challengerName, BlackjackGame challengerGame,
            String targetName,     BlackjackGame targetGame) throws Exception {
        String scoreNote = targetGame.playerBust()
                ? "\uD83D\uDCA5 Quắc!"
                : "Bài của bạn: " + targetGame.getPlayerScore() + " điểm";
        return CardImageGenerator.drawSolo(
                challengerName, challengerGame.getPlayerHand(), challengerGame.getPlayerScore(), true,
                targetName,     targetGame.getPlayerHand(),     targetGame.getPlayerScore(),     false,
                false,
                scoreNote
        );
    }

    /**
     * [PUBLIC - KẾT QUẢ] Lật hết bài cả 2, dùng khi ván đã kết thúc.
     */
    public static InputStream getSoloImageFinal(
            String challengerName, BlackjackGame challengerGame,
            String targetName,     BlackjackGame targetGame,
            String resultMsg) throws Exception {
        return CardImageGenerator.drawSolo(
                challengerName, challengerGame.getPlayerHand(), challengerGame.getPlayerScore(), false,
                targetName,     targetGame.getPlayerHand(),     targetGame.getPlayerScore(),     false,
                false,
                resultMsg
        );
    }

    // =====================================================================
    // TEXT FALLBACK
    // =====================================================================
    private String cardLabel(Card c) {
        String rank = switch (c.rank()) {
            case 1  -> "A";
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            default -> String.valueOf(c.rank());
        };
        return rank + SUIT_SYMBOLS[c.suitIndex()];
    }

    public String getPlayerHandStr() {
        return playerHand.stream().map(this::cardLabel).collect(Collectors.joining("  "))
                + "  `[" + getPlayerScore() + "]`";
    }

    public String getDealerHandStr() {
        return dealerHand.stream().map(this::cardLabel).collect(Collectors.joining("  "))
                + "  `[" + getDealerScore() + "]`";
    }

    public String getDealerHandHidden() {
        Card first = dealerHand.get(0);
        int  val   = (first.rank() == 1) ? 11 : Math.min(first.rank(), 10);
        return cardLabel(first) + "  \u2753  `[" + val + ", ?]`";
    }
}