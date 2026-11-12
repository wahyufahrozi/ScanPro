
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
    onOpen: (ScanFile) -> Unit,
    onShare: (ScanFile) -> Unit,
    onRefresh: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("Terbaru") }
    var sortExpanded by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ScanFile?>(null) }
    var deleteTarget by remember { mutableStateOf<ScanFile?>(null) }
    var newName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    val filteredFiles = remember(files, search, sort) {
        val filtered = files.filter {
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

        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari nama dokumen...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Cari")
            },
            trailingIcon = {
                if (search.isNotEmpty()) {
                    IconButton(onClick = { search = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus pencarian")
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

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${filteredFiles.size} hasil",
                color = DMMuted,
                fontSize = 12.sp
            )

            Box {
                OutlinedButton(onClick = { sortExpanded = true }) {
                    Icon(
                        Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
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

        Spacer(Modifier.height(12.dp))

        if (message != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F4EC)
                )
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        message!!,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF205D39),
                        fontSize = 12.sp
                    )
                    IconButton(onClick = { message = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (filteredFiles.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 55.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = Color(0xFF9AA8BC),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (search.isBlank())
                        "Belum ada dokumen"
                    else
                        "Dokumen tidak ditemukan",
                    color = DMInk,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Dokumen PDF akan ditampilkan di sini.",
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

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Ganti nama dokumen") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nama dokumen") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val oldFile = File(target.path)
                        val cleanName = newName.trim()
                            .removeSuffix(".pdf")
                            .replace(Regex("[/\\\\:*?\"<>|]"), "_")

                        if (cleanName.isBlank()) {
                            message = "Nama dokumen tidak boleh kosong."
                        } else {
                            val destination = File(
                                oldFile.parentFile,
                                "$cleanName.pdf"
                            )

                            when {
                                destination.absolutePath == oldFile.absolutePath -> {
                                    message = "Nama dokumen tidak berubah."
                                    renameTarget = null
                                }
                                destination.exists() -> {
                                    message = "Nama tersebut sudah digunakan."
                                }
                                oldFile.renameTo(destination) -> {
                                    message = "Nama dokumen berhasil diubah."
                                    renameTarget = null
                                    onRefresh()
                                }
                                else -> {
                                    message = "Gagal mengganti nama dokumen."
                                }
                            }
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Batal")
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus dokumen?") },
            text = {
                Text(
                    "Dokumen ${target.name} akan dihapus permanen dari penyimpanan ScanPro."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val file = File(target.path)

                        if (file.exists() && file.delete()) {
                            message = "Dokumen berhasil dihapus."
                            deleteTarget = null
                            onRefresh()
                        } else {
                            message = "Gagal menghapus dokumen."
                            deleteTarget = null
                        }
                    }
                ) {
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

@Composable
private fun DocumentCard(
    file: ScanFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val sizeBytes = File(file.path).length()
    val sizeText = if (sizeBytes >= 1024 * 1024) {
        String.format(
            java.util.Locale.US,
            "%.2f MB",
            sizeBytes / (1024.0 * 1024.0)
        )
    } else {
        String.format(
            java.util.Locale.US,
            "%.1f KB",
            sizeBytes / 1024.0
        )
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
            Box(
                Modifier
                    .size(44.dp)
                    .background(
                        Color(0xFFFFEEEE),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = "PDF",
                    tint = Color(0xFFD64B4B),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Bagikan",
                    tint = DMBlue
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Menu dokumen",
                        tint = DMMuted
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ganti nama") },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Hapus") },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
