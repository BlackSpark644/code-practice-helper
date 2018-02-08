package pw.jonak.practicehelper.testresult;

/**
 * Represents a tree of Results that are fail cases.
 */
public class Failure extends Result {
    public Failure(String methodName) {
        super(methodName);
    }

    @Override
    public String toString() {
        return "<" + methodName + "> had a general failure!";
    }

    /**
     * Represents an inconsistency between an expected and actual
     * result
     */
    public static class TestCaseFailure extends Failure {
        /** The parameters that produced the inconsistency */
        public final Object[] givenParameters;

        public final Object expectedResult;

        public final Object actualResult;

        public TestCaseFailure(String methodName, Object[] givenParameters, Object expectedResult, Object actualResult) {
            super(methodName);
            this.givenParameters = givenParameters;
            this.expectedResult = expectedResult;
            this.actualResult = actualResult;
        }

        @Override
        public String toString() {
            StringBuilder composition = new StringBuilder("(");
            for (int i = 0; i < givenParameters.length; i++) {
                composition.append(givenParameters[i].toString());
                if(i != givenParameters.length + 1) {
                    composition.append(", ");
                }
            }
            composition.append(")");

            return "Failure! <"
                    + methodName
                    + composition.toString()
                    + "> should output <"
                    + expectedResult.toString()
                    + ">, but instead it outputs <"
                    + actualResult.toString()
                    + ">.";
        }

        public static class InfiniteLoopFailure extends TestCaseFailure {
            public InfiniteLoopFailure(String methodName, Object[] givenParameters, Object expectedResult) {
                super(methodName, givenParameters, expectedResult, null);
            }

            @Override
            public String toString() {
                StringBuilder composition = new StringBuilder("(");
                for (int i = 0; i < givenParameters.length; i++) {
                    composition.append(givenParameters[i].toString());
                    if(i != givenParameters.length + 1) {
                        composition.append(", ");
                    }
                }
                composition.append(")");

                return "Failure! <"
                        + methodName
                        + composition.toString()
                        + "> should output <"
                        + expectedResult.toString()
                        + ">, but took to long -- is there an infinite loop?";
            }
        }
    }

    public static class HeaderFailure extends Failure {
        protected HeaderFailure(String methodName) {
            super(methodName);
        }

        /**
         * Represents that the method flat out wasn't found.
         */
        public static class MethodNotFoundFailure extends HeaderFailure {
            public MethodNotFoundFailure(String methodName) {
                super(methodName);
            }

            @Override
            public String toString() {
                return "Failure! The method <" + methodName + "> couldn't be found...";
            }
        }

        /**
         * Represents that the method name existed but had the wrong parameter types.
         */
        public static class WrongParameterTypeFailure extends HeaderFailure {
            public final Class<?>[] expectedParameterTypes;
            public final Class<?>[] actualParameterTypes;

            public WrongParameterTypeFailure(String methodName, Class<?>[] expectedParameterTypes, Class<?>[] actualParameterTypes) {
                super(methodName);
                this.expectedParameterTypes = expectedParameterTypes;
                this.actualParameterTypes = actualParameterTypes;
            }

            @Override
            public String toString() {
                StringBuilder expectedTypesString = new StringBuilder();
                for(int i = 0; i < expectedParameterTypes.length; i++) {
                    expectedTypesString.append(expectedParameterTypes[i].getSimpleName());
                    if(i != expectedParameterTypes.length + 1) {
                        expectedTypesString.append(", ");
                    }
                }
                StringBuilder actualTypesString = new StringBuilder();
                for(int i = 0; i < actualParameterTypes.length; i++) {
                    actualTypesString.append(actualParameterTypes[i].getSimpleName());
                    if(i != actualParameterTypes.length + 1) {
                        actualTypesString.append(", ");
                    }
                }
                return "Failure! <"
                        + methodName
                        + "> should have the parameter types <"
                        + expectedTypesString.toString()
                        + "> but instead has <"
                        + actualTypesString.toString()
                        + ">.";
            }
        }

        /**
         * Represents that the method name existed but had the wrong return type.
         */
        public static class WrongReturnTypeFailure extends HeaderFailure {
            public final Class<?> expectedReturnType;
            public final Class<?> actualReturnType;

            public WrongReturnTypeFailure(String methodName, Class<?> expectedReturnType, Class<?> actualReturnType) {
                super(methodName);
                this.expectedReturnType = expectedReturnType;
                this.actualReturnType = actualReturnType;
            }

            @Override
            public String toString() {
                return "Failure! <"
                        + methodName
                        + "> should have the return type <"
                        + expectedReturnType.getSimpleName()
                        + "> but instead has <"
                        + actualReturnType.getSimpleName()
                        + ">.";
            }
        }

        public static class MethodSecurityFailure extends HeaderFailure {
            protected MethodSecurityFailure(String methodName) {
                super(methodName);
            }

            @Override
            public String toString() {
                return "Failure! <"
                        + methodName
                        + "> should have public visibility!";
            }
        }
    }

}
