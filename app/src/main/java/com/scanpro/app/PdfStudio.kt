
package com.scanpro.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfStudioScreen(
    context: Context,
    files: List<ScanFile>,
    onBack: () -> Unit,
    onComplete: (String) -> Unit
) {
    var selected by remember(files) {
        mutableStateOf(setOf<String>())
    }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Column {
                Text(
                    "PDF Studio",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF17243A)
                )
                Text(
                    "Kelola dan susun dokumen PDF",
                    fontSize = 13.sp,
                    color = Color(0xFF77849A)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Pilih dokumen",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF17243A)
                )
                Text(
                    "Pilih minimal dua file untuk digabungkan. " +
                    "Pilih satu file untuk memisahkan setiap halamannya.",
                    fontSize = 12.sp,
                    color = Color(0xFF77849A)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (files.isEmpty()) {
            Text(
                "Belum ada PDF. Pindai dokumen terlebih dahulu.",
                color = Color(0xFF77849A)
            )
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files, key = { it.path }) { file ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected =
                                        if (selected.contains(file.path))
                                            selected - file.path
                                        else
                                            selected + file.path
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected.contains(file.path),
                                onCheckedChange = { checked ->
                                    selected =
                                        if (checked)
                                            selected + file.path
                                        else
                                            selected - file.path
                                }
                            )

                            Column(Modifier.weight(1f)) {
                                Text(
                                    file.name,
                                    color = Color(0xFF17243A),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    "${file.pages} halaman • ${file.created}",
                                    color = Color(0xFF77849A),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (message != null) {
            Text(
                message!!,
                color = Color(0xFF2864E8),
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val chosen = files.filter {
                        selected.contains(it.path)
                    }
                    busy = true
                    message = null

                    Thread {
                        val result = runCatching {
                            mergePdfFiles(context, chosen)
                        }

                        android.os.Handler(
                            android.os.Looper.getMainLooper()
                        ).post {
                            busy = false
                            result.onSuccess {
                                onComplete(
                                    "Berhasil menggabungkan " +
                                    "${chosen.size} PDF: ${it.name}"
                                )
                            }.onFailure {
                                message =
                                    "Gagal menggabungkan PDF: " +
                                    "${it.localizedMessage}"
                            }
                        }
                    }.start()
                },
                enabled = !busy && selected.size >= 2,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.MergeType, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Gabungkan")
            }

            OutlinedButton(
                onClick = {
                    val chosen = files.firstOrNull {
                        selected.contains(it.path)
                    }

                    if (chosen != null) {
                        busy = true
                        message = null

                        Thread {
                            val result = runCatching {
                                splitPdfPages(context, chosen)
                            }

                            android.os.Handler(
                                android.os.Looper.getMainLooper()
                            ).post {
                                busy = false
                                result.onSuccess {
                                    onComplete(
                                        "Berhasil memisahkan halaman " +
                                        "menjadi $it file PDF."
                                    )
                                }.onFailure {
                                    message =
                                        "Gagal memisahkan PDF: " +
                                        "${it.localizedMessage}"
                                }
                            }
                        }.start()
                    }
                },
                enabled = !busy && selected.size == 1,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CallSplit, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Pisahkan")
            }
        }

        if (busy) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                "Sedang memproses PDF…",
                color = Color(0xFF77849A),
                fontSize = 12.sp
            )
        }
    }
}

private fun mergePdfFiles(
    context: Context,
    files: List<ScanFile>
): File {
    require(files.size >= 2) {
        "Pilih minimal dua dokumen."
    }

    val output = newPdfOutput(context, "ScanPro_Gabungan")
    val document = PdfDocument()

    try {
        var pageNumber = 1

        files.forEach { file ->
            val descriptor = ParcelFileDescriptor.open(
                File(file.path),
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val renderer = PdfRenderer(descriptor)

            try {
                for (index in 0 until renderer.pageCount) {
                    val sourcePage = renderer.openPage(index)

                    try {
                        val bitmap = Bitmap.createBitmap(
                            sourcePage.width * 2,
                            sourcePage.height * 2,
                            Bitmap.Config.ARGB_8888
                        )

                        bitmap.eraseColor(android.graphics.Color.WHITE)

                        sourcePage.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_PRINT
                        )

                        val info = PdfDocument.PageInfo.Builder(
                            bitmap.width,
                            bitmap.height,
                            pageNumber++
                        ).create()

                        val page = document.startPage(info)

                        page.canvas.drawBitmap(
                            bitmap,
                            null,
                            Rect(0, 0, bitmap.width, bitmap.height),
                            null
                        )

                        document.finishPage(page)
                        bitmap.recycle()
                    } finally {
                        sourcePage.close()
                    }
                }
            } finally {
                renderer.close()
                descriptor.close()
            }
        }

        FileOutputStream(output).use {
            document.writeTo(it)
        }
    } finally {
        document.close()
    }

    return output
}

private fun splitPdfPages(
    context: Context,
    source: ScanFile
): Int {
    val descriptor = ParcelFileDescriptor.open(
        File(source.path),
        ParcelFileDescriptor.MODE_READ_ONLY
    )

    val renderer = PdfRenderer(descriptor)
    var count = 0

    try {
        for (index in 0 until renderer.pageCount) {
            val sourcePage = renderer.openPage(index)
            val document = PdfDocument()

            try {
                val bitmap = Bitmap.createBitmap(
                    sourcePage.width * 2,
                    sourcePage.height * 2,
                    Bitmap.Config.ARGB_8888
                )

                bitmap.eraseColor(android.graphics.Color.WHITE)

                sourcePage.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_PRINT
                )

                val info = PdfDocument.PageInfo.Builder(
                    bitmap.width,
                    bitmap.height,
                    1
                ).create()

                val page = document.startPage(info)

                page.canvas.drawBitmap(
                    bitmap,
                    null,
                    Rect(0, 0, bitmap.width, bitmap.height),
                    null
                )

                document.finishPage(page)

                val output = newPdfOutput(
                    context,
                    "ScanPro_Halaman_${index + 1}"
                )

                FileOutputStream(output).use {
                    document.writeTo(it)
                }

                bitmap.recycle()
                count++
            } finally {
                document.close()
                sourcePage.close()
            }
        }
    } finally {
        renderer.close()
        descriptor.close()
    }

    return count
}

private fun newPdfOutput(
    context: Context,
    prefix: String
): File {
    val directory = File(
        context.filesDir,
        "scans"
    ).apply {
        mkdirs()
    }

    val stamp = SimpleDateFormat(
        "yyyyMMdd_HHmmss_SSS",
        Locale.US
    ).format(Date())

    return File(
        directory,
        "${prefix}_${stamp}.pdf"
    )
}
