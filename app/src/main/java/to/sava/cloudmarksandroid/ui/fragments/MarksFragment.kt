package to.sava.cloudmarksandroid.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.AndroidSupportInjection
import org.greenrobot.eventbus.EventBus
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.databases.models.MarkType
import to.sava.cloudmarksandroid.databases.repositories.FaviconRepository
import to.sava.cloudmarksandroid.databases.repositories.MarkNodeRepository
import to.sava.cloudmarksandroid.ui.adapters.MarksRecyclerViewAdapter
import javax.inject.Inject

class MarksFragment : Fragment() {

    @Inject
    internal lateinit var markRepos: MarkNodeRepository

    @Inject
    internal lateinit var faviconRepos: FaviconRepository

    private var mark: MarkNode? = null
    var adapter: MarksRecyclerViewAdapter? = null

    companion object {
        const val ARG_MARK_ID = "mark-id"

        @JvmStatic
        fun newInstance(markId: Long) =
            MarksFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_MARK_ID, markId)
                }
            }
    }

    data class MarkListChangedEvent(val mark: MarkNode?)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)

        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val markId = arguments?.getLong(ARG_MARK_ID) ?: MarkNode.ROOT_ID
        val mark = markRepos.getMarkNode(markId)
        this.mark = mark

        val layout = when {
            mark == null ->
                R.layout.fragment_mark_not_found
            (mark.type == MarkType.Folder && markRepos.getMarkNodeChildrenCount(mark) == 0L) ->
                R.layout.fragment_empty_folder
            else ->
                R.layout.fragment_marks_list
        }
        val view = inflater.inflate(layout, container, false)

        if (view is RecyclerView && mark != null) {
            view.layoutManager = LinearLayoutManager(context)
            adapter = MarksRecyclerViewAdapter(
                mark.id,
                { markNode: MarkNode -> faviconRepos.findFaviconDrawable(markNode.domain) },
                { id: Long -> markRepos.getMarkNodeChildren(id) }
            )
            view.adapter = adapter
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().post(MarkListChangedEvent(mark))
    }
}
