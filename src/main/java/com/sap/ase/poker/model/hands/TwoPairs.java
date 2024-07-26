package com.sap.ase.poker.model.hands;

import com.sap.ase.poker.model.InvalidHandException;
import com.sap.ase.poker.model.deck.Card;
import com.sap.ase.poker.model.deck.Kind;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TwoPairs extends Hand {

    public static final int TWO_PAIRS_RANK = 3;

    public TwoPairs(List<Card> cards) {
        super(cards);
    }

    @Override
    public int getRank() {
        return TWO_PAIRS_RANK;
    }

    @Override
    protected List<Card> findRelevantCards(List<Card> cards) {
        return findTwoHighestPairs(cards);
    }

    @Override
    protected int compareRelevantCards(Hand hand) {
        int highestPairComparison = compareHighestPairByKind(hand);
        if (highestPairComparison != 0) {
            return highestPairComparison;
        }

        return compareLowerPairByKind(hand);
    }

    private int compareLowerPairByKind(Hand hand) {
        Integer lowestPairRank =
                this.getRelevantCards().stream().map(card -> card.getKind().getRank())
                        .min(Integer::compareTo).get();
        Integer otherLowestPairRank =
                hand.getRelevantCards().stream().map(card -> card.getKind().getRank())
                        .min(Integer::compareTo).get();

        return lowestPairRank - otherLowestPairRank;
    }

    private int compareHighestPairByKind(Hand hand) {
        Integer highestPairRank = findHighestPair(this.getRelevantCards());
        Integer otherHighestPairRank =
                findHighestPair(hand.getRelevantCards());

        return highestPairRank - otherHighestPairRank;
    }

    private Integer findHighestPair(List<Card> RelevantCards) {
        return RelevantCards.stream().map(card -> card.getKind().getRank())
                .max(Integer::compareTo).get();
    }

    private List<Card> findTwoHighestPairs(List<Card> cards) {
        List<Card> twoPairs = findAllPairs(cards);


        if (twoPairs.size() < 4) {
            throw new InvalidHandException("Two PairsNotFound");
        }

        if (twoPairs.size() > 4) {
            twoPairs.sort(Card::compareTo);
            twoPairs = twoPairs.subList(twoPairs.size() - 4, twoPairs.size());
        }

        return twoPairs;
    }

    private static List<Card> findAllPairs(List<Card> cards) {
        Map<Kind, List<Card>> kindGroups = cards.stream().collect(Collectors.groupingBy(Card::getKind));
        List<Card> twoPairs =
                kindGroups.values().stream().filter(group -> group.size() == 2).flatMap(Collection::stream)
                        .collect(Collectors.toList());
        return twoPairs;
    }
}
