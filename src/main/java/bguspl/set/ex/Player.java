package bguspl.set.ex;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate
     * key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;
    public boolean isPenalty;
    public boolean isPoint;
    private boolean stop;
    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score = 0;
    private int slot;
    public BlockingQueue<Integer> Token;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided
     *               manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        Token = new ArrayBlockingQueue<Integer>(3);
        stop=false;
    }

    /**
     * The main player thread of each player starts here (main loop for the player
     * thread).
     */

    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        if (!human) {
            createArtificialIntelligence();
        }
        while (!terminate) {
            if (!human) {
                // synchronized(aiThread){
                //     aiThread.notify();
                // }
            }
        }
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of
     * this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it
     * is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                slot = ThreadLocalRandom.current().nextInt(1, env.config.rows * env.config.columns);
                keyPressed(slot);
                // try {
                //         synchronized (this) {
                //             if(checkSet) {
                //                 aiThread.wait();
                //             }
                //         }
                //     } catch (InterruptedException ignored) {
                // }
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public synchronized void keyPressed(int slot) {
        // table.hints();
        if(!isPenalty && !isPoint && !stop)
        {
            this.slot = slot;
            boolean IsNeedRemove = false;
            Iterator<Integer> iter = Token.iterator();
            while (iter.hasNext()) {
                if (iter.next() == slot) {
                    IsNeedRemove = true;
                    Token.remove(slot);
                    table.removeToken(id, slot);
                }
            }
            if (!IsNeedRemove && Token.size() == 3) {
                return;
            }
            else if (!IsNeedRemove && Token.size() < 3) {
                Token.add(slot);
                table.placeToken(id, slot);

                if(table.playersTokens.get(id).size() > 3) {
                    System.out.println(table.playersTokens.get(id).get(0));
                    System.out.println("W");
                }
            }
            if (Token.size() == 3) {
                stop=true;
                // add set to Table (synchronized on table)
                table.setPlayers.add(id);
                // add to setid queue in table
                try {
                    synchronized(this) {
                        wait();
                        if (isPoint)
                        {
                            point();
                            isPoint = false;
                        }
                        if (isPenalty)
                        {
                            penalty();
                            isPenalty=false;
                        }  
                    }
                } catch (InterruptedException e) {

                }
            }   
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */

    public void point() {
        // int ignored = table.countCards(); // this part is just for demonstration in
        // the unit tests
        score++;
        // for (int i = 0; i < 3; i = i + 1) {     
        //     Iterator <Integer> itr = Token.iterator();      
        //     try {
        //         table.removeToken(id, itr.next());
        //     }
        //     catch (NoSuchElementException e) {
        //         System.out.println("b");
        //     }
            
        // }   
        //table.playersTokens.set(id, new ArrayList<Integer>());
        try {
            Thread.sleep(env.config.pointFreezeMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        env.ui.setScore(id, score);
        env.ui.setFreeze(id, env.config.pointFreezeMillis);
        env.ui.setFreeze(id, 0);
        stop=false;
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        try {
            for (int i=3; i>0; i--)
            {
                env.ui.setFreeze(id, i*env.config.pointFreezeMillis);
                Thread.sleep(env.config.pointFreezeMillis);
            }       
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        env.ui.setFreeze(id, 0);
        stop=false;
    }

    public int getScore() {
        return score;
    }
}