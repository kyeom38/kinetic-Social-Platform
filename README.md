# Kinetic Social

社内交流活性化ソリューションプラットフォーム。
情報共有・コミュニケーションの場として、
掲示板・イベント参加投票・社員プロフィール・組織図を一体化したWebアプリ。

---

## 開発背景・目的

総務部の発表より、社内イベントへの参加率低下という課題をきっかけに開発。

- リモート環境下でも社員同士が日常的に交流できる場を設けることで、オフラインイベントへの参加意欲向上につなげる
- 社員の趣味・志向・コミュニケーションスタイルを可視化し、イベント企画の参考情報として活用できるようにする

---

## 技術スタック

| 層           | 技術                                         |
| ------------ | -------------------------------------------- |
| バックエンド | Java 17 / Spring Boot 3.3.5                  |
| テンプレート | Thymeleaf + Thymeleaf Spring Security Extras |
| ORM          | Spring Data JPA / Hibernate                  |
| DB (本番)    | PostgreSQL (Neon serverless)                 |
| DB (テスト)  | H2 in-memory                                 |
| 認証         | Spring Security (フォームログイン)           |
| フロント     | Bootstrap 5.3 / 独自CSS                      |
| RSS取得      | ROME 2.1.0                                   |
| ビルド       | Maven (mvnw)                                 |

---

## セットアップ・起動

### 前提

- Java 17 以上

### ビルド

```bash
# Windows (日本語パスの場合は mvnw:run が失敗するため JAR 経由で起動する)
.\mvnw.cmd package -DskipTests
java -jar target\demo-0.0.1-SNAPSHOT.jar
```

ブラウザで `http://localhost:8080` を開く。

### 設定ファイル

`src/main/resources/application.properties.example` をコピーして `application.properties` にリネームし、各項目を設定する。

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

コピー後、以下の項目を必ず実際の値に書き換える。

| キー | 説明 |
| ---- | ---- |
| `spring.datasource.*` | DB接続情報 |
| `admin.email` | 初期管理者のメールアドレス |
| `admin.name` | 初期管理者の表示名 |
| `admin.password` | 初期管理者のパスワード。ローカル開発では直接パスワードを記載してよい（`application.properties` は `.gitignore` 対象のため安全）。本番環境では環境変数 `ADMIN_PASSWORD` をセットすれば `${ADMIN_PASSWORD}` のまま自動解決される |

#### DBの用意

**パターンA：既存のNeon DBに接続する（共有環境）**

管理者から共有された接続情報（URL・ユーザー名・パスワード）を `application.properties` に記載する。

**パターンB：自分用のDBを用意する（開発・テスト推奨）**

1. [Neon](https://neon.tech) で無料アカウントを作成してプロジェクトを作成する
2. 発行された接続情報を `application.properties` に記載する
3. 起動後、テストデータを投入する場合は `app.db.seed=true` にして起動し、完了後 `false` に戻す

### テスト

```bash
.\mvnw.cmd test
```

テストは H2 in-memory DB を使用するため、本番DBへの接続不要。

---

## アカウントとロール

| ロール    | できること                                                                                   |
| --------- | -------------------------------------------------------------------------------------------- |
| **ADMIN** | 全機能 + ユーザー管理（登録・削除） + 管理者専用カテゴリ(社内ニュース・イベント)への投稿など |
| **USER**  | 掲示板閲覧・投稿・コメント・投票・プロフィール編集など                                       |

ログイン後の遷移：

- ADMIN → `/admin/users`
- USER → `/home`

---

## 実装済み画面・機能（フェーズ1）

- Bootstrap 5 によるレスポンシブデザイン（モバイル・タブレット対応）

### ホーム (`/home`)

- 組織図(登録から７日以内の社員はNEWのバッジ表示)
- お知らせ（アナウンス投稿）一覧
- 最新投稿・閲覧数ランキング・ITニュースのピックアップカード
- 直近イベント一覧（eventDate/締め切り日が今日以降のポスト）

### 掲示板 (`/posts`)

- カテゴリ別・サブカテゴリ別絞り込み（ページネーション: 12件/ページ）
- キーワード検索（タイトル + 本文、大小文字無視）
- 投稿の物理削除（著者本人または管理者）
- IT関連ニュースは毎日8時に最新の情報が追加される。当然社員が直接投稿することも可能

**カテゴリ構成**

| カテゴリ               | 投稿可能ロール | 備考                |
| ---------------------- | -------------- | ------------------- |
| 社内ニュース・イベント | ADMIN のみ     | イベント日設定可    |
| IT関連ニュース         | 全員           | RSS自動取得 or 手動 |
| お誘い                 | 全員           | イベント日必須      |
| その他のイベント       | 全員           | イベント日任意      |
| 趣味・おしゃべり       | 全員           | サブカテゴリあり    |

趣味・おしゃべりのサブカテゴリ: スポーツ / 音楽 / アニメ / 旅行 / ゲーム / 動物 / その他

### 投稿作成・編集 (`/posts/new`, `/posts/{id}/edit`)

- カテゴリに応じてフォーム項目を動的切り替え（JS + サーバーサイド定数）
  - イベント系: 開催日・締め切り・定員・場所
  - IT関連ニュース: 参照元URL
  - 趣味: サブカテゴリ

### 投稿詳細 (`/posts/{id}`)

- 閲覧数カウント（詳細ページ表示ごとにインクリメント、閲覧数は管理者のみ確認可能）
- コメント（スレッド返信対応、返信フォームはトグル表示）
- コメントのソフト削除（著者本人または管理者、`(削除されたコメント)` に置換）
- イベント参加投票: 参加 / 不参加 / 未定（1人1票、upsert）
  - 参加者名・不参加者名・未定者名一覧を表示

### プロフィール (`/users/{id}`)

- 名前・ニックネーム・部署・役職・誕生日・自己紹介・趣味・MBTI・コミュニケーションスタイル
- アイコン（絵文字）または初期文字アバター

### プロフィール編集 (`/users/{id}/edit`)

- 本人のみ編集可
- 編集可能: ニックネーム・自己紹介・趣味・MBTI・コミュニケーションスタイル・社風への思い
- 編集不可（管理者のみ変更可）: 氏名・社員番号・部署・役職・生年月日

### アイコン変更 (`/users/{id}/photo/edit`)

- 絵文字アイコンをグループ別グリッドから選択
- グループ: 人 / スポーツ / ゲーム / 動物 / お酒 / 音楽・マイク / 旅行 / 食べ物

### パスワード変更 (`/users/{id}/password`)

- 現在のパスワードを確認してから変更
- 新しいパスワードの確認入力（クライアントサイドでも一致チェック）

### ユーザー管理 (ADMIN, `/admin/users`)

- 在籍中社員一覧（名前・メール・部署・役職・権限）
- 退職済み社員一覧（退職日表示）
- 社員検索（名前・メール・部署）
- 新規アカウント作成リンク

### アカウント作成 (ADMIN, `/admin/users/new`)

- 氏名・社員ID・メール・初期パスワード・部署・役職・生年月日・権限を登録
- メールアドレス・社員IDの重複チェック

### 社員情報編集 (ADMIN, `/admin/users/{id}/edit`)

- 管理者が編集可能な項目: 氏名・社員番号・部署・役職・生年月日・権限
- パスワード強制リセット

### 退職処理 (ADMIN)

- 退職日を入力して実行（誤操作防止）
- ソフト削除（`active=false` + `retiredAt` を設定、DBから物理削除しない）
- 退職後も投稿・コメントの著者名を保持

### ITニュース自動取得

- 毎朝8時に RSS フィードから自動取得（Zenn AI・プログラミング、Qiita人気記事）
- 管理者が手動実行も可能（掲示板画面のボタン）
- 同タイトルが存在する場合はスキップ（重複防止）

---

## アーキテクチャ概要

```
src/main/java/com/example/demo/
├── config/
│   ├── SecurityConfig.java         # Spring Security 設定（ロール・パスのアクセス制御）
│   ├── RoleBasedSuccessHandler.java # ログイン後のロール別リダイレクト
│   ├── GlobalModelAdvice.java      # 全テンプレートに currentUser を自動注入
│   ├── AdminInitializer.java       # 起動時に ADMIN が0人なら自動作成
│   ├── DatabasePatch.java          # 起動時のスキーマ差分適用（冪等）
│   └── DataSeeder.java             # 開発用ダミーデータ投入（app.db.seed=true で起動）
├── entity/                         # JPA エンティティ
│   ├── User.java                   # active/retiredAt でソフト削除
│   ├── Post.java
│   ├── Comment.java                # parentId でスレッド / deleted でソフト削除
│   └── EventVote.java              # attending: null=未定, true=参加, false=不参加
├── repository/                     # Spring Data JPA リポジトリ
├── service/                        # ビジネスロジック
│   ├── PostService.java            # toListDto(N+1回避) / toDto(詳細用) の2段構え
│   ├── UserService.java
│   ├── CommentService.java
│   └── NewsScheduler.java          # RSS取得 (@Scheduled)
├── controller/                     # Spring MVC コントローラー
└── dto/                            # View 層との受け渡し用 DTO
```

```
src/main/resources/
├── templates/
│   ├── fragments/sidebar.html      # 共通サイドバー断片（全ページで th:replace）
│   ├── error/404.html
│   └── *.html                      # 各ページテンプレート
└── static/
    └── css/sidebar.css             # サイドバー・レイアウト共通CSS
```

### 設計上のポイント

- **N+1 対策**: 一覧取得は `toListDto()`（vote/comment リポジトリを呼ばない）、詳細は `toDto()`（全データ取得）で分離
- **スキーマ管理**: `DatabasePatch` が起動時に `IF NOT EXISTS` / `DROP NOT NULL` などを冪等に適用。Flyway/Liquibase を使わず差分SQLを直接実行
- **認証**: Spring Security のフォームログイン。`ROLE_ADMIN` / `ROLE_USER` の2ロール
- **退職処理**: ユーザーを物理削除せず `active=false` にすることで、既存の投稿・コメントに著者情報が残る

---

## DB テーブル構成

| テーブル       | 概要                                                 |
| -------------- | ---------------------------------------------------- |
| `users`        | 社員アカウント（active/retiredAt でソフト削除）      |
| `user_hobbies` | users の趣味リスト（ElementCollection）              |
| `posts`        | 掲示板投稿（カテゴリ・イベント情報・閲覧数）         |
| `comments`     | コメント（parentId: 返信, deleted: ソフト削除）      |
| `event_votes`  | イベント参加投票（post_id + user_id に UNIQUE 制約） |

---

## テスト構成

```
src/test/java/com/example/demo/
├── DemoApplicationTests.java           # Spring コンテキスト起動確認（H2使用）
└── service/
    ├── PostServiceTest.java            # 投稿一覧・詳細・作成・更新・削除（10件）
    ├── CommentServiceTest.java         # ソフト削除・スレッド取得（7件）
    └── UserServiceTest.java            # 作成・編集・退職・パスワード変更（10件）
```

- Service テストは `@ExtendWith(MockitoExtension.class)` による純粋なユニットテスト（DB不使用）
- `PostService` の `toListDto` / `toDto` の使い分けに合わせてスタブを分離（不要スタブを排除して `UnnecessaryStubbingException` を防止）

---

## ロードマップ

### フェーズ2: エンゲージメント基盤

| 機能         | 概要                                                                    |
| ------------ | ----------------------------------------------------------------------- |
| いいね機能   | 投稿・コメントへのリアクション                                          |
| 偽名相談窓口 | 匿名で管理者にのみメッセージを送れる相談フォーム                        |
| PWA対応      | manifest.json + Service Worker によるアプリライクな体験・オフライン対応 |

### フェーズ3: パーソナライズ・コミュニティ深化

| 機能             | 概要                                                                                                   |
| ---------------- | ------------------------------------------------------------------------------------------------------ |
| 独自社内傾向診断 | MBTIに替わる社内キャラクター診断（成長型・安定型・引きこもり型など）。相性・好みのイベント傾向を可視化 |
| フォロー機能     | 気になる社員をフォローして動向を把握                                                                   |

### フェーズ4: ゲーミフィケーション

| 機能                         | 概要                                                   |
| ---------------------------- | ------------------------------------------------------ |
| アプリ内ゲーム・ボードゲーム | 社員が共同参加できる簡易ゲーム                         |
| ポイントシステム             | ゲームや活動でポイントを獲得                           |
| 景品・要望ページ             | ポイントと交換できる景品の管理・イベント景品の要望投稿 |
