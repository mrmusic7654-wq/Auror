### Build log — run 34121537298 Mon Sep  7 12:27:45 UTC 2026
```
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so, libdatastore_shared_counter.so.
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so, libdatastore_shared_counter.so.
> Task :app:lintVitalRelease FAILED
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app:lintVitalRelease'.
* Exception is:
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':app:lintVitalRelease'.
	at org.gradle.api.internal.tasks.execution.CatchExceptionTaskExecuter.execute(CatchExceptionTaskExecuter.java:36)
Caused by: java.lang.RuntimeException: Lint found fatal errors while assembling a release target.
	at com.android.build.gradle.internal.lint.AndroidLintWorkAction$Companion.maybeThrowException(AndroidLintWorkAction.kt:235)
	at org.gradle.api.internal.tasks.execution.CatchExceptionTaskExecuter.execute(CatchExceptionTaskExecuter.java:36)
```
