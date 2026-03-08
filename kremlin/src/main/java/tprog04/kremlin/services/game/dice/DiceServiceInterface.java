package tprog04.kremlin.services.game.dice;

import java.util.List;

public interface DiceServiceInterface {
    
    /* Roll a single die with given number of sides */
        // Example: roll(20) -> values between 1 and 20
    int rollSingle(int sides);

    /* Roll N dice of M sides each */
        // Example: rollMany(2, 10) -> [1..10, 1..10]
    List<Integer> rollMany(int dice, int sides);

}
