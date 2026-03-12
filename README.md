# WarforgeCore v1.0.0-BETA

FPS戦闘プラグイン for PaperMC 1.19〜1.21

## 対応バージョン
- Minecraft 1.19.x / 1.20.x / 1.21.x
- PacketEventsによるマルチバージョン対応

## 必須依存
- [PaperMC](https://papermc.io/)
- [Vault](https://github.com/MilkBowl/Vault) + 経済プラグイン（EssentialsX等）
- MySQL 5.7+

## オプション依存
- [WeaponMechanics](https://github.com/WeaponMechanics/WeaponMechanics) - より本格的な銃システム  
  `config.yml` → `weapon-mechanics.enabled: true` で有効化

## アリーナ作成手順

```
1. /arena wand <アリーナ名> <ゲームタイプ>
   ゲームタイプ: tdm / br / domination / goldrush / heist

2. ワンド(BlazeRod)を受け取る
   - 左クリック: 地点1
   - 右クリック: 地点2

3. チームモード(tdm/heist/domination)の場合:
   - 範囲内に 赤ウール(Red Wool) → 赤チームスポーン
   - 範囲内に 青ウール(Blue Wool) → 青チームスポーン
   - 両方検出されると自動的にアリーナ作成完了！

4. /arena setlobby <ID>   ← ロビースポーン設定
5. /arena info <ID>       ← 設定確認
6. /arena enable <ID>     ← 有効化
```

## コマンド一覧
| コマンド | 説明 |
|---------|------|
| `/join [ID]` | 試合参加 |
| `/leave` | 退出 |
| `/shop` | 武器ショップ |
| `/auction` | オークション |
| `/stats [プレイヤー]` | 統計 |
| `/rank [top]` | ランク |
| `/loadout [save/load/list]` | ロードアウト |
| `/mission` | デイリーミッション |
| `/spectate <ID>` | 観戦 |
| `/vote <ID>` | マップ投票 |
| `/wf admin` | 管理パネル |

## 多言語対応
`plugins/WarforgeCore/lang/` に言語ファイルを追加
`config.yml` → `settings.language: en` で切り替え
現在対応: `ja`(日本語) / `en`(English)

## ゲームモード
| モード | 説明 |
|-------|------|
| TDM | チームデスマッチ(30キル or 5分) |
| BR | バトルロイヤル(最後の1人) |
| Domination | 拠点制圧(スコア先取) |
| GoldRush | 最多ゴールド収集勝利 |
| Heist | ネクサス強奪チーム戦 |

## GoldRush ルール
- **指定数ではなく「最も多く集めた人」が勝利**
- 30秒ごとに現在の順位を発表
- キル時のゴールドドロップは**60〜75%**（全額ではない）
- 残り時間で全額ドロップに近づく設計

## WeaponMechanics連携
既存の自作銃システムを無効化し、WeaponMechanicsの武器を使用可能。
各プレイヤーが自分の武器を持参して戦う設計（キットシステムなし）。
