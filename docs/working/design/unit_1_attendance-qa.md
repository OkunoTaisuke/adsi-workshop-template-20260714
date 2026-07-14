# Unit 1: 勤怠打刻 — 設計 Q&A

> 要求仕様: `docs/units/unit_1_attendance.md`
> 全体設計: `docs/design/`
> 本ドキュメントで仕様上の判断を確定させてから実装に進む。

---

## 1. ドメインモデル

### 1.1 AttendanceRecord

[Question] AttendanceRecord の `clock_in` / `clock_out` は TIME 型（時刻のみ）で設計されています。日跨ぎ勤務（例: 23:00 出勤 → 翌 02:00 退勤）を考慮する必要はありますか？
[Answer]考慮する

[Question] 出勤打刻（clock-in）はボタン押下時のサーバー時刻を使いますか？それともクライアント時刻を送信して記録しますか？
[Answer]サーバー時刻をデフォルト表示として送信前に変更できるようにすること。退勤・休憩についても同様とする。ボタン押下時にポップアップを表示し時刻確認・変更後に送信用のボタンを押下することで反映されるようにする。休憩は出勤を送信するとデフォルトで12:00-13:00に設定されることとする

### 1.2 BreakRecord

[Question] 休憩の開始/終了について:
  a) 未終了の休憩がある状態で新しい休憩を開始できますか？（→ 自動で前の休憩を終了？or エラー？）
  b) 1日の休憩回数に上限はありますか？
[Answer]未終了の休憩がある旨をポップアップ表示する

[Question] 休憩中に退勤打刻した場合の挙動は？
  a) 休憩を自動終了して退勤を記録する
  b) エラーにして「先に休憩終了してください」と返す
[Answer]未終了の休憩がある旨をポップアップ表示する

### 1.3 AttendanceRevision（修正履歴）

[Question] 打刻修正で修正できるフィールドは `clock_in` と `clock_out` のみですか？休憩時刻の修正も必要ですか？
[Answer]休憩時間も修正できるようにする

[Question] 修正の回数制限や修正可能な期間（例: 当月分のみ修正可能）はありますか？
[Answer]当月分のみ修正可能

---

## 2. ビジネスルール

### 2.1 打刻制約

[Question] 退勤後の再出勤（同日）は不可ですが、退勤の取り消し（退勤打刻を無効にしてまた勤務中に戻る）は必要ですか？
[Answer]退勤のみでなく出勤・休憩についても時刻を空にすることで無効にすることができるようにする

[Question] 打刻時にコアタイム（10:00〜15:00）のチェックやアラートは行いますか？例:
  - 10:00 以降に出勤 → 警告を表示
  - 15:00 以前に退勤 → 警告を表示
  あるいは初期リリースではコアタイムチェックは無し？
[Answer]出勤・退勤自体は可能としチェック欄に警告マークを表示するようにする。出勤・退勤に時間や勤務時間に問題がなければグリーンのチェックを表示する

### 2.2 勤務時間計算

[Question] 勤務時間・残業時間の計算は API レスポンスで返すのみですか？それとも DB にも計算結果を永続化しますか？
  a) 都度計算（clock_in, clock_out, break から算出）— シンプル
  b) 退勤時に計算して DB に保存 — パフォーマンス考慮
[Answer]退勤時または時刻修正時に計算して DB に保存

[Question] 勤務時間の端数処理は？
  a) 分単位でそのまま（例: 7時間52分 → 472分）
  b) 15分単位で丸める
  c) 30分単位で丸める
[Answer]分単位でそのまま

---

## 3. API 設計

### 3.1 打刻 API レスポンス

[Question] 打刻 API（clock-in, clock-out, break-start, break-end）のレスポンスとして、今日の勤怠全体（AttendanceResponse）を返す設計になっています。これで問題ないですか？（フロント側で今日の状態を即座に更新できる）
[Answer]問題ない

### 3.2 勤怠一覧 API

[Question] `/api/attendance/my` の月別一覧について、その月にレコードがない日（まだ出勤していない日）は:
  a) レスポンスに含めない（レコードがある日のみ返す）
  b) 全日分を返す（未出勤日は null で埋める）
[Answer]全日分を返す

### 3.3 管理者 API

[Question] `/api/attendance/all` はページネーションが必要ですか？50人規模ならば月別で最大 50×31 = 1,550 レコード程度ですが:
  a) ページネーション不要（一括で返す）
  b) ページネーションあり（ページサイズ指定）
[Answer]所属/月を選択、ユーザーを入力することにより1ページで表示ができるようにする

---

## 4. フロントエンド

### 4.1 ダッシュボード

[Question] ダッシュボード画面のリアルタイム性はどこまで必要ですか？
  a) ページロード時に1回取得（手動リロードで更新）
  b) ポーリングで定期更新（例: 30秒ごと）
  c) ボタン操作後のみレスポンスで更新（SPA 内の状態更新）
[Answer]ページロード時に1回取得

[Question] 勤務時間のリアルタイム表示（出勤中に「現在 3時間15分経過」のようなタイマー表示）は必要ですか？
[Answer]不要

### 4.2 勤怠一覧

[Question] 勤怠一覧で表示する曜日について、土日祝日の色分けや「休日」マーク表示は必要ですか？
[Answer]祝日・休日は不要。土日と平日(月,火,水,木,金)の色分け

---

## 5. エラーハンドリング

[Question] 打刻 API のエラーレスポンスの形式は以下で問題ないですか？

```json
{
  "error": "ALREADY_CLOCKED_IN",
  "message": "本日は既に出勤打刻済みです"
}
```

ステータスコード:
- 409 Conflict: 二重出勤
- 400 Bad Request: 出勤前に退勤、休憩制約違反
- 403 Forbidden: 他人の記録修正
- 404 Not Found: 存在しないレコード

[Answer]問題ない

---

## 6. パッケージ構成

[Question] 既存の Unit 0 のパッケージ構成に倣い、以下で進めてよいですか？

```
com.example.attendance
├── application
│   ├── dto/          ← AttendanceResponse, AttendanceUpdateRequest 等
│   └── service/      ← AttendanceService, AttendanceServiceImpl
├── domain
│   ├── model/        ← AttendanceRecord, BreakRecord, AttendanceRevision, WorkDuration
│   └── repository/   ← AttendanceRecordRepository, BreakRecordRepository, AttendanceRevisionRepository
├── infrastructure
│   └── exception/    ← (既存 GlobalExceptionHandler を拡張)
└── presentation
    └── controller/   ← AttendanceController
```

[Answer]問題ない

---

## まとめ（確定後に記入）

上記すべての [Answer] が埋まったら、確定した設計を `docs/design/` 配下に反映し、`tdd-implementation` に進む。
