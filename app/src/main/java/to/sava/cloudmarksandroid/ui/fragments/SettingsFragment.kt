package to.sava.cloudmarksandroid.ui.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import to.sava.cloudmarksandroid.R
import to.sava.cloudmarksandroid.libs.toast

/**
 * プリファレンスフラグメントの汎用のやつ
 */
open class SettingsFragment : PreferenceFragmentCompat() {
    /**
     * プリファレンスが生成された時の処理．
     * キーに応じた画面設定を読み込む．
     * 画面ごとの個別設定なんかもここでやれる．
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    fun toast(message: CharSequence) {
        activity?.toast(message)
    }

    fun toast(message: Int) {
        activity?.toast(message)
    }
}