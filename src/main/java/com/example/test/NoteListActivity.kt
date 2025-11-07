package com.example.test

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore

import com.example.test.NoteItem
import com.example.test.UpdateCourse
import com.example.test.deleteImageFromStorage

class NoteListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoteListScreen()
                }
            }
        }
    }
}

fun deleteNote(note: NoteItem, context: Context, noteList: MutableList<NoteItem>) {
    val noteID = note.id
    val imageUrl = note.imageUrl

    if (noteID.isEmpty()) {
        Toast.makeText(context, "ID Note không hợp lệ!", Toast.LENGTH_SHORT).show()
        return
    }

    val db = FirebaseFirestore.getInstance()

    db.collection("Notes").document(noteID).delete()
        .addOnSuccessListener {
            noteList.remove(note)
            Toast.makeText(context, "Xóa Note thành công!", Toast.LENGTH_SHORT).show()

            if (imageUrl.isNotEmpty()) {
                deleteImageFromStorage(imageUrl)
            }

            val intent = Intent(context, NoteListActivity::class.java)
            context.startActivity(intent)
            (context as? ComponentActivity)?.finish()
        }
        .addOnFailureListener { e: Exception ->
            Toast.makeText(context, "Lỗi xóa Note: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val noteList = remember { mutableStateListOf<NoteItem>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("Notes").get()
            .addOnSuccessListener { querySnapshot ->
                noteList.clear()
                for (document in querySnapshot.documents) {
                    val note = NoteItem(
                        id = document.id,
                        title = document.getString("title") ?: "",
                        description = document.getString("description") ?: "",
                        price = document.getString("price") ?: "", // Đã thêm: Đọc giá
                        imageUrl = document.getString("imageUrl") ?: ""
                    )
                    noteList.add(note)
                }
                isLoading = false
            }
            .addOnFailureListener { e: Exception ->
                Toast.makeText(context, "Failed to get data: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Danh sách Notes (Xem/Xóa)", color = Color.White, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF4CAF50))
            )
        }
    ) { innerPadding ->
        NoteList(
            context = context,
            notes = noteList,
            isLoading = isLoading,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun NoteList(
    context: Context,
    notes: List<NoteItem>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            Text(text = "Đang tải dữ liệu...", modifier = Modifier.padding(16.dp), color = Color.Gray)
        } else if (notes.isEmpty()) {
            Text(text = "Không có dữ liệu Note.", modifier = Modifier.padding(16.dp), color = Color.Gray)
        } else {
            LazyColumn {
                items(notes) { note ->
                    NoteCard(context, note, notes.toMutableList())
                }
            }
        }
    }
}

@Composable
fun NoteCard(context: Context, note: NoteItem, noteList: List<NoteItem>) {
    val openDeleteDialog = remember { mutableStateOf(false) }

    if (openDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { openDeleteDialog.value = false },
            title = { Text("Xác nhận Xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa Note '${note.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDeleteDialog.value = false
                        deleteNote(note, context, noteList.toMutableList())
                    }
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { openDeleteDialog.value = false }
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)),
        onClick = {
            val intent = Intent(context, UpdateCourse::class.java)
            intent.putExtra("noteTitle", note.title)
            intent.putExtra("noteDescription", note.description)
            intent.putExtra("notePrice", note.price) // Đã thêm: Truyền giá
            intent.putExtra("noteID", note.id)
            intent.putExtra("noteImageUrl", note.imageUrl)
            context.startActivity(intent)
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = note.imageUrl,
                        contentDescription = "Note Image/File",
                        modifier = Modifier
                            .size(70.dp)
                            .padding(end = 8.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Title: ${note.title}",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Description: ${note.description}",
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Price: ${note.price}", // Đã thêm: Hiển thị giá
                        fontSize = 15.sp,
                        color = Color.DarkGray
                    )
                }

                Row {
                    IconButton(onClick = {
                        val intent = Intent(context, UpdateCourse::class.java)
                        intent.putExtra("noteTitle", note.title)
                        intent.putExtra("noteDescription", note.description)
                        intent.putExtra("notePrice", note.price) // Đã thêm: Truyền giá
                        intent.putExtra("noteID", note.id)
                        intent.putExtra("noteImageUrl", note.imageUrl)
                        context.startActivity(intent)
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Sửa", tint = Color(0xFF1976D2))
                    }
                    IconButton(onClick = { openDeleteDialog.value = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Xóa", tint = Color.Red)
                    }
                }
            }
        }
    }
}