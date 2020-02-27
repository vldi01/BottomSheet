package vladiachuk.com.bottomsheet

import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat

open class TouchController(private val bs: BottomSheet) {
    private var isScrolling = false
    private var isDragging = false

    private val onStartListeners = ArrayList<() -> Unit>()
    private val onDragListeners = ArrayList<(speed: Float) -> Unit>()
    private val onStopListeners = ArrayList<(speed: Float) -> Unit>()

    /**
     * Private methods
     */
    private var touchDown = 0f

    private fun delegateDrag(e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDown = e.y
                if (e.y < bs.position) touchDown = -1f
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchDown < 0) {
                    onNotDrag()
                    return
                }
                val dy = e.y - touchDown
                when {
                    bs.position + dy < bs.minPosition -> {
                        bs.position = bs.minPosition
                        onNotDrag()
                    }
                    bs.position + dy > bs.maxPosition -> {
                        bs.position = bs.maxPosition
                        onNotDrag()
                    }
                    else -> {
                        bs.translate(dy.toInt())
                        onDrag(dy)
                    }
                }

                touchDown = e.y
            }
            MotionEvent.ACTION_UP -> {
                onNotDrag()
            }
        }
    }


    /**
     * Nested events
     */
    fun onStartNestedScroll(target: View?, axes: Int): Boolean {
        isScrolling = true
        return axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    fun onStopNestedScroll() {
        isScrolling = false
        onNotDrag()
    }

    fun onNestedPreScroll(target: View?, dy: Int, consumed: IntArray?) {
        if (bs.position - dy < bs.minPosition) {
            bs.position = bs.minPosition
            isScrolling = false
        } else if (target!!.scrollY == 0) {
            isScrolling = true
            consumed!![1] = dy

            if (bs.position - dy > bs.maxPosition) {
                bs.position = bs.maxPosition
                onNotDrag()
            } else {
                bs.translate(-dy)
                onDrag(-dy.toFloat())
            }
        }
    }

    fun onNestedPreFling(target: View?): Boolean {
        return target?.scrollY == 0
    }


    /**
     * Touch events
     */
    fun onTouch(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (e.y < bs.position) return false
            }
        }
        delegateDrag(e)
        return true
    }

    fun onInterceptTouch(e: MotionEvent): Boolean {
        if (isScrolling) return false
        delegateDrag(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> return false
            MotionEvent.ACTION_MOVE -> if (touchDown < 0) return false
        }
        return true
    }

    /**
     * Listeners control
     */

    private var lastTime = 0L
    private var lastSpeed = 0f
    private fun onDrag(dy: Float) {
        if (!isDragging) {
            isDragging = true
            lastTime = System.currentTimeMillis()
            onStartListeners.forEach { it.invoke() }
        } else {
            val curTime = System.currentTimeMillis()
            lastSpeed = dy / (curTime - lastTime)
            lastTime = curTime
            onDragListeners.forEach { it.invoke(lastSpeed) }
        }
    }

    private fun onNotDrag() {
        if (isDragging) {
            isDragging = false
            val timeDelta = System.currentTimeMillis() - lastTime
            onStopListeners.forEach { it.invoke(if (timeDelta > 0) lastSpeed / timeDelta else lastSpeed) }
        }
    }

    /**
     * Listeners adding, removing
     */

    fun addOnStartDraggingListener(listener: () -> Unit) = onStartListeners.add(listener)

    fun addOnStopDraggingListener(listener: (speed: Float) -> Unit) = onStopListeners.add(listener)
    fun addOnDragListener(listener: (speed: Float) -> Unit) = onDragListeners.add(listener)

    fun removeOnStartDraggingListener(listener: () -> Unit) = onStartListeners.remove(listener)
    fun removeOnStopDraggingListener(listener: (speed: Float) -> Unit) =
        onStopListeners.remove(listener)

    fun removeOnDragListener(listener: (speed: Float) -> Unit) = onDragListeners.remove(listener)

}