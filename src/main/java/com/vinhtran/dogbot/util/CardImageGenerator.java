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
 * Vẽ ảnh bài Xì Dách bằng Java2D.
 *
 * Giải pháp 1 ảnh duy nhất:
 *   drawTable(playerHand, playerScore, dealerHand, dealerScore, hideDealerLast)
 *   → ghép cả 2 bộ bài (player + dealer) vào 1 ảnh PNG
 *   → Discord embed chỉ cần 1 setImage("attachment://table.png")
 */
public class CardImageGenerator {

    // ── Kích thước lá bài ────────────────────────────────────────────────
    private static final int CARD_W   = 90;
    private static final int CARD_H   = 126;
    private static final int RADIUS   = 10;
    private static final int GAP      = 10;   // khoảng cách giữa các lá (không chồng)
    private static final int PAD      = 16;
    private static final int LABEL_H  = 28;   // chiều cao label "Bài của bạn" / "Bot"
    private static final int SCORE_H  = 30;   // chiều cao dòng điểm
    private static final int SECTION  = 12;   // khoảng cách giữa 2 bộ bài

    // ── Màu ──────────────────────────────────────────────────────────────
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
    // PUBLIC API
    // =====================================================================

    /**
     * Vẽ toàn bộ bàn chơi (player + dealer) thành 1 PNG duy nhất.
     *
     * @param pHand         bài người chơi
     * @param pScore        điểm người chơi
     * @param dHand         bài dealer
     * @param dScore        điểm dealer
     * @param hideDealerLast  true = lá cuối của dealer bị úp
     */
    public static InputStream drawTable(List<Card> pHand, int pScore,
                                        List<Card> dHand, int dScore,
                                        boolean hideDealerLast) throws Exception {
        int pW = rowWidth(pHand.size());
        int dW = rowWidth(dHand.size());
        int totalW = PAD + Math.max(pW, dW) + PAD;
        int totalH = PAD
                + LABEL_H + CARD_H + SCORE_H   // player row
                + SECTION
                + LABEL_H + CARD_H + SCORE_H   // dealer row
                + PAD;

        BufferedImage img = new BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    g   = img.createGraphics();
        setHints(g);

        // Nền bo góc
        g.setColor(BG);
        g.fillRoundRect(0, 0, totalW, totalH, 18, 18);

        int y = PAD;

        // ── Bộ bài người chơi ──
        drawLabel(g, "🙋 Bài của bạn", PAD, y, totalW);
        y += LABEL_H;
        drawRow(g, pHand, PAD, y, totalW, false);
        y += CARD_H;
        drawScoreLine(g, pScore + " điểm", PAD, y, totalW);
        y += SCORE_H + SECTION;

        // ── Bộ bài dealer ──
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
    // INTERNAL — vẽ 1 hàng bài
    // =====================================================================
    private static void drawRow(Graphics2D g, List<Card> hand,
                                int startX, int y, int totalW, boolean hideLast) {
        int n     = hand.size();
        int rowW  = rowWidth(n);
        int offX  = startX + (totalW - 2 * startX - rowW) / 2; // căn giữa

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
        // Nền
        g.setColor(CARD_BG);
        g.fill(shape(x, y, CARD_W, CARD_H));

        // Viền
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(shape(x + 0.8f, y + 0.8f, CARD_W - 1.6f, CARD_H - 1.6f));

        boolean red   = card.suitIndex() == 1 || card.suitIndex() == 2;
        Color   color = red ? RED_SUIT : BLACK_SUIT;
        String  rank  = RANK_NAME[card.rank()];
        String  suit  = SUIT_SYM[card.suitIndex()];
        boolean is10  = rank.equals("10");

        // ── Góc trên trái ────────────────────────────────────────────
        g.setColor(color);
        g.setFont(new Font("SansSerif", Font.BOLD, is10 ? 15 : 18));
        g.drawString(rank, x + 6, y + 20);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString(suit, x + (is10 ? 4 : 7), y + 35);

        // ── Suit lớn giữa ────────────────────────────────────────────
        g.setFont(new Font("SansSerif", Font.PLAIN, 42));
        FontMetrics fm = g.getFontMetrics();
        int sw = fm.stringWidth(suit);
        int sh = fm.getAscent();
        g.drawString(suit,
                x + (CARD_W - sw) / 2,
                y + (CARD_H + sh) / 2 - 6);

        // ── Góc dưới phải (xoay 180°) ────────────────────────────────
        Graphics2D r = (Graphics2D) g.create();
        r.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        r.rotate(Math.PI, x + CARD_W / 2.0, y + CARD_H / 2.0);
        r.setColor(color);
        r.setFont(new Font("SansSerif", Font.BOLD, is10 ? 15 : 18));
        r.drawString(rank, x + 6, y + 20);
        r.setFont(new Font("SansSerif", Font.PLAIN, 14));
        r.drawString(suit, x + (is10 ? 4 : 7), y + 35);
        r.dispose();
    }

    // =====================================================================
    // VẼ LÁ BÀI MẶT SAU
    // =====================================================================
    private static void drawBack(Graphics2D g, int x, int y) {
        // Nền xanh
        g.setColor(BACK_BASE);
        g.fill(shape(x, y, CARD_W, CARD_H));

        // Viền vàng
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(shape(x + 0.8f, y + 0.8f, CARD_W - 1.6f, CARD_H - 1.6f));

        // Họa tiết chéo (clip trong viền)
        Shape clip = shape(x + 3, y + 3, CARD_W - 6, CARD_H - 6);
        g.setClip(clip);
        g.setColor(BACK_LINE);
        g.setStroke(new BasicStroke(1f));
        for (int d = -(CARD_H + CARD_W); d < CARD_W + CARD_H; d += 9) {
            g.drawLine(x + d, y, x + d + CARD_H, y + CARD_H);
            g.drawLine(x + d + CARD_H, y, x + d, y + CARD_H);
        }
        g.setClip(null);

        // Khung viền trong
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(1f));
        g.draw(shape(x + 7, y + 7, CARD_W - 14, CARD_H - 14));

        // Dấu ?
        g.setColor(new Color(255, 255, 255, 210));
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("?",
                x + (CARD_W - fm.stringWidth("?")) / 2,
                y + (CARD_H + fm.getAscent()) / 2 - 4);
    }

    // =====================================================================
    // LABEL & SCORE
    // =====================================================================
    private static void drawLabel(Graphics2D g, String text, int x, int y, int totalW) {
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(LABEL_CLR);
        FontMetrics fm = g.getFontMetrics();
        // Canh trái với padding
        g.drawString(text, x, y + fm.getAscent());
    }

    private static void drawScoreLine(Graphics2D g, String text, int x, int y, int totalW) {
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(SCORE_CLR);
        FontMetrics fm = g.getFontMetrics();
        // Canh giữa
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
    }

    private static InputStream toStream(BufferedImage img) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", b);
        return new ByteArrayInputStream(b.toByteArray());
    }
}