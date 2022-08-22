package to.sava.cloudmarksandroid.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
    val markPath by viewModel.markPath.collectAsState(initial = listOf())
    val markColumns by viewModel.markColumns.collectAsState(initial = mapOf())

    LaunchedEffect(markId) {
        viewModel.getMarks(markId)
    }

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
            for ((current, children) in markColumns) {
                MarksColumn(
                    current,
                    children,
                    modifier = Modifier
                        .weight(1f / markColumns.size)
                        .padding(end = 4.dp),
                    onMarkClick = { mark ->
                        when (mark.type) {
                            MarkType.Folder -> { onSelectFolder(mark.id) }
                            MarkType.Bookmark -> {}
                        }
                    }
                )
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
        color = Color.LightGray,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
        ) {
            markPath.firstOrNull()?.let { mark ->
                Button(
                    onClick = { onMarkClick(mark) }
                ) {
                    Text("/")
                }
                Text("/")
            }
            for (mark in markPath.drop(1).dropLast(1)) {
                Button(
                    onClick = { onMarkClick(mark) }
                ) {
                    Text(mark.title)
                }
                Text("/")
            }
            markPath.lastOrNull()?.let { mark ->
                Text(mark.title)
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
) {
    val listState = rememberLazyListState()
    LazyColumn(state = listState, modifier = modifier) {
        items(children) { mark ->
            MarksItem(
                mark,
                onMarkClicked = { onMarkClick(it) }
            )
        }
    }
}

@Composable
private fun MarksItem(
    mark: MarkNode,
    modifier: Modifier = Modifier,
    onMarkClicked: (mark: MarkNode) -> Unit = {},
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
            .clickable { onMarkClicked(mark) }
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
                modifier = Modifier
                    .align(Alignment.CenterVertically)
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
                    marks.getMarkChildren(mark)
                }
                .let {
                    _markColumns.value = it
                }
        }
    }
}
