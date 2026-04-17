/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { DashboardHeader } from './components/DashboardHeader';
import { SitacOverview } from './components/SitacOverview';
import { ProjectOverview } from './components/ProjectOverview';
import { LogisticsOverview } from './components/LogisticsOverview';
import { fetchSheetData, MOCK_DATA } from './lib/dataService';
import { DashboardData } from './types';
import { Search, Sheet, AlertCircle } from 'lucide-react';

export default function App() {
  const [data, setData] = useState<DashboardData>(MOCK_DATA);
  const [loading, setLoading] = useState(false);
  const [sheetId, setSheetId] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleFetchData = async () => {
    if (!sheetId) {
       setError('Please enter a Google Sheet ID');
       return;
    }
    
    setLoading(true);
    setError(null);
    try {
      const result = await fetchSheetData(sheetId);
      if (result) {
        setData(result);
      } else {
        setError('Failed to fetch or parse data.');
      }
    } catch (err) {
      setError('An error occurred while fetching data.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#f0f4f8] p-4 md:p-8 font-sans text-gray-900">
      <div className="max-w-7xl mx-auto">
        
        {/* Google Sheet Integration Tooltip/Bar */}
        <div className="mb-6 bg-white p-4 rounded-xl shadow-sm border border-blue-100 flex flex-col md:flex-row gap-4 items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="bg-green-100 p-2 rounded-lg text-green-600">
              <Sheet size={24} />
            </div>
            <div>
              <h3 className="font-bold text-sm">Google Sheets Integration</h3>
              <p className="text-xs text-gray-500">Publish your sheet to web (CSV) and enter ID here</p>
            </div>
          </div>
          
          <div className="flex-1 max-w-md w-full flex gap-2">
            <div className="relative flex-1">
              <input 
                type="text" 
                placeholder="Google Sheet ID..."
                className="w-full pl-10 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                value={sheetId}
                onChange={(e) => setSheetId(e.target.value)}
              />
              <Search className="absolute left-3 top-2.5 text-gray-400" size={18} />
            </div>
            <button 
              onClick={handleFetchData}
              disabled={loading}
              className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg text-sm font-bold transition-all disabled:opacity-50"
            >
              {loading ? 'Fetching...' : 'Connect'}
            </button>
          </div>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-100 p-3 rounded-lg flex items-center gap-3 text-red-600 text-sm animate-in fade-in slide-in-from-top-2">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        <DashboardHeader />

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
          <SitacOverview data={data.sitac} />
          <ProjectOverview data={data.project} />
        </div>

        <LogisticsOverview data={data.logistics} />

        <footer className="mt-12 text-center text-gray-400 text-xs pb-8">
          <p>© 2024 Project Monitoring System • All Rights Reserved</p>
          <p className="mt-1 uppercase tracking-widest font-bold">Confidential Data</p>
        </footer>
      </div>
    </div>
  );
}
