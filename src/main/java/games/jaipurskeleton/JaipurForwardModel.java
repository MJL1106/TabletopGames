package games.jaipurskeleton;

import com.google.common.collect.ImmutableMap;
import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import games.jaipurskeleton.actions.SellCards;
import games.jaipurskeleton.actions.TakeCards;
import games.jaipurskeleton.components.JaipurCard;
import games.jaipurskeleton.components.JaipurToken;

import java.util.*;

import static core.CoreConstants.GameResult.*;
import static games.jaipurskeleton.components.JaipurCard.GoodType.*;

/**
 * Jaipur rules: <a href="https://www.fgbradleys.com/rules/rules2/Jaipur-rules.pdf">pdf here</a>
 */
public class JaipurForwardModel extends StandardForwardModel {

    /**
     * Initializes all variables in the given game state. Performs initial game setup according to game rules, e.g.:
     * <ul>
     *     <li>Sets up decks of cards and shuffles them</li>
     *     <li>Gives player cards</li>
     *     <li>Places tokens on boards</li>
     *     <li>...</li>
     * </ul>
     *
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        JaipurGameState gs = (JaipurGameState) firstState;
        JaipurParameters jp = (JaipurParameters) firstState.getGameParameters();

        // Initialize variables
        gs.market = new HashMap<>();
        for (JaipurCard.GoodType gt: JaipurCard.GoodType.values()) {
            // 5 cards in the market
            gs.market.put(gt, new Counter(0, 0, 5, "Market: " + gt));
        }

        gs.drawDeck = new Deck<>("Draw deck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        gs.playerHands = new ArrayList<>();
        gs.playerHerds = new ArrayList<>();
        gs.cardAcquisitionTurns = new ArrayList<>();
        gs.nGoodTokensSold = new Counter(0, 0, JaipurCard.GoodType.values().length, "N Good Tokens Fully Sold");
        gs.goodTokens = new HashMap<>();
        gs.bonusTokens = new HashMap<>();

        // Initialize player scores, rounds won trackers, and other player-specific variables
        gs.playerScores = new ArrayList<>();
        gs.playerNRoundsWon = new ArrayList<>();
        gs.playerNGoodTokens = new ArrayList<>();
        gs.playerNBonusTokens = new ArrayList<>();
        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.playerScores.add(new Counter(0, 0, Integer.MAX_VALUE, "Player " + i + " score"));
            gs.playerNRoundsWon.add(new Counter(0, 0, Integer.MAX_VALUE, "Player " + i + " n rounds won"));
            gs.playerNGoodTokens.add(new Counter(0, 0, Integer.MAX_VALUE, "Player " + i + " n good tokens"));
            gs.playerNBonusTokens.add(new Counter(0, 0, Integer.MAX_VALUE, "Player " + i + " n bonus tokens"));

            // Create herds, maximum camels in the game
            gs.playerHerds.add(new Counter(0, 0, jp.getNCardsPerType().get(JaipurCard.GoodType.Camel), "Player " + i + " herd"));

            Map<JaipurCard.GoodType, Counter> playerHand = new HashMap<>();
            Map<JaipurCard.GoodType, List<Integer>> playerAcqTurns = new HashMap<>();
            for (JaipurCard.GoodType gt: JaipurCard.GoodType.values()) {
                if (gt != JaipurCard.GoodType.Camel) {
                    // Hand limit
                    playerHand.put(gt, new Counter(0, 0, jp.getHandLimit(), "Player " + i + " hand: " + gt));
                    playerAcqTurns.put(gt, new ArrayList<>());
                }
            }
            gs.playerHands.add(playerHand);
            gs.cardAcquisitionTurns.add(playerAcqTurns);
        }

        // Set up the first round
        setupRound(gs, jp);
    }

    private void setupRound(JaipurGameState gs, JaipurParameters jp) {
        // Market initialisation
        // Place camel cards in the market
        for (JaipurCard.GoodType gt: JaipurCard.GoodType.values()) {
            if (gt == JaipurCard.GoodType.Camel) {
                gs.market.get(gt).setValue(jp.getNInitialCamelsInMarket());
            } else {
                gs.market.get(gt).setValue(0);
            }
        }

        // Create deck of cards
        gs.drawDeck.clear();
        for (JaipurCard.GoodType gt : jp.getNCardsPerType().keySet()) {
            int nCards = jp.getNCardsPerType().get(gt);
            // Camels already placed in the market are not in the deck
            if (gt == JaipurCard.GoodType.Camel) {
                nCards -= jp.getNInitialCamelsInMarket();
            }
            for (int i = 0; i < nCards; i++) {
                gs.drawDeck.add(new JaipurCard(gt));
            }
        }
        gs.drawDeck.shuffle(gs.getRnd());

        // Deal N cards to each player
        for (int i = 0; i < gs.getNPlayers(); i++) {
            Map<JaipurCard.GoodType, Counter> playerHand = gs.playerHands.get(i);

            // First, reset
            gs.playerHerds.get(i).setValue(0);
            for (JaipurCard.GoodType gt: JaipurCard.GoodType.values()) {
                if (gt != JaipurCard.GoodType.Camel) {
                    playerHand.get(gt).setValue(0);
                    gs.cardAcquisitionTurns.get(i).get(gt).clear();
                }
            }

            // Deal cards
            for (int j = 0; j < jp.getNCardsDealPerPlayer(); j++) {
                JaipurCard card = gs.drawDeck.draw();

                // If camel, it goes into the herd instead
                if (card.goodType == JaipurCard.GoodType.Camel) {
                    gs.playerHerds.get(i).increment();
                } else {
                    // Otherwise, into the player's hand
                    playerHand.get(card.goodType).increment();
                    gs.cardAcquisitionTurns.get(i).get(card.goodType).add(0);
                }
            }
        }

        // Take cards from the deck and place them face up in the market.
        for (int i = 0; i < jp.getNInitialMarketCardsFromDeck(); i++) {
            JaipurCard card = gs.drawDeck.draw();
            gs.market.get(card.goodType).increment();
        }

        // Initialize tokens
        gs.nGoodTokensSold.setValue(0);
        gs.goodTokens.clear();
        gs.bonusTokens.clear();

        // Initialize the good tokens from parameters
        for (JaipurCard.GoodType gt : jp.getGoodTokensProgression().keySet()) {
            Deck<JaipurToken> tokenDeck = new Deck<>("Good tokens " + gt, CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
            for (int value : jp.getGoodTokensProgression().get(gt)) {
                tokenDeck.add(new JaipurToken(gt, value));
            }
            gs.goodTokens.put(gt, tokenDeck);
        }

        // Initialize the bonus tokens
        for (int nSold: jp.bonusTokensAvailable.keySet()) {
            Integer[] values = jp.bonusTokensAvailable.get(nSold);
            Deck<JaipurToken> tokenDeck = new Deck<>("Bonus tokens " + nSold, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
            for (int v: values) {
                tokenDeck.add(new JaipurToken(v));
            }
            // Shuffle
            tokenDeck.shuffle(gs.getRnd());
            gs.bonusTokens.put(nSold, tokenDeck);
        }

        // Reset player-specific variables that don't persist between rounds
        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.playerScores.get(i).setValue(0);
            gs.playerNGoodTokens.get(i).setValue(0);
            gs.playerNBonusTokens.get(i).setValue(0);
        }

        // First player
        gs.setFirstPlayer(0);
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        JaipurGameState jgs = (JaipurGameState) gameState;
        JaipurParameters jp = (JaipurParameters) gameState.getGameParameters();
        int currentPlayer = gameState.getCurrentPlayer();
        Map<JaipurCard.GoodType, Counter> playerHand = jgs.playerHands.get(currentPlayer);

        // Can sell cards from hand
        for (JaipurCard.GoodType gt : playerHand.keySet()) {
            if (playerHand.get(gt).getValue() >= jp.goodNCardsMinimumSell.get(gt)) {
                // Can sell this good type! We can choose any number of cards to sell of this type between minimum and how many we have
                for (int n = jp.goodNCardsMinimumSell.get(gt); n <= playerHand.get(gt).getValue(); n++) {
                    actions.add(new SellCards(gt, n));
                }
            }
        }

        // Can take cards from the market, respecting hand limit
        // Option C: Take all camels, they don't count towards hand limit
        // TODO 1: Check how many camel cards are in the market. If more than 0, construct one TakeCards action object and add it to the `actions` ArrayList. (The `howManyPerTypeGiveFromHand` argument should be null)
        int nCamelsInMarket = jgs.getMarket().get(JaipurCard.GoodType.Camel).getValue();
        if (nCamelsInMarket > 0) {
            actions.add(new TakeCards(ImmutableMap.of(JaipurCard.GoodType.Camel, nCamelsInMarket), null, currentPlayer));
        }

        int nCardsInHand = 0;
        for (JaipurCard.GoodType gt: playerHand.keySet()) {
            nCardsInHand += playerHand.get(gt).getValue();
        }

        // Check hand limit for taking non-camel cards in hand
        if (nCardsInHand < jp.getHandLimit()) {
            // Option B: Take a single (non-camel) card from the market
            // TODO 2: For each good type in the market, if there is at least 1 of that type (which is not a Camel), construct one TakeCards action object to take 1 of that type from the market, and add it to the `actions` ArrayList. (The `howManyPerTypeGiveFromHand` argument should be null)
            for (JaipurCard.GoodType gt : jgs.getMarket().keySet()) {
                if (gt != JaipurCard.GoodType.Camel && jgs.getMarket().get(gt).getValue() > 0) {
                    actions.add(new TakeCards(ImmutableMap.of(gt, 1), null, currentPlayer));
                }
            }
        }

        // Option A: Take several (non-camel) cards and replenish with cards of different types from hand (or with camels)
        // TODO (Advanced, bonus, optional): Calculate legal option A variations

        // Build list of non-camel types available in the market
        List<JaipurCard.GoodType> marketTypes = new ArrayList<>();
        for (JaipurCard.GoodType gt : jgs.getMarket().keySet()) {
            if (gt != JaipurCard.GoodType.Camel && jgs.getMarket().get(gt).getValue() > 0) {
                marketTypes.add(gt);
            }
        }

        // Build list of types available to give back (hand cards + camels)
        List<JaipurCard.GoodType> giveTypes = new ArrayList<>();
        for (JaipurCard.GoodType gt : playerHand.keySet()) {
            if (playerHand.get(gt).getValue() > 0) {
                giveTypes.add(gt);
            }
        }
        int nCamels = jgs.playerHerds.get(currentPlayer).getValue();

        // Generate all "take" combinations of 2+ non-camel cards from the market
        List<Map<JaipurCard.GoodType, Integer>> takeCombinations = new ArrayList<>();
        generateCombinations(marketTypes, jgs.getMarket(), 0, new HashMap<>(), 0, takeCombinations, 5);

        // For each take combination, generate all valid give combinations of the same size
        for (Map<JaipurCard.GoodType, Integer> take : takeCombinations) {
            int nToGive = take.values().stream().mapToInt(Integer::intValue).sum();
            if (nToGive < 2) continue;

            // Build available give counts, excluding types we're taking (can't swap same type)
            Map<JaipurCard.GoodType, Integer> availableGive = new HashMap<>();
            for (JaipurCard.GoodType gt : playerHand.keySet()) {
                if (!take.containsKey(gt) && playerHand.get(gt).getValue() > 0) {
                    availableGive.put(gt, playerHand.get(gt).getValue());
                }
            }
            // Camels can always be given back
            if (nCamels > 0) {
                availableGive.put(JaipurCard.GoodType.Camel, nCamels);
            }

            List<JaipurCard.GoodType> giveTypeList = new ArrayList<>(availableGive.keySet());
            List<Map<JaipurCard.GoodType, Integer>> giveCombinations = new ArrayList<>();
            generateCombinationsExact(giveTypeList, availableGive, 0, new HashMap<>(), 0, nToGive, giveCombinations);

            for (Map<JaipurCard.GoodType, Integer> give : giveCombinations) {
                actions.add(new TakeCards(ImmutableMap.copyOf(take), ImmutableMap.copyOf(give), currentPlayer));
            }
        }

        return actions;
    }

    /**
     * Generates all combinations of cards to take from the market (2+ cards, non-camel).
     * Each combination is a map of GoodType -> count.
     */
    private void generateCombinations(List<JaipurCard.GoodType> types, Map<JaipurCard.GoodType, Counter> available,
                                      int index, Map<JaipurCard.GoodType, Integer> current, int totalSoFar,
                                      List<Map<JaipurCard.GoodType, Integer>> results, int maxTotal) {
        if (totalSoFar >= 2) {
            results.add(new HashMap<>(current));
        }
        if (totalSoFar >= maxTotal || index >= types.size()) return;

        JaipurCard.GoodType gt = types.get(index);
        int maxForType = available.get(gt).getValue();

        // Try taking 0 of this type (skip to next)
        generateCombinations(types, available, index + 1, current, totalSoFar, results, maxTotal);

        // Try taking 1..maxForType of this type
        for (int n = 1; n <= maxForType && totalSoFar + n <= maxTotal; n++) {
            current.put(gt, n);
            generateCombinations(types, available, index + 1, current, totalSoFar + n, results, maxTotal);
        }
        current.remove(gt);
    }

    /**
     * Generates all combinations of cards to give back from hand/camels that sum to exactly `target`.
     */
    private void generateCombinationsExact(List<JaipurCard.GoodType> types, Map<JaipurCard.GoodType, Integer> available,
                                           int index, Map<JaipurCard.GoodType, Integer> current, int totalSoFar,
                                           int target, List<Map<JaipurCard.GoodType, Integer>> results) {
        if (totalSoFar == target) {
            results.add(new HashMap<>(current));
            return;
        }
        if (totalSoFar > target || index >= types.size()) return;

        JaipurCard.GoodType gt = types.get(index);
        int maxForType = available.get(gt);
        int remaining = target - totalSoFar;

        // Try giving 0 of this type (skip to next)
        generateCombinationsExact(types, available, index + 1, current, totalSoFar, target, results);

        // Try giving 1..min(maxForType, remaining) of this type
        for (int n = 1; n <= Math.min(maxForType, remaining); n++) {
            current.put(gt, n);
            generateCombinationsExact(types, available, index + 1, current, totalSoFar + n, target, results);
        }
        current.remove(gt);
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        if (currentState.isActionInProgress()) return;

        // Check game end
        JaipurGameState jgs = (JaipurGameState) currentState;
        JaipurParameters jp = (JaipurParameters) currentState.getGameParameters();
        if (actionTaken instanceof TakeCards && ((TakeCards)actionTaken).isTriggerRoundEnd() || jgs.nGoodTokensSold.getValue() == jp.nGoodTokensEmptyRoundEnd) {
            // Round end!
            endRound(currentState);

            // Check most camels, add extra points
            int maxCamels = 0;
            HashSet<Integer> pIdMaxCamels = new HashSet<>();
            for (int i = 0; i < jgs.getNPlayers(); i++) {
                if (jgs.playerHerds.get(i).getValue() > maxCamels) {
                    maxCamels = jgs.playerHerds.get(i).getValue();
                    pIdMaxCamels.clear();
                    pIdMaxCamels.add(i);
                } else if (jgs.playerHerds.get(i).getValue() == maxCamels) {
                    pIdMaxCamels.add(i);
                }
            }
            if (pIdMaxCamels.size() == 1) {
                // Exactly 1 player has most camels, they get bonus. If tied, nobody gets bonus.
                int player = pIdMaxCamels.iterator().next();
                jgs.playerScores.get(player).increment(jp.nPointsMostCamels);
                if (jgs.getCoreGameParameters().recordEventHistory) {
                    jgs.recordHistory("Player " + player + " earns the " + jp.nPointsMostCamels + " Camel bonus points (" + maxCamels + " camels)");
                }
            }

            // Decide winner of round
            int roundsWon = 0;
            int winner = -1;
            StringBuilder scores = new StringBuilder();
            for (int p = 0; p < jgs.getNPlayers(); p++) {
                int o = jgs.getOrdinalPosition(p);
                scores.append(p).append(":").append(jgs.playerScores.get(p).getValue());
                if (o == 1) {
                    jgs.playerNRoundsWon.get(p).increment();
                    roundsWon = jgs.playerNRoundsWon.get(p).getValue();
                    winner = p;
                    scores.append(" (win)");
                }
                scores.append(", ");
            }
            scores.append(")");
            scores = new StringBuilder(scores.toString().replace(", )", ""));
            if (jgs.getCoreGameParameters().recordEventHistory) {
                jgs.recordHistory("Round scores: " + scores);
            }

            if (roundsWon == jp.getNRoundsWinForGameWin()) {
                // Game over, this player won
                jgs.setGameStatus(CoreConstants.GameResult.GAME_END);
                for (int i = 0; i < jgs.getNPlayers(); i++) {
                    if (i == winner) {
                        jgs.setPlayerResult(WIN_GAME, i);
                    } else {
                        jgs.setPlayerResult(LOSE_GAME, i);
                    }
                }
                return;
            }

            // Reset and set up for next round
            setupRound(jgs, jp);

        } else {
            // Spoilage check: discard perishable goods held too long
            if (jp.isEnableSpoilage()) {
                int currentPlayer = jgs.getCurrentPlayer();
                int currentTurn = jgs.getTurnCounter();
                JaipurCard.GoodType[] perishableTypes = {Cloth, Spice};
                for (JaipurCard.GoodType gt : perishableTypes) {
                    List<Integer> acqTurns = jgs.cardAcquisitionTurns.get(currentPlayer).get(gt);
                    for (int k = acqTurns.size() - 1; k >= 0; k--) {
                        if (currentTurn - acqTurns.get(k) >= jp.getSpoilageTurnLimit()) {
                            acqTurns.remove(k);
                            jgs.playerHands.get(currentPlayer).get(gt).decrement();
                        }
                    }
                }
            }

            // It's next player's turn
            endPlayerTurn(jgs);
        }
    }
}
