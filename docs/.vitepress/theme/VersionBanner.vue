<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { useRoute, inBrowser } from 'vitepress';

const route = useRoute();
const bannerRef = ref<HTMLElement | null>(null);

const isNonDefaultVersion = computed(() => {
  const path = route.path;
  return /^\/(en\/)?v\d+\//.test(path) || /^\/(en\/)?v\d+$/.test(path);
});

const versionLabel = computed(() => {
  const match = route.path.match(/\/v(\d+)/);
  return match ? `v${match[1]}` : '';
});

const isJapanese = computed(() => {
  return !route.path.startsWith('/en/');
});

function updateLayoutTopHeight() {
  if (!inBrowser || !bannerRef.value) return;
  const height = bannerRef.value.getBoundingClientRect().height;
  if (height > 0) {
    document.documentElement.style.setProperty('--vp-layout-top-height', `${height}px`);
  } else {
    document.documentElement.style.removeProperty('--vp-layout-top-height');
  }
}

function updateBannerClass(show: boolean) {
  if (!inBrowser) return;
  if (show) {
    document.documentElement.classList.add('has-version-banner');
    nextTick(updateLayoutTopHeight);
  } else {
    document.documentElement.classList.remove('has-version-banner');
    document.documentElement.style.removeProperty('--vp-layout-top-height');
  }
}

let resizeObserver: ResizeObserver | null = null;

onMounted(() => {
  updateBannerClass(isNonDefaultVersion.value);
  watch(isNonDefaultVersion, (val) => updateBannerClass(val));

  if (bannerRef.value) {
    resizeObserver = new ResizeObserver(updateLayoutTopHeight);
    resizeObserver.observe(bannerRef.value);
  }
});

onUnmounted(() => {
  resizeObserver?.disconnect();
  updateBannerClass(false);
});
</script>

<template>
  <div v-if="isNonDefaultVersion" ref="bannerRef" class="version-banner">
    <span v-if="isJapanese">
      このドキュメントは <strong>{{ versionLabel }}</strong>
      版です。最新の安定版ではありません。
    </span>
    <span v-else>
      You are viewing the <strong>{{ versionLabel }}</strong> documentation.
      This is not the latest stable version.
    </span>
  </div>
</template>
