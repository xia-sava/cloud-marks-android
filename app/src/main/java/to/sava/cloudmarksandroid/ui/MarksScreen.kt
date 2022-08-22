package to.sava.cloudmarksandroid.ui

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.databases.models.MarkType
import to.sava.cloudmarksandroid.modules.Marks
import to.sava.cloudmarksandroid.modules.Settings
import java.nio.ByteBuffer
import javax.inject.Inject

@Composable
fun MarksScreen(
    viewModel: MarksScreenViewModel,
    markId: Long,
    onSelectFolder: (markId: Long) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val markPath by viewModel.markPath.collectAsState(initial = listOf())
    val markColumns by viewModel.markColumns.collectAsState(initial = mapOf())
    var openDrawer by remember { mutableStateOf(false) }
    var selectedMark by remember { mutableStateOf<MarkNode?>(null) }

    LaunchedEffect(markId) {
        viewModel.getMarks(markId)
    }

    Box() {
        Column(
            modifier = Modifier
        ) {
            MarksBreadcrumbs(
                markPath,
                onMarkClick = { onSelectFolder(it.id) }
            )
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
                for ((current, children) in markColumns) {
                    MarksColumn(
                        current,
                        children,
                        modifier = Modifier
                            .weight(1f / markColumns.size)
                            .padding(horizontal = 1.dp),
                        onMarkClick = { mark ->
                            when (mark.type) {
                                MarkType.Folder -> onSelectFolder(mark.id)
                                MarkType.Bookmark -> {}
                            }
                        },
                        onMarkLongClick = { mark ->
                            selectedMark = mark
                            openDrawer = true
                        }
                    )
                    Divider(
                        Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = openDrawer,
                onDismissRequest = { openDrawer = false },
                offset = DpOffset(16.dp, 32.dp),
            ) {
                MarksMenu(mark = selectedMark)
            }
        }
    }
}

@Composable
private fun MarksBreadcrumbs(
    markPath: List<MarkNode>,
    modifier: Modifier = Modifier,
    onMarkClick: (mark: MarkNode) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(markPath) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Surface(
        color = MaterialTheme.colors.secondary,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
        ) {
            for ((i, mark) in markPath.withIndex()) {
                val text = if (i == 0) "/" else mark.title

                @Composable
                fun TextElement(label: String) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(horizontal = 4.dp)
                    )
                }

                if (i != markPath.size - 1) {
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = MaterialTheme.colors.secondary,
                            contentColor = MaterialTheme.colors.onSecondary,
                        ),
                        contentPadding = PaddingValues(all = 0.dp),
                        onClick = { onMarkClick(mark) },
                        modifier = Modifier
                    ) {
                        TextElement(text)
                    }
                    TextElement("/")
                } else {
                    TextElement(text)
                }
            }
        }
    }
}

@Composable
private fun MarksMenu(
    mark: MarkNode?,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colors.background,
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Column {
            Text(
                text = mark?.title ?: "",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Text(
                text = mark?.url ?: "",
                fontSize = 10.sp,
                modifier = Modifier
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Divider()
            DropdownMenuItem(onClick = { /*TODO*/ }) {
                Text("めにゅー1")
            }
            DropdownMenuItem(onClick = { /*TODO*/ }) {
                Text("めにゅー2")
            }
            DropdownMenuItem(onClick = { /*TODO*/ }) {
                Text("めにゅー3")
            }
        }
    }
}

@Composable
private fun MarksColumn(
    current: MarkNode,
    children: List<MarkNode>,
    modifier: Modifier = Modifier,
    onMarkClick: (mark: MarkNode) -> Unit = {},
    onMarkLongClick: (mark: MarkNode) -> Unit = {},
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier,
    ) {
        items(children) { mark ->
            MarksItem(
                mark,
                onMarkClick = { onMarkClick(it) },
                onMarkLongClick = { onMarkLongClick(it) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarksItem(
    mark: MarkNode,
    modifier: Modifier = Modifier,
    onMarkClick: (mark: MarkNode) -> Unit = {},
    onMarkLongClick: (mark: MarkNode) -> Unit = {},
) {
    val favicon: Bitmap? = remember {
        mark.favicon?.let { favicon ->
            Bitmap.createBitmap(favicon.size, favicon.size, Bitmap.Config.ARGB_8888).also {
                it.copyPixelsFromBuffer(ByteBuffer.wrap(favicon.favicon))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .combinedClickable(
                onClick = { onMarkClick(mark) },
                onLongClick = { onMarkLongClick(mark) }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(31.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 4.dp, end = 4.dp)
            ) {
                if (favicon != null) {
                    Image(bitmap = favicon.asImageBitmap(), mark.domain)
                } else {
                    when (mark.type) {
                        MarkType.Folder -> Icon(
                            painterResource(R.drawable.ic_folder_open_black_24dp),
                            "Folder"
                        )
                        MarkType.Bookmark -> Icon(
                            painterResource(R.drawable.ic_bookmark_border_black_24dp),
                            "Bookmark"
                        )
                    }
                }
            }
            Text(
                text = mark.title,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .horizontalScroll(rememberScrollState())
            )
        }
        Divider(
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        )
    }
}


@HiltViewModel
class MarksScreenViewModel @Inject constructor(
    private val marks: Marks,
    private val settings: Settings,
) : ViewModel() {

    private var _markPath = MutableStateFlow(listOf<MarkNode>())
    val markPath: StateFlow<List<MarkNode>> get() = _markPath

    private var _markColumns = MutableStateFlow(mapOf<MarkNode, List<MarkNode>>())
    val markColumns: StateFlow<Map<MarkNode, List<MarkNode>>> get() = _markColumns

    suspend fun getMarks(markId: Long) {
        withContext(Dispatchers.IO) {
            val mark = marks.getMark(markId) ?: return@withContext
            val markPath = marks.getMarkPath(mark)
            _markPath.value = markPath

            markPath
                .takeLast(settings.getFolderColumnsValue())
                .associateWith {
                    marks.getMarkChildren(it)
                }
                .let {
                    _markColumns.value = it
                }
        }
    }
}
