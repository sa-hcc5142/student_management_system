import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {
    @Test
    void testAddition(){
        Calculator calc = new Calculator();
        assertEquals(4, calc.add(2,2));
    }

    @Test
    void testDivision(){
        Calculator calc = new Calculator();
        assertThrows(IllegalArgumentException.class,() -> {
            calc.div(10,0);
        });
    }

}