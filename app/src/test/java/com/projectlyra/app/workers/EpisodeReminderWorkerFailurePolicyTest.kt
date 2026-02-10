package com.projectlyra.app.workers

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeReminderWorkerFailurePolicyTest {
    @Test
    fun classifyWorkerFailure_marksNetworkErrorsRetryable() {
        assertEquals(WorkerFailureAction.RETRY, classifyWorkerFailure(IOException("io")))
        assertEquals(WorkerFailureAction.RETRY, classifyWorkerFailure(UnknownHostException("dns")))
        assertEquals(WorkerFailureAction.RETRY, classifyWorkerFailure(SocketTimeoutException("timeout")))
    }

    @Test
    fun classifyWorkerFailure_marksLogicErrorsNonRetryable() {
        assertEquals(WorkerFailureAction.FAIL, classifyWorkerFailure(IllegalStateException("bug")))
    }
}
