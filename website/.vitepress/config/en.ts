import type { DefaultTheme } from 'vitepress';

export const en: DefaultTheme.Config = {
  editLink: {
    pattern: 'https://github.com/m1sk9/LunaticChat/edit/main/website/src/:path',
    text: 'Edit this page on GitHub',
  },
  nav: [
    { link: '/download', text: 'Download' },
    { link: '/docs/getting-started', text: 'Documentation' },
  ],
  sidebar: {
    '/docs/': [
      {
        link: '/docs/getting-started',
        text: 'Getting Started',
      },
      {
        link: '/docs/configuration',
        text: 'Configuration',
      },
      {
        link: '/docs/permissions',
        text: 'Permissions',
      },
      {
        text: 'Feature Guides',
        items: [
          {
            link: '/docs/features/direct-message',
            text: 'Direct Messages',
          },
          {
            link: '/docs/features/channel-chat',
            text: 'Channel Chat',
          },
          {
            link: '/docs/features/japanese-conversion',
            text: 'Romaji Conversion',
          },
          {
            link: '/docs/features/velocity',
            text: 'Velocity Integration',
          },
          {
            link: '/docs/features/message-logging',
            text: 'Message Logging',
          },
          {
            link: '/docs/features/admin',
            text: 'Admin Features',
          },
        ],
      },
      {
        text: 'Reference',
        items: [
          {
            link: '/docs/reference/commands',
            text: 'Commands',
          },
          {
            link: '/docs/reference/message-format',
            text: 'Message Format',
          },
          {
            link: '/docs/reference/player-settings',
            text: 'Player Settings',
          },
          {
            link: '/docs/reference/compatibility',
            text: 'Paper / Velocity Compatibility',
          },
        ],
      },
    ],
  },
};
