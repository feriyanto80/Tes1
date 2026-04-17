/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';

interface SummaryTileProps {
  label: string;
  value: number | string;
  color?: string;
  className?: string;
}

export function SummaryTile({ label, value, color = 'text-blue-600', className = '' }: SummaryTileProps) {
  return (
    <div className={`flex flex-col items-center justify-center p-2 border-r last:border-r-0 border-gray-100 ${className}`}>
      <span className="text-[10px] uppercase font-semibold text-gray-500 mb-1">{label}</span>
      <span className={`text-lg font-bold ${color}`}>{value}</span>
    </div>
  );
}

interface ProgressBarProps {
  current: number;
  total: number;
  color?: string;
  secondaryColor?: string;
  className?: string;
  showLabels?: boolean;
}

export function ProgressBar({ 
  current, 
  total, 
  color = 'bg-green-500', 
  secondaryColor = 'bg-gray-100',
  className = '',
  showLabels = false
}: ProgressBarProps) {
  const percentage = Math.min(Math.max((current / total) * 100, 0), 100);
  
  return (
    <div className={`w-full ${className}`}>
      {showLabels && (
        <div className="flex justify-between text-[10px] mb-1 font-medium text-gray-400">
          <span>{current}</span>
          <span>{total}</span>
        </div>
      )}
      <div className={`h-2.5 w-full ${secondaryColor} rounded-full overflow-hidden flex`}>
        <div 
          className={`h-full ${color} transition-all duration-500 ease-out`} 
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
}
