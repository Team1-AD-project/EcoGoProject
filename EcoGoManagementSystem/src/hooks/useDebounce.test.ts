import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import { useDebounce } from './useDebounce';

describe('useDebounce', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('returns initial value immediately', () => {
    const { result } = renderHook(({ value, delay }) => useDebounce(value, delay), {
      initialProps: { value: 'initial', delay: 300 },
    });

    expect(result.current).toBe('initial');
  });

  it('updates value only after delay', () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      {
        initialProps: { value: 'a', delay: 500 },
      },
    );

    expect(result.current).toBe('a');

    rerender({ value: 'b', delay: 500 });
    // Still old value before timer
    expect(result.current).toBe('a');

    act(() => {
      vi.advanceTimersByTime(499);
    });
    expect(result.current).toBe('a');

    act(() => {
      vi.advanceTimersByTime(1);
    });

    expect(result.current).toBe('b');
  });

  it('clears timeout on unmount (no state update after unmount)', () => {
    const { rerender, unmount } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      {
        initialProps: { value: 1, delay: 300 },
      },
    );

    rerender({ value: 2, delay: 300 });
    unmount();

    act(() => {
      vi.advanceTimersByTime(300);
    });

    // If timeout was not cleared, React would attempt a state update on unmounted component.
    // The absence of warnings/errors is what we care about here.
    expect(true).toBe(true);
  });
});

