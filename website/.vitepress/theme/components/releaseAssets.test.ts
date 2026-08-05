import { describe, expect, test } from 'bun:test';
import {
  findPlatformAsset,
  type GitHubRelease,
  latestPlatformRelease,
  platformReleases,
} from './releaseAssets';

function release(
  tag: string,
  publishedAt: string,
  assetNames: string[],
): GitHubRelease {
  return {
    tag_name: tag,
    published_at: publishedAt,
    html_url: `https://github.com/m1sk9/LunaticChat/releases/tag/${tag}`,
    assets: assetNames.map((name) => ({
      name,
      size: 1024,
      browser_download_url: `https://example.invalid/${name}`,
    })),
  };
}

const UNIFIED_1_3_0 = release('v1.3.0', '2026-08-04T20:05:51Z', [
  'LunaticChat-1.2.0-velocity.jar',
  'LunaticChat-1.3.0.jar',
]);
const PAPER_1_2_2 = release('paper/v1.2.2', '2026-06-12T07:57:55Z', [
  'LunaticChat-1.2.2.jar',
]);
const VELOCITY_1_1_0 = release('velocity/v1.1.0', '2026-06-12T07:57:33Z', [
  'LunaticChat-1.1.0-velocity.jar',
]);

describe('findPlatformAsset', () => {
  test('reads each platform version from its own JAR, not from the shared tag', () => {
    expect(findPlatformAsset(UNIFIED_1_3_0, 'paper')?.version).toBe('1.3.0');
    expect(findPlatformAsset(UNIFIED_1_3_0, 'velocity')?.version).toBe('1.2.0');
  });

  test('returns the asset backing the reported version', () => {
    expect(findPlatformAsset(UNIFIED_1_3_0, 'velocity')?.asset.name).toBe(
      'LunaticChat-1.2.0-velocity.jar',
    );
  });

  test('does not mistake the Velocity JAR for the Paper JAR', () => {
    expect(findPlatformAsset(VELOCITY_1_1_0, 'paper')).toBeNull();
  });

  test('returns null when the release carries no JAR for the platform', () => {
    expect(findPlatformAsset(PAPER_1_2_2, 'velocity')).toBeNull();
  });
});

describe('latestPlatformRelease', () => {
  const releases = [UNIFIED_1_3_0, VELOCITY_1_1_0, PAPER_1_2_2];

  test('prefers a newer unified release over an older platform-specific tag', () => {
    expect(latestPlatformRelease(releases, 'paper')?.version).toBe('1.3.0');
    expect(latestPlatformRelease(releases, 'velocity')?.version).toBe('1.2.0');
  });

  test('prefers a newer platform-specific tag over an older unified release', () => {
    const paperOnly = release('paper/v1.4.0', '2026-09-01T00:00:00Z', [
      'LunaticChat-1.4.0.jar',
    ]);
    const withPaperOnly = [...releases, paperOnly];

    expect(latestPlatformRelease(withPaperOnly, 'paper')?.version).toBe(
      '1.4.0',
    );
    expect(latestPlatformRelease(withPaperOnly, 'velocity')?.version).toBe(
      '1.2.0',
    );
  });

  test('advances one platform without disturbing the other', () => {
    const velocityOnly = release('velocity/v1.3.0', '2026-09-01T00:00:00Z', [
      'LunaticChat-1.3.0-velocity.jar',
    ]);
    const withVelocityOnly = [...releases, velocityOnly];

    expect(latestPlatformRelease(withVelocityOnly, 'velocity')?.version).toBe(
      '1.3.0',
    );
    expect(latestPlatformRelease(withVelocityOnly, 'paper')?.version).toBe(
      '1.3.0',
    );
  });

  test('ranks by publication time rather than by response order', () => {
    const outOfOrder = [PAPER_1_2_2, UNIFIED_1_3_0];
    expect(latestPlatformRelease(outOfOrder, 'paper')?.release.tag_name).toBe(
      'v1.3.0',
    );
  });

  test('returns null when no release carries a JAR for the platform', () => {
    expect(latestPlatformRelease([PAPER_1_2_2], 'velocity')).toBeNull();
  });
});

describe('platformReleases', () => {
  test('lists every release carrying the platform JAR, newest first', () => {
    expect(
      platformReleases([PAPER_1_2_2, UNIFIED_1_3_0], 'paper').map(
        (e) => e.version,
      ),
    ).toEqual(['1.3.0', '1.2.2']);
  });

  test('excludes releases that ship only the other platform', () => {
    expect(platformReleases([VELOCITY_1_1_0], 'paper')).toEqual([]);
  });

  test('lists a version once when a later unified release re-attaches its JAR', () => {
    // v1.4.0 bumps Paper alone, so it re-attaches the unchanged Velocity 1.2.0 JAR.
    const unified_1_4_0 = release('v1.4.0', '2026-09-01T00:00:00Z', [
      'LunaticChat-1.2.0-velocity.jar',
      'LunaticChat-1.4.0.jar',
    ]);

    expect(
      platformReleases([UNIFIED_1_3_0, unified_1_4_0], 'velocity').map(
        (e) => e.version,
      ),
    ).toEqual(['1.2.0']);
  });

  test('attributes a re-attached JAR to the release that shipped it last', () => {
    const unified_1_4_0 = release('v1.4.0', '2026-09-01T00:00:00Z', [
      'LunaticChat-1.2.0-velocity.jar',
      'LunaticChat-1.4.0.jar',
    ]);

    expect(
      platformReleases([UNIFIED_1_3_0, unified_1_4_0], 'velocity')[0]?.release
        .tag_name,
    ).toBe('v1.4.0');
  });
});
