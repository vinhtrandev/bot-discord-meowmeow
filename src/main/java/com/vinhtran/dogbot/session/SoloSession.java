package com.vinhtran.dogbot.session;

import com.vinhtran.dogbot.game.BaicaoGame;
import com.vinhtran.dogbot.game.BlackjackGame;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SoloSession {

    public enum State { WAITING, CHOOSING_GAME, PLAYING_BAICAO, PLAYING_BLACKJACK }

    private String challengerId;
    private String challengerName;
    private String targetId;
    private String targetName;
    private long bet;

    private State state = State.WAITING;

    // ID của message public (để edit sau khi hit/stand)
    private String publicMessageId;

    // Bài Cào
    private BaicaoGame baicaoGame;
    private BaicaoGame.Hand challengerHand;
    private BaicaoGame.Hand targetHand;

    // Blackjack
    private BlackjackGame challengerBj;
    private BlackjackGame targetBj;
    private boolean challengerDone = false;
    private boolean targetDone = false;

    // Constructor dùng trong SoloCommand
    public SoloSession(String challengerId, String challengerName,
                       String targetId, String targetName, long bet) {
        this.challengerId   = challengerId;
        this.challengerName = challengerName;
        this.targetId       = targetId;
        this.targetName     = targetName;
        this.bet            = bet;
    }
}