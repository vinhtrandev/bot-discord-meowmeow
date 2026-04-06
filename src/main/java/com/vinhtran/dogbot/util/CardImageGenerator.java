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

public class CardImageGenerator {

    // ── Kích thước lá bài (nhỏ lại) ──────────────────────────────────────
    private static final int CARD_W  = 80;
    private static final int CARD_H  = 112;
    private static final int RADIUS  = 10;
    private static final int GAP     = 10;
    private static final int PAD     = 14;
    private static final int LABEL_H = 24;
    private static final int SCORE_H = 28;
    private static final int SECTION = 12;

    // ── Màu ──────────────────────────────────────────────────────────────
    private static final Color BG          = new Color(22,  24,  32);
    private static final Color CARD_BG     = new Color(255, 252, 240);
    private static final Color CARD_BORDER = new Color(180, 155,  85);
    private static final Color BACK_BASE   = new Color(20,  55, 120);
    private static final Color BACK_LINE   = new Color(12,  35,  80);
    private static final Color SCORE_CLR   = new Color(255, 210,  50);
    private static final Color LABEL_CLR   = new Color(195, 195, 210);
    private static final Color RED_SUIT    = new Color(205,  25,  25);
    private static final Color BLACK_SUIT  = new Color(12,   12,  12);

    private static final String[] SUIT_SYM  = {"\u2660", "\u2665", "\u2666", "\u2663"};
    private static final String[] RANK_NAME = {
            "", "A","2","3","4","5","6","7","8","9","10","J","Q","K"
    };

    // =====================================================================
    public static InputStream drawTable(List<Card> pHand, int pScore,
                                        List<Card> dHand, int dScore,
                                        boolean hideDealerLast) throws Exception {
        int innerW = Math.max(rowWidth(pHand.size()), rowWidth(dHand.size()));
        int totalW = PAD * 2 + innerW;
        int totalH = PAD
                + LABEL_H + CARD_H + SCORE_H
                + SECTION
                + LABEL_H + CARD_H + SCORE_H
                + PAD;

        BufferedImage img = new BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    g   = img.createGraphics();
        setHints(g);

        g.setColor(BG);
        g.fillRoundRect(0, 0, totalW, totalH, 18, 18);

        int y = PAD;

        drawLabel(g, "Bai cua ban", PAD, y, totalW);
        y += LABEL_H;
        drawRow(g, pHand, y, totalW, false);
        y += CARD_H;
        drawScoreLine(g, pScore + " diem", y, totalW);
        y += SCORE_H + SECTION;

        drawLabel(g, "Bot", PAD, y, totalW);
        y += LABEL_H;
        drawRow(g, dHand, y, totalW, hideDealerLast);
        y += CARD_H;
        drawScoreLine(g, hideDealerLast ? "? diem" : dScore + " diem", y, totalW);

        g.dispose();
        return toStream(img);
    }

    // =====================================================================
    private static void drawRow(Graphics2D g, List<Card> hand,
                                int y, int totalW, boolean hideLast) {
        int n    = hand.size();
        int offX = (totalW - rowWidth(n)) / 2;
        for (int i = 0; i < n; i++) {
            int x = offX + i * (CARD_W + GAP);
            if (hideLast && i == n - 1) drawBack(g, x, y);
            else                        drawFront(g, x, y, hand.get(i));
        }
    }

    private static int rowWidth(int n) { return n * CARD_W + (n - 1) * GAP; }

    // =====================================================================
    private static void drawFront(Graphics2D g, int x, int y, Card card) {
        // Bóng
        g.setColor(new Color(0, 0, 0, 55));
        g.fill(shape(x + 2, y + 3, CARD_W, CARD_H));

        g.setColor(CARD_BG);
        g.fill(shape(x, y, CARD_W, CARD_H));
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(1.8f));
        g.draw(shape(x + 1, y + 1, CARD_W - 2, CARD_H - 2));

        boolean red   = card.suitIndex() == 1 || card.suitIndex() == 2;
        Color   color = red ? RED_SUIT : BLACK_SUIT;
        String  rank  = RANK_NAME[card.rank()];
        String  suit  = SUIT_SYM[card.suitIndex()];
        boolean is10  = rank.equals("10");

        // Góc trên trái
        g.setColor(color);
        g.setFont(new Font("Arial", Font.BOLD, is10 ? 13 : 15));
        g.drawString(rank, x + 5, y + 16);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(suit, x + (is10 ? 3 : 6), y + 29);

        // Suit lớn giữa
        g.setFont(new Font("Arial", Font.PLAIN, 38));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(suit,
                x + (CARD_W - fm.stringWidth(suit)) / 2,
                y + (CARD_H + fm.getAscent()) / 2 - 6);

        // Góc dưới phải (xoay 180°)
        Graphics2D r = (Graphics2D) g.create();
        r.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        r.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        r.rotate(Math.PI, x + CARD_W / 2.0, y + CARD_H / 2.0);
        r.setColor(color);
        r.setFont(new Font("Arial", Font.BOLD, is10 ? 13 : 15));
        r.drawString(rank, x + 5, y + 16);
        r.setFont(new Font("Arial", Font.BOLD, 12));
        r.drawString(suit, x + (is10 ? 3 : 6), y + 29);
        r.dispose();
    }

    // =====================================================================
    private static void drawBack(Graphics2D g, int x, int y) {
        g.setColor(new Color(0, 0, 0, 55));
        g.fill(shape(x + 2, y + 3, CARD_W, CARD_H));

        g.setColor(BACK_BASE);
        g.fill(shape(x, y, CARD_W, CARD_H));
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(1.8f));
        g.draw(shape(x + 1, y + 1, CARD_W - 2, CARD_H - 2));

        Shape clip = shape(x + 4, y + 4, CARD_W - 8, CARD_H - 8);
        g.setClip(clip);
        g.setColor(BACK_LINE);
        g.setStroke(new BasicStroke(1f));
        for (int d = -(CARD_H + CARD_W); d < CARD_W + CARD_H; d += 9) {
            g.drawLine(x + d, y, x + d + CARD_H, y + CARD_H);
            g.drawLine(x + d + CARD_H, y, x + d, y + CARD_H);
        }
        g.setClip(null);

        g.setColor(new Color(CARD_BORDER.getRed(), CARD_BORDER.getGreen(),
                CARD_BORDER.getBlue(), 150));
        g.setStroke(new BasicStroke(1f));
        g.draw(shape(x + 7, y + 7, CARD_W - 14, CARD_H - 14));

        g.setColor(new Color(255, 255, 255, 210));
        g.setFont(new Font("Arial", Font.BOLD, 30));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("?",
                x + (CARD_W - fm.stringWidth("?")) / 2,
                y + (CARD_H + fm.getAscent()) / 2 - 4);
    }

    // =====================================================================
    private static void drawLabel(Graphics2D g, String text, int x, int y, int totalW) {
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(LABEL_CLR);
        g.drawString(text, x, y + g.getFontMetrics().getAscent());
    }

    private static void drawScoreLine(Graphics2D g, String text, int y, int totalW) {
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(SCORE_CLR);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (totalW - fm.stringWidth(text)) / 2, y + fm.getAscent());
    }

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