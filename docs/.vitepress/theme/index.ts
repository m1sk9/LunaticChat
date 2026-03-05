import DefaultTheme from 'vitepress/theme';
import VersionSwitcher from './VersionSwitcher.vue';
import Layout from './Layout.vue';
import './custom.css';

export default {
  extends: DefaultTheme,
  Layout,
  enhanceApp({ app }) {
    app.component('VersionSwitcher', VersionSwitcher);
  },
};
