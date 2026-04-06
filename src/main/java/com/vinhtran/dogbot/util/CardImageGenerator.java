package com.vinhtran.dogbot.util;

import com.vinhtran.dogbot.game.BlackjackGame.Card;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Vẽ ảnh bài Xì Dách bằng Java2D — phong cách Meow Meow Bot.
 *
 * Giải pháp 1 ảnh duy nhất:
 *   drawTable(playerHand, playerScore, dealerHand, dealerScore, hideDealerLast)
 */
public class CardImageGenerator {

    // ── Kích thước lá bài ────────────────────────────────────────────────
    private static final int CARD_W  = 110;   // rộng hơn → bài to hơn
    private static final int CARD_H  = 154;   // tỉ lệ giữ nguyên
    private static final int RADIUS  = 12;
    private static final int GAP     = 14;    // khoảng cách giữa lá
    private static final int PAD     = 18;
    private static final int LABEL_H = 30;
    private static final int SCORE_H = 34;
    private static final int SECTION = 16;

    // ── Màu (dark theme giống ảnh 2) ─────────────────────────────────────
    private static final Color BG          = new Color(22,  24,  32);   // nền cực tối
    private static final Color CARD_BG     = new Color(255, 252, 240);   // kem nhạt
    private static final Color CARD_BORDER = new Color(180, 155,  85);   // viền vàng
    private static final Color BACK_BASE   = new Color(20,  55, 120);
    private static final Color BACK_LINE   = new Color(12,  35,  80);
    private static final Color SCORE_CLR   = new Color(255, 210,  50);   // vàng điểm
    private static final Color LABEL_CLR   = new Color(195, 195, 210);   // nhãn xám sáng
    private static final Color RED_SUIT    = new Color(205,  25,  25);
    private static final Color BLACK_SUIT  = new Color(12,   12,  12);

    private static final String[] SUIT_SYM  = {"\u2660", "\u2665", "\u2666", "\u2663"}; // ♠ ♥ ♦ ♣
    private static final String[] RANK_NAME = {
            "", "A","2","3","4","5","6","7","8","9","10","J","Q","K"
    };

    // =====================================================================
    // PUBLIC API
    // =====================================================================

    /**
     * Vẽ toàn bộ bàn chơi (player + dealer) thành 1 PNG duy nhất.
     *
     * @param pHand          bài người chơi
     * @param pScore         điểm người chơi
     * @param dHand          bài dealer
     * @param dScore         điểm dealer
     * @param hideDealerLast true = lá cuối của dealer bị úp
     */
    public static InputStream drawTable(List<Card> pHand, int pScore,
                                        List<Card> dHand, int dScore,
                                        boolean hideDealerLast) throws Exception {
        int pW     = rowWidth(pHand.size());
        int dW     = rowWidth(dHand.size());
        int innerW = Math.max(pW, dW);
        int totalW = PAD * 2 + innerW;
        int totalH = PAD
                + LABEL_H + CARD_H + SCORE_H
                + SECTION
                + LABEL_H + CARD_H + SCORE_H
                + PAD;

        BufferedImage img = new BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    g   = img.createGraphics();
        setHints(g);

        // ── Nền bo góc ──
        g.setColor(BG);
        g.fillRoundRect(0, 0, totalW, totalH, 20, 20);

        int y = PAD;

        // ── Bài người chơi ──
        drawLabel(g, "Bai cua ban", PAD, y, totalW);
        y += LABEL_H;
        drawRow(g, pHand, PAD, y, totalW, false);
        y += CARD_H;
        drawScoreLine(g, pScore + " diem", PAD, y, totalW);
        y += SCORE_H + SECTION;

        // ── Bài dealer ──
        drawLabel(g, "Bot", PAD, y, totalW);
        y += LABEL_H;
        drawRow(g, dHand, PAD, y, totalW, hideDealerLast);
        y += CARD_H;
        String dScoreText = hideDealerLast ? "? diem" : dScore + " diem";
        drawScoreLine(g, dScoreText, PAD, y, totalW);

        g.dispose();
        return toStream(img);
    }

    // =====================================================================
    // VẼ 1 HÀNG BÀI
    // =====================================================================
    private static void drawRow(Graphics2D g, List<Card> hand,
                                int startX, int y, int totalW, boolean hideLast) {
        int n    = hand.size();
        int rowW = rowWidth(n);
        int offX = (totalW - rowW) / 2; // căn giữa

        for (int i = 0; i < n; i++) {
            int x    = offX + i * (CARD_W + GAP);
            boolean back = hideLast && (i == n - 1);
            if (back) drawBack(g, x, y);
            else      drawFront(g, x, y, hand.get(i));
        }
    }

    private static int rowWidth(int n) {
        return n * CARD_W + (n - 1) * GAP;
    }

    // =====================================================================
    // VẼ LÁ BÀI MẶT TRƯỚC
    // =====================================================================
    private static void drawFront(Graphics2D g, int x, int y, Card card) {
        // Bóng đổ nhẹ
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(shape(x + 3, y + 4, CARD_W, CARD_H));

        // Nền lá bài
        g.setColor(CARD_BG);
        g.fill(shape(x, y, CARD_W, CARD_H));

        // Viền vàng
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(2f));
        g.draw(shape(x + 1, y + 1, CARD_W - 2, CARD_H - 2));

        boolean red   = card.suitIndex() == 1 || card.suitIndex() == 2;
        Color   color = red ? RED_SUIT : BLACK_SUIT;
        String  rank  = RANK_NAME[card.rank()];
        String  suit  = SUIT_SYM[card.suitIndex()];
        boolean is10  = rank.equals("10");

        // ── Góc trên trái: rank + suit nhỏ ───────────────────────────
        g.setColor(color);
        // Rank
        g.setFont(new Font("Arial", Font.BOLD, is10 ? 17 : 20));
        g.drawString(rank, x + 7, y + 22);
        // Suit nhỏ bên dưới rank
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString(suit, x + (is10 ? 5 : 9), y + 40);

        // ── Suit lớn ở giữa lá ───────────────────────────────────────
        g.setFont(new Font("Arial", Font.PLAIN, 54));
        FontMetrics fm = g.getFontMetrics();
        int sw = fm.stringWidth(suit);
        int sh = fm.getAscent();
        g.drawString(suit,
                x + (CARD_W - sw) / 2,
                y + (CARD_H + sh) / 2 - 8);

        // ── Góc dưới phải: xoay 180° ─────────────────────────────────
        Graphics2D r = (Graphics2D) g.create();
        r.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        r.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        r.rotate(Math.PI, x + CARD_W / 2.0, y + CARD_H / 2.0);
        r.setColor(color);
        r.setFont(new Font("Arial", Font.BOLD, is10 ? 17 : 20));
        r.drawString(rank, x + 7, y + 22);
        r.setFont(new Font("Arial", Font.BOLD, 16));
        r.drawString(suit, x + (is10 ? 5 : 9), y + 40);
        r.dispose();
    }

    // =====================================================================
    // VẼ LÁ BÀI MẶT SAU
    // =====================================================================
    private static void drawBack(Graphics2D g, int x, int y) {
        // Bóng đổ
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(shape(x + 3, y + 4, CARD_W, CARD_H));

        // Nền xanh đậm
        g.setColor(BACK_BASE);
        g.fill(shape(x, y, CARD_W, CARD_H));

        // Viền vàng
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(2f));
        g.draw(shape(x + 1, y + 1, CARD_W - 2, CARD_H - 2));

        // Họa tiết chéo (clip trong viền)
        Shape clip = shape(x + 4, y + 4, CARD_W - 8, CARD_H - 8);
        g.setClip(clip);
        g.setColor(BACK_LINE);
        g.setStroke(new BasicStroke(1.2f));
        for (int d = -(CARD_H + CARD_W); d < CARD_W + CARD_H; d += 10) {
            g.drawLine(x + d, y, x + d + CARD_H, y + CARD_H);
            g.drawLine(x + d + CARD_H, y, x + d, y + CARD_H);
        }
        g.setClip(null);

        // Khung viền trong
        g.setColor(new Color(CARD_BORDER.getRed(), CARD_BORDER.getGreen(),
                CARD_BORDER.getBlue(), 160));
        g.setStroke(new BasicStroke(1f));
        g.draw(shape(x + 9, y + 9, CARD_W - 18, CARD_H - 18));

        // Dấu ?
        g.setColor(new Color(255, 255, 255, 220));
        g.setFont(new Font("Arial", Font.BOLD, 42));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("?",
                x + (CARD_W - fm.stringWidth("?")) / 2,
                y + (CARD_H + fm.getAscent()) / 2 - 4);
    }

    // =====================================================================
    // LABEL & SCORE
    // =====================================================================
    private static void drawLabel(Graphics2D g, String text, int x, int y, int totalW) {
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(LABEL_CLR);
        g.drawString(text, x, y + g.getFontMetrics().getAscent());
    }

    private static void drawScoreLine(Graphics2D g, String text, int x, int y, int totalW) {
        g.setFont(new Font("Arial", Font.BOLD, 19));
        g.setColor(SCORE_CLR);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (totalW - fm.stringWidth(text)) / 2, y + fm.getAscent());
    }

    // =====================================================================
    // HELPER
    // =====================================================================
    private static RoundRectangle2D.Float shape(float x, float y, float w, float h) {
        return new RoundRectangle2D.Float(x, y, w, h, RADIUS, RADIUS);
    }

    private static void setHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);
    }

    private static InputStream toStream(BufferedImage img) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", b);
        return new ByteArrayInputStream(b.toByteArray());
    }
}