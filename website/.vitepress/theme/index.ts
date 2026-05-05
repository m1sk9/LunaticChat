import type { Theme } from 'vitepress';
import DefaultTheme from 'vitepress/theme';
import CompatibilityMatrix from './components/CompatibilityMatrix.vue';
import DownloadCard from './components/DownloadCard.vue';
import './custom.css';

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('DownloadCard', DownloadCard);
    app.component('CompatibilityMatrix', CompatibilityMatrix);
  },
} satisfies Theme;
