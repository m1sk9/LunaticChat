import type { DefaultTheme } from 'vitepress';

export const en: DefaultTheme.Config = {
  nav: [
    { text: 'Player Guide', link: '/en/player-guide/getting-started' },
    { text: 'Admin Guide', link: '/en/admin-guide/getting-started' },
  ],
  sidebar: {
    '/en/player-guide/': [
      {
        text: 'Getting Started',
        link: '/en/player-guide/getting-started',
      },
      {
        text: 'About LunaticChat',
        link: '/en/player-guide/about',
      },
      {
        text: 'Channel Chat',
        link: '/en/player-guide/channel-chat/about',
        items: [
          {
            text: 'Chat Mode',
            link: '/en/player-guide/channel-chat/chatmode',
          },
          {
            text: 'Private Channel',
            link: '/en/player-guide/channel-chat/private-channel',
          },
          {
            text: 'Moderation',
            link: '/en/player-guide/channel-chat/moderation',
          },
        ],
      },
      {
        text: 'Direct Messages',
        link: '/en/player-guide/direct-message',
      },
      {
        text: 'Romanization Conversion',
        link: '/en/player-guide/japanese-romanization',
      },
      {
        text: 'Commands',
        items: [
          {
            text: '/tell',
            link: '/en/player-guide/commands/tell',
          },
          {
            text: '/reply',
            link: '/en/player-guide/commands/reply',
          },
          {
            text: '/jp',
            link: '/en/player-guide/commands/jp',
          },
          {
            text: '/notice',
            link: '/en/player-guide/commands/notice',
          },
          {
            text: '/lc',
            items: [
              {
                text: '/lc settings',
                link: '/en/player-guide/commands/lc/settings',
              },
              {
                text: '/lc status',
                link: '/en/player-guide/commands/lc/status',
              },
              {
                text: '/lc channel',
                link: '/en/player-guide/commands/lc/channel',
              },
              {
                text: '/lc chatmode',
                link: '/en/player-guide/commands/lc/chatmode',
              },
            ],
          },
          {
            text: '/lcv',
            items: [
              {
                text: '/lcv status',
                link: '/en/player-guide/commands/lcv/status',
              },
            ],
          },
        ],
      },
    ],
    '/en/admin-guide/': [
      {
        text: 'Getting Started',
        link: '/en/admin-guide/getting-started',
      },
      {
        text: 'Configuration',
        link: '/en/admin-guide/configuration',
      },
      {
        text: 'Permissions',
        link: '/en/admin-guide/permissions',
      },
      {
        text: 'Channel Chat',
        items: [
          {
            text: 'Deployment Guide',
            link: '/en/admin-guide/channel-chat/introduction',
          },
          {
            text: 'Logs',
            link: '/en/admin-guide/channel-chat/logs',
          },
        ],
      },
      {
        text: 'Velocity Integration',
        link: '/en/admin-guide/velocity',
      },
      {
        text: 'Cache System',
        link: '/en/admin-guide/cache',
      },
      {
        text: 'Data Management',
        link: '/en/admin-guide/management-data',
      },
    ],
  },
  editLink: {
    pattern: 'https://github.com/m1sk9/LunaticChat/edit/main/docs/src/:path',
    text: 'Edit this page on GitHub',
  },
  footer: {
    copyright: 'Copyright © 2026 m1sk9',
  },
};
