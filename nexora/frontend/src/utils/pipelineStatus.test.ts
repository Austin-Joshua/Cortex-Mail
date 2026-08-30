import { describe, expect, it } from 'vitest';
import { resolveSyncChip } from './syncChip';
import { unwrapApiList } from './unwrapApiList';
import { getVisibleInboxDivisions } from './inboxDivisions';

describe('resolveSyncChip', () => {
  const base = {
    phase: 'grouped' as const,
    isPipelineRunning: false,
    hasSyncedMail: true,
    syncInProgress: false,
    classifyingInBackground: false,
    enrichingInBackground: false,
  };

  it('shows synced when mail is ready and idle', () => {
    expect(resolveSyncChip(base)).toBe('synced');
  });

  it('shows classifying while grouping, not syncing', () => {
    expect(resolveSyncChip({ ...base, classifyingInBackground: true })).toBe('classifying');
  });

  it('shows enriching only while sync still running', () => {
    expect(resolveSyncChip({ ...base, enrichingInBackground: true })).toBe('enriching');
  });

  it('shows syncing while pipeline pulls mail', () => {
    expect(resolveSyncChip({ ...base, isPipelineRunning: true })).toBe('syncing');
  });

  it('surfaces error over other states', () => {
    expect(resolveSyncChip({
      ...base,
      phase: 'error',
      classifyingInBackground: true,
    })).toBe('error');
  });
});

describe('unwrapApiList', () => {
  it('accepts bare arrays (Priority API)', () => {
    expect(unwrapApiList([{ id: 1 }])).toEqual([{ id: 1 }]);
  });

  it('unwraps { data: [] } envelopes', () => {
    expect(unwrapApiList({ data: [{ id: 2 }] })).toEqual([{ id: 2 }]);
  });

  it('returns empty for nullish', () => {
    expect(unwrapApiList(null)).toEqual([]);
    expect(unwrapApiList(undefined)).toEqual([]);
    expect(unwrapApiList({})).toEqual([]);
  });
});

describe('getVisibleInboxDivisions', () => {
  it('uses universal task/opportunity labels', () => {
    const tabs = getVisibleInboxDivisions(undefined, {
      ASSIGNMENT: 2,
      PLACEMENT: 1,
    });
    expect(tabs.find((t) => t.key === 'ASSIGNMENT')?.label).toBe('Tasks');
    expect(tabs.find((t) => t.key === 'PLACEMENT')?.label).toBe('Opportunities');
  });
});
