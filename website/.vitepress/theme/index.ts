import { type Theme, useData } from 'vitepress';
import DefaultTheme from 'vitepress/theme';
import CopyOrDownloadAsMarkdownButtons from 'vitepress-plugin-llms/vitepress-components/CopyOrDownloadAsMarkdownButtons.vue';
import { h } from 'vue';
import CompatibilityMatrix from './components/CompatibilityMatrix.vue';
import DownloadCard from './components/DownloadCard.vue';
import './custom.css';

export default {
  extends: DefaultTheme,
  Layout: () => {
    const { lang } = useData();
    return h(DefaultTheme.Layout, null, {
      // The Japanese pages are excluded from the llms plugin, so they have no .md to copy.
      'doc-before': () =>
        lang.value.startsWith('ja') ? null : h(CopyOrDownloadAsMarkdownButtons),
    });
  },
  enhanceApp({ app }) {
    app.component('DownloadCard', DownloadCard);
    app.component('CompatibilityMatrix', CompatibilityMatrix);
  },
} satisfies Theme;
