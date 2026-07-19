import type { DefaultTheme } from 'vitepress';

export const ja: DefaultTheme.Config = {
  editLink: {
    pattern: 'https://github.com/m1sk9/LunaticChat/edit/main/website/src/:path',
    text: 'GitHub で編集',
  },
  nav: [
    { link: '/ja/download', text: 'ダウンロード' },
    { link: '/ja/docs/getting-started', text: 'ドキュメント' },
  ],
  sidebar: {
    '/ja/docs/': [
      {
        link: '/ja/docs/getting-started',
        text: 'はじめる',
      },
      {
        link: '/ja/docs/configuration',
        text: '設定',
      },
      {
        link: '/ja/docs/permissions',
        text: 'パーミッション',
      },
      {
        text: '機能ガイド',
        items: [
          {
            link: '/ja/docs/features/direct-message',
            text: 'ダイレクトメッセージ',
          },
          {
            link: '/ja/docs/features/channel-chat',
            text: 'チャンネルチャット',
          },
          {
            link: '/ja/docs/features/japanese-conversion',
            text: 'ローマ字変換',
          },
          {
            link: '/ja/docs/features/velocity',
            text: 'Velocity 連携',
          },
          {
            link: '/ja/docs/features/message-logging',
            text: 'メッセージログ',
          },
          {
            link: '/ja/docs/features/admin',
            text: '管理者向け機能',
          },
        ],
      },
      {
        text: 'リファレンス',
        items: [
          {
            link: '/ja/docs/reference/commands',
            text: 'コマンド一覧',
          },
          {
            link: '/ja/docs/reference/message-format',
            text: 'メッセージフォーマット',
          },
          {
            link: '/ja/docs/reference/player-settings',
            text: 'プレイヤー設定',
          },
          {
            link: '/ja/docs/reference/compatibility',
            text: 'Paper / Velocity 互換性',
          },
        ],
      },
      {
        text: '開発者向けガイド',
        items: [
          {
            link: '/ja/docs/developers/introduction',
            text: 'はじめに',
          },
          {
            text: '設計/アーキテクチャ',
            link: '/ja/docs/developers/architecture',
            items: [
              {
                link: '/ja/docs/developers/engine',
                text: 'engine - 共通カーネル',
              },
              {
                link: '/ja/docs/developers/platform-paper',
                text: 'platform-paper - Paper / Folia プラグイン本体',
              },
              {
                link: '/ja/docs/developers/platform-velocity',
                text: 'platform-velocity - Velocity プラグイン本体 (プロキシ中継)',
              },
            ],
          },
          {
            link: '/ja/docs/developers/resource',
            text: 'ビルド・リリース・バージョニング',
          },
        ],
      },
    ],
  },
};
