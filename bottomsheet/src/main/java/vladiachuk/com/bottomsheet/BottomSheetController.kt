package vladiachuk.com.bottomsheet

import android.animation.Animator
import android.animation.ValueAnimator
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class BottomSheetController(private val bs: BottomSheet) {
    private val MAX_DURATION = 400
    private val MIN_DURATION = 50


    var FAST_SPEED =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, bs.resources.displayMetrics)
    var MEDIUM_SPEED =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0.15f, bs.resources.displayMetrics)
    var SMALL_SPEED = 0.05f

    var MAX_PREV_DISTANCE =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, bs.resources.displayMetrics)
    var MAX_PREV_PERCENTAGE = 0.3f

    val COLLAPSED_STATE = createState(bs.maxPosition)
    val EXPANDED_STATE = createState(bs.minPosition)
    val HALF_EXPANDED_STATE = createState(bs.height / 2f)


    private var anim = ValueAnimator().apply {
        interpolator = DecelerateInterpolator(1.5f)
        addUpdateListener {
            bs.position = it.animatedValue as Float
        }
    }

    private var maxStateId = 0

    private var mState = EXPANDED_STATE
    var state
        set(value) {
            mState = value
            if (bs.position != value.position)
                setPositionAnim(value.position)
        }
        get() = mState

    var possibleStates: ArrayList<State> = ArrayList()
    var statesGraph: ArrayList<IntArray> = ArrayList()

    init {
        @Suppress("LeakingThis")
        bs.touchController.addOnStopDraggingListener(this::onStop)
    }


    /**
     * Private methods
     */
    private fun onStop(speed: Float) {
        println(speed)
        var nextState = this.nextState
        val bsPos = bs.position

        val delta = abs(bsPos - mState.position)
        val absSpeed = abs(speed)
        nextState = when {
            //no need to change smth
            (possibleStates.firstOrNull { it.position == bsPos } != null) -> {
                println("No need to ch")
                return
            }

            //fast speed
            (absSpeed >= FAST_SPEED) -> {
                println("Fast speed")
                if (speed > 0) {
                    possibleStates.maxBy { it.position } ?: mState
                } else {
                    possibleStates.minBy { it.position } ?: mState
                }
            }

            //closest
            (absSpeed < SMALL_SPEED) -> {
                println("Closest")
                possibleStates.minBy { abs(bsPos - it.position) } ?: mState
            }

            //previous
            ((absSpeed <= MEDIUM_SPEED
                    && delta < MAX_PREV_DISTANCE
                    && delta < MAX_PREV_PERCENTAGE * abs(mState.position - nextState.position))
                    || (bsPos > mState.position && speed < 0)
                    || (bsPos < mState.position && speed >= 0)
                    ) -> {
                println("Previous")
                mState
            }

            //next closes
            (!(bsPos > min(mState.position, nextState.position)
                    && bsPos < max(mState.position, nextState.position))
                    ) -> {
                println("Next closes")
                if (speed > 0) {
                    possibleStates.filter { it.position >= bsPos }.minBy { abs(bsPos - it.position) }
                        ?: mState
                } else {
                    possibleStates.filter { it.position <= bsPos }.minBy { abs(bsPos - it.position) }
                        ?: mState
                }
            }

            else -> {
                println("Next")
                nextState
            }
        }

        state = nextState
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
    fun createState(position: Float): State {
        println(position)
        return State(maxStateId++, position)
    }

    fun createState(view: View): State {
        return createState(with(view) { bs.height - y - height })
    }

    fun createState(viewId: Int): State {
        return createState(bs.findViewById<View>(viewId))
    }

    val nextState: State
        get() {
            val nextStateId = statesGraph.first { it[0] == mState.id }[1]
            return possibleStates.first { it.id == nextStateId }
        }

    suspend fun setStateSuspend(state: State) {
        mState = state
        if (bs.position != state.position)
            setPositionAnimSuspend(state.position)
    }
}