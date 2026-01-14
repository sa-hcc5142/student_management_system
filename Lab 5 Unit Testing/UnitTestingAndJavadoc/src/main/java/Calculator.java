/**
 * This is a calculator class
 * @author Nazirul hasan
 * @see <a href = "https://www.w3schools.com/js/">W3 School</a>
*/
public class Calculator {
    /**
     *
     * @param num1 This is first argument.
     * @param num2 This is second argument.
     * @return It returns the sum of num1 and num2.
     */
    public int add (int num1, int num2){
        return num1 + num2;
    }

    /**
     *
     * @param num1 This is first argument.
     * @param num2 This is second argument.
     * @return It returns the qoutient of num1 and num2. Return type float.
     */
    public float div (int num1, int num2){
        if (num2 == 0){
            throw new IllegalArgumentException("Argument 'divisor' is 0");
        }
        else {
            return (float) num1 / num2;
        }
    }
}
