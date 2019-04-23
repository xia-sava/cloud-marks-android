package to.sava.cloudmarksandroid.fragments

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import dagger.android.support.AndroidSupportInjection
import to.sava.cloudmarksandroid.views.adapters.MarksRecyclerViewAdapter
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.libs.Favicons
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkType
import to.sava.cloudmarksandroid.repositories.MarkNodeRepository
import javax.inject.Inject

class MarksFragment : Fragment(),
        MarksRecyclerViewAdapter.OnClickListener,
        MarksRecyclerViewAdapter.FaviconFinder {

    @Inject
    internal lateinit var repos: MarkNodeRepository

    @Inject
    internal lateinit var favicons: Favicons

    private var mark: MarkNode? = null
    var adapter: MarksRecyclerViewAdapter? = null

    private var onListItemClickListener: OnListItemClickListener? = null
    private var onListItemChangeListener: OnListItemChangListener? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)

        super.onAttach(context)
        if (context is OnListItemClickListener) {
            onListItemClickListener = context
        }
        if (context is OnListItemChangListener) {
            onListItemChangeListener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val markId = arguments?.getString(ARG_MARK_ID) ?: MarkNode.ROOT_ID

        val mark = repos.getManagedMarkNode(markId)
        this.mark = mark

        val layout = when {
            mark == null ->
                R.layout.fragment_mark_not_found
            (mark.type == MarkType.Folder && repos.getManagedMarkNodeChildren(mark).isEmpty()) ->
                R.layout.fragment_empty_folder
            else ->
                R.layout.fragment_marks_list
        }
        val view = inflater.inflate(layout, container, false)

        if (view is RecyclerView && mark != null) {
            view.layoutManager = LinearLayoutManager(context)
            adapter = MarksRecyclerViewAdapter(
                    repos.getManagedMarkNodeChildren(mark), this, this)
            view.adapter = adapter
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        onListItemChangeListener?.onListItemChange(mark)
    }

    override fun onClick(mark: MarkNode) {
        onListItemClickListener?.onListItemClick(mark)
    }

    /**
     * 登録済みのFaviconを取得する．
     */
    override fun findFavicon(mark: MarkNode): Drawable? {
        return favicons.find(mark)
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
