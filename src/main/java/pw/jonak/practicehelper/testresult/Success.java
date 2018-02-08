package pw.jonak.practicehelper.testresult;

/**
 * Represents a tree of Results that are successes.
 */
public class Success extends Result {
    public Success(String methodName) {
        super(methodName);
    }

    @Override
    public String toString() {
        return "<" + methodName + "> had a general success!";
    }

    /**
     * Represents a correct header.
     */
    public static class HeaderSuccess extends Success {
        public HeaderSuccess(String methodName) {
            super(methodName);
        }

        @Override
        public String toString() {
            return "The header for <" + methodName + "> looks good!";
        }
    }

    /**
     * Represents a successful test case run.
     */
    public static class TestCaseSuccess extends Success {
        public final Object[] givenParameters;
        public final Object result;

        public TestCaseSuccess(String methodName, Object[] givenParameters, Object output) {
            super(methodName);
            this.givenParameters = givenParameters;
            this.result = output;
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

            return "Success! <"
                    + methodName
                    + composition.toString()
                    + "> returned <"
                    + result.toString()
                    + ">";
        }
    }
}
