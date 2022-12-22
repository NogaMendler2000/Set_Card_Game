package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ExtendWith(MockitoExtension.class)
class DealerTest {
    @Mock
    private Table table;
    private Player[] players;
    @Mock
    private Dealer dealer;
    @Mock
    private Logger logger;
    @Mock
    private Env env;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    /**
     * The list of card ids that are left in the dealer's deck.
     */
    @Mock
    private List<Integer> deck;
    private int playerId;
    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;


    @Test 
    void checkRandomTableCards() {
        try{
            dealer.randomTableCards();
            for(int i=0; i<env.config.rows * env.config.columns; i++){
                assertNotEquals(-2, table.slotToCard[i]);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.print("failed at testRandomTableCards");
        }        
     }
     

    @Test 
    void checkRandomSetCards() {
        try{
            dealer.randomSetCards();
            for(int i=0; i<env.config.rows * env.config.columns; i++){
                assertNotEquals(-2, table.slotToCard[i]);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.print("failed at testRandomSetCards");
        }        
     }

}


