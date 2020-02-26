package vladiachuk.com.bottomsheet

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout


class BottomSheet(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private var mLayoutId: Int? = null
    var layoutId: Int?
        set(value) {
            mLayoutId = value
            inflateLayout()
            requestLayout()
        }
        get() = mLayoutId

    private lateinit var mView: View
    var view: View
        set(value) {
            mView = value
            removeAllViews()
            requestLayout()
        }
        get() = mView

    var maxPosition = 0f
        set(value) {
            field = value
            if (position > value) position = value
        }

    var minPosition = 0f
        set(value) {
            field = value
            if (position > value) position = value
        }

    var peekHeight = 0f
        set(value) {
            field = value
            val peekPosition = height - peekHeight
            if (maxPosition > peekPosition) {
                maxPosition = height - peekPosition
            }
        }

    var defaultPeekHeight = 0f

    val touchController: TouchController
    val controller: BottomSheetController? = null


    /**
     * Initialization
     */
    init {
        setupAttributes(attrs)
        inflateLayout()

        touchController = TouchController(this)
    }

    private fun setupAttributes(attrs: AttributeSet?) {
        if (attrs == null) return

        val arr = context.obtainStyledAttributes(attrs, R.styleable.BottomSheet, 0, 0)

        if (arr.hasValue(R.styleable.BottomSheet_layout)) {
            mLayoutId = arr.getResourceId(R.styleable.BottomSheet_layout, 0)
        }

        defaultPeekHeight = 70f*3

        arr.recycle()
    }

    private fun inflateLayout() {
        if (mLayoutId == null) return
        removeAllViews()

        mView = LayoutInflater.from(context).inflate(mLayoutId!!, this, false)
        addView(mView)
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        peekHeight = defaultPeekHeight

        maxPosition = height - peekHeight
        if (position < minPosition) position = minPosition
    }

    override fun onStartNestedScroll(child: View?, target: View?, axes: Int): Boolean {
        return touchController.onStartNestedScroll(target, axes)
    }

    override fun onStopNestedScroll(child: View?) {
        super.onStopNestedScroll(child)
        touchController.onStopNestedScroll()
    }

    override fun onNestedPreScroll(target: View?, dx: Int, dy: Int, consumed: IntArray?) {
        touchController.onNestedPreScroll(target, dy, consumed)
    }

    override fun onNestedPreFling(target: View?, velocityX: Float, velocityY: Float): Boolean {
        return touchController.onNestedPreFling(target)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        return e?.let { touchController.onTouch(e) } ?: false
    }


    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        return e?.let { touchController.onInterceptTouch(e) } ?: false
    }

    /**
     * Public methods
     */

    var position: Float
        set(value) {
            mView.y = value
        }
        get() = mView.y

    fun translate(dy: Int) {
        mView.offsetTopAndBottom(dy)
    }
}