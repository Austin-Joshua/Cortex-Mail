import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { emailApi } from '../../api/emailApi';

/* ─── types ─── */
interface DayCell {
  dateStr: string;
  count: number;
  dayOfWeek: number; // 0=Sun … 6=Sat
  month: number;
  isToday: boolean;
}

/* ─── helpers ─── */
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const DAY_LABELS = ['', 'Mon', '', 'Wed', '', 'Fri', ''];

const TOTAL_WEEKS = 12;
const TOTAL_DAYS = TOTAL_WEEKS * 7;

function buildGrid(emails: Array<{ receivedAt?: string }>): DayCell[] {
  // Count emails per day
  const counts = new Map<string, number>();
  emails.forEach((e) => {
    if (!e.receivedAt) return;
    const d = e.receivedAt.split('T')[0];
    counts.set(d, (counts.get(d) ?? 0) + 1);
  });

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const todayStr = today.toISOString().split('T')[0];

  // Align to start of week (Sunday)
  const endOfGrid = new Date(today);
  const startOfGrid = new Date(today);
  startOfGrid.setDate(startOfGrid.getDate() - TOTAL_DAYS + 1);
  // Move back to previous Sunday
  const offset = startOfGrid.getDay();
  startOfGrid.setDate(startOfGrid.getDate() - offset);

  const totalCells = Math.ceil(
    (endOfGrid.getTime() - startOfGrid.getTime()) / (1000 * 60 * 60 * 24)
  ) + 1;

  const result: DayCell[] = [];
  for (let i = 0; i < totalCells; i++) {
    const d = new Date(startOfGrid);
    d.setDate(d.getDate() + i);
    const ds = d.toISOString().split('T')[0];
    result.push({
      dateStr: ds,
      count: counts.get(ds) ?? 0,
      dayOfWeek: d.getDay(),
      month: d.getMonth(),
      isToday: ds === todayStr,
    });
  }
  return result;
}

function getIntensity(count: number, max: number): number {
  if (count === 0 || max === 0) return 0;
  const ratio = count / max;
  if (ratio <= 0.25) return 1;
  if (ratio <= 0.5) return 2;
  if (ratio <= 0.75) return 3;
  return 4;
}

const INTENSITY_COLORS = [
  'var(--surface-2)',                  // 0: no activity
  'rgba(79, 70, 229, 0.20)',          // 1: low
  'rgba(79, 70, 229, 0.40)',          // 2: medium-low
  'rgba(79, 70, 229, 0.65)',          // 3: medium-high
  'var(--accent)',                    // 4: high
];

/* ─── component ─── */
export const EmailVolumeHeatmap: React.FC = () => {
  const { data: emailPage } = useQuery({
    queryKey: ['emails', { size: 500 }],
    queryFn: () => emailApi.getEmails({ size: 500 }),
    staleTime: 300_000,
  });

  // Memoised so the `?? []` fallback does not mint a new array each render,
  // which would make the grid rebuild on every paint.
  const emails = React.useMemo(() => emailPage?.content ?? [], [emailPage]);

  const grid = React.useMemo(() => buildGrid(emails), [emails]);
  const maxCount = React.useMemo(() => Math.max(1, ...grid.map((d) => d.count)), [grid]);
  const totalEmails = React.useMemo(() => grid.reduce((s, d) => s + d.count, 0), [grid]);

  // Build weeks (columns)
  const weeks: DayCell[][] = [];
  let currentWeek: DayCell[] = [];
  grid.forEach((day) => {
    if (day.dayOfWeek === 0 && currentWeek.length > 0) {
      weeks.push(currentWeek);
      currentWeek = [];
    }
    currentWeek.push(day);
  });
  if (currentWeek.length > 0) weeks.push(currentWeek);

  // Month headers (first Sunday of each new month)
  const monthHeaders: { label: string; colStart: number }[] = [];
  let lastMonth = -1;
  weeks.forEach((week, wIdx) => {
    const firstDay = week[0];
    if (firstDay && firstDay.month !== lastMonth) {
      monthHeaders.push({ label: MONTHS[firstDay.month], colStart: wIdx });
      lastMonth = firstDay.month;
    }
  });

  const [tooltip, setTooltip] = React.useState<{ x: number; y: number; text: string } | null>(null);

  return (
    <div className="card-paper" style={{ padding: '24px 24px 20px' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
        <div>
          <span className="section-label">EMAIL ACTIVITY</span>
          <p style={{ fontSize: 13, color: 'var(--text-2)', margin: '4px 0 0' }}>
            <strong style={{ color: 'var(--text-1)' }}>{totalEmails}</strong> emails in the last {TOTAL_WEEKS} weeks
          </p>
        </div>
        {/* Legend */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11, color: 'var(--text-3)' }}>
          <span>Less</span>
          {INTENSITY_COLORS.map((bg, i) => (
            <div
              key={i}
              style={{
                width: 12,
                height: 12,
                borderRadius: 3,
                background: bg,
                border: i === 0 ? '1px solid var(--border)' : 'none',
              }}
            />
          ))}
          <span>More</span>
        </div>
      </div>

      {/* Grid */}
      <div style={{ overflowX: 'auto', paddingBottom: 4, position: 'relative' }}>
        <div style={{ display: 'flex', gap: 0 }}>
          {/* Day labels column */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 3, paddingRight: 8, paddingTop: 20 }}>
            {DAY_LABELS.map((label, i) => (
              <div
                key={i}
                style={{
                  height: 14,
                  fontSize: 10,
                  color: 'var(--text-3)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'flex-end',
                  width: 28,
                  fontWeight: 500,
                }}
              >
                {label}
              </div>
            ))}
          </div>

          {/* Weeks grid */}
          <div style={{ position: 'relative', flex: 1 }}>
            {/* Month labels */}
            <div style={{ display: 'flex', height: 16, marginBottom: 4 }}>
              {monthHeaders.map((mh, i) => {
                const nextStart = monthHeaders[i + 1]?.colStart ?? weeks.length;
                const span = nextStart - mh.colStart;
                return (
                  <div
                    key={mh.label + mh.colStart}
                    style={{
                      width: span * 17,
                      fontSize: 10,
                      fontWeight: 600,
                      color: 'var(--text-3)',
                      flexShrink: 0,
                    }}
                  >
                    {span >= 2 ? mh.label : ''}
                  </div>
                );
              })}
            </div>

            {/* Actual heatmap cells */}
            <div style={{ display: 'flex', gap: 3 }}>
              {weeks.map((week, wIdx) => (
                <div key={wIdx} style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                  {Array.from({ length: 7 }).map((_, dayIdx) => {
                    const cell = week.find((c) => c.dayOfWeek === dayIdx);
                    if (!cell) {
                      return <div key={dayIdx} style={{ width: 14, height: 14 }} />;
                    }
                    const intensity = getIntensity(cell.count, maxCount);
                    return (
                      <div
                        key={dayIdx}
                        style={{
                          width: 14,
                          height: 14,
                          borderRadius: 3,
                          background: INTENSITY_COLORS[intensity],
                          border: cell.isToday ? '2px solid var(--accent)' : intensity === 0 ? '1px solid var(--border)' : 'none',
                          cursor: 'pointer',
                          transition: 'transform 0.12s ease, box-shadow 0.12s ease',
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.transform = 'scale(1.4)';
                          e.currentTarget.style.boxShadow = '0 2px 8px rgba(79, 70, 229, 0.25)';
                          const rect = e.currentTarget.getBoundingClientRect();
                          setTooltip({
                            x: rect.left + rect.width / 2,
                            y: rect.top - 8,
                            text: `${cell.count} email${cell.count !== 1 ? 's' : ''} on ${new Date(cell.dateStr + 'T00:00').toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })}`,
                          });
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.transform = 'scale(1)';
                          e.currentTarget.style.boxShadow = 'none';
                          setTooltip(null);
                        }}
                      />
                    );
                  })}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Tooltip */}
        {tooltip && (
          <div
            style={{
              position: 'fixed',
              left: tooltip.x,
              top: tooltip.y,
              transform: 'translate(-50%, -100%)',
              background: 'var(--text-1)',
              color: 'var(--bg)',
              fontSize: 11,
              fontWeight: 600,
              padding: '5px 10px',
              borderRadius: 6,
              whiteSpace: 'nowrap',
              pointerEvents: 'none',
              zIndex: 9999,
              boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            }}
          >
            {tooltip.text}
          </div>
        )}
      </div>
    </div>
  );
};
