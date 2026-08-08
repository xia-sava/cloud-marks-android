package to.sava.cloudmarksandroid.modules

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout

/** 接続の確立を待つ上限． */
private const val CONNECT_TIMEOUT_MILLIS = 15_000L

/** 受信が止まったまま待つ上限． */
private const val STALL_TIMEOUT_MILLIS = 60_000L

/**
 * アプリ全体で使うHTTPクライアントを生成する．
 * 大きなファイルも扱うため全体の所要時間は縛らず，接続の確立と無通信だけを打ち切る．
 * リダイレクトは追うが https からの降格は許さない．
 */
fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = STALL_TIMEOUT_MILLIS
    }
    install(HttpRedirect) {
        allowHttpsDowngrade = false
    }
}
