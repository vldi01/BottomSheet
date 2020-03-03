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
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0.40f, bs.resources.displayMetrics)
    var SMALL_SPEED =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0.07f, bs.resources.displayMetrics)

    var MAX_PREV_DISTANCE =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, bs.resources.displayMetrics)
    var MAX_PREV_PERCENTAGE = 0.3f

    var STOP_TIME = 300


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
            onStateChangedListener?.invoke(value)
        }
    var state
        set(value) {
            mState = value
            bs.position = value.position
        }
        get() = mState

    val nextState: State
        get() {
            val nextStateId = statesGraph.first { it[0] == mState.id }[1]
            return possibleStates.first { it.id == nextStateId }
        }

    private var mPrevState: State = mState
    val prevState: State
        get() = mPrevState


    var possibleStates: ArrayList<State> = ArrayList()
    var statesGraph: ArrayList<IntArray> = ArrayList()

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


    private fun getAnim(toPos: Float): ValueAnimator {
        val bsPos = bs.position
        anim.setFloatValues(bs.position, toPos)
        anim.duration =
            (abs(bsPos - toPos) / (bs.height - bs.peekHeight) * (MAX_DURATION - MIN_DURATION) + MIN_DURATION).toLong()
        anim.removeAllListeners()
        return anim
    }

    private fun setPositionAnim(pos: Float) {
        val an = getAnim(pos)
        an.start()
    }

    private suspend fun setPositionAnimSuspend(pos: Float) {
        setPositionAnim(pos)
        suspendCoroutine<Unit> {
            anim.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    it.resume(Unit)
                }

                override fun onAnimationCancel(animation: Animator?) {
                    it.resume(Unit)
                }
            })
        }
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

    suspend fun setStateAnimSuspend(state: State) {
        mState = state
        if (bs.position != state.position)
            setPositionAnimSuspend(state.position)
    }

    fun setStateAnim(state: State) {
        mState = state
        if (bs.position != state.position)
            setPositionAnim(state.position)
    }
}