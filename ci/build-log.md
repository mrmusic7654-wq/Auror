### Build log — run 34103558423 Mon Sep  7 09:02:51 UTC 2026
```
/home/runner/work/Auror/Auror/app/build/tmp/kapt3/stubs/debug/aura/orchestrator/credential/CredentialBroker.java:29: error: incompatible types: Object cannot be converted to Annotation
> Task :app:kaptDebugKotlin FAILED
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app:kaptDebugKotlin'.
* Exception is:
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':app:kaptDebugKotlin'.
	at org.gradle.api.internal.tasks.execution.CatchExceptionTaskExecuter.execute(CatchExceptionTaskExecuter.java:36)
Caused by: org.gradle.workers.internal.DefaultWorkerExecutor$WorkExecutionException: A failure occurred while executing org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask$KaptExecutionWorkAction
	at org.gradle.api.internal.tasks.execution.CatchExceptionTaskExecuter.execute(CatchExceptionTaskExecuter.java:36)
Caused by: java.lang.reflect.InvocationTargetException
Caused by: java.lang.NullPointerException: processingEnv must not be null
```
