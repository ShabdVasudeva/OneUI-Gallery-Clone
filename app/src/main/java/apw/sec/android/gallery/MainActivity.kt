    package apw.sec.android.gallery

    import android.Manifest
    import android.annotation.SuppressLint
    import android.content.Context
    import android.content.pm.PackageManager
    import android.os.*
    import android.widget.*
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.app.ActivityCompat
    import androidx.core.content.ContextCompat
    import androidx.fragment.app.Fragment
    import androidx.recyclerview.widget.*
    import android.view.*
    import androidx.preference.PreferenceManager
    import android.util.Log
    import android.content.Intent
    import androidx.activity.addCallback
    import androidx.appcompat.app.AlertDialog
    import androidx.cardview.widget.CardView
    import androidx.preference.Preference
    import androidx.preference.PreferenceFragmentCompat
    import apw.sec.android.gallery.AlbumAdapter
    import apw.sec.android.gallery.AlbumItem
    import apw.sec.android.gallery.databinding.ActivityMainBinding
    import apw.sec.android.gallery.AlbumSortType
    import com.google.android.material.bottomnavigation.BottomNavigationView
    import com.jaredrummler.android.colorpicker.ColorPickerDialog
    import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
    import dev.oneuiproject.oneui.layout.ToolbarLayout

    class MainActivity : AppCompatActivity(), ColorPickerDialogListener {

        private var _binding: ActivityMainBinding? = null
        private val binding get() = _binding!!
        private val PERMISSION_REQUEST_PERM = 100
        private var albumFragment: Album? = null
        private var currentSortIndex = 0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            PreferenceManager.setDefaultValues(this, R.xml.settings, true)
            _binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Setup ToolbarLayout
            binding.toolbarLayout.setTitle("Gallery")
            // Remove back button
            binding.toolbarLayout.setNavigationButtonVisible(false)


            currentSortIndex = loadSortIndex()

            if (!hasPermissions()) {
                requestPermissions()
            } else {
                loadImages()
            }

            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
            val oldNav = findViewById<BottomNavigationView>(R.id.bottom_navigation_old)
            val newNavCard = findViewById<CardView>(R.id.bottom_nav_card_new)

            val useNewStyle = sharedPrefs.getBoolean("ENABLE_FLOATING_TAB_BAR", true)
            if (useNewStyle) {
                oldNav.visibility = View.GONE
                newNavCard.visibility = View.VISIBLE
            } else {
                oldNav.visibility = View.VISIBLE
                newNavCard.visibility = View.GONE
            }

            sharedPrefs.registerOnSharedPreferenceChangeListener { prefs, key ->
                when (key) {
                    "ENABLE_FLOATING_TAB_BAR" -> {
                        val updatedUseNewStyle = prefs.getBoolean("ENABLE_FLOATING_TAB_BAR", false)
                        if (updatedUseNewStyle) {
                            oldNav.visibility = View.GONE
                            newNavCard.visibility = View.VISIBLE
                        } else {
                            oldNav.visibility = View.VISIBLE
                            newNavCard.visibility = View.GONE
                        }
                    }
                    "SHOW_ESSENTIAL_ALBUMS" -> {
                        val showOnlyEssential = prefs.getBoolean("SHOW_ESSENTIAL_ALBUMS", false)
                        albumFragment?.reloadAlbums(showOnlyEssential)
                    }
                }
            }
        }

        override fun onCreateOptionsMenu(menu: Menu): Boolean {
            menuInflater.inflate(R.menu.menu, menu)
            return true
        }

        override fun onPrepareOptionsMenu(menu: Menu): Boolean {
            val isActionMode = binding.toolbarLayout.isActionMode
            val showOnlyEssential = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("SHOW_ESSENTIAL_ALBUMS", false)

//          menu.findItem(R.id.all)?.isVisible = !isActionMode
            menu.findItem(R.id.settings)?.isVisible = !isActionMode
            menu.findItem(R.id.sort)?.isVisible = !isActionMode && !showOnlyEssential
            menu.findItem(R.id.select)?.isVisible = !isActionMode
            menu.findItem(R.id.esential_albums)?.isVisible = !isActionMode && showOnlyEssential
            return super.onPrepareOptionsMenu(menu)
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
//                R.id.all -> {
//                    startActivity(Intent(this, AllActivity::class.java))
//                    return true
//                }
                R.id.settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    return true
                }
                R.id.sort -> {
                    showSortDialog()
                    return true
                }
                R.id.select -> {
                    albumFragment?.enterManualSelectionMode()
                    return true
                }
                R.id.esential_albums -> {
                    albumFragment?.enterEssentialSelectionMode()
                    return true
                }
            }
            return super.onOptionsItemSelected(item)
        }

        private fun setCurrentFragment(fragment: Fragment) =
            supportFragmentManager.beginTransaction().apply {
                // Exit action mode when switching fragments
                if (fragment !is Album) {
                    albumFragment?.adapter?.exitSelectionMode()
                    if (binding.toolbarLayout.isActionMode) {
                        binding.toolbarLayout.dismissActionMode()
                    }
                }

                replace(R.id.placeholder, fragment)
                commit()

                if (fragment is Album) {
                    albumFragment = fragment
                }
            }

        private fun hasPermissions(): Boolean {
            return requiredPermissions().all { permission ->
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        private fun requiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        private fun requestPermissions() {
            ActivityCompat.requestPermissions(this, requiredPermissions(), PERMISSION_REQUEST_PERM)
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == PERMISSION_REQUEST_PERM) {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    loadImages()
                } else {
                    Toast.makeText(this, "Permissions denied", Toast.LENGTH_LONG).show()
                }
            }
        }

        private fun loadImages() {
            val oldNav = findViewById<BottomNavigationView>(R.id.bottom_navigation_old)
            val newNav = findViewById<BottomNavigationView>(R.id.bottom_navigation_new)

            val useNewStyle = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("ENABLE_FLOATING_TAB_BAR", false)

            val bottomNav = if (useNewStyle) newNav else oldNav

            bottomNav?.setOnItemSelectedListener { item ->
                saveLastSelectedTab(item.itemId)
                when (item.itemId) {
                    R.id.images -> {
                        binding.toolbarLayout.setTitle("Pictures")
                        setCurrentFragment(MainFrag())
                        true
                    }
                    R.id.albums -> {
                        binding.toolbarLayout.setTitle("Albums","")
                        setCurrentFragment(Album())
                        true
                    }
                    R.id.search -> {
                        binding.toolbarLayout.setTitle("Search")
                        setCurrentFragment(Search())
                        true
                    }
                    R.id.utilities -> {
                        binding.toolbarLayout.setTitle("Utils")
                        setCurrentFragment(Album.Utils())
                        true
                    }
                    else -> false
                }
            }

            val lastTabId = getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
                .getInt("last_tab_id", R.id.images)
            bottomNav?.setSelectedItemId(lastTabId)
        }

        private fun saveLastSelectedTab(tabId: Int) {
            getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("last_tab_id", tabId)
                .apply()
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
                    applySortToAlbumFragment(currentSortIndex)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        private fun applySortToAlbumFragment(selectedIndex: Int) {
            val sortType = getSortTypeFromIndex(selectedIndex)
            albumFragment?.setSortType(sortType)
            albumFragment?.reloadAlbums(
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean("SHOW_ESSENTIAL_ALBUMS", false)
            )
        }


        fun getSortTypeFromIndex(index: Int): AlbumSortType {
            return when (index) {
                0 -> AlbumSortType.CUSTOM
                1 -> AlbumSortType.NAME_ASC
                2 -> AlbumSortType.NAME_DESC
                3 -> AlbumSortType.COUNT_DESC
                4 -> AlbumSortType.COUNT_ASC
                else -> AlbumSortType.CUSTOM
            }
        }

        private fun saveSortIndex(index: Int) {
            getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("sort_index", index)
                .apply()
        }

        fun loadSortIndex(): Int {
            return getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
                .getInt("sort_index", 0)
        }

        override fun onColorSelected(dialogId: Int, color: Int) {
            Log.d("Color", "onColorSelected() -> dialogId: $dialogId, color: $color")
            if (dialogId == 0) {
                val colorHex = String.format("#%08X", color)
                GenerateQuoteImage().createQuoteImage(this, colorHex, getSelectedFont(this))
            }
        }

        override fun onDialogDismissed(dialogId: Int) {
            Log.d("Color", "onDialogDismissed() -> dialogId: $dialogId")
        }

        fun getSelectedFont(context: Context): String {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val fontName = sharedPreferences.getString("font_preference", "Roboto Slab")

            return when (fontName) {
                "RobotoSlab.ttf" -> "RobotoSlab.ttf"
                "PermanentMarker.ttf" -> "PermanentMarker.ttf"
                "PlayfairDisplay.ttf" -> "PlayfairDisplay.ttf"
                "Handwriting.ttf" -> "Handwriting.ttf"
                else -> "RobotoSlab.ttf"
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            _binding = null
        }

        companion object {
            class MainFrag : Fragment() {
                private lateinit var mediaList: MutableList<MediaFile>
                private lateinit var adapter: MediaAdapter

                override fun onCreateView(
                    inflater: LayoutInflater, container: ViewGroup?,
                    savedInstanceState: Bundle?
                ): View? {
                    return inflater.inflate(R.layout.fragment_main, container, false)
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onResume() {
                    super.onResume()
                    requireActivity().intent.getIntExtra("deleted_position", -1).takeIf { it != -1 }
                        ?.let { pos ->
                            mediaList.removeAt(pos)
                            adapter.notifyItemRemoved(pos)
                            adapter.notifyDataSetChanged()
                            requireActivity().intent.removeExtra("deleted_position")
                        }
                }

                override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                    super.onViewCreated(view, savedInstanceState)
                    val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
                    recyclerView.layoutManager = GridLayoutManager(context, 4)
                    loadImages()
                    recyclerView.adapter = adapter
                }

                private fun loadImages() {
                    val mediaFetcher = MediaFetcher(requireContext())
                    mediaList = mediaFetcher.fetchMediaFiles().toMutableList()
                    adapter = MediaAdapter(mediaList)
                }
            }

            class Search : Fragment() {
                private var mediaList: List<MediaFile>? = null
                private lateinit var adapter: SearchAdapter

                override fun onCreateView(
                    inflater: LayoutInflater, container: ViewGroup?,
                    savedInstanceState: Bundle?
                ): View? {
                    return inflater.inflate(R.layout.fragment_search, container, false)
                }

                override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                    super.onViewCreated(view, savedInstanceState)
                    val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
                    val search = view.findViewById<ApwSearch>(R.id.search)
                    search.setSearchViewListener(object : ApwSearch.SearchViewListener {
                        override fun onQueryTextChange(query: String) {
                            adapter.filter(query)
                        }

                        override fun onQueryTextSubmit(query: String) {}
                    })
                    recyclerView.layoutManager = GridLayoutManager(context, 4)
                    loadImages()
                    recyclerView.adapter = adapter
                }

                private fun loadImages() {
                    val mediaFetcher = FetchAll(requireContext())
                    mediaList = mediaFetcher.fetchMediaFiles()
                    adapter = SearchAdapter(mediaList!!)
                }
            }

            enum class Mode {
                NORMAL,
                PICK_GROUP
            }

            private var currentMode = Mode.NORMAL

            class Album : Fragment() {
                private lateinit var mediaList: MutableList<MediaFile>
                lateinit var adapter: AlbumAdapter
                    private set

                private var showOnlyEssential = false
                private lateinit var headerAdapter: AlbumsHeaderAdapter
                private lateinit var toolbarLayout: ToolbarLayout
                private var pickGroupMode = false

                private lateinit var bottomActionCard: CardView
                private lateinit var bottomActionNav: BottomNavigationView

                override fun onCreateView(
                    inflater: LayoutInflater, container: ViewGroup?,
                    savedInstanceState: Bundle?
                ): View? {
                    return inflater.inflate(R.layout.fragment_album, container, false)
                }

                override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                    super.onViewCreated(view, savedInstanceState)

                    toolbarLayout = (requireActivity() as MainActivity).binding.toolbarLayout
                    val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)

                    val gridLayoutManager = GridLayoutManager(context, 3)
                    recyclerView.layoutManager = gridLayoutManager

                    showOnlyEssential = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getBoolean("SHOW_ESSENTIAL_ALBUMS", false)

                    loadImages()

                    headerAdapter = AlbumsHeaderAdapter {
                        startActivity(Intent(requireContext(), AllAlbumsActivity::class.java))
                    }

                    adapter.setShowOnlyEssential(showOnlyEssential)
                    headerAdapter.update(showOnlyEssential)

                    val concatAdapter = ConcatAdapter(headerAdapter, adapter)

                    gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return when (concatAdapter.getItemViewType(position)) {
                                headerAdapter.getItemViewType(0) -> 3
                                else -> 1
                            }
                        }
                    }

                    recyclerView.adapter = concatAdapter

                    setupBottomActionBar()

                    adapter.selectionListener = object : AlbumAdapter.SelectionListener {
                        override fun onSelectionChanged(count: Int, isAllSelected: Boolean) {
                            if (adapter.isManagingEssential()) {
                                val visibleCount = adapter.getVisibleSelectionCount()
                                toolbarLayout.setActionModeCount(visibleCount, adapter.getTotalVisibleCount())
                                hideBottomActionBar()
                                return
                            }

                            if (adapter.isInSelectionMode()) {
                                if (!toolbarLayout.isActionMode) {
                                    setupActionMode()
                                }
                                val visibleCount = adapter.getVisibleSelectionCount()

                                isUpdatingCount = true
                                toolbarLayout.setActionModeCount(visibleCount, adapter.getTotalVisibleCount())
                                isUpdatingCount = false

                                updateBottomMenuState(visibleCount)

                                // Hide bottom action bar when count is 0
                                if (visibleCount > 0) {
                                    showBottomActionBar(visibleCount)
                                } else {
                                    hideBottomActionBar()
                                }
                            } else {
                                if (toolbarLayout.isActionMode && !pickGroupMode) {
                                    toolbarLayout.dismissActionMode()
                                }
                                hideBottomActionBar()
                            }
                        }
                    }
                    setupBackPress()
                }

                override fun onResume() {
                    super.onResume()

                    // Reapply the saved sort type when returning from AllAlbumsActivity
                    if (::adapter.isInitialized) {
                        val savedSortIndex = (activity as? MainActivity)?.loadSortIndex() ?: 0
                        val sortType = (activity as? MainActivity)?.getSortTypeFromIndex(savedSortIndex)
                            ?: AlbumSortType.CUSTOM
                        adapter.setSortType(sortType)
                    }
                }

                private fun setupBottomActionBar() {
                    // Create floating action bar card programmatically
                    bottomActionCard = CardView(requireContext()).apply {
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

                        // Use theme background color
                        val typedValue = android.util.TypedValue()
                        requireContext().theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
                        setCardBackgroundColor(typedValue.data)

                        visibility = View.GONE
                    }

                    bottomActionNav = BottomNavigationView(requireContext()).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        inflateMenu(R.menu.album_action_mode)
                    }

                    bottomActionCard.addView(bottomActionNav)

                    // Add to main container's parent (the root view)
                    (requireActivity().findViewById<View>(android.R.id.content) as ViewGroup)
                        .addView(bottomActionCard)

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
                                    // UNGROUP
                                    val groupName = groupKeys.first().removePrefix("GROUP::")
                                    adapter.ungroup(groupName)
                                    hideBottomActionBar()
                                    toolbarLayout.dismissActionMode()
                                } else {
                                    // GROUP
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

                private fun showBottomActionBar(count: Int) {
                    bottomActionCard.visibility = View.VISIBLE
                    updateBottomMenuState(count)
                }

                private fun hideBottomActionBar() {
                    bottomActionCard.visibility = View.GONE
                }

                private var isUpdatingCount = false

                fun enterManualSelectionMode() {
                    if (adapter.itemCount > 0) {
                        adapter.triggerSelectionMode()
                    }
                }
                private fun setupActionMode() {
                    toolbarLayout.showActionMode()

                    toolbarLayout.setActionModeCheckboxListener { _, isChecked ->
                        // Only respond to user clicks, not programmatic changes
                        if (!isUpdatingCount) {
                            adapter.toggleSelectAll()
                        }
                    }
                }

                private fun updateBottomMenuState(count: Int) {
                    val menu = bottomActionNav.menu
                    menu.findItem(R.id.action_rename)?.isVisible = count == 1
                    menu.findItem(R.id.action_mark)?.isVisible = showOnlyEssential

                    val selectedKeys = adapter.getSelectedKeys()
                    val groupKeys = selectedKeys.filter { it.startsWith("GROUP::") }
                    val isUngroup = count == 1 && groupKeys.size == 1

                    val groupItem = menu.findItem(R.id.action_group)
                    if (isUngroup) {
                        groupItem.title = "Ungroup"
                        groupItem.setIcon(R.drawable.ic_ungroup)
                    } else {
                        groupItem.title = "Group"
                        groupItem.setIcon(R.drawable.ic_group)
                    }
                }


                fun enterEssentialSelectionMode() {
                    mediaList.forEach { media ->
                        media.groupName = AlbumGroupPrefs.getGroup(requireContext(), media.folderName)
                    }

                    adapter.refreshData(mediaList)
                    adapter.enterEssentialSelectionMode()
                    headerAdapter.setVisible(false)

                    toolbarLayout.showActionMode()

                    // Hide the "Select All" checkbox container
                    val selectAllContainer = toolbarLayout.findViewById<LinearLayout>(
                        dev.oneuiproject.oneui.design.R.id.toolbarlayout_selectall
                    )
                    selectAllContainer?.visibility = View.GONE

                    val titleView = toolbarLayout.findViewById<TextView>(
                        dev.oneuiproject.oneui.design.R.id.toolbar_layout_action_mode_title
                    )

                    val offset = resources.getDimensionPixelSize(R.dimen.essential_title_offset)

                    titleView?.setPaddingRelative(
                        offset,
                        titleView.paddingTop,
                        titleView.paddingEnd,
                        titleView.paddingBottom
                    )


                    val actionModeToolbar = toolbarLayout.findViewById<androidx.appcompat.widget.Toolbar>(
                        dev.oneuiproject.oneui.design.R.id.toolbarlayout_action_mode_toolbar
                    )

                    actionModeToolbar?.let { toolbar ->
                        toolbar.menu.clear()
                        toolbar.inflateMenu(R.menu.menu_essential_done)
                        toolbar.setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.action_done -> {
                                    adapter.confirmEssentialSelection()
                                    adapter.exitSelectionMode()

                                    toolbar.menu.clear()
                                    toolbar.setOnMenuItemClickListener(null)

                                    // Restore Select All visibility before dismissing
                                    selectAllContainer?.visibility = View.VISIBLE
                                    resetActionModeTitlePadding()

                                    // Dismiss action mode
                                    toolbarLayout.dismissActionMode()

                                    // Reload albums
                                    val showOnlyEssential = PreferenceManager.getDefaultSharedPreferences(requireContext())
                                        .getBoolean("SHOW_ESSENTIAL_ALBUMS", false)
                                    headerAdapter.setVisible(true)
                                    reloadAlbums(showOnlyEssential)

                                    // Refresh the main menu
                                    requireActivity().invalidateOptionsMenu()

                                    hideBottomActionBar()
                                    true
                                }
                                else -> false
                            }
                        }
                    }

                    // Update the count display
                    toolbarLayout.setActionModeCount(
                        adapter.getVisibleSelectionCount(),
                        adapter.getTotalVisibleCount()
                    )
                }
                private fun enterPickGroupMode() {
                    pickGroupMode = true
                    toolbarLayout.dismissActionMode()
                    adapter.setPickGroupMode(true)
                    Toast.makeText(requireContext(), "Select a group to move albums into", Toast.LENGTH_SHORT).show()
                }

                private fun exitPickGroupMode() {
                    pickGroupMode = false
                    adapter.setPickGroupMode(false)
                    adapter.exitSelectionMode()
                    if (toolbarLayout.isActionMode) {
                        toolbarLayout.dismissActionMode()
                    }
                    hideBottomActionBar()
                }

                private fun groupSelectedAlbums(groupName: String) {
                    val selectedAlbumNames = adapter.getSelectedAlbums()

                    // Update all media files
                    mediaList.forEach { media ->
                        if (media.folderName != null && selectedAlbumNames.contains(media.folderName)) {
                            media.groupName = groupName
                            AlbumGroupPrefs.setGroup(requireContext(), media.folderName!!, groupName)
                        }
                    }
                    adapter.refreshData(mediaList)
                    exitPickGroupMode()
                }

                private fun generateNextGroupName(): String {
                    val existing = AlbumRepository.albumItems
                        .filterIsInstance<AlbumItem.Group>()
                        .map { it.groupName }

                    var index = 1
                    while (true) {
                        val name = "Group $index"
                        if (existing.none { it.equals(name, true) }) {
                            return name
                        }
                        index++
                    }
                }

                private fun resetActionModeTitlePadding() {
                    val titleView = toolbarLayout.findViewById<TextView>(
                        dev.oneuiproject.oneui.design.R.id.toolbar_layout_action_mode_title
                    )

                    titleView?.setPaddingRelative(
                        0,
                        titleView.paddingTop,
                        titleView.paddingEnd,
                        titleView.paddingBottom
                    )
                }

                private fun setupBackPress() {
                    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                        when {
                            // Handle essential albums mode
                            adapter.isManagingEssential() -> {
                                adapter.exitSelectionMode()

                                // Clear action mode toolbar menu
                                val actionModeToolbar = toolbarLayout.findViewById<androidx.appcompat.widget.Toolbar>(
                                    dev.oneuiproject.oneui.design.R.id.toolbarlayout_action_mode_toolbar
                                )
                                actionModeToolbar?.let { toolbar ->
                                    toolbar.menu.clear()
                                    toolbar.setOnMenuItemClickListener(null)
                                }

                                // Restore Select All visibility
                                val selectAllContainer = toolbarLayout.findViewById<LinearLayout>(
                                    dev.oneuiproject.oneui.design.R.id.toolbarlayout_selectall
                                )
                                selectAllContainer?.visibility = View.VISIBLE
                                resetActionModeTitlePadding()

                                toolbarLayout.dismissActionMode()

                                // Reload and restore menu
                                val showOnlyEssential = PreferenceManager.getDefaultSharedPreferences(requireContext())
                                    .getBoolean("SHOW_ESSENTIAL_ALBUMS", false)
                                headerAdapter.setVisible(true)
                                reloadAlbums(showOnlyEssential)
                                requireActivity().invalidateOptionsMenu()

                                hideBottomActionBar()
                            }
                            pickGroupMode -> {
                                exitPickGroupMode()
                            }
                            adapter.isInSelectionMode() -> {
                                adapter.exitSelectionMode()
                                toolbarLayout.dismissActionMode()
                                hideBottomActionBar()
                            }
                            else -> {
                                isEnabled = false
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                        }
                    }
                }

                private fun loadImages() {
                    val mediaFetcher = Albums(requireContext())
                    val allAlbums = mediaFetcher.fetchAlbums().toMutableList()

                    allAlbums.forEach { media ->
                        media.groupName = AlbumGroupPrefs.getGroup(requireContext(), media.folderName)
                    }

                    mediaList = allAlbums

                    if (!::adapter.isInitialized) {
                        adapter = AlbumAdapter(requireContext(), mediaList)
                        adapter.setSortType(
                            (activity as MainActivity).getSortTypeFromIndex(
                                (activity as MainActivity).loadSortIndex()
                            )
                        )
                    }
                }

                fun reloadAlbums(showOnlyEssential: Boolean) {
                    this.showOnlyEssential = showOnlyEssential
                    adapter.setShowOnlyEssential(showOnlyEssential)
                    headerAdapter.update(showOnlyEssential)
                }

                fun setSortType(sortType: AlbumSortType) {
                    adapter.setSortType(sortType)
                }

                override fun onDestroyView() {
                    super.onDestroyView()
                    // Clean up the floating action bar
                    if (::bottomActionCard.isInitialized) {
                        (bottomActionCard.parent as? ViewGroup)?.removeView(bottomActionCard)
                    }
                }

                class Utils : PreferenceFragmentCompat() {
                    private val DIALOG_ID = 0

                    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
                        setPreferencesFromResource(R.xml.preferences, rootKey)

                        val ai: Preference? = findPreference("safe")
                        val quote: Preference? = findPreference("quotes")

                        ai?.setOnPreferenceClickListener {
                            startActivity(Intent(requireContext(), PrivateSafe::class.java))
                            true
                        }
                        quote?.setOnPreferenceClickListener {
                            ColorPickerDialog.newBuilder()
                                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                                .setAllowPresets(false)
                                .setDialogId(DIALOG_ID)
                                .setColor(android.graphics.Color.BLACK)
                                .setShowAlphaSlider(true)
                                .show(requireActivity())
                            true
                        }
                    }
                }
            }
        }
    }