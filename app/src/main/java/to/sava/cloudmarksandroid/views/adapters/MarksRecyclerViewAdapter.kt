package to.sava.cloudmarksandroid.views.adapters

import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.TextView
import io.realm.RealmRecyclerViewAdapter
import io.realm.RealmResults
import kotlinx.android.synthetic.main.fragment_marks.view.*
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkType
import java.util.*


class MarksRecyclerViewAdapter(value: RealmResults<MarkNode>, private val onClickListener: OnClickListener? = null)
    : RealmRecyclerViewAdapter<MarkNode, MarksRecyclerViewAdapter.MarksViewHolder>(value, true) {

    interface OnClickListener {
        fun onClick(mark: MarkNode)
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

    inner class MarksViewHolder(val view: View) : RecyclerView.ViewHolder(view), View.OnCreateContextMenuListener {
        val marksView: View = view.marks
        val contentView: TextView = view.content
       lateinit var data: MarkNode

        init {
            view.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenuInfo?) {
            menu?.apply {
                add(adapterPosition, R.id.mark_menu_open, Menu.NONE, R.string.mark_menu_open)
                add(adapterPosition, R.id.mark_menu_share_to, Menu.NONE, R.string.mark_menu_share_to)
                add(adapterPosition, R.id.mark_menu_copy_url, Menu.NONE, R.string.mark_menu_copy_url)
                add(adapterPosition, R.id.mark_menu_copy_title, Menu.NONE, R.string.mark_menu_copy_title)
                if (data.type == MarkType.Folder) {
                    getItem(0).isEnabled = false
                    getItem(1).isEnabled = false
                    getItem(2).isEnabled = false
                }
            }
        }
    }
}
