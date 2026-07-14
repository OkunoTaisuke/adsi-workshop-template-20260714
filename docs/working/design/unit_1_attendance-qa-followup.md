# Unit 1: 勤怠打刻 — 設計 Q&A（追加確認）

> 前回の回答を受けて、設計に影響する追加確認事項。

---

## A. 日跨ぎ勤務の扱い

回答: 日跨ぎ勤務を「考慮する」

[Question] 現在の設計では `clock_in` / `clock_out` が TIME 型（時刻のみ、日付なし）です。日跨ぎを表現するために以下のどれを採用しますか？
  a) TIME のまま、退勤が出勤より小さい場合は翌日とみなすロジックを入れる（例: 23:00→02:00 → 勤務3時間）
  b) TIMESTAMP 型に変更する（日付+時刻で保存。`date` カラムは出勤日の意味になる）
  c) `clock_out_next_day` (BOOLEAN) カラムを追加する
[Answer]TIME のまま、退勤が出勤より小さい場合は翌日とみなすロジックを入れる

---

## B. 打刻ポップアップの仕様

回答: サーバー時刻をデフォルト表示 → ポップアップで確認/変更 → 送信

[Question] 打刻 API のリクエストボディを以下のように変更してよいですか？

```json
// POST /api/attendance/clock-in
{ "clockIn": "09:15" }

// POST /api/attendance/clock-out
{ "clockOut": "18:00" }

// POST /api/attendance/break-start
{ "breakStart": "12:00" }

// POST /api/attendance/break-end
{ "breakEnd": "13:00" }
```

フロント側ではボタン押下時に現在時刻をプリセットしたポップアップを表示し、ユーザーが確認/変更後に送信する形とします。
[Answer]問題ない

---

## C. 出勤時の休憩デフォルト設定

回答: 出勤送信時にデフォルトで 12:00-13:00 の休憩が設定される

[Question] これは以下のどちらの意味ですか？
  a) 出勤 API レスポンス後、フロントが「休憩 12:00-13:00」をプリセット表示するのみ（DB には保存しない。退勤時やユーザー操作時に保存）
  b) 出勤 API 呼び出し時に、サーバー側で BreakRecord(12:00-13:00) を自動作成して DB に保存する
[Answer]この仕様は無しでよい

---

## D. 打刻の無効化

回答: 出勤・退勤・休憩の時刻を空にすることで無効化

[Question] 無効化は打刻修正 API（PUT /api/attendance/{id}）で `clockIn: null` のように送る形でよいですか？
  例: 退勤を取り消す場合 → `PUT /api/attendance/123 { "clockOut": null }`
  例: 出勤を取り消す場合 → `PUT /api/attendance/123 { "clockIn": null }` → レコード自体を削除？
[Answer]問題ない。レコードは削除しない

[Question] 出勤を無効化（clockIn を空に）した場合、その日の AttendanceRecord 自体を削除しますか？それとも clockIn = null のまま残しますか？
[Answer]レコードは基本的に削除しない

---

## E. 所属（部署）について

回答: 管理者画面で「所属/月を選択、ユーザーを入力」して1ページ表示

[Question] 現在のデータモデルには「所属（部署）」がありません。以下のどちらにしますか？
  a) Unit 1 のスコープで departments テーブルと employees.department_id を追加する
  b) Unit 1 では「所属」による絞り込みは対象外とし、「ユーザー名/月」のみで絞り込む（所属は将来 Unit で追加）
[Answer]Unit 1 のスコープで departments テーブルと employees.department_id を追加する

---

## F. 勤務時間の DB 保存

回答: 退勤時または時刻修正時に計算して DB に保存

[Question] `attendance_records` テーブルに以下のカラムを追加する形でよいですか？

```sql
ALTER TABLE attendance_records ADD COLUMN total_work_minutes INTEGER;
ALTER TABLE attendance_records ADD COLUMN total_break_minutes INTEGER;
ALTER TABLE attendance_records ADD COLUMN overtime_minutes INTEGER;
```

退勤打刻時・修正時に再計算して更新する。未退勤（clock_out = null）の場合はすべて null。
[Answer]問題ない

---

## G. コアタイムチェックの表示

回答: 問題があれば警告マーク、なければグリーンチェック

[Question] チェック条件は以下で正しいですか？

| 条件 | 結果 |
|------|------|
| 出勤 ≤ 10:00 かつ 退勤 ≥ 15:00 かつ 勤務時間 ≥ 8時間 | グリーンチェック |
| 出勤 > 10:00（コアタイム遅刻） | 警告マーク |
| 退勤 < 15:00（コアタイム早退） | 警告マーク |
| 勤務時間 < 8時間（所定労働時間不足） | 警告マーク |

[Answer]問題ない

---

すべての [Answer] が埋まったら設計確定版を作成します。
