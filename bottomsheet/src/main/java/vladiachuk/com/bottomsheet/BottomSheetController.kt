package vladiachuk.com.bottomsheet

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class BottomSheetController(private val bs: BottomSheet) {
    private val MAX_DURATION = 400

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
            setPositionAnim(value.position)
        }
        get() = mState

    var possibleStates: ArrayList<State> = ArrayList()
    var statesGraph: ArrayList<IntArray> = ArrayList()

    init {
        bs.touchController.addOnStartDraggingListener(this::onStart)
        bs.touchController.addOnDragListener(this::onDrag)
        bs.touchController.addOnStopDraggingListener(this::onStop)
    }


    /**
     * Private methods
     */
    private fun onStart() {

    }

    private fun onStop(speed: Float) {
        var nextState = this.nextState
        val bsPos = bs.position

        val delta = abs(bsPos - mState.position)
        nextState = when {
            //closest
            (abs(speed) < 0.1) -> {
                possibleStates.filter { it != mState }.minBy { abs(bsPos - it.position) } ?: mState
            }

            //previous
            ((abs(speed) < 1
                    && delta < 200
                    && delta < 0.3f * abs(mState.position - nextState.position))
                    || (bsPos > mState.position && speed < 0)
                    || (bsPos < mState.position && speed >= 0)
                    ) -> {
                mState
            }

            //next closes
            (!(bsPos > min(mState.position, nextState.position)
                    && bsPos < max(mState.position, nextState.position))
                    ) -> {
                if (speed > 0) {
                    possibleStates.filter { it.position > bsPos }.minBy { abs(bsPos - it.position) }
                        ?: mState
                } else {
                    possibleStates.filter { it.position < bsPos }.minBy { abs(bsPos - it.position) }
                        ?: mState
                }
            }

            else -> nextState
        }

        mState = nextState
    }

    private fun onDrag(speed: Float) {

    }

    private fun getAnim(toPos: Float): ValueAnimator {
        val bsPos = bs.position
        anim.setFloatValues(bs.position, toPos)
        anim.duration = (abs(bsPos - toPos) / bs.height * MAX_DURATION).toLong()
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
        return State(maxStateId++, position)
    }

    val nextState: State
        get() {
            val nextStateId = statesGraph.first { it[0] == mState.id }[1]
            return possibleStates.first { it.id == nextStateId }
        }

    suspend fun setStateSuspend(state: State) {
        mState = state
        setPositionAnimSuspend(state.position)
    }
}