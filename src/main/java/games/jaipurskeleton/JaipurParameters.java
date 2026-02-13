package games.jaipurskeleton;

import core.AbstractGameState;
import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import games.jaipurskeleton.components.JaipurCard;

import java.util.*;

/**
 * <p>This class should hold a series of variables representing game parameters (e.g. number of cards dealt to players,
 * maximum number of rounds in the game etc.). These parameters should be used everywhere in the code instead of
 * local variables or hard-coded numbers, by accessing these parameters from the game state via {@link AbstractGameState#getGameParameters()}.</p>
 *
 * <p>It should then implement appropriate {@link #_copy()}, {@link #_equals(Object)} and {@link #hashCode()} functions.</p>
 *
 * <p>The class can optionally extend from {@link evaluation.optimisation.TunableParameters} instead, which allows to use
 * automatic game parameter optimisation tools in the framework.</p>
 */
public class JaipurParameters extends TunableParameters {
    Map<JaipurCard.GoodType, Integer> goodNCardsMinimumSell = new HashMap<>() {{
        put(JaipurCard.GoodType.Diamonds, 2);
        put(JaipurCard.GoodType.Gold, 2);
        put(JaipurCard.GoodType.Silver, 2);
        put(JaipurCard.GoodType.Cloth, 1);
        put(JaipurCard.GoodType.Spice, 1);
        put(JaipurCard.GoodType.Leather, 1);
    }};
    Map<Integer, Integer[]> bonusTokensAvailable = new HashMap<>() {{
        put(3, new Integer[]{1, 1, 2, 2, 2, 3, 3});
        put(4, new Integer[]{4, 4, 5, 5, 6, 6});
        put(5, new Integer[]{8, 8, 9, 10, 10});
    }};

    Map<JaipurCard.GoodType, Integer[]> goodTokensProgression = new HashMap<>() {{
        put(JaipurCard.GoodType.Diamonds, new Integer[]{5, 5, 5, 7, 7});
        put(JaipurCard.GoodType.Gold, new Integer[]{5, 5, 5, 6, 6});
        put(JaipurCard.GoodType.Silver, new Integer[]{5, 5, 5, 5, 5});
        put(JaipurCard.GoodType.Cloth, new Integer[]{1, 1, 2, 2, 3, 3, 5});
        put(JaipurCard.GoodType.Spice, new Integer[]{1, 1, 2, 2, 3, 3, 5});
        put(JaipurCard.GoodType.Leather, new Integer[]{1, 1, 1, 1, 1, 1, 2, 3, 4});
    }};
    int nRoundsWinForGameWin = 2;

    int handLimit = 7;
    int nCardsDealPerPlayer = 5;
    int nInitialCamelsInMarket = 3;
    int nInitialMarketCardsFromDeck = 2;
    Map<JaipurCard.GoodType, Integer> nCardsPerType = new HashMap<>() {{
        put(JaipurCard.GoodType.Diamonds, 6);
        put(JaipurCard.GoodType.Gold, 6);
        put(JaipurCard.GoodType.Silver, 6);
        put(JaipurCard.GoodType.Cloth, 8);
        put(JaipurCard.GoodType.Spice, 8);
        put(JaipurCard.GoodType.Leather, 10);
        put(JaipurCard.GoodType.Camel, 11);
    }};

    int nPointsMostCamels = 5;
    int nGoodTokensEmptyRoundEnd = 3;

    // Spoilage rule: perishable goods (Spice, Cloth) are discarded if held too long
    boolean enableSpoilage = false;
    int spoilageTurnLimit = 3;

    public JaipurParameters() {
        addTunableParameter("nPointsMostCamels", 5, Arrays.asList(0, 2, 5, 7, 10));
        addTunableParameter("nGoodTokensEmptyRoundEnd", 3, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("nRoundsWinForGameWin", 2, Arrays.asList(1, 2, 3));
        addTunableParameter("handLimit", 7, Arrays.asList(7, 8, 9, 10));
        addTunableParameter("nCardsDealPerPlayer", 5, Arrays.asList(4, 5, 6, 7));
        addTunableParameter("nInitialCamelsInMarket", 3, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("nInitialMarketCardsFromDeck", 2, Arrays.asList(1, 2, 3, 4));
        addTunableParameter("enableSpoilage", false, Arrays.asList(false, true));
        addTunableParameter("spoilageTurnLimit", 3, Arrays.asList(2, 3, 4, 5, 6));
        for (JaipurCard.GoodType gt : goodNCardsMinimumSell.keySet()) {
            addTunableParameter(gt.name() + " minSell", goodNCardsMinimumSell.get(gt), Arrays.asList(1, 2, 3));
        }
        addTunableParameter("budget", -999);  // required by NTBEA framework
        _reset();
    }


    public Map<JaipurCard.GoodType, Integer> getGoodNCardsMinimumSell() {
        return goodNCardsMinimumSell;
    }

    public Map<Integer, Integer[]> getBonusTokensAvailable() {
        return bonusTokensAvailable;
    }

    public Map<JaipurCard.GoodType, Integer[]> getGoodTokensProgression() {
        return goodTokensProgression;
    }

    public int getNRoundsWinForGameWin() {
        return nRoundsWinForGameWin;
    }

    public int getHandLimit() {
        return handLimit;
    }

    public int getNCardsDealPerPlayer() {
        return nCardsDealPerPlayer;
    }

    public int getNInitialCamelsInMarket() {
        return nInitialCamelsInMarket;
    }

    public int getNInitialMarketCardsFromDeck() {
        return nInitialMarketCardsFromDeck;
    }

    public Map<JaipurCard.GoodType, Integer> getNCardsPerType() {
        return nCardsPerType;
    }

    public int getNPointsMostCamels() {
        return nPointsMostCamels;
    }

    public int getNGoodTokensEmptyGameEnd() {
        return nGoodTokensEmptyRoundEnd;
    }

    public boolean isEnableSpoilage() {
        return enableSpoilage;
    }

    public int getSpoilageTurnLimit() {
        return spoilageTurnLimit;
    }

    @Override
    public void _reset() {
        nPointsMostCamels = (int) getParameterValue("nPointsMostCamels");
        nGoodTokensEmptyRoundEnd = (int) getParameterValue("nGoodTokensEmptyRoundEnd");
        nRoundsWinForGameWin = (int) getParameterValue("nRoundsWinForGameWin");
        handLimit = (int) getParameterValue("handLimit");
        nCardsDealPerPlayer = (int) getParameterValue("nCardsDealPerPlayer");
        nInitialCamelsInMarket = (int) getParameterValue("nInitialCamelsInMarket");
        nInitialMarketCardsFromDeck = (int) getParameterValue("nInitialMarketCardsFromDeck");
        enableSpoilage = (boolean) getParameterValue("enableSpoilage");
        spoilageTurnLimit = (int) getParameterValue("spoilageTurnLimit");
        goodNCardsMinimumSell.replaceAll((gt, v) -> (Integer) getParameterValue(gt.name() + " minSell"));
    }

    @Override
    protected AbstractParameters _copy() {
        JaipurParameters copy = new JaipurParameters();
        copy.bonusTokensAvailable = new HashMap<>();
        for (int n : bonusTokensAvailable.keySet()) {
            copy.bonusTokensAvailable.put(n, bonusTokensAvailable.get(n).clone());
        }
        copy.goodTokensProgression = new HashMap<>();
        for (JaipurCard.GoodType gt : goodTokensProgression.keySet()) {
            copy.goodTokensProgression.put(gt, goodTokensProgression.get(gt).clone());
        }
        copy.nCardsPerType = new HashMap<>(nCardsPerType);
        return copy;
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Jaipur, new JaipurForwardModel(), new JaipurGameState(this, GameType.Jaipur.getMinPlayers()));
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JaipurParameters that)) return false;
        if (!super.equals(o)) return false;
        return nPointsMostCamels == that.nPointsMostCamels && nGoodTokensEmptyRoundEnd == that.nGoodTokensEmptyRoundEnd && nRoundsWinForGameWin == that.nRoundsWinForGameWin && handLimit == that.handLimit && nCardsDealPerPlayer == that.nCardsDealPerPlayer && nInitialCamelsInMarket == that.nInitialCamelsInMarket && nInitialMarketCardsFromDeck == that.nInitialMarketCardsFromDeck && enableSpoilage == that.enableSpoilage && spoilageTurnLimit == that.spoilageTurnLimit && Objects.equals(goodNCardsMinimumSell, that.goodNCardsMinimumSell) && Objects.equals(bonusTokensAvailable, that.bonusTokensAvailable) && Objects.equals(goodTokensProgression, that.goodTokensProgression) && Objects.equals(nCardsPerType, that.nCardsPerType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), goodNCardsMinimumSell, bonusTokensAvailable, goodTokensProgression, nRoundsWinForGameWin, handLimit, nCardsDealPerPlayer, nInitialCamelsInMarket, nInitialMarketCardsFromDeck, nCardsPerType, nPointsMostCamels, nGoodTokensEmptyRoundEnd, enableSpoilage, spoilageTurnLimit);
    }

    @Override
    public String toString() {
        return "JaipurParameters{" +
                "nPointsMostCamels=" + nPointsMostCamels +
                ", nGoodTokensEmptyRoundEnd=" + nGoodTokensEmptyRoundEnd +
                ", nRoundsWinForGameWin=" + nRoundsWinForGameWin +
                ", handLimit=" + handLimit +
                ", nCardsDealPerPlayer=" + nCardsDealPerPlayer +
                ", nInitialCamelsInMarket=" + nInitialCamelsInMarket +
                ", nInitialMarketCardsFromDeck=" + nInitialMarketCardsFromDeck +
                ", enableSpoilage=" + enableSpoilage +
                ", spoilageTurnLimit=" + spoilageTurnLimit +
                ", goodNCardsMinimumSell=" + goodNCardsMinimumSell +
                '}';
    }
}
