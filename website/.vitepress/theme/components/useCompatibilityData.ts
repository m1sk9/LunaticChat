import { onMounted, ref } from 'vue';
import {
  type GitHubRelease,
  type Platform,
  platformReleases,
} from './releaseAssets';

const REPO = 'm1sk9/LunaticChat';
const PROTOCOL_FILE_PATH =
  'engine/src/main/kotlin/dev/m1sk9/lunaticChat/engine/protocol/ProtocolVersion.kt';

export interface ProtocolVersion {
  major: number;
  minor: number;
  patch: number;
  minSupportedMinor: number;
}

export interface PlatformReleaseEntry {
  platform: Platform;
  version: string;
  tag: string;
  publishedAt: string;
  releaseUrl: string;
  protocol: ProtocolVersion | null;
}

export interface CompatibilityData {
  paper: PlatformReleaseEntry[];
  velocity: PlatformReleaseEntry[];
}

function parseProtocolVersion(source: string): ProtocolVersion | null {
  const match = (key: string): number | null => {
    const re = new RegExp(`const\\s+val\\s+${key}\\s*=\\s*(\\d+)`);
    const m = source.match(re);
    return m ? Number.parseInt(m[1], 10) : null;
  };

  const major = match('MAJOR');
  const minor = match('MINOR');
  const patch = match('PATCH');
  const minSupportedMinor = match('MIN_SUPPORTED_MINOR');

  if (
    major === null ||
    minor === null ||
    patch === null ||
    minSupportedMinor === null
  ) {
    return null;
  }

  return { major, minor, patch, minSupportedMinor };
}

async function fetchProtocolAtTag(
  tag: string,
): Promise<ProtocolVersion | null> {
  const url = `https://raw.githubusercontent.com/${REPO}/${encodeURIComponent(tag)}/${PROTOCOL_FILE_PATH}`;
  try {
    const res = await fetch(url);
    if (!res.ok) return null;
    const text = await res.text();
    return parseProtocolVersion(text);
  } catch {
    return null;
  }
}

function buildEntries(
  releases: GitHubRelease[],
  platform: Platform,
): PlatformReleaseEntry[] {
  return platformReleases(releases, platform).map((entry) => ({
    platform,
    version: entry.version,
    tag: entry.release.tag_name,
    publishedAt: entry.release.published_at,
    releaseUrl: entry.release.html_url,
    protocol: null,
  }));
}

export function useCompatibilityData() {
  const data = ref<CompatibilityData>({ paper: [], velocity: [] });
  const loading = ref(true);
  const error = ref(false);

  onMounted(async () => {
    try {
      const res = await fetch(
        `https://api.github.com/repos/${REPO}/releases?per_page=100`,
      );
      if (!res.ok) {
        error.value = true;
        return;
      }

      const releases: GitHubRelease[] = await res.json();

      const paper = buildEntries(releases, 'paper');
      const velocity = buildEntries(releases, 'velocity');

      const all = [...paper, ...velocity];
      const protocols = await Promise.all(
        all.map((entry) => fetchProtocolAtTag(entry.tag)),
      );
      all.forEach((entry, i) => {
        entry.protocol = protocols[i];
      });

      data.value = {
        paper: paper.filter((e) => e.protocol !== null),
        velocity: velocity.filter((e) => e.protocol !== null),
      };
    } catch {
      error.value = true;
    } finally {
      loading.value = false;
    }
  });

  return { data, loading, error };
}

export type CompatibilityResult =
  | 'compatible'
  | 'major-mismatch'
  | 'paper-too-new'
  | 'paper-too-old';

// Mirrors the gatekeeping done by Velocity in
// platform-velocity/.../PluginMessageHandler.kt — Paper does not validate.
export function checkCompatibility(
  paper: ProtocolVersion,
  velocity: ProtocolVersion,
): CompatibilityResult {
  if (paper.major !== velocity.major) return 'major-mismatch';
  if (paper.minor > velocity.minor) return 'paper-too-new';
  if (paper.minor < velocity.minSupportedMinor) return 'paper-too-old';
  return 'compatible';
}

export function isCompatible(
  paper: ProtocolVersion,
  velocity: ProtocolVersion,
): boolean {
  return checkCompatibility(paper, velocity) === 'compatible';
}

export function formatProtocol(p: ProtocolVersion): string {
  return `${p.major}.${p.minor}.${p.patch}`;
}
