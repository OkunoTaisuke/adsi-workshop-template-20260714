# Unit 0: 共通基盤

## 概要

プロジェクトの雛形・ビルド設定・テスト実行環境・認証基盤を構築する。
Phase B 以降の全 Unit がこの基盤の上に実装される。

## ユーザーストーリー

- 開発者として、`mvn test` で全テストを実行できる
- 開発者として、`npm run dev` でフロントエンドを起動できる
- ユーザーとして、メール + パスワードでログインできる
- ユーザーとして、ログイン後に認証が必要な画面にアクセスできる

## スコープ

### Backend

- Spring Boot プロジェクト雛形（Maven, パッケージ構成）
- Flyway 設定 + `V1__create_employees.sql`
- Employee Entity + EmployeeRepository
- Spring Security + JWT 認証（SecurityFilterChain, JwtTokenProvider）
- AuthService + AuthController（`POST /api/auth/login`, `GET /api/auth/me`）
- 共通例外ハンドラ（`@RestControllerAdvice`）
- テスト基盤: application-test.yml, H2, TestFixture
- ArchUnit テスト（レイヤー違反検出）

### Frontend

- Next.js プロジェクト雛形（TypeScript, Tailwind CSS）
- 共通レイアウト（Header/Nav, AuthGuard）
- API クライアント基盤（`withBasePath()`, エラーハンドリング）
- ログイン画面（`/login`）
- 共通ダッシュボード画面（`/`）: ナビゲーション + 今日の日付 + 各機能へのリンク
- 認証コンテキスト（AuthProvider, useAuth hook）

### インフラ / 設定

- Docker Compose（PostgreSQL, backend, frontend）
- 環境変数テンプレート（`.env.example`）

## テーブル

### employees

```sql
CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);
```

## API エンドポイント

| Method | Path | 説明 |
|--------|------|------|
| POST | /api/auth/login | ログイン → JWT 返却 |
| GET | /api/auth/me | ログインユーザー情報取得 |

## Enum

- `Role`: EMPLOYEE, ADMIN

## テスト観点

- AuthController: 正しい認証情報でトークンが返る
- AuthController: 不正な認証情報で 401 が返る
- AuthController: /me で認証済みユーザー情報が返る
- EmployeeRepository: 保存・検索の CRUD
- ArchUnit: Controller → Service → Repository の依存方向
- Frontend: ログイン画面のフォーム送信とエラー表示

## 完了基準

- [ ] `mvn test` が全て GREEN
- [ ] `npm run dev` でフロントエンド起動
- [ ] ログイン → JWT 取得 → /me で情報取得 の E2E 動作確認
- [ ] テストカバレッジ 80% 以上（Backend）
