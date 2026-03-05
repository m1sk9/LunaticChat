import { defineVersionedConfig } from '@viteplus/versions';
import { en } from './config/en';
import { ja } from './config/ja';

export default defineVersionedConfig({
  cleanUrls: true,
  description: 'Next-generation channel chat plugin for Paper/Velocity',
  head: [['link', { href: '/favicon.ico', rel: 'icon' }]],
  locales: {
    en: {
      label: 'English',
      lang: 'en-US',
      link: '/en/',
      themeConfig: en,
    },
    root: {
      label: '日本語',
      lang: 'ja-JP',
      themeConfig: ja,
    },
  },
  outDir: './dist',
  themeConfig: {
    socialLinks: [
      { icon: 'github', link: 'https://github.com/m1sk9/LunaticChat' },
    ],
  },
  title: 'LunaticChat Docs',
  titleTemplate: 'LunaticChat',
  versionsConfig: {
    current: 'v0 (1.21.x) - Latest',
    sources: 'src',
    archive: 'archive',
    versionSwitcher: false,
  },
  vite: {
    publicDir: '.vitepress/public',
  },
});
