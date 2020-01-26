package to.sava.cloudmarksandroid.ui.adapters

import android.graphics.drawable.Drawable
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import to.sava.cloudmarksandroid.R
import kotlinx.android.synthetic.main.fragment_marks.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.databases.models.MarkType

class MarksRecyclerViewAdapter(
    private val markId: Long,
    private val findFavicon: (mark: MarkNode) -> Drawable?,
    private val getMarkChildren: (markId: Long) -> List<MarkNode>
) : RecyclerView.Adapter<MarksRecyclerViewAdapter.MarksViewHolder>() {

    private var markNodes: List<MarkNode> = listOf()

    data class MarkClickedEvent(val mark: MarkNode)

    init {
        setHasStableIds(true)
        CoroutineScope(Dispatchers.IO).launch {
            markNodes = getMarkChildren(markId)
            withContext(Dispatchers.Main) {
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarksViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_marks, parent, false)
        return MarksViewHolder(view)
    }

    override fun onBindViewHolder(holder: MarksViewHolder, position: Int) {
        getItem(position).let { markNode ->
            holder.data = markNode
            holder.contentView.text = markNode.title
            when (markNode.type) {
                MarkType.Folder -> {
                    holder.contentView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_folder_open_black_24dp,
                        0,
                        R.drawable.ic_chevron_right_black_24dp,
                        0
                    )
                }
                MarkType.Bookmark -> {

                    val did = findFavicon(markNode)?.let { drawable ->
                        holder.contentView.setCompoundDrawablesWithIntrinsicBounds(
                            drawable, null, null, null
                        )
                        true
                    }
                    if (did != true) {
                        holder.contentView.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_bookmark_border_black_24dp, 0, 0, 0
                        )
                    }
                }
            }
            holder.marksView.setOnClickListener {
                EventBus.getDefault().post(MarkClickedEvent(markNode))
            }
        }
    }

    fun getItem(position: Int): MarkNode {
        return markNodes[position]
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun getItemCount(): Int {
        return markNodes.size
    }

    inner class MarksViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnCreateContextMenuListener {
        val marksView: View = view.marks
        val contentView: TextView = view.content
        lateinit var data: MarkNode

        init {
            view.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            menu?.apply {
                when (data.type) {
                    MarkType.Bookmark -> {
                        add(
                            adapterPosition,
                            R.id.mark_menu_open,
                            Menu.NONE,
                            R.string.mark_menu_open
                        )
                        add(
                            adapterPosition,
                            R.id.mark_menu_share_to,
                            Menu.NONE,
                            R.string.mark_menu_share_to
                        )
                        add(
                            adapterPosition,
                            R.id.mark_menu_copy_url,
                            Menu.NONE,
                            R.string.mark_menu_copy_url
                        )
                        add(
                            adapterPosition,
                            R.id.mark_menu_copy_title,
                            Menu.NONE,
                            R.string.mark_menu_copy_title
                        )
                        add(
                            adapterPosition,
                            R.id.mark_menu_fetch_favicon,
                            Menu.NONE,
                            R.string.mark_menu_fetch_favicon
                        )
                    }
                    MarkType.Folder -> {
                        add(
                            adapterPosition,
                            R.id.mark_menu_copy_title,
                            Menu.NONE,
                            R.string.mark_menu_copy_title
                        )
                        add(
                            adapterPosition,
                            R.id.mark_menu_fetch_favicon_in_this_folder,
                            Menu.NONE,
                            R.string.mark_menu_fetch_favicon_in_this_folder
                        )
                    }
                }
            }
        }
    }
}
