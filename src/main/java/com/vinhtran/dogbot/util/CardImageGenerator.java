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
 * Vẽ ảnh bài Xì Dách & Bài Cào bằng Java2D.
 * Kích thước 64x90 tối ưu cho hiển thị đa thiết bị.
 */
public class CardImageGenerator {

    // ── Kích thước tối ưu (Compact Size) ───────────────────────────────
    private static final int CARD_W   = 64;
    private static final int CARD_H   = 90;
    private static final int RADIUS   = 8;
    private static final int GAP      = 8;
    private static final int PAD      = 12;
    private static final int LABEL_H  = 22;
    private static final int SCORE_H  = 25;
    private static final int SECTION  = 10;

    // ── Màu sắc Palette ────────────────────────────────────────────────
    private static final Color BG          = new Color(28,  30,  36);
    private static final Color CARD_BG     = new Color(255, 252, 235);
    private static final Color CARD_BORDER = new Color(190, 165, 100);
    private static final Color BACK_BASE   = new Color(25,  60,  130);
    private static final Color BACK_LINE   = new Color(15,  40,   90);
    private static final Color SCORE_CLR   = new Color(255, 215,  60);
    private static final Color LABEL_CLR   = new Color(200, 200, 210);
    private static final Color RED_SUIT    = new Color(210,  30,  30);
    private static final Color BLACK_SUIT  = new Color(15,   15,  15);

    private static final String[] SUIT_SYM  = {"♠", "♥", "♦", "♣"};
    private static final String[] RANK_NAME = {
            "", "A","2","3","4","5","6","7","8","9","10","J","Q","K"
    };

    // =====================================================================
    // PUBLIC API - Vẽ bàn bài
    // =====================================================================
    public static InputStream drawTable(List<Card> pHand, int pScore,
                                        List<Card> dHand, int dScore,
                                        boolean hideDealerLast) throws Exception {
        int pW = rowWidth(pHand.size());
        int dW = rowWidth(dHand.size());
        int totalW = PAD + Math.max(pW, dW) + PAD;
        int totalH = PAD
                + LABEL_H + CARD_H + SCORE_H
                + SECTION
                + LABEL_H + CARD_H + SCORE_H
                + PAD;

        BufferedImage img = new BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    g   = img.createGraphics();
        setHints(g);

        // Nền bàn bo góc
        g.setColor(BG);
        g.fillRoundRect(0, 0, totalW, totalH, 15, 15);

        int y = PAD;

        // Vẽ hàng người chơi
        drawLabel(g, "🙋 Bài của bạn", PAD, y, totalW);
        y += LABEL_H;
        drawRow(g, pHand, PAD, y, totalW, false);
        y += CARD_H;
        drawScoreLine(g, pScore + " điểm", PAD, y, totalW);
        y += SCORE_H + SECTION;

        // Vẽ hàng Bot
        drawLabel(g, "🤖 Bot", PAD, y, totalW);
        y += LABEL_H;
        drawRow(g, dHand, PAD, y, totalW, hideDealerLast);
        y += CARD_H;
        String dScoreText = hideDealerLast ? "? điểm" : dScore + " điểm";
        drawScoreLine(g, dScoreText, PAD, y, totalW);

        g.dispose();
        return toStream(img);
    }

    // =====================================================================
    // VẼ MẶT TRƯỚC LÁ BÀI
    // =====================================================================
    private static void drawFront(Graphics2D g, int x, int y, Card card) {
        g.setColor(CARD_BG);
        g.fill(shape(x, y, CARD_W, CARD_H));

        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(shape(x + 0.6f, y + 0.6f, CARD_W - 1.2f, CARD_H - 1.2f));

        boolean red   = card.suitIndex() == 1 || card.suitIndex() == 2;
        Color   color = red ? RED_SUIT : BLACK_SUIT;
        String  rank  = RANK_NAME[card.rank()];
        String  suit  = SUIT_SYM[card.suitIndex()];
        boolean is10  = rank.equals("10");

        // Vẽ số và chất ở góc trên
        g.setColor(color);
        g.setFont(new Font("SansSerif", Font.BOLD, is10 ? 11 : 13));
        g.drawString(rank, x + 4, y + 15);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.drawString(suit, x + (is10 ? 3 : 5), y + 26);

        // Vẽ chất lớn ở giữa
        g.setFont(new Font("SansSerif", Font.PLAIN, 28));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(suit,
                x + (CARD_W - fm.stringWidth(suit)) / 2,
                y + (CARD_H + fm.getAscent()) / 2 - 4);

        // Vẽ góc dưới đối xứng (xoay 180)
        Graphics2D r = (Graphics2D) g.create();
        r.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        r.rotate(Math.PI, x + CARD_W / 2.0, y + CARD_H / 2.0);
        r.setColor(color);
        r.setFont(new Font("SansSerif", Font.BOLD, is10 ? 11 : 13));
        r.drawString(rank, x + 4, y + 15);
        r.setFont(new Font("SansSerif", Font.PLAIN, 10));
        r.drawString(suit, x + (is10 ? 3 : 5), y + 26);
        r.dispose();
    }

    // =====================================================================
    // VẼ MẶT SAU LÁ BÀI
    // =====================================================================
    private static void drawBack(Graphics2D g, int x, int y) {
        g.setColor(BACK_BASE);
        g.fill(shape(x, y, CARD_W, CARD_H));
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(shape(x + 0.6f, y + 0.6f, CARD_W - 1.2f, CARD_H - 1.2f));

        Shape clip = shape(x + 2, y + 2, CARD_W - 4, CARD_H - 4);
        g.setClip(clip);
        g.setColor(BACK_LINE);
        for (int d = -(CARD_H + CARD_W); d < CARD_W + CARD_H; d += 6) {
            g.drawLine(x + d, y, x + d + CARD_H, y + CARD_H);
            g.drawLine(x + d + CARD_H, y, x + d, y + CARD_H);
        }
        g.setClip(null);

        g.setColor(new Color(255, 255, 255, 180));
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("?", x + (CARD_W - fm.stringWidth("?")) / 2, y + (CARD_H + fm.getAscent()) / 2 - 2);
    }

    // =====================================================================
    // HÀM BỔ TRỢ (HELPERS)
    // =====================================================================
    private static void drawRow(Graphics2D g, List<Card> hand, int startX, int y, int totalW, boolean hideLast) {
        int n = hand.size();
        int rowW = rowWidth(n);
        int offX = startX + (totalW - 2 * startX - rowW) / 2;
        for (int i = 0; i < n; i++) {
            int x = offX + i * (CARD_W + GAP);
            if (hideLast && (i == n - 1)) drawBack(g, x, y);
            else drawFront(g, x, y, hand.get(i));
        }
    }

    private static int rowWidth(int n) {
        return n * CARD_W + (n - 1) * GAP;
    }

    private static void drawLabel(Graphics2D g, String text, int x, int y, int totalW) {
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(LABEL_CLR);
        g.drawString(text, x, y + g.getFontMetrics().getAscent());
    }

    private static void drawScoreLine(Graphics2D g, String text, int x, int y, int totalW) {
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(SCORE_CLR);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (totalW - fm.stringWidth(text)) / 2, y + fm.getAscent());
    }

    private static RoundRectangle2D.Float shape(float x, float y, float w, float h) {
        return new RoundRectangle2D.Float(x, y, w, h, RADIUS, RADIUS);
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