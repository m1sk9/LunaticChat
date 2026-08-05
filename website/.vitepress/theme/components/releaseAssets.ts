export type Platform = 'paper' | 'velocity';

export interface ReleaseAsset {
  name: string;
  size: number;
  browser_download_url: string;
}

export interface GitHubRelease {
  tag_name: string;
  published_at: string;
  html_url: string;
  assets: ReleaseAsset[];
}

export interface PlatformAsset {
  version: string;
  asset: ReleaseAsset;
}

export interface PlatformRelease extends PlatformAsset {
  release: GitHubRelease;
}

// A unified `vX.Y.Z` tag ships both platforms and their versions need not agree
// (v1.3.0 carried Velocity 1.2.0), so a tag name can never stand in for a
// platform version — only the JAR file name states it.
const JAR_PATTERN: Record<Platform, RegExp> = {
  paper: /^LunaticChat-(\d+\.\d+\.\d+)\.jar$/,
  velocity: /^LunaticChat-(\d+\.\d+\.\d+)-velocity\.jar$/,
};

export function findPlatformAsset(
  release: GitHubRelease,
  platform: Platform,
): PlatformAsset | null {
  for (const asset of release.assets) {
    const version = JAR_PATTERN[platform].exec(asset.name)?.[1];
    if (version) return { version, asset };
  }
  return null;
}

export function platformReleases(
  releases: GitHubRelease[],
  platform: Platform,
): PlatformRelease[] {
  // The API orders releases by tag creation, not by publication — velocity/v1.1.0
  // precedes the later-published paper/v1.2.2 — so response order cannot decide
  // which release is the newest.
  const found = releases
    .flatMap((release) => {
      const asset = findPlatformAsset(release, platform);
      return asset ? [{ release, ...asset }] : [];
    })
    .sort(
      (a, b) =>
        Date.parse(b.release.published_at) - Date.parse(a.release.published_at),
    );

  // A unified `vX.Y.Z` release always attaches both JARs, so a platform version
  // reappears under a new tag whenever only the other platform was bumped. The
  // newest publication is the one that shipped, and keeping both would list the
  // same version twice — with two protocol versions when the protocol moved.
  const seen = new Set<string>();
  return found.filter((entry) => {
    if (seen.has(entry.version)) return false;
    seen.add(entry.version);
    return true;
  });
}

export function latestPlatformRelease(
  releases: GitHubRelease[],
  platform: Platform,
): PlatformRelease | null {
  return platformReleases(releases, platform)[0] ?? null;
}
