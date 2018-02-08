package pw.jonak.practicehelper;

/**
 * Represents a single method that generates valid parameters
 * for a method.
 */
public interface TestCaseGenerator {
    /**
     * Creates an array of parameters that are automatically
     * cast to the types of the parameters in the target function.
     * @return An array, where each element represents a single parameter in the function that's being tested.
     */
    public Object[] generate();
}
