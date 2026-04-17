/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export interface DashboardData {
  sitac: {
    target: number;
    cleared: number;
    onProgress: number;
    block: number;
    statusIzin: { name: string; value: number; color: string }[];
    rankingSales: TeamKPI[];
  };
  project: {
    dailyProgress: number;
    dailyTarget: number;
    timeline: { date: string; progress: number; target: number }[];
    progressByKota: CityProgress[];
    kpiMandor: TeamKPI[];
    kpiJointer: TeamKPI[];
  };
  logistics: {
    materialStatus: CityMaterialStatus[];
    progressByKota: CityProgress[];
    hubVsImp: LogisticComparison[];
  };
}

export interface TeamKPI {
  name: string;
  hp: number;
  target: number;
  bung: number;
  progress: number;
}

export interface CityProgress {
  name: string;
  cleared: number;
  target: number;
  onProgress: number;
  pending?: number;
}

export interface CityMaterialStatus {
  name: string;
  target: number;
  cleared: number;
  onProgress: number;
  pending: number;
}

export interface LogisticComparison {
  name: string;
  current: number;
  target: number;
  percentage: number;
}
