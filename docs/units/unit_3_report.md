# Unit 3: 月次レポート

## 概要

月次の勤怠集計とCSVレポート出力機能を実装する。

## 依存

- unit_0_foundation（Employee Entity, 認証基盤）
- unit_1_attendance（AttendanceRecord, BreakRecord）

## ユーザーストーリー

- 管理者として、全社員の月次勤務集計を画面で確認したい
- 管理者として、月次レポートを CSV でダウンロードしたい

## スコープ

### Backend

- Service: ReportService（interface + impl）
- Controller: ReportController
- DTO: MonthlyReportResponse

### Frontend

- 管理者レポート画面（`/admin/reports`）: 月選択 + 集計テーブル + CSV DL ボタン

## API エンドポイント

| Method | Path | 説明 | ロール |
|--------|------|------|--------|
| GET | /api/reports/monthly?year&month | 月次集計データ | ADMIN |
| GET | /api/reports/monthly/csv?year&month | CSV ダウンロード | ADMIN |

## 集計ロジック

指定月の全社員について以下を算出する:

| 項目 | 計算方法 |
|------|---------|
| 勤務日数 | clock_in が NOT NULL のレコード数 |
| 総勤務時間 | Σ(clockOut - clockIn - 休憩合計) |
| 総休憩時間 | Σ(breakEnd - breakStart) |
| 残業時間 | Σ(1日の勤務時間 - 480分) ※マイナスは0 |

## CSV フォーマット

```csv
社員名,メールアドレス,勤務日数,総勤務時間(h),総休憩時間(h),残業時間(h)
田中太郎,tanaka@example.com,20,168.5,20.0,8.5
佐藤花子,sato@example.com,18,150.0,18.0,6.0
```

- 文字コード: UTF-8 (BOM 付き)
- 時間は小数表記（分→時間に変換、小数第1位）

## テスト観点

- ReportService: 月次集計が正しく計算される
- ReportService: 勤怠データがない社員は勤務日数 0 で返る
- ReportService: 残業がマイナスの場合は 0 になる
- ReportController: CSV のヘッダー・Content-Type が正しい
- ReportController: 一般社員がアクセスすると 403

## 完了基準

- [ ] 全テスト GREEN
- [ ] 管理者画面で月次集計確認
- [ ] CSV ダウンロードして Excel で開ける
- [ ] テストカバレッジ 80% 以上
