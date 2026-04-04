import { execSync } from 'node:child_process';
import { defineConfig } from 'vitepress';
import { en } from './config/en';
import { ja } from './config/ja';

const gitRoot = execSync('git rev-parse --show-toplevel').toString().trim();
const commitHash = execSync(`git log -1 --format=%H -- ${gitRoot}/docs/`)
  .toString()
  .trim()
  .slice(0, 7);

export default defineConfig({
  cleanUrls: true,
  description: 'Next-generation channel chat plugin for Paper/Velocity',
  head: [['link', { href: '/favicon.ico', rel: 'icon' }]],
  locales: {
    en: {
      label: 'English',
      lang: 'en-US',
      link: '/en/',
      themeConfig: {
        ...en,
        footer: {
          copyright: 'Copyright © 2026 m1sk9',
          message: `<a href="https://github.com/m1sk9/LunaticChat/commit/${commitHash}">LunaticChat/docs@${commitHash}</a>`,
        },
      },
    },
    root: {
      label: '日本語',
      lang: 'ja-JP',
      themeConfig: {
        ...ja,
        footer: {
          copyright: 'Copyright © 2026 m1sk9',
          message: `<a href="https://github.com/m1sk9/LunaticChat/commit/${commitHash}">LunaticChat/docs@${commitHash}</a>`,
        },
      },
    },
  },
  outDir: './dist',
  srcDir: 'src',
  themeConfig: {
    socialLinks: [
      { icon: 'github', link: 'https://github.com/m1sk9/LunaticChat' },
    ],
  },
  title: 'LunaticChat Docs',
  titleTemplate: 'LunaticChat',
});
