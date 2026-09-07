package aura.core.orchestrator

import aura.core.Complexity
import aura.core.orchestrator.ComplexityRouter.ExecutionPath
import aura.core.orchestrator.IntentRouter.SimpleIntent
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RoutersTest {

    @Test
    fun simpleCalculation_isDeterministicWithoutLlm() {
        val c = IntentRouter.classify("calculate 2 + 2")
        assertThat(c.complexity).isEqualTo(Complexity.SIMPLE)
        assertThat(c.simpleIntent).isEqualTo(SimpleIntent.CALCULATE)
        assertThat(ComplexityRouter.pathFor(c.complexity)).isEqualTo(ExecutionPath.DETERMINISTIC)
    }

    @Test
    fun listFiles_isSimple() {
        val c = IntentRouter.classify("list files in the project")
        assertThat(c.complexity).isEqualTo(Complexity.SIMPLE)
        assertThat(c.simpleIntent).isEqualTo(SimpleIntent.LIST_FILES)
    }

    @Test
    fun researchRequest_isMedium() {
        val c = IntentRouter.classify("research the history of the printing press")
        assertThat(c.complexity).isEqualTo(Complexity.MEDIUM)
    }

    @Test
    fun complexGoal_isComplex() {
        val c = IntentRouter.classify(
            "build an application, analyze the dataset with python and generate a PDF report"
        )
        assertThat(c.complexity).isEqualTo(Complexity.COMPLEX)
        assertThat(ComplexityRouter.pathFor(c.complexity)).isEqualTo(ExecutionPath.MULTI_AGENT)
    }

    @Test
    fun emptyText_doesNotThrow() {
        val c = IntentRouter.classify("   ")
        assertThat(c.complexity).isEqualTo(Complexity.SIMPLE)
    }
}
