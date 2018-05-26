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
import io.realm.kotlin.where
import to.sava.cloudmarksandroid.views.adapters.MarksRecyclerViewAdapter
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.models.MarkNode
import java.lang.RuntimeException

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [MarksFragment.OnListItemClickedListener] interface.
 */
class MarksFragment : Fragment() {

    // TODO: Customize parameters
    private lateinit var mark: MarkNode

    private var listItemClickedListener: OnListItemClickedListener? = null
    private var listItemChangedListener: OnListItemChangedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val realm = Realm.getDefaultInstance()
        val markId = arguments?.getString(ARG_MARK_ID) ?: "root"
        mark = realm.where<MarkNode>().equalTo("id", markId).findFirst() ?: throw RuntimeException()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_marks_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                val realm = Realm.getDefaultInstance()
                val items = realm.where<MarkNode>().equalTo("parent.id", mark.id).findAll()
                adapter = MarksRecyclerViewAdapter(items, listItemClickedListener)
            }
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListItemClickedListener) {
            listItemClickedListener = context
        }
        if (context is OnListItemChangedListener) {
            listItemChangedListener = context
        }
    }

    override fun onResume() {
        super.onResume()
        listItemChangedListener?.onListItemChanged(mark)
    }

    override fun onDetach() {
        super.onDetach()
        listItemClickedListener = null
    }

    interface OnListItemClickedListener {
        fun onListItemClicked(mark: MarkNode?)
    }

    interface OnListItemChangedListener {
        fun onListItemChanged(mark: MarkNode)
    }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_MARK_ID = "mark-id"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(markId: String) =
                MarksFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_MARK_ID, markId)
                    }
                }
    }
}
