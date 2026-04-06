package com.vinhtran.dogbot.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BaicaoGame {

    public record Hand(List<Integer> cards, int score, String rank) {}

    private final Random random = new Random();

    public Hand dealHand() {
        List<Integer> cards = new ArrayList<>();
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
}