<script setup lang="ts">
import DefaultTheme from 'vitepress/theme';
import VersionBanner from './VersionBanner.vue';

const { Layout } = DefaultTheme;
</script>

<template>
  <Layout>
    <template #layout-top>
      <VersionBanner />
    </template>
  </Layout>
</template>
