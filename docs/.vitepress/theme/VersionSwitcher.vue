<script setup lang="ts">
import { computed, ref } from 'vue';
import { useData, useRouter } from 'vitepress';
import VPFlyout from 'vitepress/dist/client/theme-default/components/VPFlyout.vue';
import VPMenuLink from 'vitepress/dist/client/theme-default/components/VPMenuLink.vue';

/**
 * Mapping from archive folder names to display labels.
 * Update this when adding new versions.
 */
const versionLabels: Record<string, string> = {
  v1: 'v1 (26.1) - Next',
};

interface VersioningPlugin {
  versions: Set<string>;
  currentVersion: string;
}

interface Props {
  versioningPlugin: VersioningPlugin;
  screenMenu?: boolean;
}

interface VersionMenuItem {
  text: string;
  link: string;
}

const props = defineProps<Props>();
const router = useRouter();
const { site } = useData();
const isOpen = ref(false);

const versionSet = computed(() => new Set([...props.versioningPlugin.versions]));
const hasVersions = computed(() => versionSet.value.size > 0);

const currentLocale = computed(
  () => site.value.locales[site.value.localeIndex]?.link?.replace(/\//g, '') || '',
);

const pathSegments = computed(() =>
  router.route.path.replace(/^\/|\/$/g, '').split('/'),
);

const activeVersion = computed(() => {
  const { currentVersion } = props.versioningPlugin;
  const segments = pathSegments.value;
  const locale = currentLocale.value;
  const hasLocalePrefix = locale !== '' && segments[0] === locale;
  const versionCandidate = hasLocalePrefix ? segments[1] : segments[0];
  return versionSet.value.has(versionCandidate) ? versionCandidate : currentVersion;
});

const activeVersionLabel = computed(
  () => versionLabels[activeVersion.value] ?? activeVersion.value,
);

const availableVersions = computed(() =>
  Array.from(props.versioningPlugin.versions).filter((v) => v !== activeVersion.value),
);

const shouldShowCurrentVersion = computed(
  () => activeVersion.value !== props.versioningPlugin.currentVersion,
);

function isLocaleFirst(segments: string[]): boolean {
  const locale = currentLocale.value;
  return locale !== '' && segments[0] === locale;
}

function removeVersionSegments(segments: string[]): string[] {
  return segments.filter((seg) => !versionSet.value.has(seg));
}

function buildVersionPath(version: string): string {
  const { currentVersion } = props.versioningPlugin;
  const baseSegments = removeVersionSegments(pathSegments.value);
  const hasLocale = isLocaleFirst(baseSegments);

  let newSegments: string[];

  if (version === currentVersion) {
    newSegments = baseSegments;
  } else if (hasLocale) {
    newSegments = [baseSegments[0], version, ...baseSegments.slice(1)];
  } else {
    newSegments = [version, ...baseSegments];
  }

  return `/${newSegments.join('/')}`;
}

function label(version: string): string {
  return versionLabels[version] ?? version;
}

function createVersionMenuItem(version: string): VersionMenuItem {
  return {
    text: label(version),
    link: buildVersionPath(version),
  };
}

function toggle(): void {
  isOpen.value = !isOpen.value;
}
</script>

<template>
  <template v-if="hasVersions">
    <!-- Desktop flyout -->
    <VPFlyout
      v-if="!screenMenu"
      class="VPVersionSwitcher"
      icon="vpi-versioning"
      :button="activeVersionLabel"
      label="Switch Version"
    >
      <div class="items">
        <VPMenuLink
          v-if="shouldShowCurrentVersion"
          :item="createVersionMenuItem(versioningPlugin.currentVersion)"
        />
        <VPMenuLink
          v-for="version in availableVersions"
          :key="version"
          :item="createVersionMenuItem(version)"
        />
      </div>
    </VPFlyout>

    <!-- Mobile dropdown -->
    <div v-else class="VPScreenVersionSwitcher" :class="{ open: isOpen }">
      <button
        class="button"
        type="button"
        aria-controls="navbar-group-version"
        :aria-expanded="isOpen"
        @click="toggle"
      >
        <span class="button-text">
          <span class="vpi-versioning icon" />
          Switch Version
        </span>
        <span class="vpi-plus button-icon" />
      </button>

      <div id="navbar-group-version" class="items">
        <VPMenuLink :item="createVersionMenuItem(versioningPlugin.currentVersion)" />
        <VPMenuLink
          v-for="version in versioningPlugin.versions"
          :key="version"
          :item="createVersionMenuItem(version)"
        />
      </div>
    </div>
  </template>
</template>

<style>
.vpi-versioning.option-icon {
  margin-right: 2px !important;
}

.vpi-versioning {
  --icon: url("data:image/svg+xml;charset=utf-8;base64,PHN2ZyB3aWR0aD0iNjRweCIgaGVpZ2h0PSI2NHB4IiB2aWV3Qm94PSIwIDAgMjQgMjQiIHN0cm9rZS13aWR0aD0iMi4yIiBmaWxsPSJub25lIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGNvbG9yPSIjMDAwMDAwIj48cGF0aCBkPSJNMTcgN0MxOC4xMDQ2IDcgMTkgNi4xMDQ1NyAxOSA1QzE5IDMuODk1NDMgMTguMTA0NiAzIDE3IDNDMTUuODk1NCAzIDE1IDMuODk1NDMgMTUgNUMxNSA2LjEwNDU3IDE1Ljg5NTQgNyAxNyA3WiIgc3Ryb2tlPSIjMDAwMDAwIiBzdHJva2Utd2lkdGg9IjIuMiIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiBzdHJva2UtbGluZWpvaW49InJvdW5kIj48L3BhdGg+PHBhdGggZD0iTTcgN0M4LjEwNDU3IDcgOSA2LjEwNDU3IDkgNUM5IDMuODk1NDMgOC4xMDQ1NyAzIDcgM0M1Ljg5NTQzIDMgNSAzLjg5NTQzIDUgNUM1IDYuMTA0NTcgNS44OTU0MyA3IDcgN1oiIHN0cm9rZT0iIzAwMDAwMCIgc3Ryb2tlLXdpZHRoPSIyLjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PC9wYXRoPjxwYXRoIGQ9Ik03IDIxQzguMTA0NTcgMjEgOSAyMC4xMDQ2IDkgMTlDOSAxNy44OTU0IDguMTA0NTcgMTcgNyAxN0M1Ljg5NTQzIDE3IDUgMTcuODk1NCA1IDE5QzUgMjAuMTA0NiA1Ljg5NTQzIDIxIDcgMjFaIiBzdHJva2U9IiMwMDAwMDAiIHN0cm9rZS13aWR0aD0iMi4yIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiPjwvcGF0aD48cGF0aCBkPSJNNyA3VjE3IiBzdHJva2U9IiMwMDAwMDAiIHN0cm9rZS13aWR0aD0iMi4yIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiPjwvcGF0aD48cGF0aCBkPSJNMTcgN1Y4QzE3IDEwLjUgMTUgMTEgMTUgMTFMOSAxM0M5IDEzIDcgMTMuNSA3IDE2VjE3IiBzdHJva2U9IiMwMDAwMDAiIHN0cm9rZS13aWR0aD0iMi4yIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiPjwvcGF0aD48L3N2Zz4=");
}
</style>

<style scoped>
.VPVersionSwitcher {
  display: flex;
  align-items: center;
}

.icon {
  padding: 8px;
}

.title {
  padding: 0 24px 0 12px;
  line-height: 32px;
  font-size: 14px;
  font-weight: 700;
  color: var(--vp-c-text-1);
}

.VPScreenVersionSwitcher {
  border-bottom: 1px solid var(--vp-c-divider);
  height: 48px;
  overflow: hidden;
  transition: border-color 0.5s;
}

.VPScreenVersionSwitcher .items {
  visibility: hidden;
}

.VPScreenVersionSwitcher.open {
  padding-bottom: 10px;
  height: auto;
}

.VPScreenVersionSwitcher.open .items {
  visibility: visible;
}

.VPScreenVersionSwitcher.open .button {
  padding-bottom: 6px;
  color: var(--vp-c-brand-1);
}

.VPScreenVersionSwitcher.open .button-icon {
  transform: rotate(45deg);
}

.VPScreenVersionSwitcher button .icon {
  margin-right: 8px;
}

.button {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 4px 11px 0;
  width: 100%;
  line-height: 24px;
  font-size: 14px;
  font-weight: 500;
  color: var(--vp-c-text-1);
  transition: color 0.25s;
}

.button:hover {
  color: var(--vp-c-brand-1);
}

.button-icon {
  transition: transform 0.25s;
}

.group:first-child {
  padding-top: 0;
}

.group + .group,
.group + .item {
  padding-top: 4px;
}
</style>
