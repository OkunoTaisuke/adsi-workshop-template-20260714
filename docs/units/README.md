# Unit of Work 分割

## 依存図

```
Phase A: unit_0_foundation（共通基盤）
              │
              ├──────────────────────┐
              ▼                      ▼
Phase B: unit_1_attendance      unit_2_leave
         （勤怠打刻）            （休暇申請）
              │                      │
              └──────────┬───────────┘
                         ▼
Phase C:        unit_3_report
              （月次レポート）
```

## Phase 一覧

| Phase | Unit | 担当 | 依存 | 並列可 |
|-------|------|------|------|--------|
| A | unit_0_foundation | 共同 | なし | — |
| B | unit_1_attendance | 担当者1 | unit_0 | unit_2 と並列 |
| B | unit_2_leave | 担当者2 | unit_0 | unit_1 と並列 |
| C | unit_3_report | どちらか | unit_1 + unit_2 | — |

## 分割方針

- **unit_0**: プロジェクト雛形 + Flyway + Employee Entity + 認証 + テスト基盤。全 Unit の前提。
- **unit_1 / unit_2**: ドメイン境界で分離。Employee Entity への FK のみが接点で、互いに依存しない。
- **unit_3**: 勤怠データと社員データを横断集計するため、unit_1 完了後に実装。

## 実装順序

1. **Phase A** を2人で協力して完成させる（プロジェクト初期設定・テスト実行確認まで）
2. **Phase B** で分岐し、各自の Unit を TDD で独立実装
3. **Phase C** で合流、レポート機能を実装
4. **統合テスト** を実施して完了
