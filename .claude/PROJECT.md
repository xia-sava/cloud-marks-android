# Cloud Marks Android

AWS S3 上のブックマーク JSON (`cloud_marks` 形式) を同期・閲覧する Android アプリ。
フォルダ階層でブックマークを管理し、差分同期で効率的に更新する。

## 技術スタック

- **言語:** Kotlin 2.0
- **UI:** Jetpack Compose (Material Design)
- **DB:** Room 2.6.1 (スキーマ v1)
- **DI:** Koin (koin-ksp-compiler でアノテーション処理)
- **クラウド:** AWS SDK for Kotlin (S3)
- **バックグラウンド処理:** WorkManager
- **設定管理:** DataStore Preferences
- **画像読込:** Coil
- **JSON:** Gson
- **クラッシュレポート:** Firebase Crashlytics

## SDK バージョン

- minSdk: 30 / targetSdk: 35 / compileSdk: 35

## パッケージ構成

パッケージ: `to.sava.cloudmarksandroid`

```
app/src/main/java/to/sava/cloudmarksandroid/
├── CloudMarksAndroidApplication.kt   # Application、Koin 初期化
├── MainActivity.kt                   # エントリポイント、Scaffold レイアウト
├── databases/
│   ├── CloudMarksAndroidDatabase.kt  # Room Database 定義
│   └── TypeConverters.kt            # MarkType, ByteArray(Base64) 変換
├── entities/
│   ├── MarkNode.kt                  # ブックマーク Entity (id, type, title, url, order, parentId)
│   └── Favicon.kt                   # favicon キャッシュ Entity (domain, favicon, size)
├── repositories/
│   ├── MarkNodeDao.kt / MarkNodeRepository.kt
│   └── FaviconDao.kt / FaviconRepository.kt  # favicon は Google/Hatena CDN から取得
├── services/
│   ├── Marks.kt                     # ブックマーク同期ロジック (差分更新, SHA-256 検証)
│   └── Settings.kt                  # DataStore による設定管理
├── storage/
│   ├── Storage.kt                   # クラウドストレージ抽象基底
│   └── AwsS3Storage.kt             # S3 実装
├── workers/
│   └── MarkWorker.kt               # WorkManager Worker (LOAD/SAVE/MERGE)
├── ui/
│   ├── theme/                       # XiaGreen テーマ定義
│   ├── common/                      # 共通 Composable
│   └── preferences/                 # 設定画面用 UI 部品
├── viewmodels/
│   ├── MainPageViewModel.kt         # アプリ全体状態 (DB 初期化, 同期状態)
│   ├── MarksScreenViewModel.kt      # ブックマーク表示・ナビゲーション
│   └── SettingsViewModel.kt         # 設定状態管理
└── libs/
    └── DiModule.kt                  # Koin モジュール定義
```

## アーキテクチャ

MVVM パターン。ViewModel + Compose で UI を構成し、Koin で DI を行う。

- **UI 層:** Compose (MarksScreen, SettingsScreen)
- **ViewModel 層:** 状態管理とビジネスロジック呼び出し
- **サービス層:** Marks (同期ロジック), Settings (設定)
- **データ層:** Room (MarkNode, Favicon), S3 (リモート JSON)

## データモデル

- **MarkTreeNode** — JSON 上の再帰的ツリー構造 (type, title, url, children)
- **MarkNode** — Room Entity、フラットな親子関係 (parentId)
- **MarksJsonContainer** — S3 上の JSON 形式 (version, hash, contents)
- **Favicon** — domain をキーとした favicon バイナリキャッシュ

## 主な機能

- S3 からブックマーク JSON を読み書き (差分更新、SHA-256 整合性検証)
- フォルダ階層ナビゲーション (パンくずリスト、戻るボタン)
- favicon 表示 (Google / Hatena CDN から取得、Room にキャッシュ)
- 「ここまで読んだ」マーカー
- グリッド列数設定 (1〜5 列)
- URL コピー・共有・ブラウザで開く
- バックグラウンド同期 (WorkManager、ネットワーク制約付き)

## ビルド

- Gradle KTS (AGP 8.13.2)
- リリース署名: `releaseSigningConfigs.properties` に keystore 情報を記載
- リリースビルド: ProGuard minify + resource shrink 有効
- Firebase Crashlytics: デバッグビルドでネイティブシンボルアップロード

## テスト

### フレームワーク・ツール

- **JUnit 5** (jupiter) — Unit Test に使用
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
