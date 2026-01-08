package apw.sec.android.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import apw.sec.android.gallery.Albums
import apw.sec.android.gallery.R
import dev.oneuiproject.oneui.layout.ToolbarLayout
import apw.sec.android.gallery.AlbumRepository
import apw.sec.android.gallery.GroupAlbumAdapter


class GroupAlbumsFragment : Fragment() {

    private var albums: List<AlbumItem.Album> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_album, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupName = requireArguments().getString(ARG_GROUP)!!

        val group = AlbumRepository.albumItems
            .filterIsInstance<AlbumItem.Group>()
            .firstOrNull { it.groupName == groupName }

        albums = group?.albums ?: emptyList()

        val recycler = view.findViewById<RecyclerView>(R.id.recycler_view)
        recycler.layoutManager = GridLayoutManager(context, 3)
        recycler.adapter = GroupAlbumAdapter(requireContext(), albums)

//        view.findViewById<View>(R.id.title_row)?.visibility = View.GONE

        (activity as? GroupAlbumActivity)?.let { act ->
            val toolbarLayout = act.findViewById<ToolbarLayout>(R.id.toolbar)

            val subtitle = "${albums.size} album${if (albums.size != 1) "s" else ""}"

            //This controls BOTH collapsed + expanded title
            toolbarLayout.setTitle(groupName)

            // Subtitle
            toolbarLayout.toolbar.subtitle = subtitle
            toolbarLayout.setExpandedSubtitle(subtitle)
        }

    }

    companion object {
        private const val ARG_GROUP = "group"

        fun newInstance(groupName: String) =
            GroupAlbumsFragment().apply {
                arguments = bundleOf(ARG_GROUP to groupName)
            }
    }
}
