<script setup lang="ts">
import { computed } from 'vue';
import { useData } from 'vitepress';
import {
  useCompatibilityData,
  checkCompatibility,
  formatProtocol,
  type CompatibilityResult,
  type PlatformReleaseEntry,
} from './useCompatibilityData';

const { lang } = useData();
const isEn = computed(() => lang.value === 'en-US');
const { data, loading, error } = useCompatibilityData();

const t = computed(() =>
  isEn.value
    ? {
        loading: 'Loading compatibility data...',
        error: 'Failed to load compatibility data. Please check ',
        errorSuffix: ' directly.',
        emptyPaper: 'No Paper releases yet.',
        emptyVelocity: 'No Velocity releases yet.',
        empty: 'No releases yet. Compatibility matrix will appear once releases are published.',
        paperVersion: 'Paper version',
        velocityVersion: 'Velocity version',
        protocol: 'Protocol',
        unknown: 'unknown',
        compatible: 'Compatible',
        incompatible: 'Incompatible',
        compatibleShort: 'OK',
        compatibilityHeader: 'Compatibility',
        reasonMajorMismatch: 'Major version mismatch',
        reasonPaperTooNew: 'Paper newer — update Velocity first',
        reasonVelocityTooNew: 'Velocity newer — update Paper',
        reasonPaperTooOld: 'Paper too old',
        reasonVelocityTooOld: 'Velocity too old',
        legend: 'Legend',
        legendCompatible: 'Compatible — both can connect.',
        legendIncompatible: 'Incompatible — handshake will be rejected.',
      }
    : {
        loading: '互換性情報を取得中...',
        error: '互換性情報の取得に失敗しました．',
        errorSuffix: ' を直接ご確認ください．',
        emptyPaper: 'Paper のリリースはまだありません．',
        emptyVelocity: 'Velocity のリリースはまだありません．',
        empty: 'リリースがまだありません．リリース後に互換性マトリクスが表示されます．',
        paperVersion: 'Paper バージョン',
        velocityVersion: 'Velocity バージョン',
        protocol: 'プロトコル',
        unknown: '不明',
        compatible: '互換',
        incompatible: '非互換',
        compatibleShort: 'OK',
        compatibilityHeader: '互換性',
        reasonMajorMismatch: 'MAJOR バージョン不一致',
        reasonPaperTooNew: 'Paper が新しすぎる — Velocity を先に更新',
        reasonVelocityTooNew: 'Velocity が新しすぎる — Paper を更新',
        reasonPaperTooOld: 'Paper が古すぎる',
        reasonVelocityTooOld: 'Velocity が古すぎる',
        legend: '凡例',
        legendCompatible: '互換 — 接続可能．',
        legendIncompatible: '非互換 — ハンドシェイクで拒否されます．',
      },
);

const sortedPaper = computed<PlatformReleaseEntry[]>(() =>
  [...data.value.paper].sort((a, b) => compareVersion(b.version, a.version)),
);
const sortedVelocity = computed<PlatformReleaseEntry[]>(() =>
  [...data.value.velocity].sort((a, b) => compareVersion(b.version, a.version)),
);

function compareVersion(a: string, b: string): number {
  const pa = a.split('.').map((n) => Number.parseInt(n, 10));
  const pb = b.split('.').map((n) => Number.parseInt(n, 10));
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const da = pa[i] ?? 0;
    const db = pb[i] ?? 0;
    if (da !== db) return da - db;
  }
  return 0;
}

function reasonLabel(result: CompatibilityResult): string {
  switch (result) {
    case 'major-mismatch':
      return t.value.reasonMajorMismatch;
    case 'paper-too-new':
      return t.value.reasonPaperTooNew;
    case 'velocity-too-new':
      return t.value.reasonVelocityTooNew;
    case 'paper-too-old':
      return t.value.reasonPaperTooOld;
    case 'velocity-too-old':
      return t.value.reasonVelocityTooOld;
    default:
      return '';
  }
}

function cellResult(
  paper: PlatformReleaseEntry,
  velocity: PlatformReleaseEntry,
): { ok: boolean; reason: string } {
  if (!paper.protocol || !velocity.protocol) {
    return { ok: false, reason: t.value.unknown };
  }
  const r = checkCompatibility(paper.protocol, velocity.protocol);
  return { ok: r === 'compatible', reason: r === 'compatible' ? '' : reasonLabel(r) };
}
</script>

<template>
  <div class="compat-matrix">
    <div v-if="loading" class="compat-loading">
      <p>{{ t.loading }}</p>
    </div>

    <div v-else-if="error" class="compat-error">
      <p>
        {{ t.error
        }}<a href="https://github.com/m1sk9/LunaticChat/releases" target="_blank" rel="noopener">GitHub Releases</a>{{ t.errorSuffix }}
      </p>
    </div>

    <div v-else-if="sortedPaper.length === 0 && sortedVelocity.length === 0" class="compat-empty">
      <p>{{ t.empty }}</p>
    </div>

    <div v-else class="compat-content">
      <div class="compat-table-wrapper">
        <table class="compat-table">
          <thead>
            <tr>
              <th class="compat-corner">
                <span class="compat-axis-paper">{{ t.paperVersion }}</span>
                <span class="compat-axis-divider">／</span>
                <span class="compat-axis-velocity">{{ t.velocityVersion }}</span>
              </th>
              <th v-for="v in sortedVelocity" :key="v.tag" class="compat-col-header">
                <div class="compat-version">v{{ v.version }}</div>
                <div class="compat-protocol">
                  {{ t.protocol }}: {{ v.protocol ? formatProtocol(v.protocol) : t.unknown }}
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="sortedPaper.length === 0">
              <td colspan="100" class="compat-empty-cell">{{ t.emptyPaper }}</td>
            </tr>
            <tr v-for="p in sortedPaper" :key="p.tag">
              <th scope="row" class="compat-row-header">
                <div class="compat-version">v{{ p.version }}</div>
                <div class="compat-protocol">
                  {{ t.protocol }}: {{ p.protocol ? formatProtocol(p.protocol) : t.unknown }}
                </div>
              </th>
              <td
                v-for="v in sortedVelocity"
                :key="v.tag"
                :class="['compat-cell', cellResult(p, v).ok ? 'compat-ok' : 'compat-ng']"
                :title="cellResult(p, v).reason || t.compatible"
              >
                <span v-if="cellResult(p, v).ok" class="compat-mark-ok" :aria-label="t.compatible">✓</span>
                <span v-else class="compat-mark-ng" :aria-label="t.incompatible">✗</span>
              </td>
              <td v-if="sortedVelocity.length === 0" class="compat-empty-cell">{{ t.emptyVelocity }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="compat-legend">
        <p class="compat-legend-title">{{ t.legend }}</p>
        <ul>
          <li><span class="compat-mark-ok">✓</span> {{ t.legendCompatible }}</li>
          <li><span class="compat-mark-ng">✗</span> {{ t.legendIncompatible }}</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.compat-matrix {
  margin: 24px 0;
}

.compat-loading,
.compat-empty {
  text-align: center;
  padding: 32px 0;
  color: var(--vp-c-text-2);
}

.compat-error {
  border: 1px solid var(--vp-c-danger-soft);
  background: var(--vp-c-danger-soft);
  border-radius: 8px;
  padding: 16px 20px;
}

.compat-error a {
  color: var(--vp-c-brand-1);
  text-decoration: underline;
}

.compat-table-wrapper {
  overflow-x: auto;
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
}

.compat-table {
  border-collapse: collapse;
  width: 100%;
  margin: 0;
  font-size: 0.875rem;
}

.compat-table th,
.compat-table td {
  border: 1px solid var(--vp-c-divider);
  padding: 8px 12px;
  text-align: center;
  vertical-align: middle;
}

.compat-corner {
  background: var(--vp-c-bg-soft);
  font-weight: 500;
  font-size: 0.75rem;
  white-space: nowrap;
  text-align: left !important;
}

.compat-axis-paper,
.compat-axis-velocity {
  display: inline-block;
}

.compat-axis-divider {
  margin: 0 4px;
  color: var(--vp-c-text-3);
}

.compat-col-header,
.compat-row-header {
  background: var(--vp-c-bg-soft);
  font-weight: 600;
  white-space: nowrap;
}

.compat-version {
  font-size: 0.95rem;
}

.compat-protocol {
  font-size: 0.7rem;
  color: var(--vp-c-text-2);
  font-weight: 400;
  margin-top: 2px;
}

.compat-cell {
  font-size: 1.1rem;
  font-weight: 700;
}

.compat-ok {
  background: rgba(20, 200, 100, 0.08);
}

.compat-ng {
  background: rgba(220, 60, 60, 0.06);
}

.compat-mark-ok {
  color: rgb(20, 160, 90);
}

.compat-mark-ng {
  color: rgb(200, 60, 60);
}

.compat-empty-cell {
  color: var(--vp-c-text-3);
  font-style: italic;
}

.compat-legend {
  margin-top: 16px;
  font-size: 0.85rem;
  color: var(--vp-c-text-2);
}

.compat-legend-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.compat-legend ul {
  margin: 0;
  padding-left: 20px;
}

.compat-legend li {
  line-height: 1.7;
}
</style>
