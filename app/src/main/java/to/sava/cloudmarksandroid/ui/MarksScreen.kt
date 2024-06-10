package to.sava.cloudmarksandroid.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.databases.models.Favicon
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.databases.models.MarkType
import to.sava.cloudmarksandroid.modules.Marks
import to.sava.cloudmarksandroid.modules.Settings
import java.nio.ByteBuffer

@Composable
fun MarksScreen(
    modifier: Modifier = Modifier,
    markId: Long,
    lastOpenedTime: String,
    viewModelConfigurator: MarksScreenViewModel.() -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    val viewModel = koinViewModel<MarksScreenViewModel>().apply(viewModelConfigurator)
    val markPath by viewModel.markPath.collectAsState(listOf())
    val markColumns by viewModel.markColumns.collectAsState(listOf())
    val openMenu by viewModel.openMenu.collectAsState(false)
    val selectedMark by viewModel.selectedMark.collectAsState(null)
    val marksMenuItems by viewModel.marksMenuItems.collectAsState(listOf())
    val markReadToHere by viewModel.markReadToHere.collectAsState(listOf())

    LaunchedEffect(lastOpenedTime) {
        viewModel.getMarks(markId)
    }
    if (markPath.size > 1) {
        BackHandler {
            viewModel.backButton()
        }
    }

    Column(
        modifier = modifier
    ) {
        MarksBreadcrumbs(
            markPath = markPath,
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
            for (children in markColumns) {
                MarksColumn(
                    children,
                    markReadToHere,
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
                mark = selectedMark,
                menuItems = marksMenuItems,
                onClick = { mark, menuItem -> viewModel.clickMenu(menuItem, mark) }
            )
        }
        content()
    }
}

@Composable
private fun MarksBreadcrumbs(
    modifier: Modifier = Modifier,
    markPath: List<MarkNode>,
    onMarkClick: (mark: MarkNode) -> Unit = {},
    content: @Composable () -> Unit = {},
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
                .padding(horizontal = 8.dp, vertical = 1.dp)
                .height(32.dp)
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
                            .padding(
                                vertical = 2.dp,
                                horizontal = 8.dp,
                            )
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
                            .height(24.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        TextElement(text)
                    }
                    TextElement("/")
                } else {
                    TextElement(text)
                }
            }
        }
        content()
    }
}

enum class MarksMenuItem(val label: String) {
    OPEN("Open"),
    SHARE("Share to ..."),
    COPY_URL("Copy URL"),
    COPY_TITLE("Copy title"),
    MARK_READ("Mark read"),
    FETCH_FAVICON("Fetch favicon"),
    FETCH_FAVICON_IN_FOLDER("Fetch favicon in this folder"),
}

@Composable
private fun MarksMenu(
    modifier: Modifier = Modifier,
    mark: MarkNode?,
    menuItems: List<MarksMenuItem> = listOf(),
    onClick: (mark: MarkNode, menuItem: MarksMenuItem) -> Unit = { _, _ -> },
    content: @Composable () -> Unit = {},
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
            content()
        }
    }
}

@Composable
private fun MarksColumn(
    children: List<Pair<MarkNode, Favicon?>>,
    markReadToHere: List<MarkNode>,
    modifier: Modifier = Modifier,
    onMarkClick: (mark: MarkNode) -> Unit = {},
    onMarkLongClick: (mark: MarkNode) -> Unit = {},
) {
    if (children.isEmpty()) {
        EmptyColumn(modifier)
    } else {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = modifier,
        ) {
            items(children) { (mark, favicon) ->
                MarksItem(
                    mark,
                    favicon,
                    modifier = if (markReadToHere.any { it.id == mark.id })
                        Modifier.background(MaterialTheme.colors.primary)
                    else
                        Modifier,
                    onMarkClick = { onMarkClick(it) },
                    onMarkLongClick = { onMarkLongClick(it) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarksItem(
    mark: MarkNode,
    favicon: Favicon?,
    modifier: Modifier = Modifier,
    onMarkClick: (mark: MarkNode) -> Unit = {},
    onMarkLongClick: (mark: MarkNode) -> Unit = {},
) {
    val faviconBitmap: Bitmap? = favicon?.let {
        Bitmap.createBitmap(it.size, it.size, Bitmap.Config.ARGB_8888).apply {
            copyPixelsFromBuffer(ByteBuffer.wrap(it.favicon))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .combinedClickable(
                onClick = { onMarkClick(mark) },
                onLongClick = { onMarkLongClick(mark) }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 4.dp, end = 4.dp)
            ) {
                if (faviconBitmap != null) {
                    Image(
                        bitmap = faviconBitmap.asImageBitmap(),
                        mark.domain,
                        modifier = Modifier.size(24.dp),
                    )
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

@Composable
private fun EmptyColumn(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 4.dp, end = 4.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_info_black_24dp),
                    "Folder"
                )
            }
            Text(
                text = "ブックマークが空です",
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .horizontalScroll(rememberScrollState())
            )
        }
    }
}


class MarksScreenViewModel(
    private val marks: Marks,
    private val settings: Settings,
) : ViewModel() {
    private var currentMarkId: Long = 0

    var showMessage: (message: String) -> Unit = {}
    var onSelectFolder: (markId: Long) -> Unit = {}
    var onCopyToClipboard: (copyText: String, typeText: String) -> Unit = { _, _ -> }
    var onMarkRead: (mark: MarkNode) -> Unit = {}
    var openMark: (url: String) -> Unit = {}
    var shareMark: (url: String) -> Unit = {}
    var fetchFavicon: (domains: List<String>) -> Unit = {}
    var backButton: () -> Unit = {}

    private val _markPath = MutableStateFlow(listOf<MarkNode>())
    val markPath get() = _markPath.asStateFlow()

    private val _markColumns = MutableStateFlow(listOf<List<Pair<MarkNode, Favicon?>>>())
    val markColumns get() = _markColumns.asStateFlow()

    private val _selectedMark = MutableStateFlow<MarkNode?>(null)
    val selectedMark get() = _selectedMark.asStateFlow()

    private val _openMenu = MutableStateFlow(false)
    val openMenu get() = _openMenu.asStateFlow()

    private val _marksMenuItems = MutableStateFlow(listOf<MarksMenuItem>())
    val marksMenuItems get() = _marksMenuItems.asStateFlow()

    val markReadToHere = settings.getMarkReadToHere().map {
        marks.getMark(it)?.let { mark ->
            marks.getMarkPath(mark)
        } ?: listOf()
    }

    suspend fun getMarks(markId: Long) {
        currentMarkId = markId
        withContext(Dispatchers.IO) {
            val mark = marks.getMark(markId) ?: return@withContext
            val markPath = marks.getMarkPath(mark)
            _markPath.value = markPath

            _markColumns.value = markPath
                .takeLast(settings.getFolderColumnsValue())
                .map { parent ->
                    val children = marks.getMarkChildren(parent)
                    val favicons = marks.findFavicons(children.map { it.domain })
                    children.map { mark ->
                        Pair(mark, favicons.firstOrNull { it.domain == mark.domain })
                    }
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
                MarksMenuItem.MARK_READ,
                MarksMenuItem.FETCH_FAVICON,
            )

            MarkType.Folder -> listOf(
                MarksMenuItem.OPEN,
                MarksMenuItem.COPY_TITLE,
                MarksMenuItem.MARK_READ,
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
                onCopyToClipboard(mark.url, "URL")
            }

            MarksMenuItem.COPY_TITLE -> {
                onCopyToClipboard(mark.title, "タイトル")
            }

            MarksMenuItem.MARK_READ -> {
                onMarkRead(mark)
            }

            MarksMenuItem.FETCH_FAVICON -> {
                fetchFavicon(listOf(mark.domain))
                showMessage("Favicon を取得しました")
            }

            MarksMenuItem.FETCH_FAVICON_IN_FOLDER -> {
                viewModelScope.launch {
                    marks.getMarkChildren(mark)
                        .filter { it.isBookmark }
                        .map { it.domain }
                        .let {
                            fetchFavicon(it)
                            showMessage("Favicon を取得しました")
                        }
                }
            }
        }
    }
}
