/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { Package, TrendingUp } from 'lucide-react';
import { DashboardData } from '../types';

interface LogisticsOverviewProps {
  data: DashboardData['logistics'];
}

export function LogisticsOverview({ data }: LogisticsOverviewProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
      {/* Logistik Overview */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="bg-[#1e8a44] text-white p-3 flex justify-between items-center">
          <div className="flex items-center gap-2">
            <div className="bg-white/20 p-1 rounded">
              <Package size={18} />
            </div>
            <h2 className="font-bold uppercase tracking-tight text-sm">Logistik Overview</h2>
          </div>
          <span className="text-[10px] opacity-70 italic">Updated: April 25, 2024, 20:20</span>
        </div>

        <div className="p-4">
          <h3 className="font-bold text-gray-700 mb-4 tracking-tight leading-4">
            Status Material Barang Masuk dan Keluar<br/>
            <span className="text-gray-400 font-medium">Internal Gudang Hub</span>
          </h3>

          <div className="mb-8">
            <div className="flex justify-end gap-10 text-[10px] font-bold text-gray-400 uppercase px-4 mb-2">
              <span>Target</span>
              <span>Cleared</span>
              <span>On Progress</span>
              <span>Pending</span>
            </div>
            <div className="flex flex-col gap-4 bg-gray-50 p-4 rounded-xl shadow-inner">
              {data.materialStatus.map((city) => (
                <div key={city.name} className="flex items-center gap-3">
                  <span className="text-[11px] font-bold text-gray-600 w-20">{city.name}</span>
                  <div className="flex-1 h-3.5 bg-white rounded-full overflow-hidden flex shadow-sm border border-gray-100">
                    <div className="h-full bg-green-500" style={{ width: '45%' }} />
                    <div className="h-full bg-blue-500" style={{ width: '25%' }} />
                    <div className="h-full bg-red-400" style={{ width: '15%' }} />
                  </div>
                  <span className="text-[11px] font-extrabold text-blue-900 w-8 text-right">{city.pending}</span>
                </div>
              ))}
            </div>
          </div>

          <h3 className="font-bold text-gray-700 mb-4 tracking-tight">Progress By Kota</h3>
          <div className="flex flex-col gap-4">
            {data.progressByKota.map((city) => (
              <div key={city.name} className="flex flex-col gap-1">
                <div className="flex justify-between items-end">
                  <span className="text-[11px] font-bold text-gray-700 uppercase">{city.name}</span>
                  <span className="text-[10px] font-bold text-gray-400">{city.target}</span>
                </div>
                <div className="h-4 w-full bg-gray-100 rounded-sm overflow-hidden flex">
                  <div className="h-full bg-green-600/80 flex items-center px-4" style={{ width: `${(city.cleared / city.target) * 100}%` }}>
                    <span className="text-white text-[9px] font-black">{city.cleared}</span>
                  </div>
                </div>
              </div>
            ))}
            
            <h4 className="font-bold text-gray-700 mt-4 mb-2 text-sm">Nganjuk</h4>
            <div className="flex flex-col gap-4">
              <CitySubRow label="Petoraiko" val={122} target={266} color="bg-green-600/60" />
              <CitySubRow label="Bagor" val={190} target={280} color="bg-green-600/60" />
            </div>
          </div>
        </div>
      </div>

      {/* Logistik vs Progress IMP */}
      <div className="flex flex-col gap-6">
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="bg-[#1e8a44] text-white p-3">
            <div className="flex items-center gap-2">
              <div className="bg-white/20 p-1 rounded">
                <TrendingUp size={18} />
              </div>
              <h2 className="font-bold uppercase tracking-tight text-sm">Logistik vs Progress IMP</h2>
            </div>
          </div>
          <div className="p-4">
             <div className="flex flex-col gap-4 mb-8">
               {data.hubVsImp.map((item) => (
                 <div key={item.name} className="flex items-center gap-4">
                   <span className="text-md font-bold text-blue-900 w-24">{item.name}</span>
                   <span className="text-md font-bold text-blue-900 w-24 whitespace-nowrap">
                     {item.current} <span className="text-[10px] text-gray-400 font-medium">dari {item.target}</span>
                   </span>
                   <div className="flex-1 flex gap-1 h-5">
                      <div className="flex-1 bg-green-600/80 rounded-sm flex items-center justify-between px-2">
                        <span className="text-white text-[10px] font-black">330</span>
                        <div className="w-1 h-3 border-r border-white/50"/>
                      </div>
                      <div className="w-16 bg-yellow-400/80 rounded-sm flex items-center justify-center">
                        <span className="text-black text-[10px] font-black">200</span>
                      </div>
                   </div>
                 </div>
               ))}
             </div>

             <h3 className="font-bold text-gray-700 mb-4 tracking-tight">Logistik vs Progress IMP</h3>
             <div className="flex flex-col gap-4 mb-8">
               {data.hubVsImp.map((item, i) => (
                 <div key={`alt-${item.name}`} className="flex items-center gap-4">
                    <span className="text-md font-bold text-blue-900 w-24">{item.name}</span>
                    <span className="text-md font-bold text-blue-900 w-24 text-[10px] text-gray-400">
                      {item.current} / {item.target}
                    </span>
                    <div className="flex-1 flex gap-1 h-5">
                      <div className="flex-1 bg-green-700/80 rounded-sm flex items-center justify-between px-4">
                        <span className="text-white text-[10px] font-black">{i === 0 ? '9245' : i === 1 ? '110' : '152'}</span>
                      </div>
                      <div className="w-16 bg-green-500/30 rounded-sm flex items-center justify-center">
                        <span className="text-green-800 text-[10px] font-black font-mono">{i === 0 ? '880' : i === 1 ? '111' : '79'}</span>
                      </div>
                    </div>
                 </div>
               ))}
             </div>

             <h3 className="font-bold text-gray-700 mb-4 tracking-tight uppercase">Kertosono</h3>
             <div className="flex flex-col gap-4">
               <CitySubRow label="Bandarkedungmulyo" val={280} target={320} color="bg-green-600/80" />
               <CitySubRow label="Garti" val={180} target={239} color="bg-green-600/80" />
               <CitySubRow label="Gondangmanis" val={100} target={195} color="bg-green-600/80" />
             </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function CitySubRow({ label, val, target, color }: { label: string, val: number, target: number, color: string }) {
  return (
    <div className="flex flex-col gap-1">
      <div className="flex justify-between items-end">
        <span className="text-[11px] font-medium text-gray-500">{label}</span>
        <span className="text-[10px] font-bold text-gray-400">{target}</span>
      </div>
      <div className="h-4 w-full bg-gray-100 rounded-sm overflow-hidden flex">
        <div className={`h-full ${color} flex items-center px-4`} style={{ width: `${(val / target) * 100}%` }}>
          <span className="text-white text-[9px] font-black">{val}</span>
        </div>
      </div>
    </div>
  );
}
