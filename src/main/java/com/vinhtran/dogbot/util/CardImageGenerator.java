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

    private static final int CARD_W    = 46;
    private static final int CARD_H    = 64;
    private static final int RADIUS    = 7;
    private static final int GAP       = 5;
    private static final int PAD_X     = 20;
    private static final int PAD_Y     = 20;
    private static final int ROW_GAP   = 14;
    private static final int LABEL_H   = 20;
    private static final int MAX_CARDS = 6;

    private static final Color BG           = new Color(24,  26,  32);
    private static final Color CARD_FACE    = new Color(255, 252, 235);
    private static final Color CARD_BORDER  = new Color(180, 150,  85);
    private static final Color BACK_BASE    = new Color(22,  55, 120);
    private static final Color BACK_STRIPE  = new Color(14,  37,  85);
    private static final Color SCORE_CLR    = new Color(255, 215,  55);
    private static final Color LABEL_CLR    = new Color(210, 210, 220);
    private static final Color RED_CLR      = new Color(210,  30,  30);
    private static final Color BLACK_CLR    = new Color(15,   15,  15);
    private static final Color RESULT_TITLE = new Color(190, 190, 200);
    private static final Color WIN_CLR      = new Color( 80, 220, 110);
    private static final Color LOSE_CLR     = new Color(220,  70,  70);
    private static final Color DRAW_CLR     = new Color(220, 200,  60);
    private static final Color DIVIDER_CLR  = new Color( 60,  63,  72);
    private static final Color WAITING_CLR  = new Color(150, 150, 160);
    private static final Color TURN_CLR     = new Color(255, 180,  50);

    private static final String[] SUIT_SYM  = {"\u2660", "\u2665", "\u2666", "\u2663"};
    private static final String[] RANK_NAME = {
            "", "A","2","3","4","5","6","7","8","9","10","J","Q","K"
    };

    // =====================================================================
    // PUBLIC API — BLACKJACK / BÀI CÀO THƯỜNG
    // =====================================================================

    public static InputStream drawTable(List<Card> pHand, int pScore,
                                        List<Card> dHand, int dScore,
                                        boolean hideDealerLast,
                                        String playerLabel) throws Exception {
        return drawTable(pHand, pScore, dHand, dScore, hideDealerLast, playerLabel, null);
    }

    public static InputStream drawTable(List<Card> pHand, int pScore,
                                        List<Card> dHand, int dScore,
                                        boolean hideDealerLast,
                                        String playerLabel,
                                        String[] resultLines) throws Exception {

        String label = (playerLabel == null || playerLabel.isBlank()) ? "Bạn" : playerLabel;

        int cardAreaW = MAX_CARDS * CARD_W + (MAX_CARDS - 1) * GAP;
        int scoreW    = 80;
        int totalW    = PAD_X + cardAreaW + scoreW + PAD_X;

        int oneRowH  = LABEL_H + CARD_H;
        int resultH  = (resultLines != null && resultLines.length > 0)
                ? (10 + 26 * resultLines.length + 6) : 0;
        int totalH   = PAD_Y
                + oneRowH + ROW_GAP + oneRowH
                + (resultLines != null ? 14 : 0)
                + resultH
                + PAD_Y;

        BufferedImage img = new BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    g   = img.createGraphics();
        setHints(g);

        g.setColor(BG);
        g.fillRoundRect(0, 0, totalW, totalH, 14, 14);

        int y = PAD_Y;

        drawBaiRow(g, label + ":", pHand, pScore + " điểm", false,
                PAD_X, y, cardAreaW, scoreW);
        y += oneRowH + ROW_GAP;

        String dScoreStr = hideDealerLast ? "?" : dScore + " điểm";
        drawBaiRow(g, "Bot:", dHand, dScoreStr, hideDealerLast,
                PAD_X, y, cardAreaW, scoreW);
        y += oneRowH;

        if (resultLines != null && resultLines.length > 0) {
            y += 10;
            g.setColor(DIVIDER_CLR);
            g.setStroke(new BasicStroke(1f));
            g.drawLine(PAD_X, y, totalW - PAD_X, y);
            y += 8;

            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(RESULT_TITLE);
            g.drawString("Kết quả:", PAD_X, y + g.getFontMetrics().getAscent());
            y += 22;

            for (int i = 0; i < resultLines.length; i++) {
                String line = resultLines[i];
                g.setFont(new Font("SansSerif", Font.BOLD, i == 0 ? 16 : 15));
                if (line.startsWith("+"))                                   g.setColor(WIN_CLR);
                else if (line.startsWith("-"))                              g.setColor(LOSE_CLR);
                else if (line.contains("thắng") || line.contains("Thắng")) g.setColor(WIN_CLR);
                else if (line.contains("thua")  || line.contains("Thua"))  g.setColor(LOSE_CLR);
                else if (line.contains("Hòa")   || line.contains("hòa"))   g.setColor(DRAW_CLR);
                else                                                        g.setColor(SCORE_CLR);
                g.drawString(line, PAD_X, y + g.getFontMetrics().getAscent());
                y += 26;
            }
        }

        g.dispose();
        return toStream(img);
    }

    // =====================================================================
    // PUBLIC API — SOLO XÌ DÁCH
    // =====================================================================
    public static InputStream drawSolo(
            String topLabel,    List<Card> topHand,    int topScore,    boolean hideTopAll,
            String bottomLabel, List<Card> bottomHand, int bottomScore, boolean hideBottomAll,
            boolean topTurn,
            String statusLine) throws Exception {

        int cardAreaW = MAX_CARDS * CARD_W + (MAX_CARDS - 1) * GAP;
        int scoreW    = 80;
        int totalW    = PAD_X + cardAreaW + scoreW + PAD_X;

        // Tách statusLine thành nhiều dòng nếu chứa " | "
        String[] statusLines = (statusLine != null)
                ? statusLine.split(" \\| ")
                : new String[0];
        int statusH = statusLines.length > 0 ? (14 + 22 * statusLines.length + 6) : 0;

        int oneRowH = LABEL_H + CARD_H;
        int totalH  = PAD_Y + oneRowH + ROW_GAP + oneRowH + statusH + PAD_Y;

        BufferedImage img = new BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    g   = img.createGraphics();
        setHints(g);

        g.setColor(BG);
        g.fillRoundRect(0, 0, totalW, totalH, 14, 14);

        int y = PAD_Y;

        // ── Hàng trên ────────────────────────────────────────────────────
        String topScoreStr = hideTopAll ? "?" : topScore + " điểm";
        String topLabelStr = (topTurn ? "\u25b6 " : "") + topLabel + ":";
        drawSoloRow(g, topLabelStr, topHand, topScoreStr, hideTopAll, topTurn,
                PAD_X, y, cardAreaW, scoreW);
        y += oneRowH + ROW_GAP;

        // ── Hàng dưới ────────────────────────────────────────────────────
        String botScoreStr = hideBottomAll ? "?" : bottomScore + " điểm";
        String botLabelStr = (!topTurn ? "\u25b6 " : "") + bottomLabel + ":";
        drawSoloRow(g, botLabelStr, bottomHand, botScoreStr, hideBottomAll, !topTurn,
                PAD_X, y, cardAreaW, scoreW);
        y += oneRowH;

        // ── Dòng trạng thái (hỗ trợ nhiều dòng, tách bởi " | ") ──────────
        if (statusLines.length > 0) {
            y += 8;
            g.setColor(DIVIDER_CLR);
            g.setStroke(new BasicStroke(1f));
            g.drawLine(PAD_X, y, totalW - PAD_X, y);
            y += 8;

            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();

            for (String line : statusLines) {
                String trimmed = line.trim();
                Color sc;
                if (trimmed.contains("thắng") || trimmed.contains("Thắng")) sc = WIN_CLR;
                else if (trimmed.contains("thua") || trimmed.contains("Thua")) sc = LOSE_CLR;
                else if (trimmed.contains("Hòa") || trimmed.contains("hòa"))   sc = DRAW_CLR;
                else if (trimmed.contains("Quắc"))                              sc = LOSE_CLR;
                else                                                            sc = TURN_CLR;
                g.setColor(sc);
                g.drawString(trimmed, PAD_X, y + fm.getAscent());
                y += 22;
            }
        }

        g.dispose();
        return toStream(img);
    }

    // =====================================================================
    // VẼ 1 ROW SOLO — có highlight lượt hiện tại
    // =====================================================================
    private static void drawSoloRow(Graphics2D g,
                                    String label,
                                    List<Card> hand,
                                    String scoreText,
                                    boolean hideAll,
                                    boolean isCurrentTurn,
                                    int startX, int y,
                                    int cardAreaW, int scoreW) {
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(isCurrentTurn ? TURN_CLR : LABEL_CLR);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, startX, y + fm.getAscent());

        int cardY = y + LABEL_H;

        for (int i = 0; i < hand.size(); i++) {
            int cx = startX + i * (CARD_W + GAP);
            if (hideAll) drawBack(g, cx, cardY);
            else         drawFront(g, cx, cardY, hand.get(i));
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(hideAll ? WAITING_CLR : SCORE_CLR);
        FontMetrics sfm = g.getFontMetrics();
        String bracket = "[" + scoreText + "]";
        int scoreX = startX + cardAreaW + 8;
        int scoreY = cardY + (CARD_H + sfm.getAscent() - sfm.getDescent()) / 2 - sfm.getDescent();
        g.drawString(bracket, scoreX, scoreY);
    }

    // =====================================================================
    // VẼ 1 ROW THƯỜNG (blackjack / bài cào)
    // =====================================================================
    private static void drawBaiRow(Graphics2D g,
                                   String label,
                                   List<Card> hand,
                                   String scoreText,
                                   boolean hideLast,
                                   int startX, int y,
                                   int cardAreaW, int scoreW) {
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(LABEL_CLR);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, startX, y + fm.getAscent());

        int cardY = y + LABEL_H;

        for (int i = 0; i < hand.size(); i++) {
            int cx = startX + i * (CARD_W + GAP);
            if (hideLast && i == hand.size() - 1) drawBack(g, cx, cardY);
            else                                   drawFront(g, cx, cardY, hand.get(i));
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(SCORE_CLR);
        FontMetrics sfm = g.getFontMetrics();
        String bracket = "[" + scoreText + "]";
        int scoreX = startX + cardAreaW + 8;
        int scoreY = cardY + (CARD_H + sfm.getAscent() - sfm.getDescent()) / 2 - sfm.getDescent();
        g.drawString(bracket, scoreX, scoreY);
    }

    // =====================================================================
    // VẼ LÁ BÀI MẶT TRƯỚC
    // =====================================================================
    private static void drawFront(Graphics2D g, int x, int y, Card card) {
        g.setColor(CARD_FACE);
        g.fill(shape(x, y, CARD_W, CARD_H));

        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(shape(x + 0.8f, y + 0.8f, CARD_W - 1.6f, CARD_H - 1.6f));

        boolean red   = card.suitIndex() == 1 || card.suitIndex() == 2;
        Color   color = red ? RED_CLR : BLACK_CLR;
        String  rank  = RANK_NAME[card.rank()];
        String  suit  = SUIT_SYM[card.suitIndex()];
        boolean is10  = rank.equals("10");

        g.setColor(color);
        g.setFont(new Font("SansSerif", Font.BOLD, is10 ? 11 : 13));
        g.drawString(rank, x + 4, y + 14);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.drawString(suit, x + (is10 ? 3 : 5), y + 26);

        g.setFont(new Font("SansSerif", Font.PLAIN, 28));
        FontMetrics fm = g.getFontMetrics();
        int sw = fm.stringWidth(suit);
        g.drawString(suit, x + (CARD_W - sw) / 2, y + (CARD_H + fm.getAscent()) / 2 - 4);

        Graphics2D r = (Graphics2D) g.create();
        r.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        r.rotate(Math.PI, x + CARD_W / 2.0, y + CARD_H / 2.0);
        r.setColor(color);
        r.setFont(new Font("SansSerif", Font.BOLD, is10 ? 11 : 13));
        r.drawString(rank, x + 4, y + 14);
        r.setFont(new Font("SansSerif", Font.PLAIN, 10));
        r.drawString(suit, x + (is10 ? 3 : 5), y + 26);
        r.dispose();
    }

    // =====================================================================
    // VẼ LÁ BÀI MẶT SAU
    // =====================================================================
    private static void drawBack(Graphics2D g, int x, int y) {
        g.setColor(BACK_BASE);
        g.fill(shape(x, y, CARD_W, CARD_H));
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(shape(x + 0.8f, y + 0.8f, CARD_W - 1.6f, CARD_H - 1.6f));

        Shape clip = shape(x + 3, y + 3, CARD_W - 6, CARD_H - 6);
        g.setClip(clip);
        g.setColor(BACK_STRIPE);
        g.setStroke(new BasicStroke(1f));
        for (int d = -(CARD_H + CARD_W); d < CARD_W + CARD_H; d += 8) {
            g.drawLine(x + d, y, x + d + CARD_H, y + CARD_H);
            g.drawLine(x + d + CARD_H, y, x + d, y + CARD_H);
        }
        g.setClip(null);

        g.setColor(new Color(255, 255, 255, 200));
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("?", x + (CARD_W - fm.stringWidth("?")) / 2,
                y + (CARD_H + fm.getAscent()) / 2 - 4);
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
    }

    private static InputStream toStream(BufferedImage img) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", b);
        return new ByteArrayInputStream(b.toByteArray());
    }
}