package com.scanpro.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF17243A)
private val Blue = Color(0xFF2864E8)
private val Muted = Color(0xFF77849A)
private val Canvas = Color(0xFFF5F7FB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ScanProApp(this) }
    }
}

data class ScanFile(val name: String, val path: String, val pages: Int, val created: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanProApp(activity: ComponentActivity) {
    val context = activity
    var files by remember { mutableStateOf(loadFiles(context)) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var ocrText by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var showPdfStudio by remember { mutableStateOf(false) }

    val scanner = remember {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(50)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF, GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options)
    }

    val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        busy = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pdf = scanResult?.pdf
            if (pdf != null) {
                try {
                    val saved = copyIntoAppStorage(context, pdf.uri, "ScanPro_${System.currentTimeMillis()}.pdf")
                    val item = ScanFile(saved.name, saved.absolutePath, pdf.pageCount, SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date()))
                    files = listOf(item) + files
                    saveFiles(context, files)
                    status = "Dokumen berhasil disimpan (${pdf.pageCount} halaman)."
                } catch (e: Exception) {
                    status = "Gagal menyimpan PDF: ${e.localizedMessage}"
                }
            } else status = "Pemindaian selesai, tetapi PDF tidak ditemukan."
        }
    }

    fun startScan() {
        busy = true
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { sender -> scanLauncher.launch(IntentSenderRequest.Builder(sender).build()) }
            .addOnFailureListener { error -> busy = false; status = "Scanner tidak dapat dibuka: ${error.localizedMessage}" }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Canvas, surface = Color.White)) {
        Scaffold(
            containerColor = Canvas,
          bottomBar = {
    if (!showPdfStudio) {
        NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Beranda") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Folder, null) }, label = { Text("Dokumen") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Pengaturan") })
                }
            }
          }
        ) { padding ->
if (showPdfStudio) {
    PdfStudioScreen(
        context = context,
        files = files,
        onBack = { showPdfStudio = false },
        onComplete = { result ->
            files = loadFiles(context)
            status = result
            selectedTab = 0
            showPdfStudio = false
        }
    )
} else {
    when (selectedTab) {
        0 -> HomeScreen(
            files, busy, status,
            onScan = { startScan() },
            onOpen = { openPdf(context, it) },
            onShare = { sharePdf(context, it) },
            onOcr = { file ->
                runOcr(context, file) { ocrText = it }
            },
            onDismiss = { status = null; ocrText = null },
            onPdfStudio = { showPdfStudio = true },
            padding = padding
        )
1 -> DocumentManagerScreen(
    files = files,
    onOpen = { openPdf(context, it) },
    onShare = { sharePdf(context, it) },
    onRefresh = {
        files = loadFiles(context)
    }
)
        else -> SettingsScreen(padding)
    }
}
        }
        ocrText?.let { text ->
            AlertDialog(onDismissRequest = { ocrText = null }, title = { Text("Hasil OCR") }, text = { Text(text, maxLines = 18) }, confirmButton = { TextButton(onClick = { android.content.ClipData.newPlainText("OCR ScanPro", text).also { clip -> (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip) }; ocrText = null }) { Text("Salin teks") } }, dismissButton = { TextButton(onClick = { ocrText = null }) { Text("Tutup") } })
        }
    }
}

@Composable
private fun HomeScreen(files: List<ScanFile>, busy: Boolean, status: String?, onScan: () -> Unit, onOpen: (ScanFile) -> Unit, onShare: (ScanFile) -> Unit, onOcr: (ScanFile) -> Unit, onDismiss: () -> Unit, onPdfStudio: () -> Unit,padding: PaddingValues) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 28.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("SCANPRO", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Dokumen lebih tertata.", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Pindai, rapikan, dan simpan dalam PDF.", color = Muted, fontSize = 13.sp)
                }
                Box(Modifier.size(46.dp).background(Color(0xFFE5EDFF), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.DocumentScanner, null, tint = Blue, modifier = Modifier.size(25.dp)) }
            }
        }
        item {
            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Blue)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text("SMART DOCUMENT SCANNER", color = Color.White.copy(alpha = .85f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                    Text("Ubah dokumen fisik menjadi PDF berkualitas.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                    Text("Deteksi tepi, koreksi perspektif, filter, dan scan multi-halaman.", color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
                    Button(onClick = onScan, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Blue), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Blue) else Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(8.dp)); Text(if (busy) "Menyiapkan scanner..." else "Mulai Scan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (status != null) item { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F4EC))) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF21834A)); Spacer(Modifier.width(8.dp)); Text(status, Modifier.weight(1f), color = Color(0xFF205D39), fontSize = 12.sp); IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Muted) } } } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Alat cepat", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("FITUR", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      QuickTool(
    "PDF",
    Icons.Default.PictureAsPdf,
    Modifier.weight(1f),
    onClick = onPdfStudio
)
                QuickTool("OCR", Icons.Default.TextSnippet, Modifier.weight(1f))
                QuickTool("Galeri", Icons.Default.PhotoLibrary, Modifier.weight(1f))
                QuickTool("Bagikan", Icons.Default.Share, Modifier.weight(1f))
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Dokumen terbaru", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("${files.size} file", color = Muted, fontSize = 12.sp) } }
        if (files.isEmpty()) item { EmptyState() } else items(files.take(5)) { file -> FileRow(file, onOpen, onShare, onOcr) }
    }
}

@Composable
private fun QuickTool(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit = {}
){
    Column(
    modifier
        .clickable(onClick = onClick)
        .background(Color.White, RoundedCornerShape(16.dp))
        .padding(vertical = 14.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
        Icon(icon, null, tint = Blue, modifier = Modifier.size(23.dp)); Spacer(Modifier.height(7.dp)); Text(label, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.FolderOpen, null, tint = Color(0xFF9AA8BC), modifier = Modifier.size(38.dp)); Spacer(Modifier.height(8.dp)); Text("Belum ada dokumen", color = Ink, fontWeight = FontWeight.SemiBold); Text("Hasil pemindaian akan muncul di sini.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun FileRow(file: ScanFile, onOpen: (ScanFile) -> Unit, onShare: (ScanFile) -> Unit, onOcr: (ScanFile) -> Unit) {
    Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().clickable { onOpen(file) }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0xFFFFEEEE), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.PictureAsPdf, null, tint = Color(0xFFD64B4B), modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(file.name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1); Text("${file.pages} halaman • ${file.created}", color = Muted, fontSize = 11.sp) }
            IconButton(onClick = { onOcr(file) }) { Icon(Icons.Default.TextFields, "OCR", tint = Blue) }
            IconButton(onClick = { onShare(file) }) { Icon(Icons.Default.Share, "Bagikan", tint = Muted) }
        }
    }
}

@Composable
private fun DocumentsScreen(files: List<ScanFile>, onOpen: (ScanFile) -> Unit, onShare: (ScanFile) -> Unit, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Semua dokumen", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("File tersimpan di penyimpanan internal ScanPro.", color = Muted, fontSize = 13.sp)
        if (files.isEmpty()) EmptyState() else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(files) { file -> FileRow(file, onOpen, onShare) { } } }
    }
}

@Composable
private fun SettingsScreen(padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Pengaturan", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("ScanPro • Versi 0.1.0", color = Muted)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Penyimpanan & privasi", color = Ink, fontWeight = FontWeight.Bold); Text("Dokumen disimpan secara lokal di perangkat. Pemindaian diproses melalui Google Play services.", color = Muted, fontSize = 13.sp) } }
    }
}

private fun copyIntoAppStorage(context: Context, uri: Uri, fileName: String): File {
    val dir = File(context.filesDir, "scans").apply { mkdirs() }
    val output = File(dir, fileName)
    context.contentResolver.openInputStream(uri).use { input -> requireNotNull(input) { "File hasil scan tidak dapat dibaca" }; FileOutputStream(output).use { out -> input.copyTo(out) } }
    return output
}

private fun loadFiles(context: Context): List<ScanFile> {
    val dir = File(context.filesDir, "scans")
    return dir.listFiles()?.filter { it.extension.equals("pdf", true) }?.sortedByDescending { it.lastModified() }?.map {
        ScanFile(it.name, it.absolutePath, 1, SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(it.lastModified())))
    } ?: emptyList()
}

private fun saveFiles(context: Context, files: List<ScanFile>) { /* Files are reloaded from the app's private scans directory. */ }

private fun openPdf(context: Context, file: ScanFile) {
    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", File(file.path))
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try { context.startActivity(intent) } catch (_: Exception) { }
}

private fun sharePdf(context: Context, file: ScanFile) {
    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", File(file.path))
    val intent = Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Bagikan dokumen"))
}

private fun runOcr(context: Context, file: ScanFile, onResult: (String) -> Unit) {
    val mainHandler = Handler(Looper.getMainLooper())
    Thread {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val descriptor = ParcelFileDescriptor.open(File(file.path), ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(descriptor)
            val extracted = StringBuilder()
            try {
                for (index in 0 until renderer.pageCount) {
                    val page = renderer.openPage(index)
                    val scale = 2f
                    val bitmap = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    val result = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
                    if (result.text.isNotBlank()) {
                        extracted.append("HALAMAN ").append(index + 1).append("\n")
                        extracted.append(result.text).append("\n\n")
                    }
                    bitmap.recycle()
                }
            } finally {
                renderer.close()
                descriptor.close()
            }
            val text = extracted.toString().ifBlank { "Tidak ada teks yang terdeteksi." }
            mainHandler.post { onResult(text) }
        } catch (e: Exception) {
            mainHandler.post { onResult("OCR gagal: ${e.localizedMessage}") }
        } finally {
            recognizer.close()
        }
    }.start()
}
