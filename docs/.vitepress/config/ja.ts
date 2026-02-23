import type { DefaultTheme } from 'vitepress';

export const ja: DefaultTheme.Config = {
  nav: [
    { text: 'プレイヤーガイド', link: '/player-guide/getting-started' },
    { text: '管理者ガイド', link: '/admin-guide/getting-started' },
  ],
  sidebar: {
    '/player-guide/': [
      {
        text: 'はじめる',
        link: '/player-guide/getting-started',
      },
      {
        text: 'LunaticChat について',
        link: '/player-guide/about',
      },
      {
        text: 'チャンネルチャット',
        link: '/player-guide/channel-chat/about',
        items: [
          {
            text: 'チャットモード',
            link: '/player-guide/channel-chat/chatmode',
          },
          {
            text: 'プライベートチャンネル',
            link: '/player-guide/channel-chat/private-channel',
          },
          {
            text: 'モデレーション',
            link: '/player-guide/channel-chat/moderation',
          },
        ],
      },
      {
        text: 'ダイレクトメッセージ',
        link: '/player-guide/direct-message',
      },
      {
        text: 'ローマ字変換',
        link: '/player-guide/japanese-romanization',
      },
      {
        text: 'コマンド',
        items: [
          {
            text: '/tell',
            link: '/player-guide/commands/tell',
          },
          {
            text: '/reply',
            link: '/player-guide/commands/reply',
          },
          {
            text: '/lc',
            items: [
              {
                text: '/lc settings',
                link: '/player-guide/commands/lc/settings',
              },
              {
                text: '/lc status',
                link: '/player-guide/commands/lc/status',
              },
              {
                text: '/lc channel',
                link: '/player-guide/commands/lc/channel',
              },
              {
                text: '/lc chatmode',
                link: '/player-guide/commands/lc/chatmode',
              },
            ],
          },
          {
            text: '/lcv',
            items: [
              {
                text: '/lcv status',
                link: '/player-guide/commands/lcv/status',
              },
            ],
          },
        ],
      },
    ],
    '/admin-guide/': [
      {
        text: 'はじめる',
        link: '/admin-guide/getting-started',
      },
      {
        text: '設定',
        link: '/admin-guide/configuration',
      },
      {
        text: 'パーミッション',
        link: '/admin-guide/permissions',
      },
      {
        text: 'チャンネルチャット',
        items: [
          {
            text: '展開ガイド',
            link: '/admin-guide/channel-chat/introduction',
          },
          {
            text: 'ログ',
            link: '/admin-guide/channel-chat/logs',
          },
        ],
      },
      {
        text: 'Velocity 連携',
        link: '/admin-guide/velocity',
      },
      {
        text: 'キャッシュシステム',
        link: '/admin-guide/cache',
      },
      {
        text: 'データの管理',
        link: '/admin-guide/management-data',
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
