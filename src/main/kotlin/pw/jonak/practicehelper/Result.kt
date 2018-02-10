@file:Suppress("RedundantVisibilityModifier")

package pw.jonak.practicehelper

import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

/**
 * Represents a [Result] emitted from a [MethodTest].
 * @property methodName The name of the method that was being tested.
 */
public open class Result protected constructor(public val methodName: String)


// === SUCCESS BRANCH ===


/**
 * Represents a general success and is the start of
 * a branch of other more specific successes.
 */
public open class Success(methodName: String) : Result(methodName) {
    override fun toString(): String {
        return "<$methodName> had a general success!"
    }
}

/**
 * Represents a well-formed header.
 */
public class HeaderSuccess(methodName: String) : Success(methodName) {
    override fun toString(): String {
        return "The header for <$methodName> looks good!"
    }
}

/**
 * Represents a successfully completed test case.
 * @property givenParameters The parameters passed into the method.
 * @property output The (correct) output.
 */
public class TestCaseSuccess(methodName: String, public val givenParameters: Array<out Any?>, public val output: Any?) :
    Success(methodName) {
    override fun toString(): String {
        return "Success! <$methodName(${givenParameters.joinToString(", ")})> returned <$output>,"
    }
}


// === FAILURE BRANCH ===


/**
 * Represents a general failure and is the start
 * of a branch of other more specific failures.
 */
public open class Failure(methodName: String) : Result(methodName) {
    override fun toString(): String {
        return "Failure! <$methodName> had a general failure."
    }
}

/**
 * Represents a malformed method header/prototype.
 */
public open class HeaderFailure protected constructor(methodName: String) : Failure(methodName)

/**
 * Represents a method that just isn't even there...
 */
public class MethodNotFoundFailure(methodName: String) : HeaderFailure(methodName) {
    override fun toString(): String {
        return "Failure! The method <$methodName> couldn't be found..."
    }
}

/**
 * Represents a header that has incorrect parameters.
 * @property expectedParameterTypes The correct parameter types.
 * @property actualparameterTypes The parameter types in the student method.
 */
public class WrongParametersFailure(
    methodName: String,
    public val expectedParameterTypes: Array<KParameter>,
    public val actualparameterTypes: Array<KParameter>
) : HeaderFailure(methodName) {
    override fun toString(): String {
        return "Failure! <$methodName> should have the parameter types <(${expectedParameterTypes.map { it.type.jvmErasure.simpleName }.joinToString(
            ", "
        )})> but instead has <(${actualparameterTypes.map { it.type.jvmErasure.simpleName }.joinToString(", ")})>"
    }
}

/**
 * Represents a header that has the wrong return type.
 * @property expectedReturnType The correct return type.
 * @property actualReturnType The return type in the student method.
 */
public class WrongReturnTypeFailure(
    methodName: String,
    public val expectedReturnType: KType?,
    public val actualReturnType: KType?
) : HeaderFailure(methodName) {
    override fun toString(): String {
        return "Failure! <$methodName> should have return type <${expectedReturnType?.jvmErasure?.simpleName
                ?: "void"}>, but instead has <${actualReturnType?.jvmErasure?.simpleName ?: "void"}>."
    }
}

/**
 * Represents a header that was unable to be made accessible.
 */
public class MethodSecurityFailure(methodName: String) : HeaderFailure(methodName) {
    override fun toString(): String {
        return "Failure! <$methodName> should have public visibility."
    }
}

/**
 * Represents a method that failed a test case.
 * @property givenParameters The parameters that resulted in the failure.
 * @property expectedResult The result that was expected.
 * @property actualResult The result that was returned by the student method.
 */
public open class TestCaseFailure(
    methodName: String,
    public val givenParameters: Array<out Any?>,
    public val expectedResult: Any?,
    public val actualResult: Any?
) : Result(methodName) {
    override fun toString(): String {
        return "Failure! <$methodName(${givenParameters.joinToString(", ")})> should output <$expectedResult> but instead outputs <$actualResult>."
    }
}

/**
 * Represents a method that failed a test case due to a timeout.
 */
public class InfiniteLoopFailure(methodName: String, givenParameters: Array<out Any?>, expectedResult: Any?) :
    TestCaseFailure(methodName, givenParameters, expectedResult, null) {
    override fun toString(): String {
        return "Failure! <$methodName(${givenParameters.joinToString(",")})> should output <$expectedResult> but took too long -- is there an infinite loop?"
    }
}


// === ERROR BRANCH ===

/**
 * Represents a failure that wasn't due to student code.
 */
public class Error(methodName: String) : Result(methodName) {
    override fun toString(): String {
        return "There was an error (not your fault) trying to deal with <$methodName>..."
    }
}