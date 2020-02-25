package vladiachuk.com.bottomsheet

import android.content.Context
import android.util.AttributeSet
import android.view.View

class BottomSheet(context: Context, attr: AttributeSet? = null): View(context, attr) {
    fun sayHello(){
        println("Hello")
    }
}