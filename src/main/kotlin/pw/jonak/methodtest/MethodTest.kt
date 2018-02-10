package pw.jonak.methodtest

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.*
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction

/**
 * A framework for testing methods.
 * @property targetClass        The class that contains the method to run.
 * @property solutionClass      The class that contains the solution method.
 * @property solution           The correct solution to whatever this MethodTest is testing.
 * @property edgeCases          Sample runs to test the method's inner functionality. It's checked against the solution.
 * @property generator          A function that randomly generates test cases.
 * @property generationRounds   The number of random arguments to generate.
 */
@Suppress("RedundantVisibilityModifier")
class MethodTest(
    private val targetClass: KClass<*>,
    private val solutionClass: KClass<*>,
    private val solution: KCallable<*>,
    private val edgeCases: Array<Array<out Any?>>?,
    private val generator: (() -> Array<out Any?>)?,
    private val generationRounds: Int?
) {

    /** @property executor The [ExecutorService] that's responsible for timing methods. */
    private val executor: ExecutorService = Executors.newFixedThreadPool(2)

    /** @property studentMethod The student's implementation of [solution] in [targetClass] */
    private val studentMethod: KCallable<*>?

    /**
     * Checks the given [edgeCases], sets up [studentMethod],
     * and ensures access to [studentMethod] and [solution].
     */
    init {
        edgeCases?.let {
            for (edgeCase in it) {
                if (!validateTestCase(edgeCase)) {
                    throw IllegalArgumentException("Your test cases are incompatible with the method you're testing!")
                }
            }
        }

        var tempFinder: KCallable<*>? = null
        for (m: KCallable<*> in targetClass.declaredMembers) {
            if (m.name == solution.name
                && tempFinder == null || validateParameterTypes(m)
            ) {
                tempFinder = m
            }
        }
        studentMethod = tempFinder

        studentMethod?.let {
            ensureAccess(it)
        }
        ensureAccess(solution)
    }

    /**
     * Constructs a [MethodTest] from Java [Class] and [Method] objects.
     * @throws IllegalArgumentException if [solution] can't be found in [solutionClass].
     */
    constructor(
        targetClass: Class<*>,
        solutionClass: Class<*>,
        solution: Method,
        edgeCases: Array<Array<out Any?>>?,
        generator: (() -> Array<out Any?>)?,
        generationRounds: Int?
    ) : this(
        targetClass.kotlin,
        solutionClass.kotlin,
        solution.kotlinFunction
                ?: throw IllegalArgumentException("${solution.name} can't be found in ${solutionClass.name}!"),
        edgeCases,
        generator,
        generationRounds
    )

    /**
     * Ensures that [m] is accessible.
     * @throws SecurityException if the accessibility can't be changed.
     */
    private fun ensureAccess(m: KCallable<*>) {
        if (!m.isAccessible) {
            m.isAccessible = true
        }
    }

    /**
     * Shuts down [executor], so no more test runs can be
     * called (and makes the program exit cleanly).
     */
    public fun end() {
        executor.shutdown()
    }

    /**
     * Runs all the available tests and returns an [ArrayList]<[Result]>
     * containing all the [Result]s from [testMethodHeader] and [runTestCases].
     */
    public fun runAllTestsThenEnd(): ArrayList<Result> {
        val builder = ArrayList<Result>()
        val headerResult = testMethodHeader()
        builder += headerResult
        if (headerResult is Success) {
            builder.addAll(runTestCases())
        }
        end()
        return builder
    }

    /**
     * Tests the method header of [studentMethod], checking
     * if it exists, and if it has the correct parameter and
     * return types.
     */
    private fun testMethodHeader(): Result {
        if (studentMethod == null) {
            return MethodNotFoundFailure(solution.name)
        }
        if (!validateParameterTypes(studentMethod)) {
            return WrongParametersFailure(
                solution.name,
                solution.parameters.toTypedArray(),
                studentMethod.parameters.toTypedArray()
            )
        }
        if (studentMethod.returnType != solution.returnType) {
            return WrongReturnTypeFailure(
                solution.name,
                solution.returnType,
                studentMethod.returnType
            )
        }
        return HeaderSuccess(solution.name)
    }

    /**
     * Runs all available test cases, including edge cases
     * and generated cases.
     */
    public fun runTestCases(): ArrayList<Result> {
        val results = ArrayList<Result>()
        edgeCases?.forEach { results += runTestCase(it) }
        generator?.let {
            generationRounds?.times {
                val test = it()
                if (validateTestCase(test)) {
                    results += runTestCase(test)
                } else {
                    throw IllegalArgumentException("Your generator method is incompatible with the method you're testing!")
                }
            }
        }
        return results
    }

    /**
     * Runs a single given [test] case.
     */
    private fun runTestCase(test: Array<out Any?>): Result {
        val testExec: Future<*> = executor.submit<Any?> { studentMethod?.call(*test) }
        val solutionExec: Future<*> = executor.submit<Any?> { solution.call(*test) }
        val testVal: Any?
        val solutionVal: Any?

        try {  // Resolve solution execution
            solutionVal = solutionExec.get(METHOD_TIMEOUT.toLong(), TimeUnit.SECONDS)
        } catch (e: InterruptedException) {  // TODO: Redundant much?
            System.err.println("Error in solution!")
            e.printStackTrace()
            return Error(solution.name)
        } catch (e: ExecutionException) {
            System.err.println("Error in solution!")
            e.printStackTrace()
            return Error(solution.name)
        } catch (e: TimeoutException) {
            System.err.println("Error in solution!")
            e.printStackTrace()
            return Error(solution.name)
        }

        try {  // Resolve student's method execution
            testVal = testExec.get(METHOD_TIMEOUT.toLong(), TimeUnit.SECONDS)
        } catch (e: InterruptedException) {  // Happens when end() is called before the method resolves, I think TODO: Verify
            e.printStackTrace()
            return Error(solution.name)
        } catch (e: ExecutionException) {  // Happens when there's an exception inside the executor submission.
            if (e.cause!!::class == InvocationTargetException::class) {  // True if the student's method threw an exception
                println("The method threw this exception:")
                e.cause?.printStackTrace()
                return TestCaseFailure(solution.name, test, solutionVal, null)
            } else {  // Otherwise, something else inside the submission failed -- like the invoke method.
                e.printStackTrace()
                return Failure(solution.name)
            }
        } catch (e: TimeoutException) {  // Happens when the method times out (see constant METHOD_TIMEOUT)
            return InfiniteLoopFailure(solution.name, test, solutionVal)
        }


        // Makes sure a failure is not emitted for a void method.
        return if (solution.returnType.jvmErasure != Unit::class && testVal != null && testVal != solutionVal) {
            TestCaseFailure(solution.name, test, solutionVal, testVal)
        } else {
            TestCaseSuccess(solution.name, test, testVal)
        }

    }

    /**
     * Repeats a function [f] as many times as the receiving [Int].
     */
    private fun Int.times(f: () -> Unit) {
        for (i in 0 until this) {
            f()
        }
    }

    /**
     * Returns `true` if [m]'s parameter types match [solution]'s.
     */
    private fun validateParameterTypes(m: KCallable<*>): Boolean {
        if (m.parameters.size != solution.parameters.size) return false
        for (i in m.parameters.indices) {
            if (m.parameters[i].type != solution.parameters[i].type) return false
        }
        return true
    }

    /**
     * Returns `true` if the [testCase] is compatible with the [solution] method.
     */
    private fun validateTestCase(testCase: Array<out Any?>): Boolean {
        if (testCase.size != solution.parameters.size) return false

        for (i in solution.parameters.indices) {
            if (testCase[i] == null && !solution.parameters[i].type.isMarkedNullable) {
                return false
            }
            if (testCase[i] != null &&
                testCase[i]!!::class isNotLike solution.parameters[i].type.jvmErasure
            ) {
                return false
            }
        }
        return true
    }

    /**
     * Tests if the receiver is the same as [target], or, if the receiver
     * is a boxed primitive, if it's equal to its matching primitive.
     */
    private infix fun KClass<*>.isLike(target: KClass<*>): Boolean {
        return this == target ||
                this == Int::class && target == Int::class.javaPrimitiveType ||
                this == Double::class && target == Double::class.javaPrimitiveType ||
                this == Char::class && target == Char::class.javaPrimitiveType ||
                this == Boolean::class && target == Boolean::class.javaPrimitiveType ||
                this == Byte::class && target == Byte::class.javaPrimitiveType ||
                this == Float::class && target == Float::class.javaPrimitiveType ||
                this == Long::class && target == Long::class.javaPrimitiveType ||
                this == Short::class && target == Short::class.javaPrimitiveType
    }

    /** The opposite of [isLike] */
    private infix fun KClass<*>.isNotLike(target: KClass<*>): Boolean = !(this isLike target)

    companion object {
        /** The number of seconds before we consider a student method "stuck." */
        const val METHOD_TIMEOUT: Int = 3
    }
}