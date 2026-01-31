import type { DefaultTheme } from 'vitepress';

export const ja: DefaultTheme.Config = {
  nav: [
    { text: 'ガイド', link: '/guide/getting-started' },
    { text: 'リファレンス', link: '/reference' },
  ],
  sidebar: {
    '/guide/': [
      {
        text: 'はじめる',
        link: '/guide/getting-started',
      },
      {
        text: 'LunaticChat について',
        link: '/guide/about',
      },
      {
        text: 'サーバー管理者向け',
        items: [
          {
            text: 'チャンネルチャット',
            items: [
              {
                text: '展開ガイド',
                link: '/guide/admin/channel-chat/introduction',
              },
              {
                text: 'ログ',
                link: '/guide/admin/channel-chat/logs',
              },
            ],
          },
          {
            text: 'キャッシュシステム',
            link: '/guide/admin/cache',
          },
          {
            text: '設定',
            link: '/guide/admin/configuration',
          },
          {
            text: 'Velocity 連携',
            link: '/guide/admin/velocity',
          },
          {
            text: 'データの管理',
            link: '/guide/admin/management-data',
          },
        ],
      },
      {
        text: 'プレイヤー向け',
        items: [
          {
            text: 'チャンネルチャット',
            link: '/guide/player/channel-chat/about',
            items: [
              {
                text: 'チャットモード',
                link: '/guide/player/channel-chat/chatmode',
              },
              {
                text: 'プライベートチャンネル',
                link: '/guide/player/channel-chat/private-channel',
              },
              {
                text: 'モデレーション',
                link: '/guide/player/channel-chat/moderation',
              },
            ],
          },
          {
            text: 'ダイレクトメッセージ',
            link: '/guide/player/direct-message',
          },
          {
            text: 'ローマ字変換',
            link: '/guide/player/japanese-romanization',
          },
        ],
      },
    ],
    '/reference/': [
      {
        text: 'パーミッション',
        link: '/reference/permissions',
      },
      {
        text: 'コマンド',
        items: [
          {
            text: '/tell',
            link: '/reference/commands/tell',
          },
          {
            text: '/reply',
            link: '/reference/commands/reply',
          },
          {
            text: '/jp',
            link: '/reference/commands/jp',
          },
          {
            text: '/notice',
            link: '/reference/commands/notice',
          },
          {
            text: '/lc',
            items: [
              {
                text: '/lc settings',
                link: '/reference/commands/lc/settings',
              },
              {
                text: '/lc status',
                link: '/reference/commands/lc/status',
              },
              {
                text: '/lc channel',
                link: '/reference/commands/lc/channel',
              },
              {
                text: '/lc chatmode',
                link: '/reference/commands/lc/chatmode',
              },
            ],
          },
        ],
      },
    ],
  },
  editLink: {
    pattern: 'https://github.com/m1sk9/LunaticChat/edit/main/docs/src/:path',
    text: 'GitHub で編集',
  },
  footer: {
    copyright: 'Copyright © 2026 m1sk9',
  },
};
