package games.jaipurskeleton.stats;

import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameHeuristic;
import games.jaipurskeleton.JaipurGameState;
import games.jaipurskeleton.actions.TakeCards;
import games.jaipurskeleton.components.JaipurCard;
import utilities.Pair;

import java.util.List;

/**
 * Custom objective function for Jaipur that evaluates game quality using 3 game-specific features:
 * 1. Camel take frequency - how often players take camels from the market
 * 2. Average bonus tokens earned - measures bulk selling frequency
 * 3. Score closeness - how balanced the final scores are between players
 */
public class JaipurObjective implements IGameHeuristic {

    @Override
    public double evaluateGame(Game game) {
        JaipurGameState state = (JaipurGameState) game.getGameState();

        // Feature 1: Count how many TakeCards actions involved taking camels
        int camelTakeCount = 0;
        int totalActions = 0;
        List<Pair<Integer, AbstractAction>> history = state.getHistory();
        for (Pair<Integer, AbstractAction> entry : history) {
            if (entry.b instanceof TakeCards tc) {
                totalActions++;
                if (tc.howManyPerTypeTakeFromMarket.containsKey(JaipurCard.GoodType.Camel)) {
                    camelTakeCount++;
                }
            }
        }
        // Normalize: ratio of camel takes to total take actions (0 to 1)
        double camelTakeRate = totalActions > 0 ? (double) camelTakeCount / totalActions : 0;

        // Feature 2: Average bonus tokens earned across all players
        double totalBonusTokens = 0;
        for (int i = 0; i < state.getNPlayers(); i++) {
            totalBonusTokens += state.getPlayerNBonusTokens().get(i).getValue();
        }
        double avgBonusTokens = totalBonusTokens / state.getNPlayers();

        // Feature 3: Score closeness — closer scores = more balanced game
        double score0 = state.getGameScore(0);
        double score1 = state.getGameScore(1);
        double scoreCloseness = -Math.abs(score0 - score1);

        // Weighted combination (higher = better game quality)
        return camelTakeRate * 0.3 + avgBonusTokens * 0.3 + scoreCloseness * 0.4;
    }
}
