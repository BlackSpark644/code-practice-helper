package pw.jonak.practicehelper;

import pw.jonak.practicehelper.testresult.Error;
import pw.jonak.practicehelper.testresult.Failure;
import pw.jonak.practicehelper.testresult.Result;
import pw.jonak.practicehelper.testresult.Success;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Defines a method test:
 * * Tests if the target type actually contains the method
 * * Tests if the target class' method contains the proper parameter types
 * * Tests functionality using a series of MethodInvokeOperations.
 */
public class MethodTest {
    /**
     * The solution method
     */
    private final Method solution;

    /**
     * Class that stores the solution method.
     */
    private final Class<?> solutionType;

    /**
     * The method name to test
     */
    private final String methodName;

    /**
     * The parameter types the method ought to have
     */
    private final Class<?>[] targetParameterTypes;

    /**
     * The return type the method ought to have
     */
    private final Class<?> targetReturnType;

    /**
     * The class in which the method we're testing resides
     */
    private final Class<?> type;

    /**
     * A list of methods in the actual type we're testing
     */
    private final Method[] methods;

    /**
     * Caches the method once found.
     */
    private Method cachedMethod;

    /**
     * Method that generates test cases
     */
    private final TestCaseGenerator testCaseGenerator;

    /**
     * The number of times random parameters should be generated.
     */
    private final int numGenerationRounds;

    /**
     * A list of specific/edge cases to test
     */
    private final Object[][] specificCases;

    /**
     * An ExecutorService responsible for making sure infinite loops don't break the tester.
     */
    private final ExecutorService executor;

    /**
     * The timeout for the ExecutorService.
     */
    private static final int METHOD_TIMEOUT = 3;

    /**
     * Constructs a method test.
     *
     * @param targetType       The class that contains the method to run.
     * @param solutionType     The class that contains the solution method.
     * @param solution         The correct solution to whatever this MethodTest is testing.
     * @param specificCases    Sample runs to test the method's inner functionality. It's typechecked against parameterTypes and returnTypes.
     * @param generator        A function that randomly generates test cases.
     * @param generationRounds The number of random arguments to generate.
     */
    public MethodTest(Class<?> targetType, Class<?> solutionType, Method solution, Object[][] specificCases, TestCaseGenerator generator, int generationRounds) {
        this.solution = solution;
        this.methodName = solution.getName();
        this.targetParameterTypes = solution.getParameterTypes();
        this.targetReturnType = solution.getReturnType();
        this.type = targetType;
        this.solutionType = solutionType;
        this.methods = targetType.getDeclaredMethods();
        this.specificCases = specificCases;
        this.testCaseGenerator = generator;
        this.numGenerationRounds = generationRounds;
        if (specificCases != null) {
            for (Object[] test : specificCases) {
                if (!validateTestCase(test)) {
                    throw new IllegalArgumentException("Your test cases are incompatible with the method you're testing!");
                }
            }
        }

        ensureAccess();

        this.executor = Executors.newFixedThreadPool(16);
    }

    /**
     * Stops the executor so the program can end cleanly.
     */
    public void end() {
        executor.shutdown();
    }

    /**
     * Runs all tests (including header and edge case testing), then shuts down the executor.
     */
    public ArrayList<Result> runAllTestsThenEnd() {
        ArrayList<Result> retVal = new ArrayList<>();
        Result methodResult = testMethodHeader();
        retVal.add(methodResult);

        if(methodResult instanceof Success) {
            retVal.addAll(runTestCases());
        }
        end();
        return retVal;
    }

    /**
     * Tests that the method exists and that its header is correct.
     * TODO: Add checks for private/public/static/etc.
     *
     * @return The Result corresponding to the result of the test.
     */
    public Result testMethodHeader() {
        if (!containsMethod()) {
            return new Failure.HeaderFailure.MethodNotFoundFailure(methodName);
        }
        Method m = getMethod();
        if (!validateParameterTypes(m)) {
            return new Failure.HeaderFailure.WrongParameterTypeFailure(methodName, targetParameterTypes, m.getParameterTypes());
        }
        if (m.getReturnType() != targetReturnType) {
            return new Failure.HeaderFailure.WrongReturnTypeFailure(methodName, targetReturnType, m.getReturnType());
        }
        return new Success.HeaderSuccess(methodName);
    }

    /**
     * Runs all test cases available.
     */
    public ArrayList<Result> runTestCases() {
        ArrayList<Result> results = new ArrayList<>();
        if(specificCases != null) {
            for(Object[] test : specificCases) {
                results.add(runTestCase(test));
            }
        }
        for(int i = 0; i < numGenerationRounds; i++) {
            Object[] test = testCaseGenerator.generate();
            if(validateTestCase(test)) {
                results.add(runTestCase(test));
            } else {
                throw new IllegalArgumentException("Your generator method is incompatible with the method you're testing!");
            }
        }
        return results;
    }

    /**
     * Runs the test cases, and tells the user which succeeded/failed.
     *
     * @param test The parameters to test against.
     */
    private Result runTestCase(Object[] test) {
        try {
            Future<Object> testExec = executor.submit(() -> getMethod().invoke(type.newInstance(), test));
            Future<Object> solutionExec = executor.submit(() -> solution.invoke(type.newInstance(), test));
            Object retVal;
            Object solutionOutput;

            try {
                solutionOutput = solutionExec.get(METHOD_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.err.println("Error in solution!");
                e.printStackTrace();
                return new Error(methodName);
            }

            try {
                retVal = testExec.get(METHOD_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.out.println("method canceled!");
                return new Failure(methodName);
            } catch (ExecutionException e) {
                if (e.getCause().getClass() == InvocationTargetException.class) {
                    System.out.println("The method threw this exception:");
                    e.getCause().printStackTrace();
                    return new Failure.TestCaseFailure(methodName, test, solutionOutput, null);
                } else {
                    e.printStackTrace();
                }
                return new Failure(methodName);
            } catch (TimeoutException e) {
                return new Failure.TestCaseFailure.InfiniteLoopFailure(methodName, test, solutionOutput);
            }

            if (targetReturnType != void.class && retVal != null && !retVal.equals(solutionOutput)) {
                String targetString = solutionOutput == null ? "null" : solutionOutput.toString();
                return new Failure.TestCaseFailure(methodName, test, solutionOutput, retVal);
            } else {
                String outputString = retVal == null ? "null" : retVal.toString();
                String targetString = solutionOutput == null ? "null" : solutionOutput.toString();
                return new Success.TestCaseSuccess(methodName, test, retVal);
            }
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return new Error(methodName);
        }
    }

    /**
     * Ensures that the tester has access to the methods.
     */
    private void ensureAccess() {
        if(containsMethod()) {
            Method m = getMethod();
            if (!Modifier.isPublic(m.getModifiers())) {
                try {
                    m.setAccessible(true);
                } catch (SecurityException e) {
                    System.out.println("\"" + methodName + "\" needs to be public!");
                }
            }
        }
        if(solution != null) {
            if(!Modifier.isPublic(solution.getModifiers())) {
                try {
                    solution.setAccessible(true);
                } catch (SecurityException e) {
                    System.out.println("Error: the solution needs to be public!");
                }
            }
        }
    }

    /**
     * Validates that the types and order of the parameters
     * in the passed Method line up with what's expected.
     *
     * @param m The method to validate.
     * @return true if the parameters are correct; false otherwise.
     */
    private boolean validateParameterTypes(Method m) {
        Class<?>[] actualParameters = m.getParameterTypes();
        if (targetParameterTypes.length != actualParameters.length) {
            return false;
        }
        for (int i = 0; i < targetParameterTypes.length; i++) {
            if (targetParameterTypes[i] != actualParameters[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if the target method is even contained in the associated class.
     *
     * @return true if the method exists; false otherwise.
     */
    private boolean containsMethod() {
        return getMethod() != null;
    }

    /**
     * Retrieves the Method object that matches the target method for this MethodTest.
     * If there are multiple Methods with the same name, prefers one
     * that matches the parameter types.
     *
     * @return The associated Method object, or null if not found.
     */
    private Method getMethod() {
        if(cachedMethod == null) {
            for (Method m : methods) {
                if (m.getName().equals(methodName) &&
                        (cachedMethod == null || validateParameterTypes(m))) {
                    cachedMethod = m;
                }
            }
        }
        return cachedMethod;
    }

    /**
     * Validates the test case, ensuring that its return and
     * parameter types are consistent with the target method.
     *
     * @param testParameters The parameter list to check
     * @return true the test case is A-OK; false otherwise.
     */
    private boolean validateTestCase(Object[] testParameters) {
        if (testParameters.length != targetParameterTypes.length) {
            return false;
        }
        for (int i = 0; i < targetParameterTypes.length; i++) {
            if (testParameters[i] != null && !checkBoxedPrimitives(testParameters[i], targetParameterTypes[i]) && testParameters[i].getClass() != targetParameterTypes[i]) {
                return false; // TODO: Check for @NotNullable
            }
        }
        return true;
    }

    /**
     * Since an Object can't superclass a primitive, in TestCase, primitive
     * types are stored as boxed primitives (while the parameter/return values
     * in MethodTest are stored as primitives). This method bridges that gap.
     *
     * @param obj    The object type to test.
     * @param target The target primitive type.
     * @return true if the object is a boxed primitive and target is a matching primitive; false otherwise.
     */
    private boolean checkBoxedPrimitives(Object obj, Class<?> target) {
        return obj.getClass() == Integer.class && target == int.class
                || obj.getClass() == Double.class && target == double.class
                || obj.getClass() == Character.class && target == char.class
                || obj.getClass() == Boolean.class && target == boolean.class
                || obj.getClass() == Byte.class && target == byte.class
                || obj.getClass() == Float.class && target == float.class
                || obj.getClass() == Long.class && target == long.class
                || obj.getClass() == Short.class && target == short.class;
    }

    /**
     * Syntactic sugar for creating an array of Object arrays for creating
     * sets of parameters for creating edge cases/etc.
     * @param parameterArrays The arrays representing parameters to pass in.
     * @return An Object[][] good for passing into the MethodTest constructor.
     */
    public static Object[][] parameterSet(Object[]... parameterArrays) {
        return parameterArrays;
    }

    /**
     * Syntactic sugar for creating an Object array for parameter passing.
     * @param parameters The parameters to put in the array.
     * @return An Object array good for passing into MethodTest methods.
     */
    public static Object[] parameters(Object... parameters) {
        return parameters;
    }

}