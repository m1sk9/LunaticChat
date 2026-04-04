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

interface PlatformRelease {
  version: string;
  publishedAt: string;
  releaseUrl: string;
  downloadUrl: string | null;
  fileName: string | null;
  fileSize: number | null;
}

interface CIBuild {
  url: string;
}

export interface DownloadData {
  paper: PlatformRelease | null;
  velocity: PlatformRelease | null;
  ci: CIBuild;
}

async function fetchLatestRelease(
  tagPrefix: string,
): Promise<GitHubRelease | null> {
  const res = await fetch(
    `https://api.github.com/repos/${REPO}/releases?per_page=20`,
  );
  if (!res.ok) return null;

  const releases: GitHubRelease[] = await res.json();
  return (
    releases.find(
      (r) =>
        r.tag_name.startsWith(tagPrefix) || r.tag_name.startsWith('v'),
    ) ?? null
  );
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

export default {
  async load(): Promise<DownloadData> {
    // Fetch all recent releases and find the latest for each platform
    const res = await fetch(
      `https://api.github.com/repos/${REPO}/releases?per_page=30`,
    );
    const releases: GitHubRelease[] = res.ok ? await res.json() : [];

    // Find latest Paper release (paper/v* or v*)
    const paperRelease =
      releases.find((r) => r.tag_name.startsWith('paper/v')) ??
      releases.find((r) => /^v\d/.test(r.tag_name)) ??
      null;

    // Find latest Velocity release (velocity/v* or v*)
    const velocityRelease =
      releases.find((r) => r.tag_name.startsWith('velocity/v')) ??
      releases.find((r) => /^v\d/.test(r.tag_name)) ??
      null;

    return {
      paper: parsePlatformRelease(
        paperRelease,
        /^LunaticChat-[\d.]+\.jar$/,
      ),
      velocity: parsePlatformRelease(
        velocityRelease,
        /^LunaticChat-[\d.]+-velocity\.jar$/,
      ),
      ci: {
        url: `https://github.com/${REPO}/actions/workflows/ci.yaml?query=branch%3Amain`,
      },
    };
  },
};
