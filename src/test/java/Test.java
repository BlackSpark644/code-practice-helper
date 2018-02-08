import junit.framework.TestCase;
import pw.jonak.practicehelper.MethodTest;
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
                TestSolution.class,
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
        System.out.println(mt.runAllTestsThenEnd());
    }
}
