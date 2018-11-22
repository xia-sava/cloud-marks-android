package to.sava.cloudmarksandroid.activities

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.fragments.MarksFragment
import to.sava.cloudmarksandroid.libs.Marks
import to.sava.cloudmarksandroid.models.MarkNode
import to.sava.cloudmarksandroid.models.MarkType
import to.sava.cloudmarksandroid.libs.Settings
import to.sava.cloudmarksandroid.services.MarksService
import android.content.pm.PackageManager
import android.os.Build




class MainActivity : AppCompatActivity(),
        MarksFragment.OnListItemClickListener,
        MarksFragment.OnListItemChangListener,
        MarksService.OnMarksServiceCompleteListener {

    private lateinit var realm: Realm

    // region Android Activity Lifecycle まわり

    /**
     * アプリ起動時の処理．
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        realm = Realm.getDefaultInstance()

        // Activity復元でなく完全な初回起動の時は，
        // 前回開いていたフォルダまで移動してやる．
        if (savedInstanceState == null) {
            reTransitLastOpenedMarksFragment()
        }
    }

    /**
     * アプリ終了時の処理．珍しいことは何もしていない．
     */
    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    /**
     * バックボタンの処理．最上階で押されたらアプリ終了．
     */
    override fun onBackPressed() {
        super.onBackPressed()
        if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
        }
    }

    // endregion

    // region 一覧画面のUI処理まわり

    /**
     * 一覧に表示しているフォルダが変更された時にコールされる処理．
     * 一覧のタイトルを表示フォルダ名に合わせたり，
     * バックボタンアイコンを表示したりする．
     */
    override fun onListItemChange(mark: MarkNode?) {
        val backCount = supportFragmentManager.backStackEntryCount
        toolbar.title =
                if (backCount == 1) getString(R.string.app_name)
                else mark?.title ?: "ブックマークが見つかりません"
        supportActionBar?.setDisplayHomeAsUpEnabled(backCount > 1)
        Settings().lastOpenedMarkId = mark?.id ?: MarkNode.ROOT_ID
    }

    /**
     * 一覧のフォルダやブックマークがタップされた時の処理．
     * フォルダ遷移するか，あるいはブックマークを開く．
     */
    private fun onListItemClick(mark: MarkNode, choiceApp: Boolean) {
        when (mark.type) {
            MarkType.Folder -> {
                transitionMarksFragment(mark.id)
            }
            MarkType.Bookmark -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mark.url))
                if (choiceApp) {
                    // URLを選択するダイアログを出すのになんでこんな色々と……
                    val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PackageManager.MATCH_ALL
                    else
                        PackageManager.MATCH_DEFAULT_ONLY
                    val intents =
                            packageManager.queryIntentActivities(intent, flag).map {
                                Intent(intent).setPackage(it.activityInfo.packageName)
                            }.toMutableList()
                    val chooser = Intent.createChooser(intents.removeAt(0), getString(R.string.mark_menu_share_to)) // *1
                    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray()) // *1
                    startActivity(chooser)
                } else {
                    // 自動Intent起動
                    startActivity(intent)
                }
            }
        }
    }

    /**
     * @see onListItemClick
     */
    override fun onListItemClick(mark: MarkNode) {
        return onListItemClick(mark, false)
    }

    /**
     * 一覧のアイテム長押しメニューの処理．
     */
    override fun onContextItemSelected(item: MenuItem?): Boolean {
        val fragment = supportFragmentManager.fragments.last()
        if (fragment is MarksFragment) {
            item?.groupId?.let {
                fragment.adapter?.getItem(it)?.let { mark ->
                    when (item.itemId) {
                        R.id.mark_menu_open -> {
                            onListItemClick(mark)
                            return true
                        }
                        R.id.mark_menu_share_to -> {
                            onListItemClick(mark, true)
                            return true
                        }
                        R.id.mark_menu_copy_url -> {
                            clipboardManager.primaryClip = ClipData.newRawUri("", Uri.parse(mark.url))
                            toast(R.string.mark_toast_copy_url)
                            return true
                        }
                        R.id.mark_menu_copy_title -> {
                            clipboardManager.primaryClip = ClipData.newPlainText("", mark.title)
                            toast(R.string.mark_toast_copy_title)
                            return true
                        }
                        else -> Unit
                    }
                }
            }
        }
        return false
    }

    // endregion

    // region 右上メニューボタンまわり

    /**
     * 右上メニューボタン作成
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * 右上メニューボタンの状態をセット
     */
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // 接続未設定およびロード中は Load メニューを disable
        val marksMenuEnabled = (
                Settings().googleConnected &&
                        !CloudMarksAndroidApplication.instance.loading
                )
        menu?.findItem(R.id.main_menu_load)?.isEnabled = marksMenuEnabled
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * 右上メニューそれぞれの処理
     */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            // 右上といいつつ実は左上のバックボタンの処理もここなのだ
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.main_menu_settings -> {
                startActivity<SettingsActivity>()
            }
            R.id.main_menu_load -> {
                MarksService.startActionLoad(this)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // endregion

    // region フォルダ遷移まわり

    /**
     * 最後に開いていたフォルダを開き直す
     */
    private fun reTransitLastOpenedMarksFragment() {
        reTransitMarksFragmentTo(Settings().lastOpenedMarkId)
    }

    /**
     * Marks一覧を最初から指定フォルダまで遷移しなおす．
     */
    private fun reTransitMarksFragmentTo(markId: String) {
        val marks = Marks(realm)
        marks.getMark(markId)?.let { lastMark ->
            for (mark in marks.getMarkPath(lastMark)) {
                transitionMarksFragment(mark.id)
            }
        } ?: transitionMarksFragment(MarkNode.ROOT_ID)
    }

    /**
     * Marks一覧を指定フォルダへ遷移する．
     */
    private fun transitionMarksFragment(markId: String) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_view_wrapper, MarksFragment.newInstance(markId))
                .addToBackStack(markId)
                .commit()
    }

    // endregion

    // region サービス処理の終了連絡対応まわり

    /**
     * Activityのフォーカスがなくなった時とかにやっときたい作業を保持する．
     * これが null の間は pause してないので即時実行でOK
     */
    private var pendingActions: ArrayList<Runnable>? = null

    /**
     * pause状態に入る時に，タスクの入れ物を用意する．
     */
    override fun onPause() {
        super.onPause()
        pendingActions = ArrayList()
    }

    /**
     * pauseから脱する時に，タスクが積まれてたら全部実行して，入れ物はnullに．
     */
    override fun onResume() {
        super.onResume()
        pendingActions?.map { it.run() }
        pendingActions = null
    }

    /**
     * pause状態かどうかは気にせずとにかくタスクを積む．
     */
    private fun runOrAddPendingActions(routine: Runnable) {
        pendingActions?.apply {
            add(routine)
        } ?: runOnUiThread {
            routine.run()
        }
    }

    /**
     * MarksServiceがload処理とかを終えた後にListener経由でコールされる処理．
     *
     */
    override fun onMarksServiceComplete() {
        runOrAddPendingActions(Runnable {
            reTransitLastOpenedMarksFragment()
        })
    }

    // endregion
}
