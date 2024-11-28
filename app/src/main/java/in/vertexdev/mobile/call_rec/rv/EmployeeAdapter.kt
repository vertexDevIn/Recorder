package `in`.vertexdev.mobile.call_rec.rv

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import `in`.vertexdev.mobile.call_rec.models.Employee
import `in`.vertexdev.mobile.call_rec.models.StatusItem
import `in`.vertexdev.mobile.call_rec.util.CustomAutoCompleteTextView


class EmployeeAdapter(
    context: Context,
    var options: List<Employee>,

    private val customAutoCompleteTextView: CustomAutoCompleteTextView,
    private val country: Boolean,
    private val multiSelect:Boolean,
    private val onSelect: (selected:Boolean, item: StatusItem, adapter:OptionAdapter) -> Unit,
) : ArrayAdapter<Employee>(context, 0, options) {

//    // List to keep track of selected items
//    private var selectedItems: ArrayList<StatusItem> = ArrayList()
//
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//        // Get the data item for this position
//        val option = getItem(position)
//
//        // Check if an existing view is being reused, otherwise inflate the view
//        val listItemView = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
//
//        // Lookup view for data population
//        val textView = listItemView.findViewById<TextView>(android.R.id.text1)
//
//        // Populate the data into the template view using the data object
//        textView.text = option?.fname +" " + option?.lname
//
//        val typedValue2 = TypedValue()
//        context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue2, true)
//        val colorPrimary2 = typedValue2.data
//
//        val typedValue3 = TypedValue()
//        context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue3, true)
//        val colorPrimary3 = typedValue3.data
//
//
//
//        textView.setTextColor(if (selectedItems.contains(option)) {
//            colorPrimary2// Color when selected
//        } else {
//            colorPrimary3 // Default color
//        })
//
//        val typedValue = TypedValue()
//        context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
//        val colorPrimary = typedValue.data
//
//        // Set background color based on selection state
//        listItemView.setBackgroundColor(if (selectedItems.contains(option)) {
//            colorPrimary// Color when selected
//        } else {
//            Color.TRANSPARENT // Default color
//        })
//
//        // Set an OnClickListener to handle item selection
//        listItemView.setOnClickListener {
//            if (option != null) {
//                Log.d("TAG", "option:$option ")
//                // Prevent dropdown from closing on selection
//                if(multiSelect){
//                    if (selectedItems.contains(option)) {
//                        // Item is already selected, remove it
//                        selectedItems.remove(option)
//                        onSelect(false,option,this@OptionAdapter)
//                    } else {
//                        // Item is not selected, add it
//                        selectedItems.add(option)
//                        onSelect(true,option,this@OptionAdapter)
//                    }
//                    customAutoCompleteTextView.setDropdownEnabled(false) // Disable dropdown closing
//                }else{
//                    selectedItems.clear()
//                    selectedItems.add(option)
//                    onSelect(true,option,this@OptionAdapter)
//                }
//
//                // Notify the adapter to refresh the view
//                notifyDataSetChanged()
//            }
//        }
//
//        // Return the completed view to render on screen
//        return listItemView
//    }
//
//    // Method to get the selected items
//    fun getSelectedItems(): List<StatusItem> {
//        return selectedItems.toList()
//    }
//
//    fun updateList(list:List<StatusItem>){
//        selectedItems.clear() // Clear existing items if needed
//        selectedItems.addAll(list) // Add all items from the new list
//        notifyDataSetChanged() // Notify the adapter that the data has changed
//
//    }
}