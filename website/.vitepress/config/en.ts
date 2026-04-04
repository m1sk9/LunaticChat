import type { DefaultTheme } from 'vitepress';

export const en: DefaultTheme.Config = {
  editLink: {
    pattern: 'https://github.com/m1sk9/LunaticChat/edit/main/docs/src/:path',
    text: 'Edit this page on GitHub',
  },
  nav: [
    { link: '/en/player-guide/getting-started', text: 'Player Guide' },
    { link: '/en/admin-guide/getting-started', text: 'Admin Guide' },
    { link: '/download', text: 'Download' },
  ],
  sidebar: {
    '/en/admin-guide/': [
      {
        link: '/en/admin-guide/getting-started',
        text: 'Getting Started',
      },
      {
        link: '/en/admin-guide/configuration',
        text: 'Configuration',
      },
      {
        link: '/en/admin-guide/permissions',
        text: 'Permissions',
      },
      {
        items: [
          {
            link: '/en/admin-guide/channel-chat/introduction',
            text: 'Deployment Guide',
          },
          {
            link: '/en/admin-guide/channel-chat/logs',
            text: 'Logs',
          },
        ],
        text: 'Channel Chat',
      },
      {
        link: '/en/admin-guide/velocity',
        text: 'Velocity Integration',
      },
      {
        link: '/en/admin-guide/cache',
        text: 'Cache System',
      },
      {
        link: '/en/admin-guide/management-data',
        text: 'Data Management',
      },
    ],
    '/en/player-guide/': [
      {
        link: '/en/player-guide/getting-started',
        text: 'Getting Started',
      },
      {
        link: '/en/player-guide/about',
        text: 'About LunaticChat',
      },
      {
        items: [
          {
            link: '/en/player-guide/channel-chat/private-channel',
            text: 'Private Channel',
          },
          {
            link: '/en/player-guide/channel-chat/moderation',
            text: 'Moderation',
          },
        ],
        link: '/en/player-guide/channel-chat/about',
        text: 'Channel Chat',
      },
      {
        link: '/en/player-guide/direct-message',
        text: 'Direct Messages',
      },
      {
        link: '/en/player-guide/japanese-romanization',
        text: 'Romanization Conversion',
      },
      {
        items: [
          {
            link: '/en/player-guide/commands/tell',
            text: '/tell',
          },
          {
            link: '/en/player-guide/commands/reply',
            text: '/reply',
          },
          {
            link: '/en/player-guide/commands/jp',
            text: '/jp',
          },
          {
            link: '/en/player-guide/commands/notice',
            text: '/notice',
          },
          {
            items: [
              {
                link: '/en/player-guide/commands/lc/settings',
                text: '/lc settings',
              },
              {
                link: '/en/player-guide/commands/lc/status',
                text: '/lc status',
              },
              {
                link: '/en/player-guide/commands/lc/channel',
                text: '/lc channel',
              },
            ],
            text: '/lc',
          },
          {
            items: [
              {
                link: '/en/player-guide/commands/lcv/status',
                text: '/lcv status',
              },
            ],
            text: '/lcv',
          },
        ],
        text: 'Commands',
      },
    ],
  },
};
