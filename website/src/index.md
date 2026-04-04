---
# https://vitepress.dev/reference/default-theme-home-page
layout: home

hero:
  name: 'LunaticChat'
  tagline: A next-generation chat plugin for Paper, Folia and Velocity.
  actions:
    - theme: brand
      text: ダウンロード
      link: /download
    - theme: brand
      text: ドキュメント
      link: /docs/getting-started
    - theme: alt
      text: GitHub
      link: https://github.com/m1sk9/LunaticChat

features:
  - title: チャンネルチャット
    details: チャンネルを作成・管理し，特定のプレイヤー間でグループチャットが可能．プライベートチャンネルやモデレーション機能も搭載
    icon: ☎️
  - title: ダイレクトメッセージ
    details: /tell や /msg コマンドで 1対1 のチャットが可能．/reply で直前の相手に素早く返信
    icon: ✉️
  - title: ローマ字変換
    details: ローマ字で入力したメッセージを自動的に日本語に変換．キャッシュにより高速に動作
    icon: 🌍
  - title: Velocity サーバー間連携
    details: Velocity プロキシを経由して複数サーバー間でグローバルチャットをリレー．どのサーバーにいても会話に参加可能
    icon: 🔗
  - title: 柔軟な設定
    details: YAML ベースの設定ファイルで機能の有効/無効を切り替え．サーバーの用途に合わせてカスタマイズ可能
    icon: ⚙️
  - title: 最新バージョン対応
    details: 外部プラグインへの依存を最小限に抑え，常に最新の Minecraft バージョンに対応
    icon: ⛏️
---

<hr class="home-divider" />

<!-- Section 1: チャンネルチャット (text left, image right) -->
<div class="feature-showcase">
  <div class="feature-showcase-text">
    <h2>チャンネルチャットで会話を整理</h2>
    <p>
      サーバー内にチャンネルを作成して，トピックやグループごとに会話を分離できます．
      全体チャットに流れることなく，必要なメンバーだけでコミュニケーションが可能です．
    </p>
    <ul>
      <li>パスワード付きのプライベートチャンネルを作成</li>
      <li>チャンネルごとのモデレーション機能（キック・ミュート・BAN）</li>
      <li>チャンネル参加・退出の通知をカスタマイズ</li>
    </ul>
  </div>
  <div class="feature-showcase-image">
    <img src="./assets/features/channel-chat.png" alt="LunaticChat のチャンネルチャット機能" />
  </div>
</div>

<hr class="home-divider" />

<!-- Section 2: ダイレクトメッセージ (image left, text right) -->
<div class="feature-showcase reverse">
  <div class="feature-showcase-text">
    <h2>ダイレクトメッセージ & クイック返信</h2>
    <p>
      プレイヤー間で手軽に 1対1 のプライベートチャットができます．
      <code>/reply</code> コマンドで直前の相手にすぐ返信でき，テンポの良いやり取りを実現します．
    </p>
    <ul>
      <li><code>/tell</code> / <code>/msg</code> でダイレクトメッセージを送信</li>
      <li><code>/reply</code> で直前の送信者に即座に返信</li>
      <li>メッセージは送信者と受信者だけに表示</li>
    </ul>
  </div>
  <div class="feature-showcase-image">
    <img src="./assets/features/dm.png" alt="LunaticChat のダイレクトメッセージ機能" />
  </div>
</div>

<hr class="home-divider" />

<!-- Section 3: ローマ字変換 (text left, image right) -->
<div class="feature-showcase">
  <div class="feature-showcase-text">
    <h2>ローマ字入力を自動で日本語に</h2>
    <p>
      日本語入力に対応していない環境でも，ローマ字で入力するだけで自動的に日本語に変換されます．
      Google IME API を活用し，自然な変換結果を提供します．
    </p>
    <ul>
      <li>チャット入力時にリアルタイムでローマ字→日本語変換</li>
      <li>変換結果のキャッシュで高速動作を実現</li>
      <li>プレイヤーごとに変換機能のオン/オフを切り替え可能</li>
    </ul>
  </div>
  <div class="feature-showcase-image">
    <img src="./assets/features/romaji.png" alt="LunaticChat のローマ字変換機能" />
  </div>
</div>

<hr class="home-divider" />

<!-- Section 4: Velocity 連携 (image left, text right) -->
<div class="feature-showcase reverse">
  <div class="feature-showcase-text">
    <h2>Velocity でサーバー間連携</h2>
    <p>
      Velocity プロキシと連携して，複数の Paper/Folia サーバー間でグローバルチャットをリレーします．
      プレイヤーはどのサーバーにいても，同じチャット空間で会話に参加できます．
    </p>
    <ul>
      <li>通常チャットを全サーバーにリアルタイムでリレー</li>
      <li>独自のプラグインメッセージングプロトコルで高速な通信</li>
      <li>プロトコルバージョニングによる後方互換性の保証</li>
    </ul>
  </div>
  <div class="feature-showcase-image">
    <img src="./assets/features/cross-chat.png" alt="LunaticChat のクロスチャット機能" />
  </div>
</div>

<hr class="home-divider" />

<!-- Section 5: プラットフォーム -->
<div class="platform-section">
  <h2>マルチプラットフォーム対応</h2>
  <p class="section-desc">サーバーの構成に合わせて柔軟に導入できます</p>
  <div class="platform-cards">
    <a class="platform-card" href="https://papermc.io/software/paper/" target="_blank" rel="noopener">
      <div class="platform-icon">
        <img src="./assets/brand/paper.svg" alt="Paper" width="40" height="40" />
      </div>
      <p class="name">Paper</p>
      <p class="desc">最も広く利用されている Minecraft サーバー実装．DM，チャンネルチャット，ローマ字変換など全機能をフルサポート．Bukkit/Spigot プラグインとの互換性も維持</p>
    </a>
    <a class="platform-card" href="https://papermc.io/software/folia" target="_blank" rel="noopener">
      <div class="platform-icon">
        <img src="./assets/brand/folia.svg" alt="Folia" width="40" height="40" />
      </div>
      <p class="name">Folia</p>
      <p class="desc">PaperMC が開発するマルチスレッド対応のサーバー実装．リージョン分割による並列処理で，大規模サーバーでも安定したチャット体験を提供</p>
    </a>
    <a class="platform-card" href="https://papermc.io/software/velocity" target="_blank" rel="noopener">
      <div class="platform-icon">
        <img src="./assets/brand/velocity.svg" alt="Velocity" width="40" height="40" />
      </div>
      <p class="name">Velocity</p>
      <p class="desc">高性能なプロキシサーバー．LunaticChat の Velocity プラグインを導入することで，複数の Paper/Folia サーバー間でグローバルチャットのリレーを実現</p>
    </a>
  </div>
</div>
