package to.sava.cloudmarksandroid.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.realm.Realm
import to.sava.cloudmarksandroid.views.adapters.MarksRecyclerViewAdapter
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.libs.Marks
import to.sava.cloudmarksandroid.models.MarkNode

class MarksFragment : Fragment(),
        MarksRecyclerViewAdapter.OnClickListener,
        MarksRecyclerViewAdapter.OnLongClickListener {
    private var mark: MarkNode? = null

    private lateinit var realm: Realm

    private var onListItemClickListener: OnListItemClickListener? = null
    private var onListItemLongClickListener: OnListItemLongClickListener? = null
    private var onListItemChangeListener: OnListItemChangListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListItemClickListener) {
            onListItemClickListener = context
        }
        if (context is OnListItemLongClickListener) {
            onListItemLongClickListener = context
        }
        if (context is OnListItemChangListener) {
            onListItemChangeListener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        realm = Realm.getDefaultInstance()

        val markId = arguments?.getString(ARG_MARK_ID) ?: MarkNode.ROOT_ID
        mark = Marks(realm).getMark(markId)

        val layout = when(mark) {
            null -> R.layout.fragment_mark_not_found
            else -> R.layout.fragment_marks_list
        }
        val view = inflater.inflate(layout, container, false)

        if (view is RecyclerView) {
            mark?.let {
                view.layoutManager = LinearLayoutManager(context)
                val marks = Marks(realm).getMarkChildren(it)
                view.adapter = MarksRecyclerViewAdapter(marks, this, this)
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        onListItemChangeListener?.onListItemChange(mark)
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    override fun onClick(mark: MarkNode) {
        onListItemClickListener?.onListItemClick(mark)
    }

    override fun onLongClick(mark: MarkNode): Boolean {
        return onListItemLongClickListener?.onListItemLongClick(mark) ?: false
    }

    interface OnListItemClickListener {
        fun onListItemClick(mark: MarkNode)
    }

    interface OnListItemLongClickListener {
        fun onListItemLongClick(mark: MarkNode): Boolean
    }

    interface OnListItemChangListener {
        fun onListItemChange(mark: MarkNode?)
    }

    companion object {
        const val ARG_MARK_ID = "mark-id"

        @JvmStatic
        fun newInstance(markId: String) =
                MarksFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_MARK_ID, markId)
                    }
                }
    }
}
