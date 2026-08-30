import React from 'react';
import { CAT_COLORS } from '../../utils/catColors';

interface CategoryTagProps {
  category: string;
}

/** Cortex category badge — compact, moderate saturation. */
export const CategoryTag: React.FC<CategoryTagProps> = ({ category }) => {
  const cfg = CAT_COLORS[category] ?? { label: category, bg: '#202734', text: '#9AA6B2' };
  return (
    <span
      className="cat-badge"
      style={{
        background: cfg.bg,
        color: cfg.text,
      }}
    >
      {cfg.label}
    </span>
  );
};
