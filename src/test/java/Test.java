import junit.framework.TestCase;
import pw.jonak.practicehelper.MethodTest;
import pw.jonak.practicehelper.Result;
import pw.jonak.practicehelper.Success;

import java.util.Random;

public class Test extends TestCase {
    protected MethodTest mt;

    protected Object[] paramGenerator() {
        Random r = new Random();
        return new Object[]{
                r.nextDouble() * r.nextInt(100),
                r.nextDouble() * r.nextInt(100),
                r.nextDouble() * r.nextInt(100)
        };
    }

    @Override
    protected void setUp() throws Exception {
        mt = new MethodTest(
                MethodsAndParameterPassingPractice.class, // Student class
                TestSolution.class,
                TestSolution.class.getDeclaredMethod("solveTrainProblem", double.class, double.class, double.class),
                new Object[][]{
                        new Object[]{0.0, 0.0, 0.0},
                        new Object[]{-100.0, 100.0, 0.3}
                },
                this::paramGenerator,
                10
        );
    }

    public void testOne() {
        for (Result r : mt.runAllTestsThenEnd()) {
            System.out.println(r);
            assertTrue(r instanceof Success);
        }
    }
}
