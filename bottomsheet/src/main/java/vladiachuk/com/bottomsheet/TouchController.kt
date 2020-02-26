package vladiachuk.com.bottomsheet

import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat

class TouchController(private val bs: BottomSheet) {
    private var isDragging = false

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
                if (touchDown < 0) return
                val dy = e.y - touchDown
                when {
                    bs.position + dy < bs.minPosition -> bs.position = bs.minPosition
                    bs.position + dy > bs.maxPosition -> bs.position = bs.maxPosition
                    else -> bs.translate(dy.toInt())
                }

                touchDown = e.y
            }
            MotionEvent.ACTION_UP -> {
            }
        }
    }

    /**
     * Nested events
     */
    fun onStartNestedScroll(target: View?, axes: Int): Boolean {
        isDragging = true
        return axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    fun onStopNestedScroll() {
        isDragging = false
    }

    fun onNestedPreScroll(target: View?, dy: Int, consumed: IntArray?) {
        if (bs.position - dy < bs.minPosition) {
            bs.position = bs.minPosition
            isDragging = false
        } else if (target!!.scrollY == 0) {
            isDragging = true
            consumed!![1] = dy

            if (bs.position - dy > bs.maxPosition) bs.position = bs.maxPosition
            else bs.translate(-dy)
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
                touchDown = e.y
            }
        }
        delegateDrag(e)
        return true
    }

    fun onInterceptTouch(e: MotionEvent): Boolean {
        if (isDragging) return false
        delegateDrag(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchDown < 0) return false
            }
        }
        return true
    }

    /**
     * Listeners
     */
}