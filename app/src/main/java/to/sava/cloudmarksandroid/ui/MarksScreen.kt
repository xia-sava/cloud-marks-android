package to.sava.cloudmarksandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.modules.Marks
import javax.inject.Inject

@Composable
fun MarksScreen(viewModel: MarksScreenViewModel, initialMarkId: Long) {
    val cols = 1
    val currentMarkId by remember { mutableStateOf(initialMarkId) }
    val markNode = viewModel.markNode.collectAsState(initial = Unit)

    Column(
        modifier = Modifier
    ) {
        MarksBreadcrumbs()
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in 1..cols) {
                MarksColumn(
                    modifier = Modifier
                        .weight(1f / cols)
                        .padding(end = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MarksBreadcrumbs(modifier: Modifier = Modifier) {
    Text(
        text = "/ hoge / fuga",
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(Color.LightGray)
    )
}

@Composable
private fun MarksColumn(modifier: Modifier = Modifier) {
    MarksList(
        modifier = modifier
    )
}

@Composable
private fun MarksList(modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val items = (0..100).toList()
    LazyColumn(state = listState, modifier = modifier) {
        items(items) {
            MarksItem("Item $it")
        }
    }
}

@Composable
private fun MarksItem(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clickable { }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(31.dp)
        ) {
            Text(
                text = name,
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
    private val marks: Marks
) : ViewModel() {
    private var _markNode: Flow<MarkNode> = flowOf()
    val markNode get() = _markNode

    fun getMarks(markId: Long) {
        _markNode = marks.getMarkFlow(markId)
    }
}
