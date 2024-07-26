package com.sap.ase.poker.service;

import com.sap.ase.poker.model.GameState;
import com.sap.ase.poker.model.IllegalActionException;
import com.sap.ase.poker.model.IllegalAmountException;
import com.sap.ase.poker.model.Player;
import com.sap.ase.poker.model.deck.Card;
import com.sap.ase.poker.model.deck.Deck;
import com.sap.ase.poker.model.rules.HandRules;
import com.sap.ase.poker.model.rules.WinnerRules;
import com.sap.ase.poker.model.rules.Winners;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;

@Service
public class TableService {

    private final Supplier<Deck> deckSupplier;
    private Deck deck;
    private List<Player> playerList;
    private GameState gameState;
//    private Player currentPlayer;
    private int currentPlayerIndex;
    private List<Card> communityCard;
    private final int initial_Amount = 100;
    private boolean isCheck;
    private int potAmount;
    private Map<String, Integer> betMap;
    private int currentBetAmount;
    private int numberOfCheckPlayer;
    private int numberOfFoldPlayer;
    private int numberOfAction;
    private String winnerPlayerId;
    private Winners winnersList;

    public Winners getWinnersList() {
        return winnersList;
    }

//    public void setWinnersList(Winners winnersList) {
//        this.winnersList = winnersList;
//    }

    public TableService(Supplier<Deck> deckSupplier) {
        this.deckSupplier = deckSupplier;
        this.playerList = new ArrayList<>();
        this.gameState = GameState.OPEN;
        this.currentPlayerIndex = -1;
        this.communityCard = new ArrayList<>();
        this.isCheck = true;
        this.deck = deckSupplier.get();
        betMap = new HashMap<>();
        currentBetAmount = 0;
        this.potAmount = 0;
        this.numberOfCheckPlayer=0;
        this.numberOfFoldPlayer=0;
        this.numberOfAction=0;
        this.winnerPlayerId ="";
    }

    public GameState getState() {
        return gameState;
    }

    public List<Player> getPlayers() {
        return playerList;
    }

    public List<Card> getPlayerCards(String playerId) {
        for(Player player:playerList){
            if(player.getId().equals(playerId)){
                return player.getHandCards();
            }
        }
        return new ArrayList<Card>();
    }

    public List<Card> getCommunityCards() {
        return communityCard;
    }

    public Optional<Player> getCurrentPlayer() {
        if(currentPlayerIndex != -1)
            return Optional.of(playerList.get(currentPlayerIndex));
        return Optional.empty();
    }

    public Map<String, Integer> getBets() {
        return betMap;
    }

    public int getPot() {
        return potAmount;
    }

    public Player getPlayerWithId(String id){
        for(Player player:playerList){
            if(player.getId().equals(id)){
                return player;
            }
        }
        return null;
    }

    public Optional<Player> getWinner() {
        // TODO: implement me
        if(!winnerPlayerId.isEmpty()){
            return Optional.of(getPlayerWithId(winnerPlayerId));
        }
        return Optional.empty();
    }

//    public List<Card> getWinnerHand() {
//        WinnerRules winnerRules = new WinnerRules(new HandRules());
//        winnersList = winnerRules.findWinners(getCommunityCards(), getActivePlayer());
//        winnersList.getWinningHand();
//        if(!winnerPlayerId.isEmpty() && getActivePlayer().size()!=1){
////            return getPlayerWithId(winnerPlayerId).getHandCards();
//            return winnersList.getWinningHand().get().getCards();
//        }
//        return new ArrayList<>();
//    }

    public List<Card> getWinnerHand() {
        if(!winnerPlayerId.isEmpty() && getActivePlayer().size()!=1){
            return getPlayerWithId(winnerPlayerId).getHandCards();
        }
        return new ArrayList<>();
    }

    public void start() {
            if(playerList.size() < 2){
                throw new IllegalActionException("Number of player should be greater than 2");
            }

        setParameterToInitial();

    }

    private void setParameterToInitial() {
        gameState = GameState.PRE_FLOP;
        currentPlayerIndex = 0;
        this.communityCard.clear();
        this.betMap.clear();
        this.winnerPlayerId="";
        for(Player player:playerList){
            initializePlayer(player);
            betMap.put(player.getId(), 0);
        }
    }

    public void initializePlayer(Player player) {
        player.setActive();
        List<Card> handCard = new ArrayList<Card>();
        handCard.add(drawCard());
        handCard.add(drawCard());
        player.setHandCards(handCard);
    }

    public void addPlayer(String playerId, String playerName) {
        // TODO: implement me
        System.out.printf("Player joined the table: %s%n", playerId);
        Player player = new Player(playerId, playerName, initial_Amount);
        playerList.add(player);
        betMap.put(playerId, 0);
        player.setInactive();
    }

    public void performAction(String action, int amount) throws IllegalAmountException {
        numberOfAction++;
        if(getState() == GameState.OPEN || getState() == GameState.ENDED){
            return;
        }
        switch(action.toLowerCase()){
            case  "check":
                performCheckAction();
                break;
            case "raise":
                performRaiseAction(amount);
                break;
            case "fold":
                performFoldAction();
                break;
            case "call":
                performCallAction();
        }
        if(numberOfCheckPlayer+numberOfFoldPlayer == playerList.size()) {
            advanceToNextRound();
        }
        if(isRoundEnded()){
            for(Map.Entry<String, Integer> mp:betMap.entrySet()){
                potAmount += mp.getValue();
            }
            advanceToNextRound();
            for(Player player:getPlayers()){
                player.clearBet();
            }
            clearBetMap();
        }
    }
    public void clearBetMap(){
        for (Map.Entry<String, Integer> entry : betMap.entrySet()) {
            entry.setValue(0);
        }
    }
    private void advanceToNextRound(){
        switch(gameState){
            case PRE_FLOP :
                gameState = GameState.FLOP;
                drawInitialCommunityCard();
                break;
            case FLOP :
                gameState = GameState.TURN;
                communityCard.add(drawCard());
                break;
            case TURN:
                gameState = GameState.RIVER;
                communityCard.add(drawCard());
                break;
            case RIVER:
                gameState = GameState.ENDED;
                verifyWinner();
                resetHandVariable();
                break;
        }
        currentBetAmount = 0;
        numberOfCheckPlayer = 0;
//        System.out.println(deck.getCards().size());
    }

    private void verifyWinner() {
        WinnerRules winnerRules = new WinnerRules(new HandRules());
        winnersList = winnerRules.findWinners(getCommunityCards(), getActivePlayer());
        winnerPlayerId = winnersList.getWinners().get(0).getId();
        getPlayerWithId(winnerPlayerId).addCash(potAmount);
    }

    private void resetHandVariable() {
        this.deck = deckSupplier.get();
        this.potAmount = 0;
        this.betMap.clear();
        this.currentBetAmount = 0;
        this.numberOfCheckPlayer = 0;
        this.numberOfFoldPlayer = 0;
    }

    private void performCallAction() {

        if(currentBetAmount == 0){
            throw new IllegalActionException("Call only can be performed if one of the previous players performed a raise action");
        }
        setBetForCall();
        determineNextActivePlayer();

    }

    private void setBetForCall() {
        int playerBet = playerList.get(currentPlayerIndex).getBet();
        playerList.get(currentPlayerIndex).bet(currentBetAmount-playerBet);
        betMap.put(playerList.get(currentPlayerIndex).getId(), currentBetAmount);
    }

    private void performFoldAction() {
        numberOfFoldPlayer++;
        playerList.get(currentPlayerIndex).setInactive();
        checkGameEndedinFoldAction();
        determineNextActivePlayer();
    }

    private void checkGameEndedinFoldAction() {
        List<Player> activePlayerList = getActivePlayer();
        if(activePlayerList.size()==1){
            System.out.println("Winner "+activePlayerList.get(0).getName());
            winnerPlayerId = playerList.get(currentPlayerIndex).getId();
            gameState = GameState.ENDED;
        }
    }

    private void performRaiseAction(int amount) {
        int minCash = getMinimumCash();
        int additionalBet = amount - playerList.get(currentPlayerIndex).getBet();

        handleExceptionInRaised(amount, additionalBet, minCash);
        setPlayerBet(amount, additionalBet);
        determineNextActivePlayer();
    }

    private void setPlayerBet(int amount, int additionalBet) {
        playerList.get(currentPlayerIndex).bet(additionalBet);
        currentBetAmount = amount;
        betMap.put(playerList.get(currentPlayerIndex).getId(), amount);
    }

    private void handleExceptionInRaised(int amount, int additionalBet, int minCash) {
        if(amount <= currentBetAmount){
            throw new IllegalAmountException("Raised amount should be greater than current bet amount");
        }
        if(additionalBet > playerList.get(currentPlayerIndex).getCash()){
            throw new IllegalAmountException("Raised amount should be less than available cash");
        }
        if(minCash < additionalBet){
            throw new IllegalAmountException("Raised amount should not be greater than minimum cash of other player");
        }
    }

    public int getMinimumCash(){
        return getPlayers().stream().mapToInt(Player::getCash).min().getAsInt();
    }

    public List<Player> getActivePlayer(){
        List<Player> activePlayer = new ArrayList<>();
        for(Player player:playerList){
            if(player.isActive()){
                activePlayer.add(player);
            }
        }
        return activePlayer;
    }

    private void drawInitialCommunityCard() {
        communityCard.add(drawCard());
        communityCard.add(drawCard());
        communityCard.add(drawCard());
    }

    private Card drawCard() {
        return deck.draw();
    }

    private void performCheckAction() {
            if(currentBetAmount != 0){
                throw new IllegalActionException("You can't check since someone already bet");
            }
            numberOfCheckPlayer++;
            determineNextActivePlayer();

    }

    public void determineNextActivePlayer() {
//        int currentPlayerIndex = playerList.indexOf(currentPlayer);
        int i=1;
        while(!playerList.get((currentPlayerIndex+i)%playerList.size()).isActive()){
            i++;
        }
        currentPlayerIndex = (currentPlayerIndex+i)%playerList.size();
    }

    public boolean isRoundEnded(){
        if(currentBetAmount > 0){
            List<Player> activePlayer = getActivePlayer();
            for(Player player:activePlayer){
                if(betMap.get(player.getId()) != currentBetAmount){
                    return false;
                }
            }
            return true;
        }
        return false;
    }


}
