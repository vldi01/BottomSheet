package vladiachuk.com

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_for_bottomsheet.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vladiachuk.com.bottomsheet.BottomSheetController
import vladiachuk.com.bottomsheet.State


class MainActivity : AppCompatActivity() {

    private val firstFragment = FirstFragment()
    private val secondFragment = SecondFragment()
    private var isFirstState = true

    private val controller by lazy { BottomSheetController(bottomSheet) }
    private val customState by lazy { controller.createState(smallSize, true) }

    private var smallSize = 900f
    private var biggerSize = 700f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomSheet.run {
            controller = this@MainActivity.controller
            post {
                smallSize = height * 0.8f
                biggerSize = height * 0.65f
                customState.position = smallSize
            }
        }

        setupFirstState()

        bottom_navigation.setOnItemSelectedListener { onBottomNavigationClick(it) }

        next_state_btn.setOnClickListener { nextState() }

        bottomSheet.reactToOrientationEvent()
        small_state_btn.setOnClickListener {
            customState.position = smallSize
        }
        bigger_state_btn.setOnClickListener {
            customState.position = biggerSize
        }
        demo_btn.setOnClickListener {
            demoMode()
        }
    }

    override fun onBackPressed() {
        bottomSheet.controller?.run {
            if (state != COLLAPSED_STATE)
                setStateAnim(COLLAPSED_STATE)
            else
                super.onBackPressed()
        }
    }


    private fun nextState() {
        bottomSheet.controller?.run { setStateAnim(nextState) }
    }

    private fun onBottomNavigationClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.first_action -> {
                if (isFirstState)
                    nextState()
                else
                    collapseAndOpen(customState)
            }
            R.id.second_action -> {
                if (!isFirstState)
                    nextState()
                else
                    collapseAndOpen(controller.HALF_EXPANDED_STATE)
            }
        }
        return false
    }

    private fun collapseAndOpen(state: State) {
        CoroutineScope(Dispatchers.Main).launch {
            bottomSheet.controller?.run {
                setStateAnimSuspend(COLLAPSED_STATE)
                setupSecondState()
                setStateAnim(state)
            }
        }
    }

    private fun setupFirstState() {
        supportFragmentManager.beginTransaction().replace(R.id.frame, firstFragment).commit()
        bottomSheet.controller?.run {
            possibleStates = arrayListOf(COLLAPSED_STATE, EXPANDED_STATE, customState)
            statesGraph = arrayListOf(
                arrayOf(COLLAPSED_STATE, customState),
                arrayOf(EXPANDED_STATE, COLLAPSED_STATE),
                arrayOf(customState, COLLAPSED_STATE)
            )
        }
        isFirstState = true
    }

    private fun setupSecondState() {
        supportFragmentManager.beginTransaction().replace(R.id.frame, secondFragment).commit()
        bottomSheet.controller?.run {
            possibleStates = arrayListOf(COLLAPSED_STATE, HALF_EXPANDED_STATE, EXPANDED_STATE)
            statesGraph = arrayListOf(
                arrayOf(COLLAPSED_STATE, HALF_EXPANDED_STATE),
                arrayOf(HALF_EXPANDED_STATE, EXPANDED_STATE),
                arrayOf(EXPANDED_STATE, COLLAPSED_STATE)
            )
        }
        isFirstState = false
    }

    private fun demoMode() {
        CoroutineScope(Dispatchers.Main).launch {
            bottomSheet.controller?.let { controller ->
                val lastState = controller.state
                controller.possibleStates.forEach {
                    controller.setStateAnimSuspend(it)
                    delay(500)
                }

                controller.setStateAnimSuspend(lastState)
            }
        }
    }
}
