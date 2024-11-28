package `in`.vertexdev.mobile.call_rec.rv

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import `in`.vertexdev.mobile.call_rec.databinding.ItemViewBinding
import `in`.vertexdev.mobile.call_rec.models.Items
import `in`.vertexdev.mobile.call_rec.models.LeadCategory

class ItemAdapter(
    private val onClick: (item: LeadCategory) -> Unit,
) : RecyclerView.Adapter<ItemAdapter.ItemHolder>() {

    var list = ArrayList<LeadCategory>()

    class ItemHolder(val binding: ItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup): ItemHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding =
                    ItemViewBinding.inflate(layoutInflater, parent, false)
                return ItemHolder(binding)
            }

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
       return ItemHolder.from(parent)
    }

    override fun getItemCount(): Int {
      return list.size
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
       val currentItem = list[position]
        holder.binding.title.text = currentItem.name
        holder.binding.value.text = currentItem.count

        holder.binding.main.setOnClickListener {
            onClick(currentItem)
        }
    }

    // Update data and notify the adapter
    fun updateData(newItems: ArrayList<LeadCategory>) {
        val diffResult = DiffUtil.calculateDiff(ItemsDiffUtil(list, newItems))

        list = newItems
        diffResult.dispatchUpdatesTo(this)
    }


    // DiffUtil Callback
    private class ItemsDiffUtil(
        private val oldList: List<LeadCategory>,
        private val newList: List<LeadCategory>
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
}

