# LunaticChat について

LunaticChat は Minecraft サーバソフトウェアである Paper / Velocity 向けのチャットプラグインです．

2013年，[ucchyocean](https://github.com/ucchyocean) 氏が公開した [LunaChat](https://github.com/ucchyocean/LunaChat) の後継，そして最新版で動くように一から書き直したプラグインとして開発しています．

現在 LunaChat は開発が終了しており，最新の Minecraft バージョンには対応していません．多くのサーバー管理者が LunaChat を Fork していますが， LunaticChat はその代替として，LunaChat の機能を引き継ぎつつ，最新の Minecraft バージョンで動作するよう設計されています．

## 主な特徴

- **軽量で高速**: LunaticChat は高いパフォーマンスを維持しつつ，サーバーへの負荷を最小限に抑えるよう設計されています．
- **1on1 ダイレクトメッセージ機能**: /tell や /msg コマンドで，1対1のチャットが可能です．
- **クイック返信**: /reply で，直前にメッセージを送信した相手に素早く返信可能です．
- **かな・ローマ字変換**: ローマ字で入力したメッセージを，自動的に日本語に変換します．
- **チャンネルチャット機能**: 複数のチャットチャンネルを作成し，特定のチャンネルでのみメッセージを送信できる環境を実現します．
- **CoreProtect への対応**: LunaticChat のチャットログは CoreProtect と互換性があります．
- **キャッシュによる高速動作**: ローマ字から日本語の変換はキャッシュを使用し，従来の LunaChat よりも高速に動作します．
- **最新版の対応**: 他プラグインとの依存を完全に排除し，常に最新の Minecraft バージョンに対応します．

## 比較

| | LunaticChat | LunaChat                               |
| ----- |-------------|----------------------------------------|
| 開発状況 | 継続的に開発中     | 開発終了                                   |
| 対応プラットフォーム | Paper / Velocity | Bukkit / Spigot / BungeeCord           |
| サポートバージョン | 1.21.x ~    | 1.16.x まで                              |
| 使用言語 | Kotlin      | Java                                   |
| 依存プラグイン | なし          | EssentialsX など                         |
| キャッシュ | あり          | なし                                     |
| ドキュメント | あり          | なし                                     |
| ライセンス | GNU General Public License v3.0 | GNU Lesser General Public License v3.0 |

## FAQ

### LunaticChat は無料で使えますか？

オープンソースソフトウェアとして，LunaticChat は無料で使用，修正，配布できます．

### LunaticChat はどの Minecraft バージョンに対応していますか？

LunaticChat は Minecraft 1.21.x 以降のバージョンに対応しています．

### LunaticChat は他のチャットプラグインと併用できますか？

LunaticChat は他のチャットプラグインと併用できますが，競合を避けるために，チャット関連の機能が重複しないように注意してください．

### LunaticChat のサポートはどこで受けられますか？

LunaticChat のサポートは，[公式の GitHub Discussion](https://github.com/m1sk9/LunaticChat/discussions) で受けられます．

### LunaticChat のソースコードはどこで入手できますか？

LunaticChat のソースコードは，[GitHub](https://github.com/m1sk9/LunaticChat) で公開されています．

### Spigot / BungeeCord 版の LunaticChat はありますか？

ありません．対応する予定もありません．Paper / Velocity でご利用ください．

### LunaticChat はどのようなライセンスで配布されていますか？

LunaticChat は GNU General Public License v3.0 (GPLv3) の下で配布されています．
