/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { LayoutDashboard, Calendar, MapPin, RadioTower, Activity, ChevronDown } from 'lucide-react';

export function DashboardHeader() {
  return (
    <div className="bg-[#1e5dab] text-white p-4 rounded-xl shadow-lg mb-6">
      <div className="flex justify-between items-center mb-4">
        <div className="flex items-center gap-3">
          <div className="bg-white/20 p-2 rounded-lg">
            <LayoutDashboard size={24} />
          </div>
          <div>
            <h1 className="text-xl font-bold uppercase tracking-tight">Overview</h1>
            <p className="text-[10px] opacity-70">Updated: April 25, 2024, 08:00</p>
          </div>
        </div>
        <div className="text-right">
          <h2 className="text-2xl font-bold">Sitac Overview</h2>
          <p className="text-[10px] opacity-70 uppercase">updated: April 25, 2024, 08:00</p>
        </div>
      </div>
      
      <div className="bg-white/10 backdrop-blur-sm p-3 rounded-xl flex flex-wrap gap-3 items-center">
        <div className="flex items-center gap-2 bg-white text-gray-800 px-3 py-1.5 rounded-lg text-sm font-medium">
          <span className="text-gray-400">Tanggal</span>
          <span className="border-l pl-2 border-gray-200">April 1 - 24, 2024</span>
          <ChevronDown size={16} className="text-blue-500" />
        </div>
        
        <FilterDropdown icon={<MapPin size={14} />} label="Filter Area" />
        <FilterDropdown icon={<MapPin size={14} />} label="Filter Area" value="Jombang" />
        <FilterDropdown icon={<RadioTower size={14} />} label="Filter Stasiun" />
        
        <button className="ml-auto bg-orange-500 hover:bg-orange-600 px-6 py-2 rounded-lg font-bold flex items-center gap-2 transition-colors">
          Status <ChevronDown size={18} />
        </button>
      </div>
    </div>
  );
}

interface FilterDropdownProps {
  label: string;
  value?: string;
  icon?: React.ReactNode;
}

function FilterDropdown({ label, value, icon }: FilterDropdownProps) {
  return (
    <div className="flex items-center gap-2 bg-white/20 hover:bg-white/30 px-3 py-1.5 rounded-lg text-sm font-medium cursor-pointer transition-colors">
      {icon && <span className="opacity-70">{icon}</span>}
      <span className="opacity-70">{label}</span>
      {value && <span className="border-l border-white/20 pl-2">{value}</span>}
      <ChevronDown size={14} className="opacity-70" />
    </div>
  );
}
