package to.sava.cloudmarksandroid.update

/** 更新の適用(ダウンロードからインストーラの起動まで)の進み具合． */
sealed interface UpdateInstallState {

    /** 配布物をダウンロードしている．[totalBytes] は全体長が判らなければ 0． */
    data class Downloading(val receivedBytes: Long, val totalBytes: Long) : UpdateInstallState {

        /** 0.0〜1.0 の進み具合．全体長が判らなければ null． */
        val fraction: Float?
            get() = if (totalBytes > 0) {
                (receivedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
            } else {
                null
            }
    }

    /** ダウンロードした配布物を照合している． */
    data object Verifying : UpdateInstallState

    /** インストーラへ引き渡した．インストールするかどうかは利用者が確認画面で決める． */
    data object Launching : UpdateInstallState

    /** 適用できなかった．理由を握り潰さず保持する． */
    data class Failed(val reason: String) : UpdateInstallState
}

/** ダウンロードからインストーラの起動までの途中かどうか．途中なら操作を受け付けない． */
val UpdateInstallState?.isRunning: Boolean
    get() = when (this) {
        is UpdateInstallState.Downloading,
        UpdateInstallState.Verifying,
        UpdateInstallState.Launching -> true

        else -> false
    }
