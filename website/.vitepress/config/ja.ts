import type { DefaultTheme } from 'vitepress';

export const ja: DefaultTheme.Config = {
  editLink: {
    pattern: 'https://github.com/m1sk9/LunaticChat/edit/main/docs/src/:path',
    text: 'GitHub で編集',
  },
  nav: [
    { link: '/download', text: 'ダウンロード' },
    { link: '/docs/getting-started', text: 'ドキュメント'　}
  ],
  sidebar: {
    '/docs/': [
      {
        link: '/docs/getting-started',
        text: 'はじめる',
      },
      {
        link: '/docs/configuration',
        text: '設定',
      },
      {
        link: '/docs/permissions',
        text: 'パーミッション',
      },
      {
        text: '機能ガイド',
        items: [
          {
            link: '/docs/features/direct-message',
            text: 'ダイレクトメッセージ',
          },
          {
            link: '/docs/features/channel-chat',
            text: 'チャンネルチャット',
          },
          {
            link: '/docs/features/japanese-conversion',
            text: 'ローマ字変換',
          },
          {
            link: '/docs/features/velocity',
            text: 'Velocity 連携',
          },
          {
            link: '/docs/features/message-logging',
            text: 'メッセージログ',
          },
          {
            link: '/docs/features/admin',
            text: '管理者向け機能',
          },
        ],
      },
      {
        text: 'リファレンス',
        items: [
          {
            link: '/docs/reference/commands',
            text: 'コマンド一覧',
          },
          {
            link: '/docs/reference/message-format',
            text: 'メッセージフォーマット',
          },
          {
            link: '/docs/reference/player-settings',
            text: 'プレイヤー設定',
          },
        ],
      },
    ]
  },
};
