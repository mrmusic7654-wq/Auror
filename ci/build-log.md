### Build log — run 34103884263 Mon Sep  7 09:05:51 UTC 2026
```
e: The daemon has terminated unexpectedly on startup attempt #1 with error code: 0. The daemon process output:
> Task :app:compileDebugKotlin FAILED
e: file:///home/runner/work/Auror/Auror/app/src/main/kotlin/aura/orchestrator/data/SettingsRepository.kt:84:63 Unresolved reference 'first'.
e: file:///home/runner/work/Auror/Auror/app/src/main/kotlin/aura/orchestrator/data/SettingsRepository.kt:87:96 Unresolved reference 'first'.
e: file:///home/runner/work/Auror/Auror/app/src/main/kotlin/aura/orchestrator/ui/components/Components.kt:133:103 Unresolved reference 'stroke'.
e: file:///home/runner/work/Auror/Auror/app/src/main/kotlin/aura/orchestrator/ui/components/Components.kt:135:122 Unresolved reference 'stroke'.
e: file:///home/runner/work/Auror/Auror/app/src/main/kotlin/aura/orchestrator/worker/DownloadWorker.kt:65:46 Argument type mismatch: actual type is 'kotlin.Long', but 'kotlin.Int' was expected.
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app:compileDebugKotlin'.
* Exception is:
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':app:compileDebugKotlin'.
	at org.gradle.api.internal.tasks.execution.CatchExceptionTaskExecuter.execute(CatchExceptionTaskExecuter.java:36)
Caused by: org.gradle.workers.internal.DefaultWorkerExecutor$WorkExecutionException: A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
	at org.gradle.api.internal.tasks.execution.CatchExceptionTaskExecuter.execute(CatchExceptionTaskExecuter.java:36)
Caused by: org.jetbrains.kotlin.gradle.tasks.CompilationErrorException: Compilation error. See log for more details
	at org.jetbrains.kotlin.gradle.tasks.TasksUtilsKt.throwExceptionIfCompilationFailed(tasksUtils.kt:21)
```
