package vladiachuk.com.bottomsheet

import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

open class TouchController(private val bs: BottomSheet) {
    private val MAX_LAST_SPEEDS_COUNT = 5
    var MIN_DRAG_PIXELS = 10

    private var isScrolling = false
    private var isDragging = false

    private val onStartListeners = ArrayList<() -> Unit>()
    private val onDragListeners = ArrayList<(speed: Float) -> Unit>()
    private val onStopListeners = ArrayList<(speed: Float, stopTime: Int) -> Unit>()

    private var touchDown = 0f
    private var badTouchDown = false

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
        } else if ((target !is RecyclerView && target!!.scrollY == 0)
            || (target is RecyclerView && isRecyclerScrollZero(target))
        ) {
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
        return if (target !is RecyclerView)
            target?.scrollY == 0
        else
            isRecyclerScrollZero(target)
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
        val isDrag = delegateDrag(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> return false
            MotionEvent.ACTION_MOVE -> if (touchDown < 0) return false
            MotionEvent.ACTION_UP -> return false
            MotionEvent.ACTION_CANCEL -> return false
        }
        return isDrag
    }

    /**
     * Listeners control
     */
    private var lastTime = 0L

    private var lastSpeed = 0f
    private var lastSpeeds = Array(MAX_LAST_SPEEDS_COUNT) { 0f }
    private var lastSpeedsCount = 0
    private var lastSpeedsIndex = 0


    private fun delegateDrag(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDown = e.y
                badTouchDown = e.y < bs.position
            }
            MotionEvent.ACTION_MOVE -> {
                if (badTouchDown) {
                    onNotDrag()
                    return false
                }
                val dy = e.y - touchDown
                if (!isDragging && abs(dy) < MIN_DRAG_PIXELS) return false
                when {
                    bs.position + dy < bs.minPosition -> {
                        bs.position = bs.minPosition
                    }
                    bs.position + dy > bs.maxPosition -> {
                        bs.position = bs.maxPosition
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
                return false
            }
        }
        return true
    }

    private fun onDrag(dy: Float) {
        if (!isDragging) {
            isDragging = true
            lastTime = System.currentTimeMillis()
            onStartListeners.forEach { it.invoke() }
        } else {
            val curTime = System.currentTimeMillis()
            val timeDelta = curTime - lastTime
            if (timeDelta == 0L || dy == 0f) {
                return
            }

            lastSpeed = dy / timeDelta

            lastSpeeds[lastSpeedsIndex++ % MAX_LAST_SPEEDS_COUNT] = abs(lastSpeed)
            if (lastSpeedsCount < MAX_LAST_SPEEDS_COUNT) lastSpeedsCount++

            lastTime = curTime

            onDragListeners.forEach { it.invoke(lastSpeed) }
        }
    }

    private fun onNotDrag() {
        if (isDragging) {
            isDragging = false
            val timeDelta = System.currentTimeMillis() - lastTime

            val speed = lastSpeeds.sum() / lastSpeedsCount * (lastSpeed / abs(lastSpeed))
            onStopListeners.forEach { it.invoke(speed, timeDelta.toInt()) }

            lastSpeedsCount = 0
            lastSpeedsIndex = 0
            lastSpeeds.fill(0f)
        }
    }

    /**
     * Listeners adding, removing
     */

    fun addOnStartDraggingListener(listener: () -> Unit) = onStartListeners.add(listener)

    fun addOnStopDraggingListener(listener: (speed: Float, stopTime: Int) -> Unit) =
        onStopListeners.add(listener)

    fun addOnDragListener(listener: (speed: Float) -> Unit) = onDragListeners.add(listener)

    fun removeOnStartDraggingListener(listener: () -> Unit) = onStartListeners.remove(listener)
    fun removeOnStopDraggingListener(listener: (speed: Float, stopTime: Int) -> Unit) =
        onStopListeners.remove(listener)

    fun removeOnDragListener(listener: (speed: Float) -> Unit) = onDragListeners.remove(listener)


    /**
     * Private methods
     */
    private fun isRecyclerScrollZero(target: RecyclerView): Boolean {
        if (target.childCount == 0) return true

        val view = target.layoutManager?.findViewByPosition(0)
        return if (view != null) (view.y - target.paddingTop - view.marginTop) >= 0f
        else false
    }

}