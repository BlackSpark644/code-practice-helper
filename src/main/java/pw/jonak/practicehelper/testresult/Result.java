package pw.jonak.practicehelper.testresult;

/**
 * Represents the result of a test -- subclassed by Success and Failure
 */
public class Result {
    /** The method that emitted this result */
    public final String methodName;


    protected Result(String methodName) {
        this.methodName = methodName;
    }
}

