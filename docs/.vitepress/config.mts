import { defineConfig } from 'vitepress';

export default defineConfig({
  title: 'LunaticChat Docs',
  titleTemplate: 'LunaticChat',
  description: 'Next-generation channel chat plugin for Paper/Velocity',
  lang: 'ja-JP',
  cleanUrls: true,
  srcDir: './src',
  outDir: './dist',
  head: [['link', { rel: 'icon', href: '/static/favicon.ico' }]],
  themeConfig: {
    nav: [
      { text: 'Guide', link: '/guide/getting-started' },
      { text: 'Reference', link: '/reference' },
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
      pattern: 'https://github.com/m1sk9/LunaticChat/edit/main/docs/:path',
      text: 'Edit this page on GitHub',
    },
    footer: {
      copyright: 'Copyright © 2026 m1sk9',
    },
    socialLinks: [
      { icon: 'github', link: 'https://github.com/m1sk9/LunaticChat' },
    ],
  },
});
