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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)

class DealerTest {
    @Mock
    private Table table;
    @Mock
    private Player[] players;
    @Mock
    private boolean isSet;
    @Mock
    private boolean startGame = true;
    @Mock
    private boolean reshuffleGame = true;
    /**
     * The list of card ids that are left in the dealer's deck.
     */
    @Mock
    private List<Integer> deck;
    @Mock
    private int playerId;
    /**
     * True iff game should be terminated due to an external event.
     */
    @Mock
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    @Mock
    private long reshuffleTime = Long.MAX_VALUE;
    
    @Test 
    void checkSet() {
        try{
            Player P1 = new Player(null, null, table, playerId, isSet);
            
            assertEquals();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("failed at testCheckSet");
        }
    }



}
