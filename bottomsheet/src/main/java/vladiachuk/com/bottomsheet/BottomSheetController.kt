package vladiachuk.com.bottomsheet

import android.animation.Animator
import android.animation.ValueAnimator
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class BottomSheetController(private val bs: BottomSheet, private val starState: State? = null) {
    private val TAG = "BottomSheetController"

    var MAX_DURATION = 500
    var MIN_DURATION = 50


    val COLLAPSED_STATE = createState(bs.maxPosition)
    val EXPANDED_STATE = createState(bs.minPosition)
    val HALF_EXPANDED_STATE = createState(bs.height / 2f)
    val HIDDEN_STATE = createState(bs.height.toFloat())


    var FAST_SPEED =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.2f, bs.resources.displayMetrics)
    var MEDIUM_SPEED =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0.4f, bs.resources.displayMetrics)
    var SMALL_SPEED =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0.1f, bs.resources.displayMetrics)

    var MAX_PREV_DISTANCE =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, bs.resources.displayMetrics)
    var MAX_PREV_PERCENTAGE = 0.3f

    var STOP_TIME = 300


    var onReload: (() -> Unit)? = null


    private var anim = ValueAnimator().apply {
        interpolator = DecelerateInterpolator(1.5f)
        addUpdateListener {
            bs.position = it.animatedValue as Float
        }
    }

    private var maxStateId = 0

    private var mState = EXPANDED_STATE
        set(value) {
            if (value != field) {
                mPrevState = field
            }

            field = value
            Log.d(TAG, "State $value")
            onStateStartChangingListener?.invoke(value)
        }
    var state
        set(value) {
            mState = value
            bs.position = value.position
        }
        get() = mState

    val nextState: State
        get() {
            return statesGraph.firstOrNull { it[0] == mState }?.get(1) ?: mState
        }

    private var mPrevState: State = mState
    val prevState: State
        get() = mPrevState


    var possibleStates: ArrayList<State> = ArrayList()
    var statesGraph: ArrayList<Array<State>> = ArrayList()

    var onStateStartChangingListener: ((state: State) -> Unit)? = null
    var onStateChangingCanceledListener: (() -> Unit)? = null
    var onStateChangedListener: ((state: State) -> Unit)? = null

    init {
        Log.d(TAG, "Slow: $SMALL_SPEED")
        Log.d(TAG, "Medium: $MEDIUM_SPEED")
        Log.d(TAG, "FAST: $FAST_SPEED")
        @Suppress("LeakingThis")
        bs.touchController.addOnStopDraggingListener(this::onStop)
    }


    /**
     * Private methods
     */
    private fun onStop(speed: Float, stopTime: Int) {
        Log.d(TAG, "======OnStop======")
        Log.d(TAG, "OnStop speed: $speed  stopTime: $stopTime")

        var nextState = this.nextState
        val bsPos = bs.position

        val delta = abs(bsPos - mState.position)
        val absSpeed = abs(speed)

        //probably bottomSheet on this state
        val probableState = possibleStates.firstOrNull { it.position == bsPos }

        nextState = when {
            //BS is on right place already
            (probableState != null) -> {
                Log.d(TAG, "BS is on right place already")
                mState = probableState
                if (state != lastChanged) {
                    onStateChangedListener?.invoke(state)
                    lastChanged = state
                }
                return
            }

            //fast speed
            (absSpeed >= FAST_SPEED) -> {
                Log.d(TAG, "Fast speed")
                if (speed > 0) {
                    possibleStates.maxBy { it.position } ?: mState
                } else {
                    possibleStates.minBy { it.position } ?: mState
                }
            }

            //closest
            (stopTime > STOP_TIME || absSpeed <= SMALL_SPEED) -> {
                Log.d(TAG, "Closest")
                possibleStates.minBy { abs(bsPos - it.position) } ?: mState
            }

            //current
            ((absSpeed <= MEDIUM_SPEED
                    && delta < MAX_PREV_DISTANCE
                    && delta < MAX_PREV_PERCENTAGE * abs(mState.position - nextState.position))
                    || (bsPos > mState.position && speed < 0)
                    || (bsPos < mState.position && speed >= 0)
                    ) -> {
                Log.d(TAG, "Current")
                mState
            }

            //next closest
            (!(bsPos > min(mState.position, nextState.position)
                    && bsPos < max(mState.position, nextState.position))
                    ) -> {
                Log.d(TAG, "Next closest")
                if (speed > 0) {
                    possibleStates.filter { it.position >= bsPos }.minBy { abs(bsPos - it.position) }
                        ?: mState
                } else {
                    possibleStates.filter { it.position <= bsPos }.minBy { abs(bsPos - it.position) }
                        ?: mState
                }
            }

            else -> {
                Log.d(TAG, "Next state")
                nextState
            }
        }

        setStateAnim(nextState)
    }


    private fun setupAnim(toPos: Float) {
        val bsPos = bs.position
        anim.setFloatValues(bs.position, toPos)
        anim.duration =
            (abs(bsPos - toPos) / (bs.height - bs.peekHeight) * (MAX_DURATION - MIN_DURATION) + MIN_DURATION).toLong()
        anim.removeAllListeners()
    }

    /**
     * Public methods
     */
    fun reload() {
        COLLAPSED_STATE.position = bs.maxPosition
        EXPANDED_STATE.position = bs.minPosition
        HALF_EXPANDED_STATE.position = bs.height / 2f
        HIDDEN_STATE.position = bs.height.toFloat()

        state = COLLAPSED_STATE
        onReload?.invoke()
    }

    fun createStateByHeight(height: Float): State {
        return State(maxStateId++, bs.height - height)
    }

    fun createState(position: Float): State {
        return State(maxStateId++, position)
    }

    fun createState(view: View): State {
        return createState(with(view) { bs.height - y - height })
    }

    fun createState(viewId: Int): State {
        return createState(bs.findViewById<View>(viewId))
    }

    private fun onAnimFinished() {
        if (bs.position == state.position && state != lastChanged) {
            onStateChangedListener?.invoke(state)
            lastChanged = state
        }
    }
    private fun onAnimStarted() {
        lastChanged = null
    }

    private var lastChanged: State? = null
    fun setPositionAnim(pos: Float, duration: Int = -1) {
        anim.cancel()
        setupAnim(pos)
        if (duration > 0)
            anim.duration = duration.toLong()
        anim.start()
        onAnimStarted()
        anim.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                onAnimFinished()
            }

            override fun onAnimationCancel(animation: Animator?) {
                onStateChangingCanceledListener?.invoke()
                onAnimFinished()
            }
        })
    }

    suspend fun setPositionAnimSuspend(pos: Float, duration: Int = -1) {
        setPositionAnim(pos, duration)
        suspendCoroutine<Unit> {
            anim.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    try {
                        it.resume(Unit)
                        onAnimFinished()
                    } catch (ignore: Exception) {
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {
                    try {
                        it.resume(Unit)
                        onStateChangingCanceledListener?.invoke()
                        onAnimFinished()
                    } catch (ignore: Exception) {
                    }
                }
            })
        }
    }

    suspend fun setStateAnimSuspend(state: State, duration: Int = -1) {
        mState = state
        anim.cancel()
        if (bs.position != state.position)
            setPositionAnimSuspend(state.position, duration)
    }

    fun setStateAnim(state: State, duration: Int = -1) {
        mState = state
        anim.cancel()
        if (bs.position != state.position)
            setPositionAnim(state.position, duration)
        else if (state != lastChanged) {
            onStateChangedListener?.invoke(state)
            lastChanged = state
        }
    }
}