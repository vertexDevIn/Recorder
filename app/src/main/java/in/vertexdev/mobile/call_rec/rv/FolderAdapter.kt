package `in`.vertexdev.mobile.call_rec.rv

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

import `in`.vertexdev.mobile.call_rec.databinding.FolderItemBinding
import `in`.vertexdev.mobile.call_rec.models.Folder


class FolderAdapter(
    private val onClick: (folder: Folder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderHolder>() {



    private var list :List<Folder> = ArrayList()


    class FolderHolder( val binding: FolderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup): FolderHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding =
                    FolderItemBinding.inflate(layoutInflater, parent, false)
                return FolderHolder(binding)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderHolder {
        return FolderHolder.from(parent)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: FolderHolder, position: Int) {
        val currentItem  = list[position]

        holder.binding.folderName.text = currentItem.name
        holder.binding.itemCount.text = currentItem.itemCount.toString() +" items"
        holder.binding.folderCard.setOnClickListener {
            onClick(currentItem)
        }

    }

    // Update data and notify the adapter
    fun updateData(newItems: List<Folder>) {
        val diffResult = DiffUtil.calculateDiff(FolderDiffUtil(list, newItems))

        list = newItems
        diffResult.dispatchUpdatesTo(this)
    }

}
// DiffUtil Callback
private class FolderDiffUtil(
    private val oldList: List<Folder>,
    private val newList: List<Folder>
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