
package com.scanpro.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val DMInk = Color(0xFF17243A)
private val DMBlue = Color(0xFF2864E8)
private val DMMuted = Color(0xFF77849A)
private val DMCanvas = Color(0xFFF5F7FB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentManagerScreen(
    files: List<ScanFile>,
    rootDirectory: File,
    onOpen: (ScanFile) -> Unit,
    onShare: (ScanFile) -> Unit,
    onRefresh: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("Terbaru") }
    var sortExpanded by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    var showCreateFolder by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf<ScanFile?>(null) }
    var renameFolder by remember { mutableStateOf<String?>(null) }
    var deleteFolder by remember { mutableStateOf<String?>(null) }

    var folderName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ScanFile?>(null) }
    var deleteTarget by remember { mutableStateOf<ScanFile?>(null) }
    var newName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    val folders = remember(files) {
        rootDirectory.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    val visibleFiles = remember(files, selectedFolder) {
        if (selectedFolder == null) {
            files
        } else {
            files.filter {
                File(it.path).parentFile?.name == selectedFolder
            }
        }
    }

    val filteredFiles = remember(visibleFiles, search, sort) {
        val filtered = visibleFiles.filter {
            it.name.contains(search.trim(), ignoreCase = true)
        }

        when (sort) {
            "Terlama" -> filtered.sortedBy {
                File(it.path).lastModified()
            }
            "Nama A-Z" -> filtered.sortedBy {
                it.name.lowercase()
            }
            "Nama Z-A" -> filtered.sortedByDescending {
                it.name.lowercase()
            }
            else -> filtered.sortedByDescending {
                File(it.path).lastModified()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DMCanvas)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Semua dokumen",
                    color = DMInk,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${files.size} dokumen tersimpan",
                    color = DMMuted,
                    fontSize = 13.sp
                )
            }

            FilledIconButton(
                onClick = {
                    folderName = ""
                    showCreateFolder = true
                }
            ) {
                Icon(Icons.Default.CreateNewFolder, "Buat folder")
            }
        }

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari nama dokumen...") },
            leadingIcon = {
                Icon(Icons.Default.Search, "Cari")
            },
            trailingIcon = {
                if (search.isNotEmpty()) {
                    IconButton(onClick = { search = "" }) {
                        Icon(Icons.Default.Close, "Hapus pencarian")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DMBlue,
                unfocusedBorderColor = Color(0xFFE0E5ED)
            )
        )

        Spacer(Modifier.height(14.dp))

        Text(
            "Folder",
            color = DMInk,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedFolder == null,
                onClick = { selectedFolder = null },
                label = { Text("Semua") },
                leadingIcon = {
                    Icon(Icons.Default.FolderOpen, null)
                }
            )
        }

        if (folders.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 150.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(folders, key = { it.absolutePath }) { folder ->
                    FolderRow(
                        folder = folder,
                        selected = selectedFolder == folder.name,
                        count = files.count {
                            File(it.path).parentFile?.name == folder.name
                        },
                        onSelect = {
                            selectedFolder = folder.name
                        },
                        onRename = {
                            renameFolder = folder.name
                            folderName = folder.name
                        },
                        onDelete = {
                            deleteFolder = folder.name
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (selectedFolder == null) "Semua file" else selectedFolder!!,
                color = DMInk,
                fontWeight = FontWeight.Bold
            )

            Box {
                OutlinedButton(onClick = { sortExpanded = true }) {
                    Icon(Icons.Default.Sort, null)
                    Spacer(Modifier.width(5.dp))
                    Text(sort)
                }

                DropdownMenu(
                    expanded = sortExpanded,
                    onDismissRequest = { sortExpanded = false }
                ) {
                    listOf(
                        "Terbaru",
                        "Terlama",
                        "Nama A-Z",
                        "Nama Z-A"
                    ).forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                sort = option
                                sortExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (message != null) {
            Text(
                message!!,
                color = DMBlue,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        if (filteredFiles.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 35.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    null,
                    tint = Color(0xFF9AA8BC),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Belum ada dokumen",
                    color = DMInk,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Dokumen pada folder ini akan muncul di sini.",
                    color = DMMuted,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredFiles, key = { it.path }) { file ->
                    DocumentCard(
                        file = file,
                        onOpen = { onOpen(file) },
                        onShare = { onShare(file) },
                        onMove = { showMoveDialog = file },
                        onRename = {
                            renameTarget = file
                            newName = file.name.removeSuffix(".pdf")
                        },
                        onDelete = { deleteTarget = file }
                    )
                }
            }
        }
    }

    if (showCreateFolder) {
        FolderNameDialog(
            title = "Buat folder baru",
            value = folderName,
            onValueChange = { folderName = it },
            onDismiss = { showCreateFolder = false },
            onConfirm = {
                val clean = cleanFolderName(folderName)
                val destination = File(rootDirectory, clean)

                when {
                    clean.isBlank() -> message = "Nama folder tidak boleh kosong."
                    destination.exists() -> message = "Folder sudah tersedia."
                    destination.mkdirs() -> {
                        message = "Folder berhasil dibuat."
                        showCreateFolder = false
                        onRefresh()
                    }
                    else -> message = "Gagal membuat folder."
                }
            }
        )
    }

    renameFolder?.let { oldName ->
        FolderNameDialog(
            title = "Ganti nama folder",
            value = folderName,
            onValueChange = { folderName = it },
            onDismiss = { renameFolder = null },
            onConfirm = {
                val clean = cleanFolderName(folderName)
                val oldDir = File(rootDirectory, oldName)
                val newDir = File(rootDirectory, clean)

                when {
                    clean.isBlank() -> message = "Nama folder tidak boleh kosong."
                    clean == oldName -> {
                        renameFolder = null
                    }
                    newDir.exists() -> message = "Nama folder sudah digunakan."
                    oldDir.renameTo(newDir) -> {
                        message = "Folder berhasil diganti nama."
                        if (selectedFolder == oldName) selectedFolder = clean
                        renameFolder = null
                        onRefresh()
                    }
                    else -> message = "Gagal mengganti nama folder."
                }
            }
        )
    }

    deleteFolder?.let { name ->
        AlertDialog(
            onDismissRequest = { deleteFolder = null },
            title = { Text("Hapus folder?") },
            text = {
                Text(
                    "Folder $name hanya dapat dihapus jika masih kosong."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val dir = File(rootDirectory, name)
                    if (dir.isDirectory && dir.listFiles()?.isEmpty() == true && dir.delete()) {
                        message = "Folder berhasil dihapus."
                        if (selectedFolder == name) selectedFolder = null
                        onRefresh()
                    } else {
                        message = "Folder tidak kosong atau gagal dihapus."
                    }
                    deleteFolder = null
                }) {
                    Text("Hapus", color = Color(0xFFD64545))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteFolder = null }) {
                    Text("Batal")
                }
            }
        )
    }

    showMoveDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { showMoveDialog = null },
            title = { Text("Pindahkan dokumen") },
            text = {
                Column {
                    Text("Pilih folder tujuan:")
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        moveFile(file, rootDirectory, null) {
                            message = it
                            onRefresh()
                        }
                        showMoveDialog = null
                    }) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Penyimpanan utama")
                    }
                    folders.forEach { folder ->
                        TextButton(onClick = {
                            moveFile(file, rootDirectory, folder.name) {
                                message = it
                                onRefresh()
                            }
                            showMoveDialog = null
                        }) {
                            Icon(Icons.Default.Folder, null)
                            Spacer(Modifier.width(8.dp))
                            Text(folder.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveDialog = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    renameTarget?.let { target ->
        FolderNameDialog(
            title = "Ganti nama dokumen",
            value = newName,
            onValueChange = { newName = it },
            onDismiss = { renameTarget = null },
            onConfirm = {
                val oldFile = File(target.path)
                val clean = cleanFolderName(newName)
                val destination = File(oldFile.parentFile, "$clean.pdf")

                when {
                    clean.isBlank() -> message = "Nama dokumen tidak boleh kosong."
                    destination.exists() && destination.absolutePath != oldFile.absolutePath ->
                        message = "Nama dokumen sudah digunakan."
                    oldFile.renameTo(destination) -> {
                        message = "Nama dokumen berhasil diubah."
                        renameTarget = null
                        onRefresh()
                    }
                    else -> message = "Gagal mengganti nama dokumen."
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus dokumen?") },
            text = { Text("Dokumen ${target.name} akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    if (File(target.path).delete()) {
                        message = "Dokumen berhasil dihapus."
                        onRefresh()
                    } else {
                        message = "Gagal menghapus dokumen."
                    }
                    deleteTarget = null
                }) {
                    Text("Hapus", color = Color(0xFFD64545))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

private fun cleanFolderName(value: String): String {
    return value.trim()
        .replace(Regex("[/\\\\:*?\"<>|]"), "_")
}

private fun moveFile(
    file: ScanFile,
    root: File,
    folderName: String?,
    result: (String) -> Unit
) {
    val source = File(file.path)
    val destinationDir = if (folderName == null) {
        root
    } else {
        File(root, folderName)
    }

    if (!destinationDir.exists()) destinationDir.mkdirs()

    val destination = File(destinationDir, source.name)

    when {
        source.parentFile?.absolutePath == destinationDir.absolutePath ->
            result("Dokumen sudah berada di folder tersebut.")
        destination.exists() ->
            result("Nama file sudah ada di folder tujuan.")
        source.renameTo(destination) ->
            result("Dokumen berhasil dipindahkan.")
        else ->
            result("Gagal memindahkan dokumen.")
    }
}

@Composable
private fun FolderNameDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Nama") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun FolderRow(
    folder: File,
    selected: Boolean,
    count: Int,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFE5EDFF) else Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                null,
                tint = DMBlue
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    folder.name,
                    color = DMInk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    "$count dokumen",
                    color = DMMuted,
                    fontSize = 11.sp
                )
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, "Menu folder")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ganti nama") },
                        onClick = {
                            expanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus folder kosong") },
                        onClick = {
                            expanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(
    file: ScanFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val sizeBytes = File(file.path).length()
    val sizeText = if (sizeBytes >= 1024 * 1024) {
        String.format(java.util.Locale.US, "%.2f MB", sizeBytes / (1024.0 * 1024.0))
    } else {
        String.format(java.util.Locale.US, "%.1f KB", sizeBytes / 1024.0)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PictureAsPdf,
                null,
                tint = Color(0xFFD64B4B),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    file.name,
                    color = DMInk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    "${file.created} • $sizeText",
                    color = DMMuted,
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, "Bagikan", tint = DMBlue)
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, "Menu dokumen")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Pindahkan ke folder") },
                        onClick = {
                            expanded = false
                            onMove()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ganti nama") },
                        onClick = {
                            expanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus") },
                        onClick = {
                            expanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
