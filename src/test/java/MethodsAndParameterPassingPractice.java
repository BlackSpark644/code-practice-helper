/*

 ██████╗███████╗ ██╗ ██████╗  ██████╗  █████╗ 
██╔════╝██╔════╝███║██╔═████╗██╔════╝ ██╔══██╗
██║     ███████╗╚██║██║██╔██║███████╗ ███████║
██║     ╚════██║ ██║████╔╝██║██╔═══██╗██╔══██║
╚██████╗███████║ ██║╚██████╔╝╚██████╔╝██║  ██║
 ╚═════╝╚══════╝ ╚═╝ ╚═════╝  ╚═════╝ ╚═╝  ╚═╝
           --- Method Practice ---

Do each of these, then run "Tester" using the running man.
NO INSTANCE VARIABLES ARE ALLOWED!

               --- TASK 1 ---
Write a method called "solveTrainProblem" that solves the following problem:

	Train A, traveling X miles per hour (mph), leaves Westford heading toward Eastford, DIST miles away.
	At the same time Train B, traveling Y mph, leaves Eastford heading toward Westford.
	
	How long does it take for the trains to meet, in minutes?

X, Y, and DIST will be passed in as parameters. Your return value should be as specific as possible.

               --- TASK 2 ---
Write a method called "isMultiple" that tells if one integer is a multiple of another.

               --- TASK 3 ---
Write a method called "collatzCount" that does the same Hailstone problem from Assignment 2,
but instead of printing the steps, just returns the count -- the start number will be passed in.
Use at least 2 methods.
Return -1 if an invalid value is passed in.

               --- TASK 4 ---
Write a method called "collatzEquals" that returns true if two passed values have the same
hailstone count.

 */

public class MethodsAndParameterPassingPractice {

    // === Example Student Solution ===
    // Normally this file would be empty aside from the class and the task list above.

    private static double solveTrainProblem(double eastTrainSpeed, double westTrainSpeed, double townDistance) {
        double combinedSpeed = eastTrainSpeed + westTrainSpeed;
        return townDistance / combinedSpeed * 60;
    }

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
