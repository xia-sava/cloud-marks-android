package to.sava.cloudmarksandroid.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import io.realm.Realm
import to.sava.cloudmarksandroid.views.adapters.MarksRecyclerViewAdapter
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.libs.Marks
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkType

class MarksFragment : Fragment(), MarksRecyclerViewAdapter.OnClickListener {
    private var mark: MarkNode? = null
    var adapter: MarksRecyclerViewAdapter? = null

    private lateinit var realm: Realm

    private var onListItemClickListener: OnListItemClickListener? = null
    private var onListItemChangeListener: OnListItemChangListener? = null
    private var faviconFinder: MarksRecyclerViewAdapter.FaviconFinder? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListItemClickListener) {
            onListItemClickListener = context
        }
        if (context is OnListItemChangListener) {
            onListItemChangeListener = context
        }
        if (context is MarksRecyclerViewAdapter.FaviconFinder) {
            faviconFinder = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        realm = Realm.getDefaultInstance()

        val marks = Marks(realm)
        val markId = arguments?.getString(ARG_MARK_ID) ?: MarkNode.ROOT_ID
        mark = marks.getMark(markId)

        val layout = when(mark) {
            null -> R.layout.fragment_mark_not_found
            else -> {
                mark?.let {
                    if (it.type == MarkType.Folder && marks.getMarkChildren(it).isEmpty()) {
                        R.layout.fragment_empty_folder
                    } else {
                        null
                    }
                }
                ?: R.layout.fragment_marks_list
            }
        }
        val view = inflater.inflate(layout, container, false)

        if (view is RecyclerView) {
            mark?.let {
                view.layoutManager = LinearLayoutManager(context)
                val children = marks.getMarkChildren(it)
                adapter = MarksRecyclerViewAdapter(children, faviconFinder, this)
                view.adapter = adapter
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

    interface OnListItemClickListener {
        fun onListItemClick(mark: MarkNode)
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
