package vladiachuk.com.bottomsheet

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat


class BottomSheet(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private var mLayoutId: Int? = null
    var layoutId: Int?
        set(value) {
            mLayoutId = value
            inflateLayout()
            requestLayout()
        }
        get() = mLayoutId

    private var mView: View? = null
    var view: View?
        set(value) {
            mView = value
            removeAllViews()
            requestLayout()
        }
        get() = mView


    init {
        setupAttributes(attrs)
        inflateLayout()
    }

    private fun setupAttributes(attrs: AttributeSet?) {
        if (attrs == null) return

        val arr = context.obtainStyledAttributes(attrs, R.styleable.BottomSheet, 0, 0)

        if (arr.hasValue(R.styleable.BottomSheet_layout))
            mLayoutId = arr.getResourceId(R.styleable.BottomSheet_layout, 0)

        arr.recycle()
    }

    private fun inflateLayout() {
        if (mLayoutId == null) return
        removeAllViews()

        mView = inflate(context, mLayoutId!!, this)
    }

    override fun onStartNestedScroll(child: View?, target: View?, nestedScrollAxes: Int): Boolean {
        return nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    override fun onNestedPreScroll(target: View?, dx: Int, dy: Int, consumed: IntArray?) {
        view?.let { view ->
            if (view.y - dy < 0) {
                view.y = 0f
            } else {
                if (target!!.scrollY == 0) {
                    consumed!![1] = dy
                    view.offsetTopAndBottom(-dy)
                }
            }
        }
    }

    override fun onNestedPreFling(target: View?, velocityX: Float, velocityY: Float): Boolean {
        if (target == null) return false
        return target.scrollY == 0
    }

    var down = 0f

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        when (e!!.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                down = e.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (view!!.y + e.y - down < 0) {
                    view!!.y = 0f
                } else {
                    view!!.offsetTopAndBottom((e.y - down).toInt())
                }
            }
            MotionEvent.ACTION_UP -> {
            }
        }
        return true
    }
}