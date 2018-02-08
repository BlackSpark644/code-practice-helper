package pw.jonak.practicehelper.testresult;

public class Error extends Result {
    public Error(String methodName) {
        super(methodName);
    }

    @Override
    public String toString() {
        return "There was an error (not your fault) trying to deal with <" + methodName + ">...";
    }
}
