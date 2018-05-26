package to.sava.cloudmarksandroid.views.adapters

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.NO_ID
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
import java.util.*


/**
 * [RecyclerView.Adapter] that can display a [MarkNode] and makes a call to the
 * specified [OnListItemClickedListener].
 * TODO: Replace the implementation with code for your data type.
 */
class MarksRecyclerViewAdapter(
        private val value: RealmResults<MarkNode>,
        private val itemClickedListener: OnListItemClickedListener?)
    : RealmRecyclerViewAdapter<MarkNode, MarksRecyclerViewAdapter.MarksViewHolder>(value, true) {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as MarkNode
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            itemClickedListener?.onListItemClicked(item)
        }
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarksViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_marks, parent, false)
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
            holder.idView.text = it.id.substring(0..2)
            holder.contentView.text = it.title
        }
    }

    override fun getItemId(index: Int): Long {
        val item = getItem(index)
        return when {
            item !== null -> UUID.fromString(item.id).leastSignificantBits
            else -> NO_ID
        }
    }

    inner class MarksViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val marksView: View = view.marks
        val idView: TextView = view.item_number
        val contentView: TextView = view.content
        lateinit var data: MarkNode
    }
}
