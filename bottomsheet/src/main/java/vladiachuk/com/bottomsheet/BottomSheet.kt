package vladiachuk.com.bottomsheet

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.hardware.SensorManager
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.View
import android.widget.FrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


open class BottomSheet : FrameLayout {
    companion object {
        private const val TAG = "BottomSheet"
    }

    var orientationListener: OrientationEventListener? = null

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
        }

    var minPosition = 0f
        set(value) {
            field = value
            Log.d(TAG, "Min position $value")
            if (position < value) position = value
        }

    var peekHeight = 0f
        set(value) {
            field = value
            Log.d(TAG, "Peek height $value")
            val peekPosition = height - peekHeight
            if (maxPosition > peekPosition) {
                maxPosition = peekPosition
            }
        }

    var defaultPeekHeight = 0f

    var onPositionChangedListeners = ListenerHolder<Float>()

    lateinit var touchController: TouchController
    var controller: BottomSheetController? = null

    /**
     * Initialization
     */
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(
        context, attrs, defStyleAttr
    ) {
        initialize(attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context, attrs, defStyleAttr, defStyleRes
    ) {
        initialize(attrs)
    }

    private fun initialize(attrs: AttributeSet?) {
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
            defaultPeekHeight = arr.getDimensionPixelOffset(R.styleable.BottomSheet_peekHeight, 0)
                .toFloat()
        }

        arr.recycle()
    }

    private fun inflateLayout() {
        if (mLayoutId == null) return

        mView = LayoutInflater.from(context)
            .inflate(mLayoutId!!, this, false)
        mView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (v.y != posShouldBe) {
                mView.y = posShouldBe
            }
        }
        addView(mView)
    }

    /**
     * Overrides
     */
    private var isFirstLoaded = true
    private var lastHeightOnLayout = 0
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        if (newConfig?.orientation == Configuration.ORIENTATION_PORTRAIT || newConfig?.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            if (::mView.isInitialized) {
                mView.post { // view.post is needed, otherwise View.height might be incorrect
                    val positionBefore = position
                    reload()
                    if (position != positionBefore) {
                        position = positionBefore
                    }
                    if (lastHeightOnLayout != height) {
                        lastHeightOnLayout = height
                        maxPosition = height - peekHeight
                        controller?.fixHiddenState()
                    }
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (lastHeight != height) {
            lastHeight = height
            reload()
        }
    }

    fun reload() {
        if (::mView.isInitialized) {
            mView.bringToFront()
        }

        peekHeight = defaultPeekHeight

        maxPosition = height - peekHeight
        if (position < minPosition) position = minPosition

        controller?.reload()
        isFirstLoaded = false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        orientationListener?.disable()
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

    private var posShouldBe: Float = 0f
    var position: Float
        set(value) {
            posShouldBe = value
            mView.y = value
            onPositionChangedListeners.notify(value)
        }
        get() = mView.y

    fun translate(dy: Int) {
        mView.offsetTopAndBottom(dy)
        onPositionChangedListeners.notify(position)
    }

    private var lastHeight = 0
    private var lastOrientation = -1
    fun reactToOrientationEvent(
        isReact: Boolean = true,
        onOrientationChanged: (() -> Unit)? = null
    ) {
        if (isReact) {
            orientationListener = object :
                OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
                override fun onOrientationChanged(orientation: Int) {
                    if (lastOrientation == -1 || lastOrientation % 180 == orientation % 180) {
                        lastHeight = height
                        lastOrientation = orientation
                        return
                    }
                    lastOrientation = orientation

                    CoroutineScope(Dispatchers.Main).launch {
                        for (i in 0 until 20) {
                            if (height != lastHeight) {
                                lastHeight = height
                                minPosition = 0f
                                reload()
                            }
                            delay(500)
                        }
                    }
                    onOrientationChanged?.invoke()
                }
            }

            if (orientationListener!!.canDetectOrientation()) {
                orientationListener!!.enable()
            } else {
                orientationListener!!.disable()
            }
        } else {
            orientationListener?.disable()
        }
    }
}
