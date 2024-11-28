package `in`.vertexdev.mobile.call_rec.rv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import `in`.vertexdev.mobile.call_rec.databinding.LeadItemBinding
import `in`.vertexdev.mobile.call_rec.models.Lead
import `in`.vertexdev.mobile.call_rec.models.LeadCategory

class LeadAdapter(
    private val onCall: (item: Lead) -> Unit,
    private val onCountryClick: (item: Lead) -> Unit,
    private val onIntakeClick: (item: Lead) -> Unit,
    private val onLabelClick: (item: Lead) -> Unit,
    private val onStatusClick: (item: Lead) -> Unit,


) : RecyclerView.Adapter<LeadAdapter.LeadHolder>() {


    var list: List<Lead> = listOf()
    var isLead:Boolean = true


    class LeadHolder(val binding: LeadItemBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup): LeadHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = LeadItemBinding.inflate(layoutInflater, parent, false)
                return LeadHolder(binding)
            }

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeadHolder {
        return LeadHolder.from(parent)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: LeadHolder, position: Int) {
        val currentItem = list[position]
        holder.binding.nameText.text = currentItem.fname

        holder.binding.lastUpdatedText.text = currentItem.reg_date
        holder.binding.cardCountry.setOnClickListener {
            onCountryClick(currentItem)
        }
        holder.binding.iconButtonE.setOnClickListener {
            onCountryClick(currentItem)
        }
        holder.binding.cardIntake.setOnClickListener {
            onIntakeClick(currentItem)
        }

        holder.binding.iconButtonER.setOnClickListener {
            onIntakeClick(currentItem)
        }
        holder.binding.cardLabel.setOnClickListener {
            onLabelClick(currentItem)
        }

        holder.binding.iconButtonEG.setOnClickListener {
            onLabelClick(currentItem)
        }
        holder.binding.cardStatus.setOnClickListener {
            onStatusClick(currentItem)
        }
        holder.binding.iconButtonEJ.setOnClickListener {
                    onStatusClick(currentItem)
                }
                // Set country text only if `goingto_country_short` is not empty or null
        if (!currentItem.goingto_country_short?.trim().isNullOrEmpty()) {
                    holder.binding.countryText.text = currentItem.goingto_country_short
        }
// Set intake text only if `goingto_intake` is not empty or null
        if (!currentItem.goingto_intake?.trim().isNullOrEmpty()) {
            holder.binding.intakeText.text = currentItem.goingto_intake
        }

// Set label text only if `label` is not empty or null
        if (!currentItem.label?.trim().isNullOrEmpty()) {
            holder.binding.labelText.text = currentItem.student_label_name
        }

// Set status text only if `public_student_status` is not empty or null
        if (!currentItem.public_student_status?.trim().isNullOrEmpty()) {
            holder.binding.statusText.text = currentItem.public_student_status
        }

        if (currentItem.student_status == "Followup") {
            holder.binding.followUpText.text =
                "Follow-Up: ${currentItem.fdate} ${currentItem.ftime} "
            holder.binding.followUpCard.visibility = View.VISIBLE
        } else {
            holder.binding.followUpCard.visibility = View.GONE
        }
        holder.binding.callButton.setOnClickListener {
            onCall(currentItem)
        }

//        if(isLead){
//            holder.binding.cardCountry.visibility = View.VISIBLE
//            holder.binding.cardIntake.visibility = View.VISIBLE
//        }else{
//            holder.binding.cardCountry.visibility = View.GONE
//            holder.binding.cardIntake.visibility = View.GONE
//        }
    }

    // Update data and notify the adapter
    fun updateData(newItems: List<Lead>, lead:Boolean) {
        val diffResult = DiffUtil.calculateDiff(LeadDiffUtil(list, newItems))
        isLead = lead

        list = newItems
        diffResult.dispatchUpdatesTo(this)
    }


}

// DiffUtil Callback
private class LeadDiffUtil(
    private val oldList: List<Lead>, private val newList: List<Lead>
) : DiffUtil.Callback() {
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


