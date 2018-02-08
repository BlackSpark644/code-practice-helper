import junit.framework.TestCase;
import pw.jonak.practicehelper.MethodTest;
import pw.jonak.practicehelper.testresult.Result;
import pw.jonak.practicehelper.testresult.Success;

import static pw.jonak.practicehelper.MethodTest.parameters;
import static pw.jonak.practicehelper.MethodTest.parameterSet;

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
                parameterSet (
                        parameters(0.0, 0.0, 0.0),
                        parameters(-100.0, 100.0, 0.3)
                ),
                this::paramGenerator,
                10
        );
    }

    public void testOne() {
        for(Result r : mt.runAllTestsThenEnd()) {
            System.out.println(r);
            assertTrue(r instanceof Success);
        }
    }
}
