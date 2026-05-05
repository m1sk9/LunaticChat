import type { DefaultTheme } from 'vitepress';

export const en: DefaultTheme.Config = {
  editLink: {
    pattern: 'https://github.com/m1sk9/LunaticChat/edit/main/website/src/:path',
    text: 'Edit this page on GitHub',
  },
  nav: [
    { link: '/en/download', text: 'Download' },
    { link: '/en/docs/getting-started', text: 'Documentation' },
  ],
  sidebar: {
    '/en/docs/': [
      {
        link: '/en/docs/getting-started',
        text: 'Getting Started',
      },
      {
        link: '/en/docs/configuration',
        text: 'Configuration',
      },
      {
        link: '/en/docs/permissions',
        text: 'Permissions',
      },
      {
        text: 'Feature Guides',
        items: [
          {
            link: '/en/docs/features/direct-message',
            text: 'Direct Messages',
          },
          {
            link: '/en/docs/features/channel-chat',
            text: 'Channel Chat',
          },
          {
            link: '/en/docs/features/japanese-conversion',
            text: 'Romaji Conversion',
          },
          {
            link: '/en/docs/features/velocity',
            text: 'Velocity Integration',
          },
          {
            link: '/en/docs/features/message-logging',
            text: 'Message Logging',
          },
          {
            link: '/en/docs/features/admin',
            text: 'Admin Features',
          },
        ],
      },
      {
        text: 'Reference',
        items: [
          {
            link: '/en/docs/reference/commands',
            text: 'Commands',
          },
          {
            link: '/en/docs/reference/message-format',
            text: 'Message Format',
          },
          {
            link: '/en/docs/reference/player-settings',
            text: 'Player Settings',
          },
          {
            link: '/en/docs/reference/compatibility',
            text: 'Paper / Velocity Compatibility',
          },
        ],
      },
    ],
  },
};
