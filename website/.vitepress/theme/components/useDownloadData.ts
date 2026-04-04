import { ref, onMounted } from 'vue';

const REPO = 'm1sk9/LunaticChat';

interface ReleaseAsset {
  name: string;
  size: number;
  browser_download_url: string;
}

interface GitHubRelease {
  tag_name: string;
  published_at: string;
  html_url: string;
  assets: ReleaseAsset[];
}

export interface PlatformRelease {
  version: string;
  publishedAt: string;
  releaseUrl: string;
  downloadUrl: string | null;
  fileName: string | null;
  fileSize: number | null;
}

export interface DownloadData {
  paper: PlatformRelease | null;
  velocity: PlatformRelease | null;
  ci: { url: string };
}

function parsePlatformRelease(
  release: GitHubRelease | null,
  jarPattern: RegExp,
): PlatformRelease | null {
  if (!release) return null;

  const asset = release.assets.find((a) => jarPattern.test(a.name));
  const version = release.tag_name.replace(/^(paper\/|velocity\/)?v/, '');

  return {
    version,
    publishedAt: release.published_at,
    releaseUrl: release.html_url,
    downloadUrl: asset?.browser_download_url ?? null,
    fileName: asset?.name ?? null,
    fileSize: asset?.size ?? null,
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

      const paperRelease =
        releases.find((r) => r.tag_name.startsWith('paper/v')) ??
        releases.find((r) => /^v\d/.test(r.tag_name)) ??
        null;

      const velocityRelease =
        releases.find((r) => r.tag_name.startsWith('velocity/v')) ??
        releases.find((r) => /^v\d/.test(r.tag_name)) ??
        null;

      data.value = {
        paper: parsePlatformRelease(
          paperRelease,
          /^LunaticChat-[\d.]+\.jar$/,
        ),
        velocity: parsePlatformRelease(
          velocityRelease,
          /^LunaticChat-[\d.]+-velocity\.jar$/,
        ),
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
