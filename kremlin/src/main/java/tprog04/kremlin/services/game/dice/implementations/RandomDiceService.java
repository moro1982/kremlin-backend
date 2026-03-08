package tprog04.kremlin.services.game.dice.implementations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;
import tprog04.kremlin.services.game.dice.DiceServiceInterface;

@Service
public class RandomDiceService implements DiceServiceInterface {

    private final Random random = new Random();

    @Override
    public int rollSingle(int sides) {
        return this.random.nextInt(sides) + 1;
    }

    @Override
    public List<Integer> rollMany(int dice, int sides) {
        
        List<Integer> results = new ArrayList<>();
        for (int i = 0; i < dice; i++) {
            results.add(this.rollSingle(sides));
        }
        return results;
    }
}
