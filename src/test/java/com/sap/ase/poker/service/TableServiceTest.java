package com.sap.ase.poker.service;

import com.sap.ase.poker.model.IllegalActionException;
import com.sap.ase.poker.model.IllegalAmountException;
import com.sap.ase.poker.model.Player;
import com.sap.ase.poker.model.deck.Card;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import com.sap.ase.poker.model.deck.Deck;
import org.junit.jupiter.api.Test;
import com.sap.ase.poker.model.GameState;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TableServiceTest {
    private TableService tableService;
    @Autowired
    private Supplier<Deck> deckSupplier;
    @BeforeEach
    void setup(){
        tableService = new TableService(deckSupplier);
    }

    @AfterEach
    void cleanUp() {

    }

    @Test
    void getGameStateShouldGiveOpenState(){
        GameState gameState = tableService.getState();
        assertEquals(gameState, GameState.OPEN);
    }
    @Test
    void getPlayerShouldGiveEmptyPlayer(){
        List<Player> players = tableService.getPlayers();
        assertEquals(players, new ArrayList<>());
    }

    @Test
    void addPlayerAndCallGetPlayer() {
        tableService.addPlayer("01","Batman");
        List<Player> players = tableService.getPlayers();
        assertEquals( "Batman", players.get(0).getName());
        assertFalse(players.get(0).isActive());
    }

    @Test
    void startGameWithPlayerLessThanTwo(){
        //tableService.start();
        assertThrows(IllegalActionException.class, () -> tableService.start());
        assertEquals(tableService.getCurrentPlayer(), Optional.empty());
    }

    @Test
    void startGameWithTwoPlayer(){
        tableService.addPlayer("01", "Batman");
        tableService.addPlayer("02", "Superman");
        tableService.start();
        assertEquals(tableService.getState(), GameState.PRE_FLOP);
        assertEquals(tableService.getCurrentPlayer(), Optional.of(tableService.getPlayers().get(0)));
    }

    @Test
    void getCurrentCardBeforeGameStart(){
        tableService.addPlayer("01", "Batman");
        assertEquals(tableService.getPlayerCards("01").size(), 0);
        assertEquals(tableService.getPlayerCards("NAN").size(),0);
    }
    @Test
    void getCurrentPlayerCard(){
        startGameWithThreePlayer();
        assertEquals(tableService.getPlayerCards("01").size(), 2);
    }

    @Test
    void getCommunityCard(){
        assertEquals(tableService.getCommunityCards(), new ArrayList<Card>());
    }

    @Test
    void performActionBeforeGameStart(){
        tableService.performAction("Check", 0);
        assertEquals(tableService.getState(), GameState.OPEN);
    }
    @Test
    void performActionforCheckThreeTimes(){
        startGameWithThreePlayer();
        tableService.performAction("Check", 0);
        assertEquals(tableService.getCurrentPlayer(),Optional.of(tableService.getPlayers().get(1)));
        tableService.performAction("Check",0);
        tableService.performAction("Check",0);
        assertEquals(tableService.getCommunityCards().size(),3);
        assertEquals(tableService.getState(), GameState.FLOP);
    }

//    @Test
//    void performRaisedFunctionForIllegalAction(){
//        startGameWithThreePlayer();
////        tableService.performAction("Raise", 110);
////        assertThrows(IllegalAmountException.class, () -> {
////            throw new IllegalAmountException("Raised amount should be less than available cash");
////        });
//        tableService.performAction("Raise", 20);
//        tableService.performAction("Raise", 90);
//        assertThrows(IllegalAmountException.class, () -> {
//            throw new IllegalAmountException("Raised amount should not be greater than minimum cash of other player");
//        });
//    }
    @Test
    void performRaiseActionShouldGiveErrorFor(){
        startGameWithThreePlayer();
        tableService.performAction("Raise", 10);
        assertEquals(tableService.getPlayers().get(0).getCash(), 90);
    }

    @Test
    void UnsupportedAction(){
        startGameWithThreePlayer();;
        tableService.performAction("Bingo", 0);
    }

    @Test
    void currentPlayerFold(){
        startGameWithThreePlayer();
        tableService.performAction("Fold",0);
        assertFalse(tableService.getPlayers().get(0).isActive());
    }

    @Test
    void allPlayerExceptOneFold(){
        startGameWithThreePlayer();
        tableService.performAction("Fold",0);
        tableService.performAction("Fold",0);
        assertEquals(tableService.getActivePlayer().size(), 1);
        assertEquals(tableService.getState(), GameState.ENDED);
    }

    @Test
    void currentPlayerCall(){
        startGameWithThreePlayer();
//        tableService.performAction("Raise",20);
//        assertThrows(IllegalActionException.class, () -> {
//            throw new IllegalActionException("Call only can be performed if one of the previous players performed a raise action");
//        });
        assertThrows(IllegalActionException.class, () -> tableService.performAction("Call", 0)
        );

        tableService.performAction("Raise",20);
        tableService.performAction("Call",0);
        assertEquals(tableService.getPlayers().get(1).getCash(), 80);
    }

    @Test
    void currentPlayerCheckWithExistingBetAmount(){
        startGameWithThreePlayer();
        tableService.performAction("Raise", 10);
        //tableService.performAction("Check", 0);
        assertThrows(IllegalActionException.class, () -> tableService.performAction("Check", 0)
        );
    }
    @Test
    void gameEndedAndAdvanceToNextRound(){
        startGameWithThreePlayer();
        tableService.performAction("Raise", 10);
        tableService.performAction("Call", 0);
        tableService.performAction("Call", 0);
        assertEquals(tableService.getState(), GameState.FLOP);
        assertEquals(tableService.getPot(), 30);
    }
    @Test
    void gameEndedWithFold(){
        startGameWithThreePlayer();
        tableService.performAction("Raise",20);
        tableService.performAction("Call",0);
        tableService.performAction("Fold",0);
        assertEquals(tableService.getState(), GameState.FLOP);
        assertEquals(tableService.getPot(), 40);

    }

    @Test
    void nextBetAmountLessThanCurrentBetAmount(){
        startGameWithThreePlayer();
        tableService.performAction("Raise",20);
        //tableService.performAction("Raise",20);
        assertThrows(IllegalAmountException.class, () -> tableService.performAction("Raise", 0)
        );
    }
    @Test
    void gameAdvanceToTurn(){
        startGameWithThreePlayer();
        tableService.performAction("Raise",20);
        tableService.performAction("Call",0);
        tableService.performAction("Call",0);
        assertEquals(tableService.getState(), GameState.FLOP);
        assertEquals(tableService.getPot(), 60);

        tableService.performAction("Raise",30);
        tableService.performAction("Call",0);
        tableService.performAction("Fold",0);
        assertEquals(tableService.getState(), GameState.TURN);
        assertEquals(tableService.getPot(), 120);

    }
    @Test
    void testforWinner(){
        startGameWithThreePlayer();
        tableService.performAction("Raise",10);
        tableService.performAction("Call",0);
        assertEquals(tableService.getBets().get("01"), 10);
        tableService.performAction("Call",0);

        tableService.performAction("Raise",20);
        tableService.performAction("Call",0);
        tableService.performAction("Call",0);

        tableService.performAction("Raise",30);
        tableService.performAction("Call",0);
        tableService.performAction("Call",0);
        assertTrue(tableService.getWinner().isEmpty());
        assertTrue(tableService.getWinnerHand().isEmpty());
        tableService.performAction("Raise",40);
        tableService.performAction("Call",0);
        tableService.performAction("Call",0);
        assertFalse(tableService.getWinner().isEmpty());
        assertFalse(tableService.getWinnerHand().isEmpty());
        assertFalse(tableService.getWinnersList().getWinners().isEmpty());
        tableService.getActivePlayer().get(0).setInactive();
        tableService.getActivePlayer().get(1).setInactive();
        assertTrue(tableService.getWinnerHand().isEmpty());
    }

    @Test
    void performActionAfterGameEnd(){
        testforWinner();
        tableService.performAction("Raise",30);
    }

    @Test
    void playHandTwoTimes(){
        testforWinner();
        startGameWithThreePlayer();
        assertEquals(tableService.getPot(), 0);
        assertEquals(tableService.getCommunityCards().size(), 0);
    }
    @Test
    void emptyAndInvalidPlayerId(){
        assertNull(tableService.getPlayerWithId("01"));
        startGameWithThreePlayer();
        assertNull(tableService.getPlayerWithId("Null"));
    }
    @Test
    void performIllegalRaise(){
        startGameWithThreePlayer();
        assertThrows(IllegalAmountException.class, () -> tableService.performAction("Raise", 110)
        );
        tableService.performAction("Raise", 50);
        assertThrows(IllegalAmountException.class, () -> tableService.performAction("Raise", 60)
        );
    }

    @Test
    void differentCaseForDetermineNextPlayer(){
        startGameWithThreePlayer();
        tableService.performAction("Fold",0);
        tableService.performAction("Raise",10);
        tableService.performAction("Call",0);
    }
    @Test
    void switchStatement(){
        startGameWithThreePlayer();
        tableService.performAction("", 0);
        tableService.performAction("Raise",10);
        tableService.performAction("Call",0);
        assertThrows(IllegalActionException.class, () -> tableService.performAction("Check",0));
        tableService.performAction("Throw",0);
    }

    @Test
    void preflopToFlopWithBidding(){
        startGameWithThreePlayer();
        tableService.performAction("Raise",10);
        tableService.performAction("Call",0);
        tableService.performAction("Call",0);
    }

    public void startGameWithThreePlayer(){
        tableService.addPlayer("01", "Batman");
        tableService.addPlayer("02", "Superman");
        tableService.addPlayer("03", "Hulk");
        tableService.start();
    }



}