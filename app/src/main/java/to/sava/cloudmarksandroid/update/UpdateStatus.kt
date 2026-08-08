package to.sava.cloudmarksandroid.update

/** 更新確認の結果． */
sealed interface UpdateStatus {

    /** 配布中の版が自分以下． */
    data object UpToDate : UpdateStatus

    /**
     * より新しい配布物がある．[url] は固定の配布元から組み立てた取得先，
     * [sha256] はダウンロード後の照合に使う．
     */
    data class Available(
        val versionName: String,
        val url: String,
        val sha256: String,
    ) : UpdateStatus

    /** 取得・解析・署名検証のいずれかに失敗した．理由を握り潰さず保持する． */
    data class Failed(val reason: String) : UpdateStatus
}
