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

class MarksFragment : Fragment() {

    private var mark: MarkNode? = null

    private lateinit var realm: Realm

    private var listItemClickedListener: OnListItemClickedListener? = null
    private var listItemChangedListener: OnListItemChangedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListItemClickedListener) {
            listItemClickedListener = context
        }
        if (context is OnListItemChangedListener) {
            listItemChangedListener = context
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

        if (mark != null && view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                val marks = Marks(realm).getMarkChildren(mark!!)
                adapter = MarksRecyclerViewAdapter(marks, listItemClickedListener)
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        listItemChangedListener?.onListItemChanged(mark)
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    interface OnListItemClickedListener {
        fun onListItemClicked(mark: MarkNode?)
    }

    interface OnListItemChangedListener {
        fun onListItemChanged(mark: MarkNode?)
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
