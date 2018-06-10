package to.sava.cloudmarksandroid.views.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.realm.RealmRecyclerViewAdapter
import io.realm.RealmResults
import kotlinx.android.synthetic.main.fragment_marks.view.*
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkType
import java.util.*


class MarksRecyclerViewAdapter(value: RealmResults<MarkNode>,
                               private val onClickListener: OnClickListener? = null,
                               private val onLongClickListener: OnLongClickListener? = null)
    : RealmRecyclerViewAdapter<MarkNode, MarksRecyclerViewAdapter.MarksViewHolder>(value, true) {

    interface OnClickListener {
        fun onClick(mark: MarkNode)
    }

    interface OnLongClickListener {
        fun onLongClick(mark: MarkNode): Boolean
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarksViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_marks, parent, false)
        val viewHolder = MarksViewHolder(view)

        viewHolder.marksView.setOnClickListener {
            getItem(viewHolder.adapterPosition)?.let { mark ->
                onClickListener?.onClick(mark)
            }
        }
        viewHolder.marksView.setOnLongClickListener {
            getItem(viewHolder.adapterPosition)?.let { mark ->
                onLongClickListener?.onLongClick(mark)
            } ?: false
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: MarksViewHolder, position: Int) {
        getItem(position)?.let {
            holder.data = it
            holder.contentView.text = it.title
            val (icon, chevron) = when (it.type) {
                MarkType.Folder -> Pair(R.drawable.ic_folder_open_black_24dp, R.drawable.ic_chevron_right_black_24dp)
                MarkType.Bookmark -> Pair(R.drawable.ic_bookmark_border_black_24dp, 0)
            }
            holder.contentView.setCompoundDrawablesWithIntrinsicBounds(icon, 0, chevron, 0)
        }
    }

    override fun getItemId(index: Int): Long {
        val item = getItem(index)
        return when {
            item !== null -> UUID.fromString(item.id).leastSignificantBits
            else -> RecyclerView.NO_ID
        }
    }

    inner class MarksViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val marksView: View = view.marks
        val contentView: TextView = view.content
       lateinit var data: MarkNode
    }
}
