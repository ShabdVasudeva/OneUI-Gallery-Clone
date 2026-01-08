package apw.sec.android.gallery

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import apw.sec.android.gallery.components.AlbumImage
import apw.sec.android.gallery.components.GroupCoverView
import com.bumptech.glide.Glide

class AlbumAdapter(
    private val context: Context,
    private var allMediaFiles: List<MediaFile>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ALBUM = 0
        private const val TYPE_GROUP = 1
    }

    enum class SelectionPurpose {
        NORMAL,
        MANAGE_ESSENTIAL
    }

    private var sortType: AlbumSortType = AlbumSortType.CUSTOM
    private val visibleItems: MutableList<AlbumItem> = mutableListOf()
    private var allItems: List<AlbumItem> = emptyList()

    private var selectionPurpose = SelectionPurpose.NORMAL

    private fun albumKey(folder: String) = "ALBUM::$folder"
    private fun groupKey(group: String) = "GROUP::$group"

    private val essentialAlbums = AlbumPrefs.getEssentialAlbums(context).toMutableSet()
    private val selectedAlbums = mutableSetOf<String>()
    fun getSelectedKeys(): Set<String> = selectedAlbums

    private var selectionMode = false
    private var showOnlyEssential = false
    var selectionListener: SelectionListener? = null
    private var pickGroupMode = false

    init {
        setHasStableIds(true)
        allItems = AlbumItemBuilder.build(allMediaFiles, sortType)
        AlbumRepository.albumItems = allItems
        rebuildVisibleItems()
    }

    interface SelectionListener {
        fun onSelectionChanged(count: Int, isAllSelected: Boolean)
    }

    fun isManagingEssential(): Boolean = selectionPurpose == SelectionPurpose.MANAGE_ESSENTIAL

    fun getSelectedCount(): Int = selectedAlbums.size

    fun getTotalVisibleCount(): Int = visibleItems.size

    fun setPickGroupMode(enabled: Boolean) {
        pickGroupMode = enabled
        notifyDataSetChanged()
    }

    fun getVisibleSelectionCount(): Int {
        var count = 0
        visibleItems.forEach { item ->
            when (item) {
                is AlbumItem.Album -> {
                    if (selectedAlbums.contains(albumKey(item.folderName))) {
                        count++
                    }
                }
                is AlbumItem.Group -> {
                    if (selectedAlbums.contains(groupKey(item.groupName))) {
                        count++
                    }
                }
            }
        }
        return count
    }

    fun enterEssentialSelectionMode() {
        selectionPurpose = SelectionPurpose.MANAGE_ESSENTIAL
        selectionMode = true

        showOnlyEssential = false
        rebuildVisibleItems()

        selectedAlbums.clear()

        essentialAlbums.forEach { folderName ->
            selectedAlbums.add(albumKey(folderName))
        }

        allItems.filterIsInstance<AlbumItem.Group>().forEach { group ->
            if (group.albums.all { essentialAlbums.contains(it.folderName) }) {
                selectedAlbums.add(groupKey(group.groupName))
            }
        }

        notifyDataSetChanged()
        notifySelectionChanged()
    }

    fun confirmEssentialSelection() {
        val newEssentialAlbums = mutableSetOf<String>()
        selectedAlbums
            .filter { it.startsWith("ALBUM::") }
            .forEach { key ->
                val folderName = key.removePrefix("ALBUM::")
                newEssentialAlbums.add(folderName)
            }

        selectedAlbums
            .filter { it.startsWith("GROUP::") }
            .forEach { key ->
                val groupName = key.removePrefix("GROUP::")
                val group = allItems.filterIsInstance<AlbumItem.Group>()
                    .firstOrNull { it.groupName == groupName }

                group?.albums?.forEach { album ->
                    newEssentialAlbums.add(album.folderName)
                }
            }
        essentialAlbums.clear()
        essentialAlbums.addAll(newEssentialAlbums)
        AlbumPrefs.setEssentialAlbums(context, essentialAlbums)

        showOnlyEssential = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("SHOW_ESSENTIAL_ALBUMS", false)

        selectionPurpose = SelectionPurpose.NORMAL
        selectionMode = false
        selectedAlbums.clear()
        rebuildVisibleItems()
        notifyDataSetChanged()
    }

    fun setSortType(type: AlbumSortType) {
        sortType = type
        allItems = AlbumItemBuilder.build(allMediaFiles, sortType)
        AlbumRepository.albumItems = allItems
        rebuildVisibleItems()
        notifyDataSetChanged()
    }

    fun setShowOnlyEssential(show: Boolean) {
        showOnlyEssential = show
        rebuildVisibleItems()
        notifyDataSetChanged()
    }

    private fun rebuildVisibleItems() {
        visibleItems.clear()

        if (allItems.isEmpty()) return

        if (selectionPurpose == SelectionPurpose.MANAGE_ESSENTIAL) {
            visibleItems.addAll(allItems)
            return
        }

        if (!showOnlyEssential) {
            visibleItems.addAll(allItems)
            return
        }

        allItems.forEach { item ->
            when (item) {
                is AlbumItem.Album -> {
                    if (essentialAlbums.contains(item.folderName)) {
                        visibleItems.add(item)
                    }
                }
                is AlbumItem.Group -> {
                    if (item.albums.any { essentialAlbums.contains(it.folderName) }) {
                        visibleItems.add(item)
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        when (visibleItems[position]) {
            is AlbumItem.Album -> TYPE_ALBUM
            is AlbumItem.Group -> TYPE_GROUP
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.album_item, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as AlbumViewHolder).bind(visibleItems[position])
    }

    override fun getItemCount() = visibleItems.size

    override fun getItemId(position: Int): Long {
        return when (val item = visibleItems[position]) {
            is AlbumItem.Album -> ("ALBUM::" + item.folderName).hashCode().toLong()
            is AlbumItem.Group -> ("GROUP::" + item.groupName).hashCode().toLong()
        }
    }

    private fun notifySelectionChanged() {
        val visibleCount = getVisibleSelectionCount()
        selectionListener?.onSelectionChanged(visibleCount, isAllSelected())
    }

    fun isInSelectionMode() = selectionMode

    fun getSelectedAlbums(): Set<String> =
        selectedAlbums.mapNotNull {
            when {
                it.startsWith("ALBUM::") -> it.removePrefix("ALBUM::")
                else -> null
            }
        }.toSet()

    fun isAllSelected(): Boolean {
        val total = visibleItems.count { it is AlbumItem.Album || it is AlbumItem.Group }
        val visibleCount = getVisibleSelectionCount()
        return total > 0 && visibleCount == total
    }

    private fun enterSelectionMode() {
        if (selectionMode) return
        selectionMode = true
    }

    fun exitSelectionMode() {
        selectionPurpose = SelectionPurpose.NORMAL
        selectionMode = false
        selectedAlbums.clear()
        rebuildVisibleItems()
        notifyDataSetChanged()
        notifySelectionChanged()
    }

    fun triggerSelectionMode() {
        if (!selectionMode) {
            enterSelectionMode()
            notifyDataSetChanged()
            notifySelectionChanged()
        }
    }

    private fun toggleSelection(key: String) {
        if (pickGroupMode) return
        enterSelectionMode()

        if (selectedAlbums.contains(key)) {
            // DESELECTING
            selectedAlbums.remove(key)

            if (key.startsWith("GROUP::")) {
                val groupName = key.removePrefix("GROUP::")
                val group = allItems.filterIsInstance<AlbumItem.Group>()
                    .firstOrNull { it.groupName == groupName }

                group?.albums?.forEach { album ->
                    selectedAlbums.remove(albumKey(album.folderName))
                }
            }
            else if (key.startsWith("ALBUM::")) {
                val folderName = key.removePrefix("ALBUM::")

                // Find if this album is part of any selected group
                allItems.filterIsInstance<AlbumItem.Group>().forEach { group ->
                    if (group.albums.any { it.folderName == folderName } &&
                        selectedAlbums.contains(groupKey(group.groupName))) {
                        // Remove the group selection too
                        selectedAlbums.remove(groupKey(group.groupName))
                    }
                }
            }
        } else {
            // SELECTING
            selectedAlbums.add(key)

            if (key.startsWith("GROUP::")) {
                val groupName = key.removePrefix("GROUP::")
                val group = allItems.filterIsInstance<AlbumItem.Group>()
                    .firstOrNull { it.groupName == groupName }

                group?.albums?.forEach { album ->
                    selectedAlbums.add(albumKey(album.folderName))
                }
            }
            else if (key.startsWith("ALBUM::")) {
                val folderName = key.removePrefix("ALBUM::")
                allItems.filterIsInstance<AlbumItem.Group>().forEach { group ->
                    if (group.albums.any { it.folderName == folderName }) {
                        val allSelected = group.albums.all { album ->
                            selectedAlbums.contains(albumKey(album.folderName))
                        }
                        if (allSelected) {
                            // Add the group selection
                            selectedAlbums.add(groupKey(group.groupName))
                        }
                    }
                }
            }
        }
        notifyDataSetChanged()
        notifySelectionChanged()
    }

    fun toggleSelectAll() {
        if (!selectionMode) {
            selectionMode = true
        }

        val visibleCount = getVisibleSelectionCount()
        val total = visibleItems.count {
            it is AlbumItem.Album || it is AlbumItem.Group
        }

        if (visibleCount == total && total > 0) {
            selectedAlbums.clear()
        } else {
            selectedAlbums.clear()
            visibleItems.forEach {
                when (it) {
                    is AlbumItem.Album -> selectedAlbums.add(albumKey(it.folderName))
                    is AlbumItem.Group -> {
                        selectedAlbums.add(groupKey(it.groupName))
                        it.albums.forEach { album ->
                            selectedAlbums.add(albumKey(album.folderName))
                        }
                    }
                }
            }
        }
        notifyDataSetChanged()
        notifySelectionChanged()
    }

    fun shareSelectedAlbums() {
        if (selectedAlbums.isEmpty()) return

        val uriSet = LinkedHashSet<android.net.Uri>()

        allItems.forEach { item ->
            when (item) {
                is AlbumItem.Album -> {
                    if (selectedAlbums.contains(albumKey(item.folderName))) {
                        item.media.forEach { media ->
                            uriSet.add(media.uri.toUri())
                        }
                    }
                }

                is AlbumItem.Group -> {
                    if (selectedAlbums.contains(groupKey(item.groupName))) {
                        item.albums.forEach { album ->
                            album.media.forEach { media ->
                                uriSet.add(media.uri.toUri())
                            }
                        }
                    }
                }
            }
        }

        if (uriSet.isEmpty()) {
            Toast.makeText(context, "Nothing to share", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uriSet))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share files"))
        exitSelectionMode()
    }

    inner class AlbumViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: AlbumImage = view.findViewById(R.id.imageView)
        private val name: TextView = view.findViewById(R.id.text)
        private val count: TextView = view.findViewById(R.id.text2)
        private val essentialIcon: ImageView = view.findViewById(R.id.essential_icon)
        private val cameraIcon: ImageView = view.findViewById(R.id.camera_icon)
        private val selectionIndicator: View = view.findViewById(R.id.selection_indicator)
        private val checkIcon: ImageView = view.findViewById(R.id.check_icon)
        private val dimOverlay: View = view.findViewById(R.id.dim_overlay)
        private val groupCover: GroupCoverView = view.findViewById(R.id.groupCover)

        fun bind(item: AlbumItem) {
            when (item) {
                is AlbumItem.Album -> bindAlbum(item)
                is AlbumItem.Group -> bindGroup(item)
            }
        }

        private fun bindAlbum(album: AlbumItem.Album) {
            name.text = AlbumNamePrefs.getName(context, album.folderName) ?: album.folderName
            count.text = album.media.size.toString()
            count.visibility = View.VISIBLE
            imageView.visibility = View.VISIBLE
            groupCover.visibility = View.GONE

            Glide.with(context)
                .load(album.media.firstOrNull()?.uri)
                .centerCrop()
                .into(imageView)

            cameraIcon.visibility =
                if (album.folderName.equals("Camera", ignoreCase = true)) View.VISIBLE
                else View.GONE

            essentialIcon.visibility =
                if (essentialAlbums.contains(album.folderName)) View.VISIBLE
                else View.GONE

            if (selectionMode && !pickGroupMode) {
                selectionIndicator.visibility = View.VISIBLE

                val key = albumKey(album.folderName)

                if (selectedAlbums.contains(key)) {
                    selectionIndicator.background =
                        ContextCompat.getDrawable(context, R.drawable.album_bg_circle_selected)
                    checkIcon.visibility = View.VISIBLE
                    dimOverlay.visibility = View.VISIBLE
                } else {
                    selectionIndicator.background =
                        ContextCompat.getDrawable(context, R.drawable.album_bg_circle_unselected)
                    checkIcon.visibility = View.GONE
                    dimOverlay.visibility = View.GONE
                }

                itemView.setOnClickListener {
                    toggleSelection(key)
                }

                itemView.setOnLongClickListener {
                    toggleSelection(key)
                    true
                }
            } else {
                selectionIndicator.visibility = View.GONE
                checkIcon.visibility = View.GONE
                dimOverlay.visibility = View.GONE

                itemView.setOnClickListener {
                    if (!pickGroupMode) {
                        context.startActivity(
                            Intent(context, AlbumViewer::class.java)
                                .putExtra("folderName", album.folderName)
                        )
                    }
                }

                itemView.setOnLongClickListener {
                    toggleSelection(albumKey(album.folderName))
                    true
                }
            }

            if (pickGroupMode) {
                itemView.alpha = 0.4f
                itemView.isClickable = false
                itemView.isEnabled = false
            } else {
                itemView.alpha = 1f
                itemView.isClickable = true
                itemView.isEnabled = true
            }
        }

        private fun bindGroup(group: AlbumItem.Group) {
            name.text = group.groupName
            count.visibility = View.GONE

            essentialIcon.visibility =
                if (group.albums.any { essentialAlbums.contains(it.folderName) }) View.VISIBLE
                else View.GONE

            imageView.visibility = View.GONE
            groupCover.visibility = View.VISIBLE

            val uris = group.albums.mapNotNull { it.media.firstOrNull()?.uri?.toString() }
            groupCover.setMedia(uris)

            val key = groupKey(group.groupName)

            if (selectionMode && !pickGroupMode) {
                selectionIndicator.visibility = View.VISIBLE

                if (selectedAlbums.contains(key)) {
                    selectionIndicator.background =
                        ContextCompat.getDrawable(context, R.drawable.album_bg_circle_selected)
                    checkIcon.visibility = View.VISIBLE
                    dimOverlay.visibility = View.VISIBLE
                } else {
                    selectionIndicator.background =
                        ContextCompat.getDrawable(context, R.drawable.album_bg_circle_unselected)
                    checkIcon.visibility = View.GONE
                    dimOverlay.visibility = View.GONE
                }

                itemView.setOnClickListener {
                    toggleSelection(key)
                }

                itemView.setOnLongClickListener {
                    toggleSelection(key)
                    true
                }
            } else {
                selectionIndicator.visibility = View.GONE
                checkIcon.visibility = View.GONE
                dimOverlay.visibility = View.GONE

                itemView.setOnClickListener {
                    if (pickGroupMode) {
                        moveSelectedToGroup(group.groupName)
                        pickGroupMode = false
                        return@setOnClickListener
                    }

                    context.startActivity(
                        Intent(context, GroupAlbumActivity::class.java)
                            .putExtra("groupName", group.groupName)
                    )
                }

                itemView.setOnLongClickListener {
                    toggleSelection(groupKey(group.groupName))
                    true
                }
            }
        }
    }

    private fun showNameDialog(
        title: String,
        initialText: String,
        confirmText: String,
        hint: String,
        onConfirm: (String) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.alert_dialog, null)
        val input = view.findViewById<EditText>(R.id.input)

        input.setText(initialText)
        input.setSelection(0, initialText.length)
        input.hint = hint

        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(confirmText, null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.isEnabled = initialText.isNotBlank()

            input.addTextChangedListener {
                positive.isEnabled = it.toString().trim().isNotEmpty()
            }

            positive.setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    onConfirm(name)
                    dialog.dismiss()
                }
            }

            input.requestFocus()
        }

        dialog.show()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    fun renameSelected() {
        val visibleCount = getVisibleSelectionCount()
        if (visibleCount != 1) return

        val key = selectedAlbums.firstOrNull { it.startsWith("ALBUM::") || it.startsWith("GROUP::") } ?: return

        when {
            key.startsWith("ALBUM::") -> {
                val folder = key.removePrefix("ALBUM::")
                val album = allItems.filterIsInstance<AlbumItem.Album>()
                    .firstOrNull { it.folderName == folder } ?: return

                showNameDialog(
                    title = "Rename album",
                    initialText = AlbumNamePrefs.getName(context, album.folderName) ?: album.folderName,
                    hint = "Enter album name",
                    confirmText = "Rename"
                ) { newName ->
                    AlbumNamePrefs.setName(context, album.folderName, newName)
                    notifyDataSetChanged()
                    exitSelectionMode()
                }
            }

            key.startsWith("GROUP::") -> {
                val groupName = key.removePrefix("GROUP::")
                val group = allItems.filterIsInstance<AlbumItem.Group>()
                    .firstOrNull { it.groupName == groupName } ?: return

                showGroupNameDialog(
                    title = "Rename group",
                    initialText = group.groupName,
                    hint = "Enter group name",
                    confirmText = "Rename",
                    excludeGroupName = group.groupName
                ) { newName ->
                    group.albums.forEach { album ->
                        album.media.forEach { media ->
                            media.groupName = newName
                            AlbumGroupPrefs.setGroup(context, media.folderName!!, newName)
                        }
                    }

                    allItems = AlbumItemBuilder.build(allMediaFiles, sortType)
                    AlbumRepository.albumItems = allItems
                    rebuildVisibleItems()
                    notifyDataSetChanged()
                    exitSelectionMode()
                }
            }
        }
    }

    fun createGroup(defaultName: String, onCreated: (String) -> Unit) {
        showGroupNameDialog(
            title = "Create group",
            initialText = defaultName,
            hint = "Enter group name",
            confirmText = "Create",
            excludeGroupName = null
        ) { name ->
            onCreated(name)
        }
    }

    private fun showGroupNameDialog(
        title: String,
        initialText: String,
        confirmText: String,
        hint: String,
        excludeGroupName: String?,
        onConfirm: (String) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.alert_dialog, null)
        val input = view.findViewById<EditText>(R.id.input)
        val errorText = view.findViewById<TextView>(R.id.error_text)

        input.setText(initialText)
        input.setSelection(0, initialText.length)
        input.hint = hint

        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(confirmText, null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.isEnabled = initialText.isNotBlank()

            fun checkDuplicateName(name: String): Boolean {
                if (name.trim().isEmpty()) return false

                if (excludeGroupName != null && name.equals(excludeGroupName, ignoreCase = true)) {
                    return false
                }

                return allItems.any {
                    it is AlbumItem.Group && it.groupName.equals(name, ignoreCase = true)
                }
            }

            input.addTextChangedListener {
                val text = it.toString().trim()
                val isEmpty = text.isEmpty()
                val isDuplicate = checkDuplicateName(text)

                when {
                    isEmpty -> {
                        positive.isEnabled = false
                        errorText.visibility = View.GONE
                    }
                    isDuplicate -> {
                        positive.isEnabled = false
                        errorText.text = "Group name already exists"
                        errorText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                        errorText.visibility = View.VISIBLE
                    }
                    else -> {
                        positive.isEnabled = true
                        errorText.visibility = View.GONE
                    }
                }
            }

            positive.setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isNotEmpty() && !checkDuplicateName(name)) {
                    onConfirm(name)
                    dialog.dismiss()
                }
            }
            input.requestFocus()
        }

        dialog.show()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    fun ungroup(groupName: String) {
        val group = allItems.filterIsInstance<AlbumItem.Group>()
            .firstOrNull { it.groupName == groupName } ?: return

        group.albums.forEach { album ->
            album.media.forEach { media ->
                media.groupName = null
                allMediaFiles.find { it.uri == media.uri }?.groupName = null
                if (media.folderName != null) {
                    AlbumGroupPrefs.clearGroup(context, media.folderName!!)
                }
            }
        }

        allItems = AlbumItemBuilder.build(allMediaFiles, sortType)
        AlbumRepository.albumItems = allItems
        rebuildVisibleItems()
        notifyDataSetChanged()
        exitSelectionMode()
    }

    fun removeSelectedFromEssential() {
        selectedAlbums
            .filter { it.startsWith("ALBUM::") }
            .map { it.removePrefix("ALBUM::") }
            .forEach { folder ->
                essentialAlbums.remove(folder)
            }

        selectedAlbums
            .filter { it.startsWith("GROUP::") }
            .forEach { key ->
                val groupName = key.removePrefix("GROUP::")
                val group = allItems.filterIsInstance<AlbumItem.Group>()
                    .firstOrNull { it.groupName == groupName }

                group?.albums?.forEach { album ->
                    essentialAlbums.remove(album.folderName)
                }
            }

        AlbumPrefs.setEssentialAlbums(context, essentialAlbums)
        rebuildVisibleItems()
        notifyDataSetChanged()
        exitSelectionMode()
    }

    fun deleteSelected() {
        val toDelete = selectedAlbums.filter { it.startsWith("ALBUM::") }
            .map { it.removePrefix("ALBUM::") }
            .toSet()
        if (toDelete.isEmpty()) return

        val resolver = context.contentResolver
        var deletedCount = 0

        allItems.forEach { item ->
            if (item is AlbumItem.Album && toDelete.contains(item.folderName)) {
                item.media.forEach { file ->
                    try {
                        val rows = resolver.delete(file.uri.toUri(), null, null)
                        if (rows > 0) deletedCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        Toast.makeText(context, "$deletedCount file(s) deleted", Toast.LENGTH_SHORT).show()
        exitSelectionMode()
    }

    fun moveSelectedToGroup(groupName: String) {
        val selected = selectedAlbums
            .filter { it.startsWith("ALBUM::") }
            .map { it.removePrefix("ALBUM::") }
            .toSet()

        if (selected.isEmpty()) return

        allItems.forEach { item ->
            if (item is AlbumItem.Album && selected.contains(item.folderName)) {
                item.media.forEach { media ->
                    media.groupName = groupName
                    AlbumGroupPrefs.setGroup(context, media.folderName!!, groupName)
                }
            }
        }

        allItems = AlbumItemBuilder.build(allMediaFiles, sortType)
        AlbumRepository.albumItems = allItems
        rebuildVisibleItems()
        notifyDataSetChanged()
        exitSelectionMode()
    }

    fun refreshData(newMediaFiles: List<MediaFile>) {
        allMediaFiles = newMediaFiles
        allItems = AlbumItemBuilder.build(allMediaFiles, sortType)
        AlbumRepository.albumItems = allItems
        rebuildVisibleItems()
        notifyDataSetChanged()
    }
}
