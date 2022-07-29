package to.sava.cloudmarksandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MarksScreen() {
    val cols = 1
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
