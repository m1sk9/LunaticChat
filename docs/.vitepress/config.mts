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
          text: '設定',
          link: '/guide/configuration',
        },
        {
          text: 'パーミッション',
          link: '/guide/permissions',
        },
        {
          text: 'ローマ字変換',
          link: '/guide/japanese-romanization',
        },
      ],
      '/reference/': [
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
