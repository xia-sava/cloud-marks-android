# Cloud Marks Android

AWS S3 上のブックマーク JSON (`cloud_marks` 形式) を同期・閲覧する Android アプリ。
フォルダ階層でブックマークを管理し、差分同期で効率的に更新する。

## 技術スタック

- **言語:** Kotlin 2.3.10（AGP 9 built-in を `android.kotlin.version` でオーバーライド）
- **UI:** Jetpack Compose 1.10.3 (Material Design)
- **DB:** Room 2.8.4 (スキーマ v1)
- **DI:** Koin 4.1.1 (BOM, DSL ベース。KSP 不使用)
- **クラウド:** AWS SDK for Kotlin (S3) 1.6.19
- **HTTP:** Ktor Client 3.3.1 (OkHttp エンジン)
- **バックグラウンド処理:** WorkManager
- **設定管理:** DataStore Preferences
- **画像読込:** Coil 2.7.0
- **JSON:** kotlinx.serialization

## SDK バージョン

- minSdk: 30 / targetSdk: 36 / compileSdk: 36

## パッケージ構成

パッケージ: `to.sava.cloudmarksandroid`

```
app/src/main/java/to/sava/cloudmarksandroid/
├── CloudMarksAndroidApplication.kt   # Application、Koin 初期化、dataStore 拡張プロパティ
├── databases/
│   ├── CloudMarksAndroidDatabase.kt  # Room Database 定義、ByteArrayConverter / MarkTypeConverter
│   ├── dao/
│   │   ├── MarkNodeDao.kt
│   │   └── FaviconDao.kt
│   ├── models/
│   │   ├── MarkNode.kt              # MarkType, MarkNode (Entity), MarkTreeNode
│   │   └── Favicon.kt               # favicon キャッシュ Entity (domain, favicon, size)
│   └── repositories/
│       ├── MarkNodeRepository.kt
│       └── FaviconRepository.kt     # favicon は Google/Hatena CDN から取得
├── di/
│   └── DiModule.kt                  # Koin モジュール定義
├── modules/
│   ├── Marks.kt                     # ブックマーク同期ロジック (差分更新, SHA-256 検証)
│   ├── Settings.kt                  # PreferenceKeys, BaseSettings, Settings
│   ├── Storage.kt                   # FileInfo, MarksJsonContainer, Storage (interface), AwsS3Storage
│   ├── MarkWorker.kt                # WorkManager Worker (LOAD/SAVE/MERGE)
│   ├── Exceptions.kt
│   └── Extensions.kt
├── update/
│   ├── LatestManifest.kt            # latest.json の構造
│   ├── ManifestSignature.kt         # 検証鍵と署名検証
│   ├── ReleaseDownload.kt           # https 限定・上限つきダウンロード
│   ├── UpdateStatus.kt              # 更新確認の結果
│   ├── UpdateInstallState.kt        # 適用の進み具合
│   ├── UpdateChecker.kt             # 取得 → 署名検証 → 版比較
│   ├── UpdateInstaller.kt           # 取得 → 照合 → インストーラへ引き渡し
│   └── Updater.kt                   # 更新機能の配線
└── ui/
    ├── MainActivity.kt              # エントリポイント、Scaffold レイアウト、MainPageViewModel
    ├── MarksScreen.kt               # ブックマーク表示・ナビゲーション、MarksScreenViewModel
    ├── SettingsScreen.kt            # 設定画面、SettingsViewModel
    ├── AwsS3ConnectionPreference.kt # S3 接続確認の設定項目
    ├── preferences/                 # 設定画面用 UI 部品
    └── theme/                       # XiaGreen テーマ定義
```

## アーキテクチャ

MVVM パターン。ViewModel + Compose で UI を構成し、Koin で DI を行う。

- **UI 層:** Compose (MarksScreen, SettingsScreen)
- **ViewModel 層:** 状態管理とビジネスロジック呼び出し。ViewModel は対応する画面のファイルに同居する
- **サービス層:** Marks (同期ロジック), Settings (設定), Updater (自己更新)
- **データ層:** Room (MarkNode, Favicon), S3 (リモート JSON)

## データモデル

- **MarkTreeNode** — JSON 上の再帰的ツリー構造 (type, title, url, children)
- **MarkNode** — Room Entity、フラットな親子関係 (parentId)
- **MarksJsonContainer** — S3 上の JSON 形式 (version, hash, contents)
- **Favicon** — domain をキーとした favicon バイナリキャッシュ

`MarksJsonContainer.hash` は `contents` をシリアライズした JSON 文字列の SHA-256 で、読み込み時に
再計算して突き合わせる (version 1)。**照合はバイト列の一致で成り立っているため、シリアライズの
出力が変わると既存のデータが読めなくなる。** `MarkType` は列挙子の名前ではなく `rawValue`
(0 = Folder / 1 = Bookmark) として出し、HTML エスケープは行なわない。

## 主な機能

- S3 からブックマーク JSON を読み書き (差分更新、SHA-256 整合性検証)
- フォルダ階層ナビゲーション (パンくずリスト、戻るボタン)
- favicon 表示 (Google / Hatena CDN から取得、Room にキャッシュ)
- 「ここまで読んだ」マーカー
- グリッド列数設定 (1〜5 列)
- URL コピー・共有・ブラウザで開く
- バックグラウンド同期 (WorkManager、ネットワーク制約付き)
- アプリの自己更新 (GitHub Releases から取得)

## ビルド

- Gradle 9.3.1 KTS (AGP 9.0.1, KSP 2.3.5)
- リリースビルド: ProGuard minify + resource shrink 有効
- リリース署名: keystore とその資格は環境変数 `CLOUD_MARKS_RELEASE_*` から受け取る。
  鍵はリポジトリの外 (`~/.keys/cloud-marks/`) に置き、手元では `signing.env` を読み込んでから
  ビルドする。設定が無ければ署名せずに組む (debug は AGP 既定の署名で組まれる)

```sh
set -a; . ~/.keys/cloud-marks/signing.env; set +a
export CLOUD_MARKS_RELEASE_KEYSTORE="$(cygpath -m ~/.keys/cloud-marks/release.jks)"
./gradlew :app:assembleRelease
```

`storeFile` は Java の `File` として解決されるため、`/c/...` 形式では通らない。
git bash からは `cygpath -m` で `C:/...` 形式に直して渡す。

## 配布と自己更新

Play ストアには出さず GitHub Releases で独自配布し、アプリ自身が更新を確認して適用する。

### 配布フロー

`vMAJOR.MINOR.PATCH` の形のタグを push したときだけ、GitHub Actions
(`.github/workflows/release.yml`) が配布物を組んで公開する。配布物は固定名
(`cloud-marks.apk` / `latest.json`) で、URL はビルドを跨いで変わらない。

1. `version` ジョブが版数を決める。`versionCode` は `YYYYMMDDnn` (JST の日付 + 2 桁の連番) とし、
   連番は配布中の `latest.json` を見て、同じ日付なら +1、別の日付なら `01` にする。
   `versionName` はタグ名から先頭の `v` を除いたもの。
   公開の前に、配布中の `latest.json` より `versionCode` が増えていることを確かめる。
   タグの打ち間違いは配布物としては正常に見えるため、機械で止める。
2. `check` ジョブが Unit Test を回す。ここが落ちればリリースまで進まない。
3. `android` ジョブが APK を配布用の署名鍵で署名して組む。鍵は CI の secrets に置き、
   ビルド時に環境変数で Gradle へ渡す。署名鍵を替えると端末上で上書き更新できなくなる
   (アンインストールが要る) ため、鍵は替えない。
4. `manifest` ジョブが APK の sha256 を採って `latest.json` を書き、その生バイトへ署名して
   `latest.json.sig` を作る。署名鍵は secrets (パスフレーズなしの PKCS#8 PEM) に置き、
   環境変数で受け取って権限を絞った一時ファイルへ書いて使い、ステップを抜けるときに消す。
   鍵をコマンドライン引数へ載せるとプロセス一覧から読めるため渡さない。
   署名したら、アプリに埋め込んであるのと同じ公開鍵で検証してから次へ進む。
   鍵が食い違ったまま公開すると、配布済みの全端末が更新できなくなる。
5. `release` ジョブが配布物を 2 か所へ公開する。打ったタグのリリース (版ごとの記録。配布物の実物が
   残るので、後からどの版で何を配ったかを辿れる) と、タグ `latest` の prerelease (配布済みのアプリが
   引く更新経路。マニフェストと署名もここに置く)。**`latest` は置き場を動かさず中身だけ差し替える。**
   アプリは取得先を `latest` 固定で組み立てるため、ここを動かすと配布済みの端末が更新を見つけられない。

`workflow_dispatch` で手動実行したときは、実機で試すための署名済み APK を成果物として残すだけで、
リリースには触れない。`versionName` は `dev-<短縮 SHA>` とする。

### CI の権限

- ワークフロー全体の既定は権限なしとし、各ジョブが必要な分だけ宣言する。リリースを公開できる
  権限 (`contents: write`) を持つのは `release` ジョブだけとする。ビルドスクリプトやプラグインの
  コードを実行するジョブが公開の権限を持つと、依存の 1 つが汚染されただけで配布物を差し替えられる。
- `actions/checkout` は `persist-credentials: false` で使う。既定では資格情報が作業ツリーの
  `.git/config` に残り、同じジョブで走るビルドコードから読める。
- 署名鍵を持つのは `manifest` ジョブだけとし、このジョブはリリースを書き換える権限を持たない。
  リリースへ書ける権限を奪われても署名は作れず、署名鍵を奪われてもリリースは書き換えられない。

### latest.json

```json
{
  "android": {
    "versionCode": 2026080801,
    "versionName": "1.4.0",
    "url": "https://github.com/xia-sava/cloud-marks-android/releases/download/latest/cloud-marks.apk",
    "sha256": "..."
  }
}
```

- `sha256` は必須。欠けたマニフェストは受理せず、更新確認は失敗になる。照合を省く経路を作らない。
- **配布物の取得先はアプリが組み立てる。** マニフェストが動かせるのは版と照合値だけで、`url` は
  人が読むための記載としてリリースには載せるがアプリは参照しない。

`latest.json` の**生バイト全体**に対する署名を、同じリリースへ `latest.json.sig` として並べて置く。

- 方式は ECDSA P-256 (`SHA256withECDSA`)。署名は DER のまま base64 にした 1 行のテキストで置く。
  検証鍵は X.509 SubjectPublicKeyInfo の DER を base64 にした公開鍵で、ソースへ定数として埋め込む。
  配布経路 (TLS) ともマニフェストの中身とも独立した信頼の起点になる。
- Ed25519 は Android の標準提供が API 33 からで minSdk 30 では使えないため採らない。

### 更新確認と適用

- 更新確認は設定画面の「Application Update」からの手動実行のみ。起動時の自動チェックは行なわない
  (開発環境ではマニフェストが配信されておらず、失敗表示が常時出てしまうため)。
- **マニフェストは署名を確かめてからでないと解釈しない。** 取得したバイト列をそのまま検証し、
  通ったものだけを解析する。署名が取得できない・base64 として読めない・埋め込みの公開鍵と合わない・
  バイト列と食い違う場合はいずれも拒否する。表示は「配布物の署名を確認できませんでした」とし、
  取得や解析の失敗と区別できるようにする。どこで落ちたかの内訳は利用者向けの文言には出さず、
  ログに残す。
- **配布物として動いていないビルドでは、確認ボタンを操作できない状態で見せ、配布版でのみ更新できる
  旨を注記する。** 判定は `debuggable` フラグで行う。開発ビルドの版数は配布版より常に古いため
  確認すれば必ず「更新あり」になるが、署名やインストール形態が食い違うので適用は必ず失敗する。
- 適用は「ダウンロード → sha256 と packageName の照合 → インストーラへ引き渡し」の順に進み、
  進み具合を確認結果の下に表示する。照合に失敗した配布物は破棄して適用しない。
- ダウンロードは https に限る。受け取る量には上限 (200 MiB) を設け、超える応答は書き込みを止めて
  失敗させる。接続の確立と無通信にも打ち切りを課すが、全体の所要時間は縛らない。
- APK は `REQUEST_INSTALL_PACKAGES` 権限と FileProvider (`${applicationId}.updates`) 経由で
  インストール確認の Intent を発行する。インストールの可否はユーザーが確認画面で決める。
  Play ストア外の配布物なので Play プロテクトの警告を経由する。

### 版を上げるときの約束

- **配布物を下げる手段は無い。** Android は `versionCode` が下がるインストールを拒むため、旧版へ
  戻すにはアンインストールが要る (ブックマークの DB と設定が消える)。問題が出たら戻すのではなく、
  直した版を出す。
- 版数の桁が日付に依存するため、`versionCode` を手で書き換えない。CI が決めた値より小さい値を
  一度でも配ると、その端末は次の版が降りるまで更新できなくなる。

## テスト

### フレームワーク・ツール

- **JUnit 6** (jupiter 6.0.3) — Unit Test に使用
- **Kover** — カバレッジ計測。`./gradlew koverHtmlReport` で HTML レポート出力

### テストの書き方ルール

- メソッド名は英語、テストの説明は KDoc コメント (`/** ... */`) で日本語で書く
- ユーティリティ関数にも KDoc コメントを付ける
- 関連するテストは `@Nested inner class` でグルーピングする
- パラメータ化テストは `@CsvSource` を優先する（enum やプリミティブなら文字列で書ける）
- `@CsvSource` では表現できない複雑なオブジェクトを渡す場合のみ、トップレベル関数 + `@MethodSource("FQCN#関数名")` を使う
  - `companion object` + `@JvmStatic` は使わない

## テーマ

- Primary: XiaGreen (#80FFC0)
- Dark: XiaDarkGreen (#408060)
- Deep: XiaDeepGreen (#204030)
- システムのダーク/ライトモードに追従
