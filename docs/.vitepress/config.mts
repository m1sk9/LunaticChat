import { defineConfig } from 'vitepress';
import { ja } from './config/ja';
import { en } from './config/en';

export default defineConfig({
  title: 'LunaticChat Docs',
  titleTemplate: 'LunaticChat',
  description: 'Next-generation channel chat plugin for Paper/Velocity',
  cleanUrls: true,
  srcDir: './src',
  outDir: './dist',
  head: [['link', { rel: 'icon', href: '/favicon.ico' }]],
  themeConfig: {
    socialLinks: [
      { icon: 'github', link: 'https://github.com/m1sk9/LunaticChat' },
    ],
  },
  locales: {
    root: {
      label: '日本語',
      lang: 'ja-JP',
      themeConfig: ja,
    },
    en: {
      label: 'English',
      lang: 'en-US',
      link: '/en/',
      themeConfig: en,
    },
  },
});
