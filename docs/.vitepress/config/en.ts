import type { DefaultTheme } from 'vitepress';

export const en: DefaultTheme.Config = {
  nav: [
    { text: 'Guide', link: '/en/guide/getting-started' },
    { text: 'Reference', link: '/en/reference' },
  ],
  sidebar: {
    '/en/guide/': [
      {
        text: 'Getting Started',
        link: '/en/guide/getting-started',
      },
      {
        text: 'About LunaticChat',
        link: '/en/guide/about',
      },
      {
        text: 'For Server Administrators',
        items: [
          {
            text: 'Cache System',
            link: '/en/guide/admin/cache',
          },
          {
            text: 'Configuration',
            link: '/en/guide/admin/configuration',
          },
          {
            text: 'Velocity Integration',
            link: '/en/guide/admin/velocity',
          },
        ],
      },
      {
        text: 'For Players',
        items: [
          {
            text: 'Channel Chat',
            link: '/en/guide/player/channel-chat',
          },
          {
            text: 'Direct Messages',
            link: '/en/guide/player/direct-message',
          },
          {
            text: 'Romanization Conversion',
            link: '/en/guide/player/japanese-romanization',
          },
        ],
      },
      {
        text: 'Patch Notes',
        items: [
          {
            text: 'Plugin',
            link: '/en/guide/patch-notes/plugin',
          },
          {
            text: 'Cache File',
            link: '/en/guide/patch-notes/cache-file',
          },
        ],
      },
    ],
    '/en/reference/': [
      {
        text: 'Permissions',
        link: '/en/reference/permissions',
      },
      {
        text: 'Commands',
        items: [
          {
            text: '/tell',
            link: '/en/reference/commands/tell',
          },
          {
            text: '/reply',
            link: '/en/reference/commands/reply',
          },
          {
            text: '/jp',
            link: '/en/reference/commands/jp',
          },
          {
            text: '/notice',
            link: '/en/reference/commands/notice',
          },
          {
            text: '/lc',
            items: [
              {
                text: '/lc settings',
                link: '/en/reference/commands/lc/settings',
              },
              {
                text: '/lc status',
                link: '/en/reference/commands/lc/status',
              },
            ],
          },
        ],
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
