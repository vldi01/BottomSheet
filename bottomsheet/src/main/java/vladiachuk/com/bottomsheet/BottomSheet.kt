package vladiachuk.com.bottomsheet

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout


open class BottomSheet(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val TAG = "BottomSheet"

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
            if (::mView.isInitialized) removeView(mView)
            mView = value
        }
        get() = mView

    var maxPosition = 0f
        set(value) {
            field = value
            Log.d(TAG, "Max position $value")
            if (position > value) position = value
            controller?.run { COLLAPSED_STATE.position = value }
        }

    var minPosition = 0f
        set(value) {
            field = value
            Log.d(TAG, "Min position $value")
            if (position > value) position = value
            controller?.run { EXPANDED_STATE.position = value }
        }

    var peekHeight = 0f
        set(value) {
            field = value
            Log.d(TAG, "Peek height $value")
            val peekPosition = height - peekHeight
            if (maxPosition > peekPosition) {
                maxPosition = height - peekPosition
            }
        }

    var defaultPeekHeight = 0f

    var onPositionChangedListener: ((position: Float) -> Unit)? = null

    var touchController: TouchController
    var controller: BottomSheetController? = null

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
        if (arr.hasValue(R.styleable.BottomSheet_peekHeight)) {
            defaultPeekHeight = arr.getDimensionPixelOffset(R.styleable.BottomSheet_peekHeight, 0).toFloat()
        }

        arr.recycle()
    }

    private fun inflateLayout() {
        if (mLayoutId == null) return

        mView = LayoutInflater.from(context).inflate(mLayoutId!!, this, false)
        addView(mView)
    }


    /**
     * Overrides
     */
    private var isFirstLoaded = true
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (isFirstLoaded) {
            Log.d(TAG, "OnFirstLoaded")
            if (::mView.isInitialized) {
                mView.bringToFront()
            }

            peekHeight = defaultPeekHeight

            maxPosition = height - peekHeight
            if (position < minPosition) position = minPosition

            controller?.onFirstLoad()
            isFirstLoaded = false
        }
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
            onPositionChangedListener?.invoke(value)
        }
        get() = mView.y

    fun translate(dy: Int) {
        mView.offsetTopAndBottom(dy)
        onPositionChangedListener?.invoke(position)
    }
}