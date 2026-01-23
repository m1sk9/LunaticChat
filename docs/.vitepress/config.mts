import { defineConfig } from 'vitepress';

export default defineConfig({
  title: 'LunaticChat Docs',
  titleTemplate: 'LunaticChat',
  description: 'Next-generation channel chat plugin for Paper/Velocity',
  cleanUrls: true,
  srcDir: './src',
  outDir: './dist',
  head: [['link', { rel: 'icon', href: '/static/favicon.ico' }]],
  themeConfig: {
    socialLinks: [
      { icon: 'github', link: 'https://github.com/m1sk9/LunaticChat' },
    ],
  },
  locales: {
    root: {
      label: '日本語',
      lang: 'ja-JP',
      themeConfig: {
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
              ],
            },
            {
              text: 'プレイヤー向け',
              items: [
                {
                  text: 'チャンネルチャット',
                  link: '/guide/player/channel-chat',
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
            {
              text: 'パッチノート',
              items: [
                {
                  text: 'プラグイン',
                  link: '/guide/patch-notes/plugin',
                },
                {
                  text: 'キャッシュファイル',
                  link: '/guide/patch-notes/cache-file',
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
      },
    },
    en: {
      label: 'English',
      lang: 'en-US',
      link: '/en/',
      themeConfig: {
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
      },
    },
  },
});
