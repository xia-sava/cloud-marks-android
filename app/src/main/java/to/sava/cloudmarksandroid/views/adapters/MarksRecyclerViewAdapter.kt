package to.sava.cloudmarksandroid.views.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter
import kotlinx.android.synthetic.main.fragment_marks.view.*
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkType
import java.util.*


class MarksRecyclerViewAdapter(value: OrderedRealmCollection<MarkNode>, private val faviconFinder: FaviconFinder?, private val onClickListener: OnClickListener? = null)
    : RealmRecyclerViewAdapter<MarkNode, MarksRecyclerViewAdapter.MarksViewHolder>(value, true) {

    interface OnClickListener {
        fun onClick(mark: MarkNode)
    }

    init {
        setHasStableIds(true)
    }

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarksViewHolder {
        context = parent.context
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
        getItem(position)?.let { markNode ->
            holder.data = markNode
            holder.contentView.text = markNode.title
            when (markNode.type) {
                MarkType.Folder -> {
                    holder.contentView.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_folder_open_black_24dp, 0, R.drawable.ic_chevron_right_black_24dp, 0)
                }
                MarkType.Bookmark -> {
                    val did = faviconFinder?.let {finder ->
                        finder.findFavicon(markNode)?.let {drawable ->
                            holder.contentView.setCompoundDrawablesWithIntrinsicBounds(
                                    drawable, null, null, null)
                            true
                        }
                    }
                    if (did != true) {
                        holder.contentView.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_bookmark_border_black_24dp, 0, 0, 0)
                    }
                }
            }
        }
    }

    override fun getItemId(index: Int): Long {
        val item = getItem(index)
        return when {
            item !== null -> UUID.fromString(item.id).leastSignificantBits
            else -> RecyclerView.NO_ID
        }
    }

    inner class MarksViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnCreateContextMenuListener {
        val marksView: View = view.marks
        val contentView: TextView = view.content
       lateinit var data: MarkNode

        init {
            view.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenuInfo?) {
            menu?.apply {
                when (data.type) {
                    MarkType.Bookmark -> {
                        add(adapterPosition, R.id.mark_menu_open, Menu.NONE, R.string.mark_menu_open)
                        add(adapterPosition, R.id.mark_menu_share_to, Menu.NONE, R.string.mark_menu_share_to)
                        add(adapterPosition, R.id.mark_menu_copy_url, Menu.NONE, R.string.mark_menu_copy_url)
                        add(adapterPosition, R.id.mark_menu_copy_title, Menu.NONE, R.string.mark_menu_copy_title)
                        add(adapterPosition, R.id.mark_menu_fetch_favicon, Menu.NONE, R.string.mark_menu_fetch_favicon)
                    }
                    MarkType.Folder -> {
                        add(adapterPosition, R.id.mark_menu_copy_title, Menu.NONE, R.string.mark_menu_copy_title)
                        add(adapterPosition, R.id.mark_menu_fetch_favicon_in_this_folder, Menu.NONE, R.string.mark_menu_fetch_favicon_in_this_folder)
                    }
                }
            }
        }
    }

    interface FaviconFinder {
        fun findFavicon(mark: MarkNode): Drawable?
    }
}
