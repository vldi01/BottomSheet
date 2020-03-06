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
            bottomSheet.post {
                cusState = createState(cus)

                possibleStates = arrayListOf(COLLAPSED_STATE, HALF_EXPANDED_STATE, EXPANDED_STATE, cusState)
                statesGraph = arrayListOf(
                    arrayOf(COLLAPSED_STATE, HALF_EXPANDED_STATE),
                    arrayOf(HALF_EXPANDED_STATE, COLLAPSED_STATE),
                    arrayOf(EXPANDED_STATE, cusState),
                    arrayOf(cusState, EXPANDED_STATE)
                )
                state = cusState
            }


            btn.setOnClickListener {
                setStateAnim(nextState)
            }

            scbtn.setOnClickListener {
                state = prevState
            }
        }
    }
}
