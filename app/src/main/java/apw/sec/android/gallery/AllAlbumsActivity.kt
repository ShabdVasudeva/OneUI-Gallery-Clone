package apw.sec.android.gallery

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import apw.sec.android.gallery.databinding.ActivityAllAlbumsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class AllAlbumsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllAlbumsBinding
    private lateinit var adapter: AlbumAdapter
    private lateinit var mediaFiles: MutableList<MediaFile>

    private lateinit var bottomActionCard: CardView
    private lateinit var bottomActionNav: BottomNavigationView
    private var isUpdatingCount = false
    private var pickGroupMode = false
    private var currentSortIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllAlbumsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Load saved sort preference
        currentSortIndex = loadSortIndex()

        val albumsFetcher = Albums(this)
        mediaFiles = albumsFetcher.fetchAlbums().toMutableList()

        mediaFiles.forEach { media ->
            media.groupName = AlbumGroupPrefs.getGroup(this, media.folderName)
        }

        adapter = AlbumAdapter(this, mediaFiles)

        //Apply saved sort type
        adapter.setSortType(getSortTypeFromIndex(currentSortIndex))

        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.adapter = adapter
        binding.toolbar.setNavigationButtonAsBack()

        val albumCount = mediaFiles.map { it.folderName }.distinct().size
        val subtitleText = "$albumCount album${if (albumCount != 1) "s" else ""}"
        binding.toolbar.toolbar.setSubtitle(subtitleText)
        binding.toolbar.setExpandedSubtitle(subtitleText)

        setupBottomActionBar()
        setupSelectionListener()
        setupBackPress()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_all_albums, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isActionMode = binding.toolbar.isActionMode
        menu.findItem(R.id.sort)?.isVisible = !isActionMode
        menu.findItem(R.id.select)?.isVisible = !isActionMode
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sort -> {
                showSortDialog()
                true
            }
            R.id.select -> {
                enterManualSelectionMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "Custom Order",
            "Name (A-Z)",
            "Name (Z-A)",
            "Item (Most to Fewest)",
            "Item (Fewest to Most)"
        )

        AlertDialog.Builder(this)
            .setTitle("Sort by")
            .setSingleChoiceItems(sortOptions, currentSortIndex) { _, which ->
                currentSortIndex = which
                saveSortIndex(currentSortIndex)
            }
            .setPositiveButton("OK") { dialog, _ ->
                applySortType(currentSortIndex)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun applySortType(selectedIndex: Int) {
        val sortType = getSortTypeFromIndex(selectedIndex)
        adapter.setSortType(sortType)
    }

    private fun getSortTypeFromIndex(index: Int): AlbumSortType {
        return when (index) {
            0 -> AlbumSortType.CUSTOM
            1 -> AlbumSortType.NAME_ASC
            2 -> AlbumSortType.NAME_DESC
            3 -> AlbumSortType.COUNT_DESC
            4 -> AlbumSortType.COUNT_ASC
            else -> AlbumSortType.CUSTOM
        }
    }

    // Save sort preference
    private fun saveSortIndex(index: Int) {
        getSharedPreferences("gallery_prefs", MODE_PRIVATE)
            .edit()
            .putInt("sort_index", index)
            .apply()
    }

    private fun loadSortIndex(): Int {
        return getSharedPreferences("gallery_prefs", MODE_PRIVATE)
            .getInt("sort_index", 0)
    }

    private fun enterManualSelectionMode() {
        if (adapter.itemCount > 0) {
            adapter.triggerSelectionMode()
        }
    }

    private fun setupBottomActionBar() {
        bottomActionCard = CardView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM
                setMargins(
                    resources.getDimensionPixelSize(R.dimen.floating_card_margin),
                    0,
                    resources.getDimensionPixelSize(R.dimen.floating_card_margin),
                    resources.getDimensionPixelSize(R.dimen.floating_card_margin)
                )
            }
            radius = resources.getDimension(R.dimen.floating_card_radius)
            cardElevation = resources.getDimension(R.dimen.floating_card_elevation)

            val typedValue = android.util.TypedValue()
            this@AllAlbumsActivity.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
            setCardBackgroundColor(typedValue.data)

            visibility = View.GONE
        }

        bottomActionNav = BottomNavigationView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            inflateMenu(R.menu.album_action_mode)
        }

        bottomActionCard.addView(bottomActionNav)
        (findViewById<View>(android.R.id.content) as ViewGroup).addView(bottomActionCard)

        bottomActionNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_share -> {
                    adapter.shareSelectedAlbums()
                    true
                }
                R.id.action_move -> {
                    enterPickGroupMode()
                    true
                }
                R.id.action_delete -> {
                    adapter.deleteSelected()
                    true
                }
                R.id.action_rename -> {
                    adapter.renameSelected()
                    true
                }
                R.id.action_mark -> {
                    adapter.removeSelectedFromEssential()
                    true
                }
                R.id.action_group -> {
                    val selectedKeys = adapter.getSelectedKeys()
                    val visibleCount = adapter.getVisibleSelectionCount()

                    val groupKeys = selectedKeys.filter { it.startsWith("GROUP::") }
                    val isUngroup = visibleCount == 1 && groupKeys.size == 1

                    if (isUngroup) {
                        val groupName = groupKeys.first().removePrefix("GROUP::")
                        ungroupAlbums(groupName)
                    } else {
                        adapter.createGroup(generateNextGroupName()) { groupName ->
                            groupSelectedAlbums(groupName)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSelectionListener() {
        adapter.selectionListener = object : AlbumAdapter.SelectionListener {
            override fun onSelectionChanged(count: Int, isAllSelected: Boolean) {
                if (adapter.isInSelectionMode()) {
                    if (!binding.toolbar.isActionMode) {
                        setupActionMode()
                    }

                    val displayCount = adapter.getVisibleSelectionCount()

                    isUpdatingCount = true
                    binding.toolbar.setActionModeCount(displayCount, adapter.getTotalVisibleCount())
                    isUpdatingCount = false

                    updateBottomMenuState(displayCount)

                    //Hide bottom action bar when count is 0
                    if (displayCount > 0) {
                        showBottomActionBar(displayCount)
                    } else {
                        hideBottomActionBar()
                    }
                } else {
                    if (binding.toolbar.isActionMode && !pickGroupMode) {
                        binding.toolbar.dismissActionMode()
                    }
                    hideBottomActionBar()
                }
            }
        }
    }

    private fun setupActionMode() {
        binding.toolbar.showActionMode()

        binding.toolbar.setActionModeCheckboxListener { _, isChecked ->
            if (!isUpdatingCount) {
                adapter.toggleSelectAll()
            }
        }
    }
    private fun showBottomActionBar(count: Int) {
        bottomActionCard.visibility = View.VISIBLE
        updateBottomMenuState(count)
    }

    private fun hideBottomActionBar() {
        bottomActionCard.visibility = View.GONE
    }

    private fun updateBottomMenuState(count: Int) {
        val menu = bottomActionNav.menu
        menu.findItem(R.id.action_rename)?.isVisible = count == 1
        menu.findItem(R.id.action_mark)?.isVisible = false

        val selectedKeys = adapter.getSelectedKeys()
        val groupKeys = selectedKeys.filter { it.startsWith("GROUP::") }
        val isUngroup = count == 1 && groupKeys.size == 1

        val groupItem = menu.findItem(R.id.action_group)
        if (isUngroup) {
            groupItem?.title = "Ungroup"
            groupItem?.setIcon(R.drawable.ic_ungroup)
        } else {
            groupItem?.title = "Group"
            groupItem?.setIcon(R.drawable.ic_group)
        }
    }

    private fun enterPickGroupMode() {
        pickGroupMode = true
        binding.toolbar.dismissActionMode()
        adapter.setPickGroupMode(true)
        Toast.makeText(this, "Select a group to move albums into", Toast.LENGTH_SHORT).show()
    }

    private fun exitPickGroupMode() {
        pickGroupMode = false
        adapter.setPickGroupMode(false)
        adapter.exitSelectionMode()

        if (binding.toolbar.isActionMode) {
            binding.toolbar.dismissActionMode()
        }

        hideBottomActionBar()
    }

    private fun groupSelectedAlbums(groupName: String) {
        val selectedAlbumNames = adapter.getSelectedAlbums()

        mediaFiles.forEach { media ->
            if (media.folderName != null && selectedAlbumNames.contains(media.folderName)) {
                media.groupName = groupName
                AlbumGroupPrefs.setGroup(this, media.folderName!!, groupName)
            }
        }

        adapter.refreshData(mediaFiles)
        exitPickGroupMode()
    }

    private fun generateNextGroupName(): String {
        val existing = AlbumRepository.albumItems
            .filterIsInstance<AlbumItem.Group>()
            .map { it.groupName }

        var index = 1
        while (true) {
            val name = "Group $index"
            if (existing.none { it.equals(name, ignoreCase = true) }) {
                return name
            }
            index++
        }
    }

    private fun ungroupAlbums(groupName: String) {
        mediaFiles.forEach { media ->
            if (media.groupName == groupName) {
                media.groupName = null
                if (media.folderName != null) {
                    AlbumGroupPrefs.clearGroup(this, media.folderName!!)
                }
            }
        }

        adapter.refreshData(mediaFiles)
        adapter.exitSelectionMode()
        hideBottomActionBar()
        binding.toolbar.dismissActionMode()

        Toast.makeText(this, "Group ungrouped", Toast.LENGTH_SHORT).show()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this) {
            when {
                pickGroupMode -> {
                    exitPickGroupMode()
                }
                adapter.isInSelectionMode() -> {
                    adapter.exitSelectionMode()
                    binding.toolbar.dismissActionMode()
                    hideBottomActionBar()
                }
                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bottomActionCard.isInitialized) {
            (bottomActionCard.parent as? ViewGroup)?.removeView(bottomActionCard)
        }
    }
}
