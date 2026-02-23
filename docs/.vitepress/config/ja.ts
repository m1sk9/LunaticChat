import type { DefaultTheme } from 'vitepress';

export const ja: DefaultTheme.Config = {
  editLink: {
    pattern: 'https://github.com/m1sk9/LunaticChat/edit/main/docs/src/:path',
    text: 'GitHub で編集',
  },
  footer: {
    copyright: 'Copyright © 2026 m1sk9',
  },
  nav: [
    { link: '/player-guide/getting-started', text: 'プレイヤーガイド' },
    { link: '/admin-guide/getting-started', text: '管理者ガイド' },
  ],
  sidebar: {
    '/admin-guide/': [
      {
        link: '/admin-guide/getting-started',
        text: 'はじめる',
      },
      {
        link: '/admin-guide/configuration',
        text: '設定',
      },
      {
        link: '/admin-guide/permissions',
        text: 'パーミッション',
      },
      {
        items: [
          {
            link: '/admin-guide/channel-chat/introduction',
            text: '展開ガイド',
          },
          {
            link: '/admin-guide/channel-chat/logs',
            text: 'ログ',
          },
        ],
        text: 'チャンネルチャット',
      },
      {
        link: '/admin-guide/velocity',
        text: 'Velocity 連携',
      },
      {
        link: '/admin-guide/cache',
        text: 'キャッシュシステム',
      },
      {
        link: '/admin-guide/management-data',
        text: 'データの管理',
      },
    ],
    '/player-guide/': [
      {
        link: '/player-guide/getting-started',
        text: 'はじめる',
      },
      {
        link: '/player-guide/about',
        text: 'LunaticChat について',
      },
      {
        items: [
          {
            link: '/player-guide/channel-chat/chatmode',
            text: 'チャットモード',
          },
          {
            link: '/player-guide/channel-chat/private-channel',
            text: 'プライベートチャンネル',
          },
          {
            link: '/player-guide/channel-chat/moderation',
            text: 'モデレーション',
          },
        ],
        link: '/player-guide/channel-chat/about',
        text: 'チャンネルチャット',
      },
      {
        link: '/player-guide/direct-message',
        text: 'ダイレクトメッセージ',
      },
      {
        link: '/player-guide/japanese-romanization',
        text: 'ローマ字変換',
      },
      {
        items: [
          {
            link: '/player-guide/commands/tell',
            text: '/tell',
          },
          {
            link: '/player-guide/commands/reply',
            text: '/reply',
          },
          {
            items: [
              {
                link: '/player-guide/commands/lc/settings',
                text: '/lc settings',
              },
              {
                link: '/player-guide/commands/lc/status',
                text: '/lc status',
              },
              {
                link: '/player-guide/commands/lc/channel',
                text: '/lc channel',
              },
              {
                link: '/player-guide/commands/lc/chatmode',
                text: '/lc chatmode',
              },
            ],
            text: '/lc',
          },
          {
            items: [
              {
                link: '/player-guide/commands/lcv/status',
                text: '/lcv status',
              },
            ],
            text: '/lcv',
          },
        ],
        text: 'コマンド',
      },
    ],
  },
};
