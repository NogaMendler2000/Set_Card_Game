package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.crypto.KeyGenerator;
import javax.lang.model.util.ElementScanner6;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;
    private boolean isSet;
    private boolean startGame = true;
    private boolean reshuffleGame = true;
    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;
    private int playerId;
    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        while (!shouldFinish()) {
            reshuffleGame = true;
            placeCardsOnTable(); // start Game
            startGame = false;
            reshuffleGame = false;
            for (int j = 0; j < players.length; j++) {
                players[j].isPenalty = false;
                players[j].isPoint = false;
                Thread t = new Thread(players[j]);
                t.start();
            }
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
            timerLoop();
            updateTimerDisplay();
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did
     * not time out.
     */
    private void timerLoop() {
        env.ui.setCountdown(env.config.turnTimeoutMillis, false);
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay();
            removeCardsFromTable();
            placeCardsOnTable(); // start Iteration
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
        removeAllCardsFromTable();
        announceWinners();
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        int [] arrToken = new int [3];
        int j=0;
        if (isSet) {
            for (int checkIfItIs : players[playerId].Token) {
                for (int i = 0; i < env.config.rows * env.config.columns; i++) {
                    if (checkIfItIs == i) {
                        table.removeToken(playerId, i);
                        table.removeCard(i);
                        try {
                            arrToken[j] = i;
                        }
                        catch (ArrayIndexOutOfBoundsException e) {
                            System.out.println("B");
                        }
                        j++;
                    }
                }
            }
            //needs to change!!!
            for (int t = 0; t < players.length; t++) {
                for (int token: players[t].Token) {
                    if (token == arrToken[t]) {
                        table.removeToken(t, arrToken[t]);
                    }
                }
            }
            players[playerId].Token.clear();
        }
        else if (reshuffleGame) {
            for (int i = 0; i < players.length; i = i + 1) {
                Queue<Integer> listOfValue = players[i].Token;
                for (int checkSlot : listOfValue){
                    table.removeToken(i, checkSlot);
                } 
                players[i].Token.clear();
            }
            env.ui.removeTokens();
            table.setPlayers.clear();
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement

        // Beginning of the game - Put randomly all the cards on the deck
        if (startGame || reshuffleGame) {
            int[] cardsOnTable = new int[env.config.rows * env.config.columns];
            boolean legal = true;
            for (int i = 0; i < env.config.rows * env.config.columns; i++) {
                int randomNum;
                do {
                    legal = true;
                    randomNum = ThreadLocalRandom.current().nextInt(0, env.config.deckSize);
                    for (int j = 0; j < i; j++) {
                        if (cardsOnTable[j] == randomNum || table.cardToSlot[randomNum] == -2) 
                            legal = false;
                    }
                } while (!legal);
                cardsOnTable[i] = randomNum;
                table.slotToCard[i] = randomNum; // 12
                table.cardToSlot[randomNum] = i; // 81
                table.placeCard(randomNum, i);
            }
        }
        // Each Iteration - Place new Three cards on the deck
        else if (isSet) {
            // 3 = number of creating set
            int[] newCards = new int[3];
            int index = 0;
            for (int i = 0; i < table.slotToCard.length; i = i + 1) {
                if (table.slotToCard[i] == -2) {
                    newCards[index] = i;
                    index++;
                }
            }
            // 3 = is number oueue<Integer> listOfValue = players[i].Token;
            for (int i = 0; i < 3; i++) {
                boolean legal;
                int randomNum;
                do {
                    legal = true;
                    randomNum = ThreadLocalRandom.current().nextInt(0, env.config.deckSize);
                    for (int j = 0; j < env.config.rows * env.config.columns; j++) {
                        if (table.slotToCard[j] == randomNum || table.cardToSlot[randomNum] == -2)
                            legal = false;
                    }
                } while (!legal);
                table.slotToCard[newCards[i]] = randomNum; // 12
                table.cardToSlot[randomNum] = newCards[i]; // 81
                table.placeCard(randomNum, newCards[i]);
            }
        }
    }

    /**
     * Sleep for a fixed amount of timea or until the thread is awakened for some
     * purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        if(reshuffleTime - System.currentTimeMillis()<5000)
        {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        isSet = false;
        if (!table.setPlayers.isEmpty()) {
            playerId = table.setPlayers.remove();
            int[] arr = new int[3];
            List<Integer> itr = table.playersTokens.get(playerId);
            for (int k = 0; k < itr.size(); k = k + 1) {
                try {
                    arr[k] = table.slotToCard[itr.get(k)];
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("A");
                }
            }
            isSet = env.util.testSet(arr);
            // while (players[playerId].playerThread.getState() != State.WAITING){
                
            // }
            if (isSet) {
                synchronized(players[playerId]) {
                    players[playerId].isPoint=true;
                    players[playerId].notify();
                }
            } else {
                synchronized(players[playerId]){
                    players[playerId].isPenalty=true;
                    players[playerId].notify();
                }
            }
        }

        // player interpted / notify after giving pentatly or point
    }

    /**
     * Reset and/or update the countdown and the countdown display.removeAllCardsFromTable
     */
    private void updateTimerDisplay() {
        // TODO implement
        // The time to reshuffle or is receiving set or finished a round
        if (System.currentTimeMillis() >= reshuffleTime || isSet) {
            env.ui.setCountdown(env.config.turnTimeoutMillis, false);
        }
        // Not The time to reshuffle
        else if(reshuffleTime - System.currentTimeMillis()<5000)
        {
            env.ui.setCountdown((reshuffleTime - System.currentTimeMillis()), true);
        }
        else {  
            env.ui.setCountdown((reshuffleTime - System.currentTimeMillis()), false);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
        for (int i = 0; i < env.config.rows * env.config.columns; i++) {
            table.removeCard(i);
        }
        table.playersTokens.clear();
        for (int i = 0; i < players.length; i = i + 1) {
            players[i].Token.clear();
            table.playersTokens.add(new ArrayList<Integer>());
        }
        env.ui.removeTokens();
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        try {
            // -- Check the highest score in the game --
            int maxScore = -1;
            for (int i = 0; i < players.length; i = i + 1) {
                if (players[i].getScore() > maxScore) {
                    maxScore = players[i].getScore();
                }
            }

            // -- Compare who receives the highest score and save their ids in an array
            // (length of array the length of players) --
            int[] idOfWinners = new int[players.length];
            int index = 0;
            for (int i = 0; i < players.length; i = i + 1) {
                if (players[i].getScore() == maxScore) {
                    idOfWinners[index] = i;
                    index = index + 1;
                }
            }

            if (index == 1) {
                System.out.println("THE WINNER IS: " + env.config.playerNames[idOfWinners[0]]);
            } else if (index > 1) {
                int Winners = 0;
                System.out.print("IT IS A DRAW: ");
                while (Winners < index) {
                    System.out.print(env.config.playerNames[idOfWinners[Winners]] + " ");
                    Winners += 1;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("UNEXCEPTED ERROR", e);
        }
    }

}