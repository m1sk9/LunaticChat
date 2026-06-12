<script setup lang="ts">
import { computed } from 'vue';
import { useData } from 'vitepress';
import { useDownloadData } from './useDownloadData';

const { lang } = useData();
const isEn = computed(() => lang.value === 'en-US');
const { data, loading, error } = useDownloadData();

const t = computed(() =>
  isEn.value
    ? {
        title: 'Download',
        description: 'Download the latest releases of LunaticChat.',
        requirements: 'Requirements',
        javaReq: '25 or later',
        paperReq: '26.1.x or later',
        velocityReq: '3.4.x or later',
        noRelease: 'No release',
        releaseDate: 'Release date',
        fileSize: 'File size',
        download: 'Download',
        releaseNotes: 'Release notes',
        modrinthDesc: 'LunaticChat is also available on Modrinth.',
        notYetReleased: 'Not yet released.',
        devBuilds: 'Development Builds',
        devBuildsDesc:
          'The latest build from the main branch is available from CI. Development builds are not guaranteed to be stable.',
        viewCiBuilds: 'View latest CI builds',
        compatNotice:
          'Using the latest Paper and the latest Velocity always works. If you need to mix older versions, check the compatibility matrix below.',
        compatLink: 'See Paper / Velocity Compatibility for details.',
        compatMatrixTitle: 'Compatibility Matrix',
        compatMatrixDesc:
          'Combinations marked ✓ can connect. Hover a cell for details.',
        spigotNotice:
          'LunaticChat only supports Paper / Folia servers. It does not work on Spigot or BungeeCord, and there are no plans to support them in the future.',
        spigotAlt:
          'For Spigot environments, we recommend using a',
        spigotLink: 'fork of LunaChat',
      }
    : {
        title: 'ダウンロード',
        description: 'LunaticChat の最新リリースをダウンロードできます。',
        requirements: '動作要件',
        javaReq: '25 以降',
        paperReq: '26.1.x 以降',
        velocityReq: '3.4.x 以降',
        noRelease: 'リリースなし',
        releaseDate: 'リリース日',
        fileSize: 'ファイルサイズ',
        download: 'ダウンロード',
        releaseNotes: 'リリースノート',
        modrinthDesc: 'LunaticChat は Modrinth でも公開しています。',
        notYetReleased: 'まだリリースされていません。',
        devBuilds: '開発ビルド',
        devBuildsDesc:
          '最新の main ブランチのビルドは CI から取得できます。開発ビルドは安定性が保証されていません。',
        viewCiBuilds: '最新の CI ビルドを確認',
        compatNotice:
          '両プラグインの最新版同士は常に互換性があります．古いバージョンを混ぜる場合のみ，下の互換性マトリクスを確認してください．',
        compatLink: '詳細は Paper / Velocity 互換性 を参照してください．',
        compatMatrixTitle: '互換性マトリクス',
        compatMatrixDesc:
          '✓ の組み合わせは接続可能です．セルにホバーすると詳細が表示されます．',
        spigotNotice:
          'LunaticChat は Paper / Folia サーバーのみをサポートしています．Spigot / BungeeCord では動作せず，今後も対応予定はありません．',
        spigotAlt:
          'Spigot 環境では',
        spigotLink: 'LunaChat の Fork',
      },
);

function formatSize(bytes: number | null): string {
  if (!bytes) return '-';
  const mb = bytes / (1024 * 1024);
  return `${mb.toFixed(1)} MB`;
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '-';
  const locale = isEn.value ? 'en-US' : 'ja-JP';
  return new Date(dateStr).toLocaleDateString(locale, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}
</script>

<template>
  <div class="download-page">
    <h1>{{ t.title }}</h1>
    <p>{{ t.description }}</p>

    <div class="download-requirements">
      <p class="download-requirements-title">{{ t.requirements }}</p>
      <ul>
        <li><strong>Java</strong>: {{ t.javaReq }}</li>
        <li><strong>Paper / Folia</strong>: {{ t.paperReq }}</li>
        <li><strong>Velocity</strong>: {{ t.velocityReq }}</li>
      </ul>
    </div>

    <div class="download-spigot-notice">
      <p class="download-spigot-title">Spigot / BungeeCord {{ isEn ? 'is not supported' : 'は非対応です' }}</p>
      <p>{{ t.spigotNotice }}</p>
      <p>{{ t.spigotAlt }} <a href="https://github.com/f1w3/LunaChat" target="_blank" rel="noopener">{{ t.spigotLink }}</a> {{ isEn ? '.' : 'の使用を推奨します．' }}</p>
    </div>

    <div v-if="loading" class="download-loading">
      <p>{{ isEn ? 'Loading releases...' : 'リリース情報を取得中...' }}</p>
    </div>

    <div v-else-if="error" class="download-error">
      <p>{{ isEn ? 'Failed to load release information. Please check ' : 'リリース情報の取得に失敗しました．' }}<a href="https://github.com/m1sk9/LunaticChat/releases" target="_blank" rel="noopener">GitHub Releases</a>{{ isEn ? ' directly.' : ' を直接ご確認ください．' }}</p>
    </div>

    <div v-else class="download-grid">
      <!-- Paper / Folia -->
      <div class="download-card">
        <div class="download-card-header">
          <img src="/assets/brand/paper.svg" alt="Paper" class="download-icon" />
          <div>
            <h2>Paper / Folia</h2>
            <p class="download-version" v-if="data.paper">v{{ data.paper.version }}</p>
            <p class="download-version" v-else>{{ t.noRelease }}</p>
          </div>
        </div>
        <div class="download-card-body" v-if="data.paper">
          <dl class="download-meta">
            <div>
              <dt>{{ t.releaseDate }}</dt>
              <dd>{{ formatDate(data.paper.publishedAt) }}</dd>
            </div>
            <div>
              <dt>{{ t.fileSize }}</dt>
              <dd>{{ formatSize(data.paper.fileSize) }}</dd>
            </div>
            <div>
              <dd><code>{{ data.paper.fileName ?? '-' }}</code></dd>
            </div>
          </dl>
          <div class="download-actions">
            <a v-if="data.paper.downloadUrl" :href="data.paper.downloadUrl" class="download-btn primary">{{ t.download }}</a>
            <a :href="data.paper.releaseUrl" class="download-btn" target="_blank" rel="noopener">{{ t.releaseNotes }}</a>
          </div>
        </div>
        <div class="download-card-body" v-else>
          <p class="download-empty">{{ t.notYetReleased }}</p>
        </div>
      </div>

      <!-- Velocity -->
      <div class="download-card">
        <div class="download-card-header">
          <img src="/assets/brand/velocity.svg" alt="Velocity" class="download-icon" />
          <div>
            <h2>Velocity</h2>
            <p class="download-version" v-if="data.velocity">v{{ data.velocity.version }}</p>
            <p class="download-version" v-else>{{ t.noRelease }}</p>
          </div>
        </div>
        <div class="download-card-body" v-if="data.velocity">
          <dl class="download-meta">
            <div>
              <dt>{{ t.releaseDate }}</dt>
              <dd>{{ formatDate(data.velocity.publishedAt) }}</dd>
            </div>
            <div>
              <dt>{{ t.fileSize }}</dt>
              <dd>{{ formatSize(data.velocity.fileSize) }}</dd>
            </div>
            <div>
              <dd><code>{{ data.velocity.fileName ?? '-' }}</code></dd>
            </div>
          </dl>
          <div class="download-actions">
            <a v-if="data.velocity.downloadUrl" :href="data.velocity.downloadUrl" class="download-btn primary">{{ t.download }}</a>
            <a :href="data.velocity.releaseUrl" class="download-btn" target="_blank" rel="noopener">{{ t.releaseNotes }}</a>
          </div>
        </div>
        <div class="download-card-body" v-else>
          <p class="download-empty">{{ t.notYetReleased }}</p>
        </div>
      </div>
    </div>

    <h2>{{ t.compatMatrixTitle }}</h2>
    <p>{{ t.compatMatrixDesc }}</p>
    <p>{{ t.compatNotice }}</p>
    <CompatibilityMatrix />

    <h2>Modrinth</h2>
    <p>{{ t.modrinthDesc }}</p>
    <div class="download-ci">
      <a href="https://modrinth.com/plugin/lunaticchat" class="download-btn" target="_blank" rel="noopener">Modrinth</a>
    </div>

    <h2>{{ t.devBuilds }}</h2>
    <p>{{ t.devBuildsDesc }}</p>
    <div class="download-ci">
      <a :href="data.ci.url" class="download-btn" target="_blank" rel="noopener">{{ t.viewCiBuilds }}</a>
    </div>
  </div>
</template>

<style scoped>
.download-page {
  max-width: 768px;
  margin: 0 auto;
  padding: 48px 24px;
}

.download-page h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 8px;
}

.download-page > p {
  color: var(--vp-c-text-2);
  margin-bottom: 24px;
}

.download-page h2 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-top: 40px;
  margin-bottom: 8px;
  border: none;
  padding: 0;
}

.download-spigot-notice {
  border: 1px solid var(--vp-c-danger-soft);
  background: var(--vp-c-danger-soft);
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 24px;
  font-size: 0.9rem;
  line-height: 1.7;
}

.download-spigot-notice p {
  margin: 0;
}

.download-spigot-notice p + p {
  margin-top: 8px;
}

.download-spigot-title {
  font-weight: 600;
  margin-bottom: 8px !important;
}

.download-spigot-notice a {
  color: var(--vp-c-brand-1);
  text-decoration: underline;
}

.download-requirements {
  border: 1px solid var(--vp-c-brand-soft);
  background: var(--vp-c-brand-soft);
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 24px;
}

.download-requirements-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.download-requirements ul {
  margin: 0;
  padding-left: 20px;
}

.download-requirements li {
  font-size: 0.9rem;
  line-height: 1.7;
}

.download-compat-notice {
  border: 1px solid var(--vp-c-warning-soft);
  background: var(--vp-c-warning-soft);
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 24px;
  font-size: 0.9rem;
  line-height: 1.7;
}

.download-compat-notice p {
  margin: 0;
}

.download-compat-notice p + p {
  margin-top: 8px;
}

.download-compat-title {
  font-weight: 600;
  margin-bottom: 8px !important;
}

.download-compat-notice a {
  color: var(--vp-c-brand-1);
  text-decoration: underline;
}

.download-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin: 24px 0;
}

@media (max-width: 640px) {
  .download-grid {
    grid-template-columns: 1fr;
  }
}

.download-card {
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  overflow: hidden;
  background: var(--vp-c-bg-soft);
}

.download-card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 24px 0;
}

.download-card-header h2 {
  margin: 0;
  font-size: 1.25rem;
}

.download-icon {
  width: 40px;
  height: 40px;
  object-fit: contain;
  filter: brightness(0) saturate(100%);
}

:global(.dark .download-icon) {
  filter: brightness(0) saturate(100%) invert(1);
}

.download-version {
  margin: 2px 0 0;
  font-size: 0.875rem;
  color: var(--vp-c-text-2);
}

.download-card-body {
  padding: 20px 24px 24px;
}

.download-meta {
  margin: 0 0 20px;
  padding: 0;
}

.download-meta div {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid var(--vp-c-divider);
  font-size: 0.875rem;
}

.download-meta div:last-child {
  border-bottom: none;
}

.download-meta dt {
  color: var(--vp-c-text-2);
}

.download-meta dd {
  margin: 0;
  color: var(--vp-c-text-1);
}

.download-actions {
  display: flex;
  gap: 8px;
}

.download-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  text-decoration: none !important;
  color: var(--vp-c-text-1);
  border: 1px solid var(--vp-c-divider);
  background: var(--vp-c-bg);
  transition: border-color 0.25s, background 0.25s;
}

.download-btn:hover {
  border-color: var(--vp-c-brand-1);
}

.download-btn.primary {
  background: var(--vp-button-brand-bg);
  color: var(--vp-button-brand-text);
  border-color: var(--vp-button-brand-border);
}

.download-btn.primary:hover {
  background: var(--vp-button-brand-hover-bg);
  border-color: var(--vp-button-brand-hover-border);
}

.download-empty {
  color: var(--vp-c-text-3);
  font-size: 0.875rem;
}

.download-ci {
  margin: 16px 0;
}

.download-loading {
  text-align: center;
  padding: 48px 0;
  color: var(--vp-c-text-2);
}

.download-error {
  border: 1px solid var(--vp-c-danger-soft);
  background: var(--vp-c-danger-soft);
  border-radius: 8px;
  padding: 16px 20px;
  margin: 24px 0;
}

.download-error a {
  color: var(--vp-c-brand-1);
  text-decoration: underline;
}
</style>
