/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { ClipboardList } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area } from 'recharts';
import { ProgressBar } from './Common';
import { DashboardData } from '../types';

interface ProjectOverviewProps {
  data: DashboardData['project'];
}

export function ProjectOverview({ data }: ProjectOverviewProps) {
  return (
    <div className="flex flex-col gap-4">
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        {/* Header */}
        <div className="bg-[#1e5dab] text-white p-3 flex justify-between items-center">
          <div className="flex items-center gap-2">
            <div className="bg-white/20 p-1 rounded">
              <ClipboardList size={18} />
            </div>
            <h2 className="font-bold uppercase tracking-tight text-sm">Project Overview</h2>
          </div>
          <span className="text-[10px] opacity-70">Updated: April 25, 2024, 08:00</span>
        </div>

        <div className="p-4">
          <div className="flex justify-between items-baseline mb-4">
            <h3 className="font-bold text-gray-700 tracking-tight">Daily Progress</h3>
            <div className="flex items-baseline gap-2">
              <span className="text-2xl font-black text-blue-900">{data.dailyProgress}</span>
              <span className="text-xs font-bold text-gray-400 font-mono italic">Jobs Selesai</span>
            </div>
          </div>

          <div className="bg-gray-50 p-3 rounded-xl mb-6">
            {data.progressByKota.map((city, i) => (
              <div key={city.name} className="mb-3 last:mb-0">
                <div className="flex justify-between text-[10px] font-bold text-gray-400 mb-1">
                  <span>{city.name}</span>
                  <div className="flex gap-4">
                    <span>Target Cleared</span>
                    <span className="text-red-400">On Progress</span>
                    <span>Pending</span>
                    <span>Target</span>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-[10px] font-bold text-gray-600 w-16">{city.name}</span>
                  <div className="flex-1 h-3 bg-white rounded-full overflow-hidden flex shadow-inner">
                    <div className="h-full bg-green-500" style={{ width: '40%' }} />
                    <div className="h-full bg-orange-400" style={{ width: '25%' }} />
                    <div className="h-full bg-red-400" style={{ width: '15%' }} />
                  </div>
                  <span className="text-[10px] font-bold text-blue-800 w-10 text-right">{data.dailyTarget}</span>
                </div>
              </div>
            ))}
          </div>

          <h3 className="font-bold text-gray-700 mb-2 tracking-tight">Timeline Progress</h3>
          <div className="h-40 mb-6 bg-gray-50 rounded-xl p-2">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={data.timeline}>
                <defs>
                  <linearGradient id="colorProgress" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.1}/>
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5e7eb" />
                <XAxis 
                  dataKey="date" 
                  axisLine={false} 
                  tickLine={false} 
                  tick={{ fontSize: 10, fill: '#9ca3af' }} 
                />
                <YAxis hide />
                <Tooltip />
                <Area 
                  type="monotone" 
                  dataKey="progress" 
                  stroke="#3b82f6" 
                  strokeWidth={2}
                  fillOpacity={1} 
                  fill="url(#colorProgress)" 
                />
                <Area 
                  type="monotone" 
                  dataKey="target" 
                  stroke="#fbbf24" 
                  strokeWidth={2}
                  fill="transparent"
                  strokeDasharray="5 5" 
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          <h3 className="font-bold text-gray-700 mb-4 tracking-tight">Progress By Kota</h3>
          <div className="flex flex-col gap-4 mb-8">
            {data.progressByKota.map((city) => (
              <div key={city.name} className="flex flex-col gap-1">
                <div className="flex justify-between items-center mb-1">
                  <span className="text-xs font-bold text-gray-600 uppercase">{city.name}</span>
                  <div className="flex gap-4 text-[10px] font-bold">
                    <span className="text-gray-400">Cleared <span className="text-blue-900 ml-1">{city.cleared} / {city.target}</span></span>
                    <span className="text-orange-400">Padala <span className="text-blue-900 ml-1">{city.onProgress}</span></span>
                  </div>
                </div>
                <div className="h-4 w-full bg-gray-100 rounded-sm overflow-hidden flex">
                  <div className="h-full bg-green-600/80" style={{ width: `${(city.cleared / (city.cleared + city.onProgress)) * 100}%` }} />
                  <div className="h-full bg-blue-600/80" style={{ width: '20%' }} />
                  <div className="h-full bg-yellow-400/80" style={{ width: '10%' }} />
                </div>
              </div>
            ))}
          </div>

          <TeamSection title="KPI Tim Mandor" teams={data.kpiMandor} />
          <div className="mt-6">
            <TeamSection title="KPI Tim Jointer" teams={data.kpiJointer} />
          </div>

        </div>
      </div>
    </div>
  );
}

function TeamSection({ title, teams }: { title: string, teams: any[] }) {
  return (
    <div className="flex flex-col gap-4">
      <h3 className="font-bold text-gray-700 tracking-tight">{title}</h3>
      <div className="flex flex-col gap-3">
        {teams.map((team) => (
          <div key={team.name} className="flex items-center gap-3">
            <span className="text-[11px] font-bold text-gray-600 w-16 truncate">{team.name}</span>
            <span className="text-[11px] font-black text-blue-900 w-12 text-right">{team.hp}/{team.target}</span>
            <div className="flex-1 h-3.5 bg-gray-100 rounded-sm overflow-hidden flex">
              <div className="h-full bg-green-600/80" style={{ width: `${60 - Math.random() * 20}%` }} />
              <div className="h-full bg-green-500/40" style={{ width: `${20}%` }} />
              <div className="h-full bg-yellow-400/60" style={{ width: `${10}%` }} />
            </div>
            <span className="text-[10px] font-black text-gray-400 w-8 text-right bg-gray-100 px-1 rounded">{team.bung}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
