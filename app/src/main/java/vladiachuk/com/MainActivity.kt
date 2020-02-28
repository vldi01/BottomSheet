package vladiachuk.com

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout.*
import vladiachuk.com.bottomsheet.BottomSheetController
import vladiachuk.com.bottomsheet.State

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomSheet.controller = BottomSheetController(bottomSheet)

        var cusState: State

        bottomSheet.controller!!.run {
            cus.post {
                cusState = createState(cus)

                possibleStates = arrayListOf(COLLAPSED_STATE, EXPANDED_STATE, HALF_EXPANDED_STATE, cusState)
                statesGraph = arrayListOf(
                    intArrayOf(COLLAPSED_STATE.id, HALF_EXPANDED_STATE.id),
                    intArrayOf(HALF_EXPANDED_STATE.id, COLLAPSED_STATE.id),
                    intArrayOf(EXPANDED_STATE.id, cusState.id),
                    intArrayOf(cusState.id, EXPANDED_STATE.id)
                )

                state = cusState
            }


            btn.setOnClickListener {
                state = nextState
            }

            onStateChangedListener = {
                println(it)
            }
        }

        bottomSheet.onPositionChangedListener = {position ->
            println(position)
        }


    }
}
