package ru.testit.aspects


//import org.aspectj.lang.JoinPoint
//import org.aspectj.lang.annotation.*
//import ru.testit.annotations.Step
//import ru.testit.models.ItemStatus
//import ru.testit.models.StepResult
//import ru.testit.services.Adapter
//import ru.testit.services.AdapterManager
//import ru.testit.services.Utils
//
//import java.lang.reflect.Method
//import java.lang.reflect.Parameter
//import java.util.*
//
//@Aspect
//class StepAspect {
//    private val adapterManager: InheritableThreadLocal<AdapterManager> = object :
//        InheritableThreadLocal<AdapterManager>() {
//        override fun initialValue(): AdapterManager {
//            return Adapter.getAdapterManager()
//        }
//    }
//
//    @Pointcut("@annotation(Step)")
//    fun withStepAnnotation() {}
//
//    @Pointcut("execution(* *(..))")
//    fun anyMethod() {}
//
//    @Before("anyMethod() && withStepAnnotation()")
//    fun startStep(joinPoint: JoinPoint) {
//        val signature = joinPoint.signature as MethodSignature
//        val uuid = UUID.randomUUID().toString()
//        val method = signature.method
//
//        val parameters = method.parameters
//        val stepParameters = mutableMapOf<String, String>()
//
//        for (i in parameters.indices) {
//            val parameter = parameters[i]
//            val name = parameter.name
//            val value = joinPoint.args[i]?.toString() ?: ""
//
//            stepParameters[name] = value
//        }
//
//        val result = StepResult()
//            .setName(Utils.extractTitle(method, stepParameters))
//            .setDescription(Utils.extractDescription(method, stepParameters))
//            .setParameters(stepParameters)
//
//        getManager().startStep(uuid, result)
//    }
//
//    @AfterReturning(pointcut = "anyMethod() && withStepAnnotation()")
//    fun finishStep() {
//        getManager().updateStep { it.setItemStatus(ItemStatus.PASSED) }
//        getManager().stopStep()
//    }
//
//    @AfterThrowing(pointcut = "anyMethod() && withStepAnnotation()", throwing = "throwable")
//    fun failedStep(throwable: Throwable) {
//        getManager().updateStep {
//            it.setItemStatus(ItemStatus.FAILED)
//                .setThrowable(throwable)
//        }
//        getManager().stopStep()
//    }
//
//    private fun getManager(): AdapterManager {
//        return adapterManager.get()
//    }
//}
