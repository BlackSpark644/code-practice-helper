/**
 * These are the solutions to a practice set I sent out to my students.
 */
public class TestSolution {
    /**
     * Solves the train problem using relative speed.
     */
    private static double solveTrainProblem(double eastTrainSpeed, double westTrainSpeed, double townDistance) {
        double combinedSpeed = eastTrainSpeed + westTrainSpeed;
        return townDistance / combinedSpeed * 60;
    }

    /**
     * Solves the multiple situation using %.
     */
    private static boolean isMultiple(int a, int b) {
        return a % b == 0;
    }

    private static int collatzCount(int input) {
        if (input < 1) {
            return -1;
        }
        int count = 0;
        while (input > 1) {
            count++;
            input = collatzIncrement(input);
        }
        return count;
    }

    private static int collatzIncrement(int n) {
        return n % 2 == 0 ? n / 2 : 3 * n + 1;
    }

    private static boolean collatzEquals(int a, int b) {
        return collatzCount(a) == collatzCount(b);
    }

}
