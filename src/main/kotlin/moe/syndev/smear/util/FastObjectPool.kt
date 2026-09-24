package moe.syndev.smear.util

import com.intellij.openapi.Disposable

class FastObjectPool<T : Any>(
    private val capacity: Int = 32,
    private val activator: () -> T,
) : Disposable {

    private val freeList = ArrayDeque<T>(capacity)
    private var isDisposed = false

    init {
        repeat(capacity) {
            freeList.addLast(activator())
        }
    }

    fun rent(): T {
        check(!isDisposed) { "Cannot rent from a disposed pool" }
        val item = freeList.removeLastOrNull() ?: activator()
        if (item is IPooledObject) {
            item.isPooled = true
        }
        return item
    }

    fun recycle(item: T) {
        if (isDisposed) return
        if (item is IPooledObject) item.reset()
        if (freeList.size < capacity) {
            freeList.addLast(item)
        }
    }

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        freeList.forEach { if (it is Disposable) it.dispose() }
        freeList.clear()
    }
}