package com.vinhtran.dogbot.util;

import com.vinhtran.dogbot.game.BlackjackGame;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

public class CardImageGenerator {

    // --- Kích thước tối ưu giúp ảnh gọn và đẹp ---
    private static final int CARD_W  = 72;
    private static final int CARD_H  = 100;
    private static final int RADIUS  = 8;
    private static final int GAP     = 6;
    private static final int PAD     = 10;
    private static final int LABEL_H = 18;
    private static final int SCORE_H = 24;
    private static final int SECTION = 8;

    private static final Color BG          = new Color(22, 24, 32);
    private static final Color CARD_BG     = new Color(255, 252, 240);
    private static final Color CARD_BORDER = new Color(180, 155, 85);
    private static final Color BACK_BASE   = new Color(20, 55, 120);
    private static final Color SCORE_CLR   = new Color(255, 210, 50);
    private static final Color LABEL_CLR   = new Color(160, 160, 180);
    private static final Color RED_SUIT    = new Color(220, 30, 30);
    private static final Color BLACK_SUIT  = new Color(20, 20, 20);

    private static final String[] SUIT_SYM  = {"♠", "♥", "♦", "♣"};
    private static final String[] RANK_NAME = {"", "A","2","3","4","5","6","7","8","9","10","J","Q","K"};

    // =====================================================================
    // VẼ BÀI CÀO (Cố định 3 lá mỗi bên)
    // =====================================================================
    public static InputStream drawBaicaoTable(
            List<com.vinhtran.dogbot.game.BaicaoGame.Card> pHand, String pRank,
            List<com.vinhtran.dogbot.game.BaicaoGame.Card> dHand, String dRank) throws Exception {

        int totalW = PAD * 2 + (3 * CARD_W + 2 * GAP);
        int totalH = PAD + LABEL_H + CARD_H + SCORE_H + SECTION + LABEL_H + CARD_H + SCORE_H + PAD;

        BufferedImage img = new BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setHints(g);

        g.setColor(BG);
        g.fillRoundRect(0, 0, totalW, totalH, 15, 15);

        int y = PAD;
        drawLabel(g, "BÀI CỦA BẠN", y, totalW);
        y += LABEL_H;
        drawBaicaoRow(g, pHand, y, totalW);
        y += CARD_H;
        drawScoreLine(g, pRank, y, totalW);

        y += SCORE_H + SECTION;

        drawLabel(g, "BOT", y, totalW);
        y += LABEL_H;
        drawBaicaoRow(g, dHand, y, totalW);
        y += CARD_H;
        drawScoreLine(g, dRank, y, totalW);

        g.dispose();
        return toStream(img);
    }

    // =====================================================================
    // VẼ BLACKJACK (Số lá bài thay đổi động)
    // =====================================================================
    public static InputStream drawTable(
            List<BlackjackGame.Card> pHand, String pRank,
            List<BlackjackGame.Card> dHand, String dRank,
            boolean hideDealerLast) throws Exception {

        int maxCards = Math.max(pHand.size(), dHand.size());
        int innerW = maxCards * CARD_W + (maxCards - 1) * GAP;
        int totalW = PAD * 2 + Math.max(innerW, 150); // Tối thiểu 150px cho đẹp
        int totalH = PAD + LABEL_H + CARD_H + SCORE_H + SECTION + LABEL_H + CARD_H + SCORE_H + PAD;

        BufferedImage img = new BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setHints(g);

        g.setColor(BG);
        g.fillRoundRect(0, 0, totalW, totalH, 15, 15);

        int y = PAD;
        drawLabel(g, "BÀI CỦA BẠN", y, totalW);
        y += LABEL_H;
        drawBlackjackRow(g, pHand, y, totalW, false);
        y += CARD_H;
        drawScoreLine(g, pRank, y, totalW);

        y += SCORE_H + SECTION;

        drawLabel(g, "BOT", y, totalW);
        y += LABEL_H;
        drawBlackjackRow(g, dHand, y, totalW, hideDealerLast);
        y += CARD_H;
        drawScoreLine(g, hideDealerLast ? "? điểm" : dRank, y, totalW);

        g.dispose();
        return toStream(img);
    }

    // --- Helper Vẽ Hàng Bài ---
    private static void drawBaicaoRow(Graphics2D g, List<com.vinhtran.dogbot.game.BaicaoGame.Card> hand, int y, int totalW) {
        int offX = (totalW - (hand.size() * CARD_W + (hand.size() - 1) * GAP)) / 2;
        for (int i = 0; i < hand.size(); i++) {
            drawFront(g, offX + i * (CARD_W + GAP), y, hand.get(i).rank(), hand.get(i).suitIndex());
        }
    }

    private static void drawBlackjackRow(Graphics2D g, List<BlackjackGame.Card> hand, int y, int totalW, boolean hideLast) {
        int n = hand.size();
        int offX = (totalW - (n * CARD_W + (n - 1) * GAP)) / 2;
        for (int i = 0; i < n; i++) {
            int x = offX + i * (CARD_W + GAP);
            if (hideLast && i == n - 1) drawBack(g, x, y);
            else drawFront(g, x, y, hand.get(i).rank(), hand.get(i).suitIndex());
        }
    }

    // --- Helper Vẽ Lá Bài ---
    private static void drawFront(Graphics2D g, int x, int y, int rankIdx, int suitIdx) {
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRoundRect(x + 2, y + 2, CARD_W, CARD_H, RADIUS, RADIUS);
        g.setColor(CARD_BG);
        g.fillRoundRect(x, y, CARD_W, CARD_H, RADIUS, RADIUS);
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(x, y, CARD_W, CARD_H, RADIUS, RADIUS);

        boolean red = (suitIdx == 1 || suitIdx == 2);
        g.setColor(red ? RED_SUIT : BLACK_SUIT);

        String rank = RANK_NAME[rankIdx];
        String suit = SUIT_SYM[suitIdx];

        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString(rank, x + 5, y + 16);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString(suit, x + 5, y + 26);

        g.setFont(new Font("Arial", Font.PLAIN, 32));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(suit, x + (CARD_W - fm.stringWidth(suit)) / 2, y + CARD_H / 2 + 10);
    }

    private static void drawBack(Graphics2D g, int x, int y) {
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRoundRect(x + 2, y + 2, CARD_W, CARD_H, RADIUS, RADIUS);
        g.setColor(BACK_BASE);
        g.fillRoundRect(x, y, CARD_W, CARD_H, RADIUS, RADIUS);
        g.setColor(CARD_BORDER);
        g.drawRoundRect(x, y, CARD_W, CARD_H, RADIUS, RADIUS);

        g.setColor(new Color(255, 255, 255, 180));
        g.setFont(new Font("Arial", Font.BOLD, 25));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("?", x + (CARD_W - fm.stringWidth("?")) / 2, y + CARD_H / 2 + 10);
    }

    // --- Helper Text ---
    private static void drawLabel(Graphics2D g, String text, int y, int totalW) {
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(LABEL_CLR);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (totalW - fm.stringWidth(text)) / 2, y + fm.getAscent());
    }

    private static void drawScoreLine(Graphics2D g, String text, int y, int totalW) {
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(SCORE_CLR);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (totalW - fm.stringWidth(text)) / 2, y + fm.getAscent());
    }

    private static void setHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static InputStream toStream(BufferedImage img) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", b);
        return new ByteArrayInputStream(b.toByteArray());
    }
}