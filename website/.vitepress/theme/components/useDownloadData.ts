import { onMounted, ref } from 'vue';
import {
  type GitHubRelease,
  latestPlatformRelease,
  type Platform,
} from './releaseAssets';

const REPO = 'm1sk9/LunaticChat';

export interface PlatformRelease {
  version: string;
  publishedAt: string;
  releaseUrl: string;
  downloadUrl: string;
  fileName: string;
  fileSize: number;
}

export interface DownloadData {
  paper: PlatformRelease | null;
  velocity: PlatformRelease | null;
  ci: { url: string };
}

function resolvePlatformRelease(
  releases: GitHubRelease[],
  platform: Platform,
): PlatformRelease | null {
  const latest = latestPlatformRelease(releases, platform);
  if (!latest) return null;

  return {
    version: latest.version,
    publishedAt: latest.release.published_at,
    releaseUrl: latest.release.html_url,
    downloadUrl: latest.asset.browser_download_url,
    fileName: latest.asset.name,
    fileSize: latest.asset.size,
  };
}

export function useDownloadData() {
  const data = ref<DownloadData>({
    paper: null,
    velocity: null,
    ci: {
      url: `https://github.com/${REPO}/actions/workflows/ci.yaml?query=branch%3Amain`,
    },
  });
  const loading = ref(true);
  const error = ref(false);

  onMounted(async () => {
    try {
      const res = await fetch(
        `https://api.github.com/repos/${REPO}/releases?per_page=30`,
      );
      if (!res.ok) {
        error.value = true;
        return;
      }

      const releases: GitHubRelease[] = await res.json();

      data.value = {
        paper: resolvePlatformRelease(releases, 'paper'),
        velocity: resolvePlatformRelease(releases, 'velocity'),
        ci: data.value.ci,
      };
    } catch {
      error.value = true;
    } finally {
      loading.value = false;
    }
  });

  return { data, loading, error };
}
