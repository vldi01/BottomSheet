package vladiachuk.com

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import vladiachuk.com.bottomsheet.BottomSheetController

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomSheet.controller = BottomSheetController(bottomSheet)

        bottomSheet.controller!!.run {
            possibleStates = arrayListOf(COLLAPSED_STATE, EXPANDED_STATE, HALF_EXPANDED_STATE)
            statesGraph = arrayListOf(
                intArrayOf(COLLAPSED_STATE.id, HALF_EXPANDED_STATE.id),
                intArrayOf(HALF_EXPANDED_STATE.id, COLLAPSED_STATE.id),
                intArrayOf(EXPANDED_STATE.id, COLLAPSED_STATE.id)
            )
        }
    }
}
