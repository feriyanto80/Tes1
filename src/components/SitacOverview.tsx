/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { Ship, ChevronRight } from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer, Label } from 'recharts';
import { SummaryTile } from './Common';
import { DashboardData } from '../types';

interface SitacOverviewProps {
  data: DashboardData['sitac'];
}

export function SitacOverview({ data }: SitacOverviewProps) {
  const chartData = data.statusIzin;
  const total = chartData.reduce((acc, curr) => acc + curr.value, 0);
  const clearedRate = Math.round((chartData.find(d => d.name === 'Target')?.value || 0) / data.target * 100);

  return (
    <div className="flex flex-col gap-4">
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        {/* Header */}
        <div className="bg-[#1e5dab] text-white p-3 flex items-center gap-2">
          <div className="bg-white/20 p-1 rounded">
            <Ship size={18} />
          </div>
          <h2 className="font-bold uppercase tracking-tight text-sm">Sitac Overview</h2>
        </div>

        <div className="p-4">
          <h3 className="font-bold text-gray-700 mb-4 tracking-tight">The Pipeline By Kota & Stasiun</h3>
          
          <div className="grid grid-cols-4 bg-gray-50 rounded-xl mb-6">
            <SummaryTile label="Target" value={data.target} />
            <SummaryTile label="Cleared" value={data.cleared} />
            <SummaryTile label="On Progress" value={data.onProgress} />
            <SummaryTile label="Block" value={data.block} color="text-red-500" />
          </div>

          <h3 className="font-bold text-gray-700 mb-4 tracking-tight">Status Izin Per Kota</h3>
          
          <div className="flex items-center gap-4 mb-6">
            <div className="w-1/2">
              <div className="flex flex-col gap-2">
                {chartData.map((item) => (
                  <div key={item.name} className="flex justify-between items-center text-xs">
                    <div className="flex items-center gap-2">
                      <div className="w-2.5 h-2.5 rounded-sm" style={{ backgroundColor: item.color }} />
                      <span className="text-gray-600 font-medium">{item.name}</span>
                    </div>
                    <span className="font-bold text-blue-900">{item.value}</span>
                  </div>
                ))}
              </div>
            </div>
            
            <div className="w-1/2 h-32 relative">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={chartData}
                    cx="50%"
                    cy="50%"
                    innerRadius={35}
                    outerRadius={50}
                    paddingAngle={2}
                    dataKey="value"
                  >
                    {chartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                    <Label
                      value={`${clearedRate}%`}
                      position="center"
                      className="text-xl font-bold fill-gray-800"
                    />
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
              <div className="absolute -bottom-2 w-full text-center">
                <span className="bg-green-100 text-green-700 text-[10px] font-bold px-2 py-0.5 rounded-full">
                  85%
                </span>
              </div>
            </div>
          </div>

          <div className="flex justify-center gap-4 text-[10px] font-bold text-gray-400 uppercase mb-6">
            <span className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-green-500"/> Cleared</span>
            <span className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-orange-400"/> On Progress</span>
            <span className="flex items-center gap-1"><div className="w-2 h-2 rounded-full bg-red-400"/> Block</span>
          </div>

          <h3 className="font-bold text-gray-700 mb-4 tracking-tight">Ranking KPI Tim Sales</h3>
          
          <div className="flex flex-col gap-3">
            <div className="flex justify-between text-[10px] font-bold text-gray-400 uppercase px-1">
              <span>Jombang</span>
              <div className="flex gap-6 mr-10">
                <span>HP</span>
                <span>Target</span>
                <span>Bung {'>'}</span>
              </div>
            </div>
            {data.rankingSales.map((team) => (
              <div key={team.name} className="flex items-center gap-3">
                <span className="text-xs font-bold text-blue-900 w-20 truncate">{team.name}</span>
                <span className="text-xs font-bold text-blue-700 w-8">{team.hp}</span>
                <div className="flex-1 h-3.5 bg-gray-100 rounded-sm overflow-hidden flex">
                  <div className="h-full bg-green-500" style={{ width: `${team.progress}%` }} />
                  <div className="h-full bg-orange-400" style={{ width: `${Math.random() * 20}%` }} />
                  <div className="h-full bg-red-400" style={{ width: `${Math.random() * 15}%` }} />
                </div>
                <span className="text-[10px] font-bold text-red-500 w-8 text-right">{team.bung}</span>
              </div>
            ))}
          </div>

          {/* Repeat for other team types if data exists, for now just one duplicate to match visual */}
          <div className="mt-8">
            <h3 className="font-bold text-gray-700 mb-4 tracking-tight">Ranking KPI Tim Sales</h3>
            <div className="flex flex-col gap-3">
              {/* Using same data for layout consistency */}
              {data.rankingSales.map((team, i) => (
                <div key={`${team.name}-${i}`} className="flex items-center gap-3">
                 <span className="text-xs font-bold text-blue-900 w-20 truncate">{i === 0 ? 'Vivan' : i === 1 ? 'Rahmat' : 'Andri'}</span>
                 <div className="flex gap-2">
                   <span className="text-[10px] font-bold text-blue-700">{team.hp}</span>
                   <span className="text-[10px] font-bold text-gray-400 leading-none">94</span>
                 </div>
                 <div className="flex-1 h-3.5 bg-gray-100 rounded-sm overflow-hidden flex">
                    <div className="h-full bg-green-500" style={{ width: `${70 - i * 10}%` }} />
                    <div className="h-full bg-orange-400" style={{ width: `${15}%` }} />
                    <div className="h-full bg-red-400" style={{ width: `${15}%` }} />
                  </div>
                  <span className="text-[10px] font-bold text-gray-400 w-8 text-right">90</span>
                </div>
              ))}
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
