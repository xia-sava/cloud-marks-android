package to.sava.cloudmarksandroid.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import to.sava.cloudmarksandroid.views.adapters.MarksRecyclerViewAdapter
import to.sava.cloudmarksandroid.R

import to.sava.cloudmarksandroid.dummy.DummyContent
import to.sava.cloudmarksandroid.dummy.DummyContent.DummyItem

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [MarksFragment.onMarkListItemClickedListener] interface.
 */
class MarksFragment : Fragment() {

    // TODO: Customize parameters
    private var markId: String = "root"

    private var listItemClickedListener: onListItemClickedListener? = null
    private var listItemChangedListener: onListItemChangedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            markId = it.getString(ARG_MARK_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_marks_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter = MarksRecyclerViewAdapter(DummyContent(markId).ITEMS, listItemClickedListener)
            }
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is onListItemClickedListener) {
            listItemClickedListener = context
        }
        if (context is onListItemChangedListener) {
            listItemChangedListener = context
        }
    }

    override fun onResume() {
        super.onResume()
        listItemChangedListener?.onListItemChanged(markId)
    }

    override fun onDetach() {
        super.onDetach()
        listItemClickedListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface onListItemClickedListener {
        // TODO: Update argument type and name
        fun onListItemClicked(item: DummyItem?)
    }

    interface onListItemChangedListener {
        fun onListItemChanged(markId: String)
    }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_MARK_ID = "column-count"

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
