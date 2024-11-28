package `in`.vertexdev.mobile.call_rec.rv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import `in`.vertexdev.mobile.call_rec.databinding.CallLogItemBinding
import `in`.vertexdev.mobile.call_rec.models.Folder
import `in`.vertexdev.mobile.call_rec.room.entitiy.UploadedCallLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallLogsRv(
    private val onViewError: (error: String) -> Unit
) : RecyclerView.Adapter<CallLogsRv.CallLogHolder>() {

    private var list: List<UploadedCallLog> = arrayListOf()


    class CallLogHolder(val binding: CallLogItemBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup): CallLogHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding =
                    CallLogItemBinding.inflate(layoutInflater, parent, false)
                return CallLogHolder(binding)
            }

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogHolder {
        return CallLogHolder.from(parent)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: CallLogHolder, position: Int) {
        val currentItem = list[position]
        holder.binding.nameText.text = currentItem.name
        if(currentItem.callDuration.isNotEmpty()){
            holder.binding.durationTxt.text = convertSecondsToMinutesSeconds(currentItem.callDuration.toInt())
        }else{
            holder.binding.durationTxt.text = "00:00"
        }

        holder.binding.lastUpdatedText.text = convertTimestampToFormattedString(currentItem.callStarted)
        holder.binding.statusText.text = currentItem.callType
        holder.binding.statusFileUploaded.text = if(currentItem.fileUploaded){
            "File Uploaded"
        }else{
            "File Not Uploaded"
        }
        holder.binding.attempt.text = currentItem.fileUploadAttempts.toString() +" Attempts"

        if(currentItem.lastAttemptError.isNotEmpty() ){
            if( currentItem.lastAttemptError =="null"){
                holder.binding.errorButton.visibility = View.GONE
            }else{
                holder.binding.errorButton.visibility = View.VISIBLE
            }

        }else{
            holder.binding.errorButton.visibility = View.INVISIBLE
        }
        holder.binding.errorButton.setOnClickListener {
            onViewError(currentItem.lastAttemptError)
        }


    }


    // Update data and notify the adapter
    fun updateData(newItems: List<UploadedCallLog>) {
        val diffResult = DiffUtil.calculateDiff(CallLogDiffUtil(list, newItems))

        list = newItems
        diffResult.dispatchUpdatesTo(this)
    }
    fun convertTimestampToFormattedString(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("hh:mm a dd MMM", Locale.getDefault())
        val date = Date(timestamp)
        return dateFormat.format(date)
    }

    fun convertSecondsToMinutesSeconds(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

}

// DiffUtil Callback
private class CallLogDiffUtil(
    private val oldList: List<UploadedCallLog>,
    private val newList: List<UploadedCallLog>
) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}


