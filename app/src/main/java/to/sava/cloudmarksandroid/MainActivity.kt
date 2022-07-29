package to.sava.cloudmarksandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import to.sava.cloudmarksandroid.ui.MainPage
import to.sava.cloudmarksandroid.ui.theme.CloudMarksAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CloudMarksAndroidTheme {
                MainPage()
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainPagePreview() {
    CloudMarksAndroidTheme {
        MainPage()
    }
}