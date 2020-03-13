package vladiachuk.com.bottomsheet.friendlyLayouts

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import vladiachuk.com.bottomsheet.BottomSheet
import vladiachuk.com.bottomsheet.R

class BSFriendlyConstraintLayout : ConstraintLayout {
    private val attrs: AttributeSet?
    private var id: Int? = null
    var bs: BottomSheet? = null

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ): super(context, attrs, defStyleAttr) {
        this.attrs = attrs
        setupAttributes()
    }

    fun setupAttributes() {
        if (attrs == null) return

        val arr = context.obtainStyledAttributes(attrs, R.styleable.BSFriendlyConstraintLayout, 0, 0)

        if (arr.hasValue(R.styleable.BSFriendlyConstraintLayout_bottom_sheet_id)) {
            id = arr.getResourceId(R.styleable.BSFriendlyConstraintLayout_bottom_sheet_id, 0)
        }

        arr.recycle()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (id != null){
            bs = (parent as View).findViewById(id!!)
        }
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        bs?.run {
            bs!!.touchController.onInterceptTouch(e, true)
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        bs?.run {
            return bs!!.touchController.onTouch(e, true)
        }
        return false
    }
}