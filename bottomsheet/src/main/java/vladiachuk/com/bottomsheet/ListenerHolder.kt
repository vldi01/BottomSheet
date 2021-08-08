package vladiachuk.com.bottomsheet

/**
 * Used to hold multiple listeners (Units) for events
 * @param T is used for listener's "input", if no value is needed, you can use [Unit]
 */
class ListenerHolder<T> {

    /**
     * [Set] is used, so listeners are not accidentally duplicated
     */
    private val listenerList: MutableSet<(T) -> Unit> = hashSetOf()

    fun add(listener: (value: T) -> Unit) {
        listenerList.add(listener)
    }

    fun remove(listener: (value: T) -> Unit) {
        listenerList.remove(listener)
    }

    fun removeAll() {
        listenerList.clear()
    }

    /**
     * Will invoke all added listeners
     */
    fun notify(value: T) {
        listenerList.forEach { it.invoke(value) }
    }
}