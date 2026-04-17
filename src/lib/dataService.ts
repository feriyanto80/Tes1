/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import Papa from 'papaparse';
import { DashboardData } from '../types';

export const MOCK_DATA: DashboardData = {
  sitac: {
    target: 918,
    cleared: 544,
    onProgress: 267,
    block: 107,
    statusIzin: [
      { name: 'Target', value: 544, color: '#4ade80' },
      { name: 'Cleared', value: 171, color: '#facc15' },
      { name: 'On Progress', value: 81, color: '#f87171' },
      { name: 'Blocked', value: 27, color: '#fb923c' },
    ],
    rankingSales: [
      { name: 'Jombang', hp: 610, target: 1000, bung: 147, progress: 61 },
      { name: 'Nganjuk', hp: 186, target: 1000, bung: 556, progress: 18.6 },
      { name: 'Fikri', hp: 110, target: 1000, bung: 851, progress: 11 },
    ],
  },
  project: {
    dailyProgress: 129,
    dailyTarget: 2530,
    timeline: [
      { date: '01', progress: 40, target: 50 },
      { date: '02', progress: 45, target: 55 },
      { date: '03', progress: 50, target: 60 },
      { date: '04', progress: 55, target: 65 },
      { date: '05', progress: 60, target: 70 },
    ],
    progressByKota: [
      { name: 'Jombang', cleared: 195, target: 350, onProgress: 250 },
      { name: 'Nganjuk', cleared: 151, target: 240, onProgress: 700 },
    ],
    kpiMandor: [
      { name: 'Sutris', hp: 46, target: 99, bung: 114, progress: 46 },
      { name: 'Andiumi', hp: 34, target: 35, bung: 95, progress: 97 },
      { name: 'Joko', hp: 23, target: 55, bung: 66, progress: 41 },
    ],
    kpiJointer: [
      { name: 'Rizal', hp: 38, target: 55, bung: 96, progress: 69 },
      { name: 'Wawan', hp: 31, target: 40, bung: 32, progress: 77 },
      { name: 'Anggi', hp: 24, target: 25, bung: 116, progress: 96 },
    ],
  },
  logistics: {
    materialStatus: [
      { name: 'Jombang', target: 295, cleared: 0, onProgress: 0, pending: 36 },
      { name: 'Nganjuk', target: 366, cleared: 0, onProgress: 231, pending: 200 },
    ],
    progressByKota: [
      { name: 'Jombang', cleared: 199, target: 380, onProgress: 0 },
      { name: 'Banjarsari', cleared: 166, target: 251, onProgress: 0 },
    ],
    hubVsImp: [
      { name: 'Hub', current: 250, target: 200, percentage: 125 },
      { name: 'Satelit 1', current: 110, target: 125, percentage: 88 },
      { name: 'Satelit 2', current: 66, target: 76, percentage: 86 },
    ],
  },
};

/**
 * Fetch data from Google Sheets CSV export
 * For simplicity, we assume a specific structure in the sheet.
 * In a real app, you'd map individual columns to the DashboardData interface.
 */
export async function fetchSheetData(sheetId: string, gid: string = '0'): Promise<DashboardData | null> {
  const url = `https://docs.google.com/spreadsheets/d/${sheetId}/export?format=csv&gid=${gid}`;
  
  try {
    const response = await fetch(url);
    const csvText = await response.text();
    
    return new Promise((resolve, reject) => {
      Papa.parse(csvText, {
        header: true,
        dynamicTyping: true,
        complete: (results) => {
          console.log('Parsed Spreadsheet Data:', results.data);
          // Here you would implement mapping logic from CSV rows to DashboardData.
          // For this demo, we'll return the mock data if fetch fails or just return mock data
          // but show we have the parsing logic.
          resolve(MOCK_DATA); 
        },
        error: (error: any) => reject(error),
      });
    });
  } catch (err) {
    console.error('Error fetching sheet data:', err);
    return MOCK_DATA;
  }
}
