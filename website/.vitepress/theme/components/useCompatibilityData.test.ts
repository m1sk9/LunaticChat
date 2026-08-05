import { describe, expect, test } from 'bun:test';
import {
  checkCompatibility,
  isCompatible,
  olderSide,
  type ProtocolVersion,
} from './useCompatibilityData';

function protocol(
  major: number,
  minor: number,
  patch: number,
  minSupportedMinor = 0,
): ProtocolVersion {
  return { major, minor, patch, minSupportedMinor };
}

describe('checkCompatibility', () => {
  test('calls an identical protocol on both ends fully compatible', () => {
    expect(checkCompatibility(protocol(1, 0, 1), protocol(1, 0, 1))).toBe(
      'compatible',
    );
  });

  test('reports a degraded pair when Paper sends a sub-channel Velocity ignores', () => {
    // Paper 1.3.0 speaks 1.0.1 — the PATCH that added cross-server direct
    // messages — while Velocity 1.1.0 stopped at 1.0.0.
    expect(checkCompatibility(protocol(1, 0, 1), protocol(1, 0, 0))).toBe(
      'degraded',
    );
  });

  test('reports a degraded pair when Velocity offers a sub-channel Paper never sends', () => {
    expect(checkCompatibility(protocol(1, 0, 0), protocol(1, 0, 1))).toBe(
      'degraded',
    );
  });

  test('reports a degraded pair when Paper trails by a MINOR still inside the window', () => {
    expect(checkCompatibility(protocol(1, 0, 0), protocol(1, 1, 0))).toBe(
      'degraded',
    );
  });

  test('rejects a MAJOR mismatch', () => {
    expect(checkCompatibility(protocol(1, 0, 0), protocol(2, 0, 0))).toBe(
      'major-mismatch',
    );
  });

  test('rejects Paper running ahead of Velocity by a MINOR', () => {
    expect(checkCompatibility(protocol(1, 1, 0), protocol(1, 0, 0))).toBe(
      'paper-too-new',
    );
  });

  test('rejects Paper older than the deprecation window admits', () => {
    expect(checkCompatibility(protocol(1, 0, 0), protocol(1, 2, 0, 1))).toBe(
      'paper-too-old',
    );
  });
});

describe('isCompatible', () => {
  test('holds for a degraded pair, which still completes the handshake', () => {
    expect(isCompatible(protocol(1, 0, 1), protocol(1, 0, 0))).toBe(true);
  });

  test('fails for a pair the handshake rejects', () => {
    expect(isCompatible(protocol(1, 1, 0), protocol(1, 0, 0))).toBe(false);
  });
});

describe('olderSide', () => {
  test('names Velocity when it trails by a PATCH', () => {
    expect(olderSide(protocol(1, 0, 1), protocol(1, 0, 0))).toBe('velocity');
  });

  test('names Paper when it trails by a PATCH', () => {
    expect(olderSide(protocol(1, 0, 0), protocol(1, 0, 1))).toBe('paper');
  });

  test('lets a MINOR gap outrank the PATCH comparison', () => {
    expect(olderSide(protocol(1, 0, 9), protocol(1, 1, 0))).toBe('paper');
  });
});
