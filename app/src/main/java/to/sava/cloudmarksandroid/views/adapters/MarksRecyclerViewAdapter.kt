package to.sava.cloudmarksandroid.views.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.realm.RealmRecyclerViewAdapter
import io.realm.RealmResults
import to.sava.cloudmarksandroid.fragments.MarksFragment.OnListItemClickedListener
import kotlinx.android.synthetic.main.fragment_marks.view.*
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkType
import java.util.*


class MarksRecyclerViewAdapter(
        value: RealmResults<MarkNode>,
        private val itemClickedListener: OnListItemClickedListener?)
    : RealmRecyclerViewAdapter<MarkNode, MarksRecyclerViewAdapter.MarksViewHolder>(value, true) {

    private val onClickListener: View.OnClickListener

    init {
        onClickListener = View.OnClickListener { v ->
            val item = v.tag as MarkNode
            itemClickedListener?.onListItemClicked(item)
        }
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarksViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_marks, parent, false)
        val holder = MarksViewHolder(view)
        holder.marksView.setOnClickListener {
            val mark = getItem(holder.adapterPosition)
            mark?.let {
                itemClickedListener?.onListItemClicked(mark)
            }
        }
        return holder
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
