# WarforgeCore v1.0.0-BETA

FPS 戦闘プラグイン for PaperMC 1.16〜1.21

## 対応バージョン

- Minecraft 1.16.5 / 1.17.x / 1.18.x / 1.19.x / 1.20.x / 1.21.x
- PacketEvents によるマルチバージョン対応

## 必須依存

- [PaperMC](https://papermc.io/)
- [Vault](https://github.com/MilkBowl/Vault) + 経済プラグイン（EssentialsX 等）
- MySQL 5.7+

## オプション依存

- [WeaponMechanics](https://github.com/WeaponMechanics/WeaponMechanics) — 武器システム  
  プレイヤーが自分の武器を持参して戦う設計。キットシステムなし。
- **[WarforgeAuction](https://github.com/woxlo1/WarforgeAuction)** — オークションシステム  
  WarforgeCore 専用オークションプラグイン。導入するだけで `/auction` が使えるようになります。

> WarforgeCore は武器・オークション機能を自前実装せず、実績ある外部プラグインや  
> 専用サブプラグインに委譲することで安定性と保守性を高めています。

## GitHub Packages の設定

WarforgeCore は GitHub Packages で公開されています。依存として使う場合は以下を設定してください。

**`~/.gradle/gradle.properties`:**
```properties
gpr.user=あなたのGitHubユーザー名
gpr.key=ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**`build.gradle`:**
```gradle
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/woxlo1/WarforgeCore")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key")  ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
dependencies {
    compileOnly 'com.warforge:warforge-core:+'
}
```

## アリーナ作成手順

```
1. /arena wand <アリーナ名> <ゲームタイプ>
   ゲームタイプ: tdm / br / domination / goldrush / heist

2. ワンド (BlazeRod) を受け取る
   - 左クリック: 地点1
   - 右クリック: 地点2

3. チームモード (tdm/heist/domination) の場合:
   - 範囲内に 赤ウール (Red Wool)  → 赤チームスポーン
   - 範囲内に 青ウール (Blue Wool) → 青チームスポーン
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
| `/stats [プレイヤー]` | 統計 |
| `/rank [top]` | ランク |
| `/loadout [save/load/list]` | ロードアウト（WeaponMechanics 連携） |
| `/mission` | デイリーミッション |
| `/spectate <ID>` | 観戦 |
| `/vote <ID>` | マップ投票 |
| `/wf admin` | 管理パネル |
| `/auction` | オークション（**WarforgeAuction 必須**） |

## 多言語対応

`plugins/WarforgeCore/lang/` に言語ファイルを追加  
`config.yml` → `settings.language: ja` で切り替え  
現在対応: `ja`（日本語）/ `en`（English）

## ゲームモード

| モード | 説明 |
|-------|------|
| TDM | チームデスマッチ（30 キル or 5 分） |
| BR | バトルロイヤル（最後の 1 人） |
| Domination | 拠点制圧（スコア先取） |
| GoldRush | 最多ゴールド収集勝利 |
| Heist | ネクサス強奪チーム戦 |

## GoldRush ルール

- **指定数ではなく「最も多く集めた人」が勝利**
- 30 秒ごとに現在の順位を発表
- キル時のゴールドドロップは **60〜75%**（全額ではない）
- 残り時間で全額ドロップに近づく設計

## WeaponMechanics 連携

WarforgeCore は武器の管理を WeaponMechanics に完全委譲しています。  
プレイヤーは自分の WeaponMechanics 武器を持参して試合に参加します。

- WarforgeCore 側の自作銃システム（GunManager 等）は廃止済み
- ロードアウト保存（`/loadout save`）はスロット名のみ記録
- `config.yml` → `weapon-mechanics.enabled: true` で WeaponMechanics の存在を明示

## WarforgeAuction 連携

オークション機能は **WarforgeAuction** プラグインに完全委譲しています。  
[WarforgeAuction](https://github.com/woxlo1/WarforgeAuction) を導入するだけで `/auction` コマンドが使えるようになります。

WarforgeAuction がない環境でも WarforgeCore は正常に動作します。

## ライセンス

MIT License — 詳細は [LICENSE](LICENSE) を参照してください。
