import DefaultTheme from 'vitepress/theme';
import Layout from './Layout.vue';
import VersionSwitcher from './VersionSwitcher.vue';
import './custom.css';

export default {
  extends: DefaultTheme,
  Layout,
  enhanceApp({ app }) {
    app.component('VersionSwitcher', VersionSwitcher);
  },
};
