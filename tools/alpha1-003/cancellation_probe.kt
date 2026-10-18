// Control-flow reproducer ONLY. No candidate code, JNI, GGUF or actual resource allocation.
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

fun main() = runBlocking {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { worker ->
        val allocated = AtomicBoolean(false)
        val delivered = AtomicBoolean(false)
        val entered = CountDownLatch(1)
        val returnAllowed = CountDownLatch(1)
        val task = launch(Dispatchers.Default) {
            withContext(worker) {
                allocated.set(true) // synthetic ownership marker only
                entered.countDown()
                check(returnAllowed.await(5, TimeUnit.SECONDS))
                "synthetic-handle"
            }
            delivered.set(true)
        }
        check(entered.await(5, TimeUnit.SECONDS))
        task.cancel()
        returnAllowed.countDown()
        task.join()
        check(allocated.get() && !delivered.get())
        allocated.set(false) // dispose the fake marker, not a native release proof
        println("PASS: cancelled withContext return drops synthetic handle delivery; no JNI")
    }
}
