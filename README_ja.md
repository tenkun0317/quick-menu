[English](README.md)

# Quick Menu (Renewed)

Minecraft用 Fabric MOD。繰り返し実行するコマンドやキーバインドをボタンに登録し、メニューから素早く実行できる。

**オリジナル (ImCodist) の Fork を、`owo-lib` 等の重い依存なしで再構築したもの。**

---

## 主な機能

| 機能 | 説明 |
|------|------|
| **フォルダ navigation** | ボタンをフォルダ分けて整理可能 |
| **パンくずリスト** | フォルダ階層を視覚的に表示 |
| **検索 (Fキー)** | ボタン名を検索して瞬時に実行 |
| **編集モード (Eキー)** | ボタンの追加・編集・削除 |
| **クイック並べ替え** | 編集モードで Ctrl+クリック |
| **クイック削除** | 編集モードで Shift+クリック |
| **離して実行** | キーを離した瞬間にアクション実行 |

---

## 使い方

1. **メニューを開く**: `G` (デフォルト)
2. **編集モード**: `E` キー または 鉛筆アイコン
3. **検索**: `F` キー または 虫眼鏡アイコン
4. **フォルダ移動**: フォルダアイコンをクリック
5. **戻る**: パンくずリストの `Root` をクリック

---

## 設定項目

- 1行あたりのボタン数
- 表示行数
- キーを離した時に閉じる
- アクション実行後に閉じる
- 編集アイコン非表示

---

## 技術仕様

- **依存**: Fabric Loader, Fabric API
- **推奨**: YACL, ModMenu
- **言語**: Kotlin
- **データ保存**: JSON (Global)
- **ライセンス**: GPL-3.0

---

## リンク

- [Modrinth](https://modrinth.com/mod/quick-menu-renewed)
- [Repository](https://github.com/tenkun0317/quick-menu)
- [Report Issues](https://github.com/tenkun0317/quick-menu/issues)
- [TODO](todo.md)
