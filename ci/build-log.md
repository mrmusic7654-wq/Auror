### Build log — run 34104698458 Mon Sep  7 09:16:53 UTC 2026
```
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so, libdatastore_shared_counter.so.
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so, libdatastore_shared_counter.so.
> Task :app:minifyReleaseWithR8 FAILED
ERROR: Missing classes detected while running R8. Please add the missing classes or apply additional keep rules that are generated in /home/runner/work/Auror/Auror/app/build/outputs/mapping/release/missing_rules.txt.
ERROR: R8: Missing class com.google.errorprone.annotations.CanIgnoreReturnValue (referenced from: com.google.crypto.tink.KeysetManager com.google.crypto.tink.KeysetManager.add(com.google.crypto.tink.KeyTemplate) and 52 other contexts)
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app:minifyReleaseWithR8'.
> A failure occurred while executing com.android.build.gradle.internal.tasks.R8Task$R8Runnable
* Exception is:
org.gradle.api.tasks.TaskExecutionException: Execution failed for task ':app:minifyReleaseWithR8'.
	at org.gradle.api.internal.tasks.execution.CatchExceptionTaskExecuter.execute(CatchExceptionTaskExecuter.java:36)
Caused by: org.gradle.workers.internal.DefaultWorkerExecutor$WorkExecutionException: A failure occurred while executing com.android.build.gradle.internal.tasks.R8Task$R8Runnable
	at org.gradle.api.internal.tasks.execution.CatchExceptionTaskExecuter.execute(CatchExceptionTaskExecuter.java:36)
Caused by: com.android.tools.r8.CompilationFailedException: Compilation failed to complete
	at com.android.tools.r8.T.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:5)
	at com.android.tools.r8.internal.to.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:82)
	at com.android.tools.r8.internal.to.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:32)
	at com.android.tools.r8.internal.to.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:31)
	at com.android.tools.r8.R8.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:79)
	at com.android.tools.r8.R8.run(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:3)
	at com.android.builder.dexing.R8Tool.runR8(r8Tool.kt:332)
	at com.android.build.gradle.internal.tasks.R8Task$Companion.shrink(R8Task.kt:782)
	at com.android.build.gradle.internal.tasks.R8Task$R8Runnable.execute(R8Task.kt:856)
Caused by: com.android.tools.r8.internal.g: Missing class com.google.errorprone.annotations.CanIgnoreReturnValue (referenced from: com.google.crypto.tink.KeysetManager com.google.crypto.tink.KeysetManager.add(com.google.crypto.tink.KeyTemplate) and 52 other contexts)
	at com.android.tools.r8.internal.x50.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:21)
	at com.android.tools.r8.internal.x50.error(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:1)
	at com.android.tools.r8.shaking.M.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:1962)
	at com.android.tools.r8.shaking.M.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:1643)
	at com.android.tools.r8.R8.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:1471)
	at com.android.tools.r8.R8.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:233)
	at com.android.tools.r8.R8.b(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:1)
	at com.android.tools.r8.internal.to.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:28)
	Suppressed: java.lang.RuntimeException: com.android.tools.r8.internal.g: Missing class com.google.errorprone.annotations.CanIgnoreReturnValue (referenced from: com.google.crypto.tink.KeysetManager com.google.crypto.tink.KeysetManager.add(com.google.crypto.tink.KeyTemplate) and 52 other contexts)
		at com.android.tools.r8.internal.x50.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:29)
		at com.android.tools.r8.shaking.M.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:1964)
		at com.android.tools.r8.shaking.M.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:1643)
		at com.android.tools.r8.R8.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:1471)
		at com.android.tools.r8.R8.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:233)
		at com.android.tools.r8.R8.b(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:1)
		at com.android.tools.r8.internal.to.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:28)
		at com.android.tools.r8.R8.a(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:79)
		at com.android.tools.r8.R8.run(R8_8.5.35_9c55004e7c41a17b1ed47c4e1952cb6778b3dac6afb6afc113a2737c3cde13e0:3)
		at com.android.builder.dexing.R8Tool.runR8(r8Tool.kt:332)
		at com.android.build.gradle.internal.tasks.R8Task$Companion.shrink(R8Task.kt:782)
		at com.android.build.gradle.internal.tasks.R8Task$R8Runnable.execute(R8Task.kt:856)
	Caused by: [CIRCULAR REFERENCE: com.android.tools.r8.internal.g: Missing class com.google.errorprone.annotations.CanIgnoreReturnValue (referenced from: com.google.crypto.tink.KeysetManager com.google.crypto.tink.KeysetManager.add(com.google.crypto.tink.KeyTemplate) and 52 other contexts)
```
