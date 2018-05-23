package to.sava.cloudmarksandroid

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.AppCompatButton
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_blank.*
import org.jetbrains.anko.support.v4.toast


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PATH = "path"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [BlankFragment.OnFragmentButtonClickedListener] interface
 * to handle interaction events.
 * Use the [BlankFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class BlankFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var path: String = ""
    private var listener: OnFragmentButtonClickedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            path = it.getString(ARG_PATH)
            toast(path)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_blank, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pathText.text = path
        button1.setOnClickListener {
            onButtonPressed((it as AppCompatButton).text as String)
        }
        button2.setOnClickListener {
            onButtonPressed((it as AppCompatButton).text as String)
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(next: String) {
        listener?.onFragmentButtonClicked(path, next)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentButtonClickedListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentButtonClickedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentButtonClickedListener {
        // TODO: Update argument type and name
        fun onFragmentButtonClicked(current: String, next: String)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @return A new instance of fragment BlankFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String) =
                BlankFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PATH, param1)
                    }
                }
    }
}
