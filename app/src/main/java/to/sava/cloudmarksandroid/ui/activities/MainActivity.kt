package to.sava.cloudmarksandroid.ui.activities

import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.fragment.app.commit
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import to.sava.cloudmarksandroid.CloudMarksAndroidApplication
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.databases.models.MarkNode
import to.sava.cloudmarksandroid.databases.models.MarkType
import to.sava.cloudmarksandroid.libs.Marks
import to.sava.cloudmarksandroid.libs.Settings
import to.sava.cloudmarksandroid.libs.clipboardManager
import to.sava.cloudmarksandroid.libs.toast
import to.sava.cloudmarksandroid.services.FaviconService
import to.sava.cloudmarksandroid.services.MarksService
import to.sava.cloudmarksandroid.ui.adapters.MarksRecyclerViewAdapter
import to.sava.cloudmarksandroid.ui.fragments.MarksFragment
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    internal lateinit var marks: Marks

    @Inject
    internal lateinit var settings: Settings

    // region Android Activity Lifecycle まわり

    /**
     * アプリ起動時の処理．
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // 前回開いていたフォルダまで移動する
        reTransitLastOpenedMarksFragment()
    }

    /**
     * pause状態から戻る時の処理．
     */
    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    /**
     * pause状態に入る時の処理．
     */
    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
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
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onListItemChange(event: MarksFragment.MarkListChangedEvent) {
        val mark = event.mark
        val backCount = supportFragmentManager.backStackEntryCount
        toolbar.title =
            if (backCount == 1) getString(R.string.app_name)
            else mark?.title ?: "ブックマークが見つかりません"
        supportActionBar?.setDisplayHomeAsUpEnabled(backCount > 1)
        settings.lastOpenedMarkId = mark?.id ?: MarkNode.ROOT_ID
    }

    /**
     * 一覧のアイテムがタップされた時にコールされる処理．
     * 長押しメニューからの動作もあるので本処理は [listItemClicked] を参照．
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onListItemClicked(event: MarksRecyclerViewAdapter.MarkClickedEvent) {
        return listItemClicked(event.mark, false)
    }

    /**
     * 一覧のフォルダやブックマークがタップされた時の処理．
     * フォルダ遷移するか，あるいはブックマークを開く．
     */
    private fun listItemClicked(mark: MarkNode, choiceApp: Boolean) {
        when (mark.type) {
            MarkType.Folder -> {
                transitionMarksFragment(mark.id)
            }
            MarkType.Bookmark -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mark.url))
                if (choiceApp) {
                    // URLを選択するダイアログを出すのになんでこんな色々と……
                    val flag = PackageManager.MATCH_ALL
                    val intents =
                        packageManager.queryIntentActivities(intent, flag).map {
                            Intent(intent).setPackage(it.activityInfo.packageName)
                        }.toMutableList()
                    val chooser = Intent.createChooser(
                        intents.removeAt(0),
                        getString(R.string.mark_menu_share_to)
                    ) // *1
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
     * 一覧のアイテム長押しメニューの処理．
     */
    override fun onContextItemSelected(item: MenuItem?): Boolean {
        val fragment = supportFragmentManager.fragments.last()
        if (fragment is MarksFragment) {
            item?.groupId?.let {
                fragment.adapter?.getItem(it)?.let { mark ->
                    when (item.itemId) {
                        R.id.mark_menu_open -> {
                            listItemClicked(mark, false)
                            return true
                        }
                        R.id.mark_menu_share_to -> {
                            listItemClicked(mark, true)
                            return true
                        }
                        R.id.mark_menu_copy_url -> {
                            clipboardManager.setPrimaryClip(
                                ClipData.newRawUri("", Uri.parse(mark.url))
                            )
                            toast(R.string.mark_toast_copy_url)
                            return true
                        }
                        R.id.mark_menu_copy_title -> {
                            clipboardManager.setPrimaryClip(
                                ClipData.newPlainText("", mark.title)
                            )
                            toast(R.string.mark_toast_copy_title)
                            return true
                        }
                        R.id.mark_menu_fetch_favicon -> {
                            FaviconService.startAction(this, mark.id)
                            return true
                        }
                        R.id.mark_menu_fetch_favicon_in_this_folder -> {
                            FaviconService.startAction(this, mark.id)
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
                settings.googleConnected &&
                        !CloudMarksAndroidApplication.instance.loading
                )
        menu?.findItem(R.id.main_menu_load)?.isEnabled = marksMenuEnabled
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * 右上メニューそれぞれの処理
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // 右上といいつつ実は左上のバックボタンの処理もここなのだ
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.main_menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            R.id.main_menu_load -> {
                MarksService.startActionLoad(this)
            }
            R.id.main_menu_about -> {
                val version = packageManager.getPackageInfo(packageName, 0).versionName
                AlertDialog.Builder(this).apply {
                    setTitle(R.string.app_name)
                    setMessage("Version: $version")
                    setPositiveButton("OK") { _, _ -> }
                }.show()
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
        reTransitMarksFragmentTo(settings.lastOpenedMarkId)
    }

    /**
     * Marks一覧を最初から指定フォルダまで遷移しなおす．
     */
    private fun reTransitMarksFragmentTo(markId: Long) {
        // 既にバックスタックがある場合は先頭まで戻してから遷移する
        supportFragmentManager.let {
            if (it.backStackEntryCount > 0) {
                it.popBackStack(it.getBackStackEntryAt(0).id, POP_BACK_STACK_INCLUSIVE)
            }
        }
        marks.getMark(markId)?.let { lastMark ->
            for (mark in marks.getMarkPath(lastMark)) {
                transitionMarksFragment(mark.id)
            }
        } ?: transitionMarksFragment(MarkNode.ROOT_ID)
    }

    /**
     * Marks一覧を指定フォルダへ遷移する．
     */
    private fun transitionMarksFragment(markId: Long) {
        supportFragmentManager.commit {
            replace(R.id.main_view_wrapper, MarksFragment.newInstance(markId))
            addToBackStack("$markId")
        }
    }

    // endregion

    // region サービス処理の終了連絡対応まわり

    /**
     * MarksServiceがload処理とかを終えた後にEventBus経由で通知される処理．
     * stickyにより，pause中はペンディングされてresume時に飛んでくる．
     */
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onMarksServiceComplete(event: MarksService.MarksServiceCompleteEvent) {
        reTransitLastOpenedMarksFragment()
    }

    /**
     * FaviconServiceが取得処理を終えた後にEventBus経由で通知される処理．
     * stickyにより，pause中はペンディングされてresume時に飛んでくる．
     */
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onFaviconServiceComplete(event: FaviconService.FaviconServiceCompleteEvent) {
        reTransitLastOpenedMarksFragment()
    }

    // endregion
}
