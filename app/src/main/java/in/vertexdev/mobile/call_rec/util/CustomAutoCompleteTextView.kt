package `in`.vertexdev.mobile.call_rec.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.AutoCompleteTextView

@SuppressLint("AppCompatCustomView")
class CustomAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AutoCompleteTextView(context, attrs, defStyleAttr) {

    // A flag to control dropdown behavior
    private var isDropdownEnabled = true



    override fun dismissDropDown() {
        // Prevent dropdown from closing if it should be enabled
        if (isDropdownEnabled) {
            super.dismissDropDown()
        }
    }

    fun setDropdownEnabled(enabled: Boolean) {
        isDropdownEnabled = enabled
    }
}
