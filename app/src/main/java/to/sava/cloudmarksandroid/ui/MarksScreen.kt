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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.databases.models.MarkType
import to.sava.cloudmarksandroid.modules.Marks
import to.sava.cloudmarksandroid.modules.Settings
import to.sava.cloudmarksandroid.modules.toast
import java.nio.ByteBuffer
import javax.inject.Inject

@Composable
fun MarksScreen(
    viewModel: MarksScreenViewModel,
    markId: Long,
) {
    val markPath by viewModel.markPath.collectAsState(listOf())
    val markColumns by viewModel.markColumns.collectAsState(mapOf())
    val openMenu by viewModel.openMenu.collectAsState(false)
    val selectedMark by viewModel.selectedMark.collectAsState(null)
    val marksMenuItems by viewModel.marksMenuItems.collectAsState(listOf())
    val message by viewModel.message.collectAsState("")

    LaunchedEffect(markId) {
        viewModel.getMarks(markId)
    }

    message.takeIf { it.isNotEmpty() }?.let {
        LocalContext.current.toast(it).show()
        viewModel.markMessageShown(it)
    }

    Column(
        modifier = Modifier
    ) {
        MarksBreadcrumbs(
            markPath,
            onMarkClick = { viewModel.clickMark(it) }
        )
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Divider(
                Modifier
                    .fillMaxHeight()
                    .width(1.dp)
            )
            for ((_, children) in markColumns) {
                MarksColumn(
                    children,
                    modifier = Modifier
                        .weight(1f / markColumns.size)
                        .padding(horizontal = 1.dp),
                    onMarkClick = { viewModel.clickMark(it) },
                    onMarkLongClick = { viewModel.selectMark(it) }
                )
                Divider(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
            }
        }
        DropdownMenu(
            expanded = openMenu,
            onDismissRequest = { viewModel.dismissMenu() },
            offset = DpOffset(16.dp, 32.dp),
        ) {
            MarksMenu(
                selectedMark,
                marksMenuItems,
                { mark, menuItem -> viewModel.clickMenu(menuItem, mark) }
            )
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

enum class MarksMenuItem(val label: String) {
    OPEN("Open"),
    SHARE("Share to ..."),
    COPY_URL("Copy URL"),
    COPY_TITLE("Copy title"),
    FETCH_FAVICON("Fetch favicon"),
    FETCH_FAVICON_IN_FOLDER("Fetch favicon in this folder"),
}

@Composable
private fun MarksMenu(
    mark: MarkNode?,
    menuItems: List<MarksMenuItem> = listOf(),
    onClick: (mark: MarkNode, menuItem: MarksMenuItem) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    mark ?: return
    Surface(
        color = MaterialTheme.colors.background,
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Column {
            Text(
                text = mark.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Text(
                text = mark.url,
                fontSize = 10.sp,
                modifier = Modifier
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Divider()
            for (menuItem in menuItems) {
                DropdownMenuItem({ onClick(mark, menuItem) }) {
                    Text(menuItem.label)
                }
            }
        }
    }
}

@Composable
private fun MarksColumn(
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

    var onSelectFolder: (markId: Long) -> Unit = {}
    var onCopyToClipboard: (text: String) -> Unit = {}
    var openMark: (url: String) -> Unit = {}
    var shareMark: (url: String) -> Unit = {}

    private val _markPath = MutableStateFlow(listOf<MarkNode>())
    val markPath get() = _markPath.asStateFlow()

    private val _markColumns = MutableStateFlow(mapOf<MarkNode, List<MarkNode>>())
    val markColumns get() = _markColumns.asStateFlow()

    private val _selectedMark = MutableStateFlow<MarkNode?>(null)
    val selectedMark get() = _selectedMark.asStateFlow()

    private val _openMenu = MutableStateFlow(false)
    val openMenu get() = _openMenu.asStateFlow()

    private val _marksMenuItems = MutableStateFlow(listOf<MarksMenuItem>())
    val marksMenuItems get() = _marksMenuItems.asStateFlow()

    private val _message = MutableStateFlow("")
    val message get() = _message.asSharedFlow()

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

    fun clickMark(mark: MarkNode) {
        when (mark.type) {
            MarkType.Folder -> onSelectFolder(mark.id)
            MarkType.Bookmark -> openMark(mark.url)
        }
    }

    fun selectMark(mark: MarkNode) {
        _selectedMark.value = mark
        _openMenu.value = true
        _marksMenuItems.value = when (mark.type) {
            MarkType.Bookmark -> listOf(
                MarksMenuItem.OPEN,
                MarksMenuItem.SHARE,
                MarksMenuItem.COPY_URL,
                MarksMenuItem.COPY_TITLE,
                MarksMenuItem.FETCH_FAVICON,
            )
            MarkType.Folder -> listOf(
                MarksMenuItem.OPEN,
                MarksMenuItem.COPY_TITLE,
                MarksMenuItem.FETCH_FAVICON_IN_FOLDER,
            )
        }
    }

    fun dismissMenu() {
        _openMenu.value = false
    }

    fun clickMenu(menuItem: MarksMenuItem, mark: MarkNode) {
        dismissMenu()
        when (menuItem) {
            MarksMenuItem.OPEN -> clickMark(mark)
            MarksMenuItem.SHARE -> shareMark(mark.url)
            MarksMenuItem.COPY_URL -> {
                onCopyToClipboard(mark.url)
                showMessage("URLをクリップボードにコピーしました。")
            }
            MarksMenuItem.COPY_TITLE -> {
                onCopyToClipboard(mark.title)
                showMessage("タイトルをクリップボードにコピーしました。")
            }
            MarksMenuItem.FETCH_FAVICON -> {}
            MarksMenuItem.FETCH_FAVICON_IN_FOLDER -> {}
        }
    }

    fun showMessage(message: String) {
        _message.value = message
    }

    fun markMessageShown(message: String) {
        if (_message.value == message) {
            _message.value = ""
        }
    }
}
