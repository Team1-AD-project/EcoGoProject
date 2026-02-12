import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { BarChart3, TrendingUp, Users, Leaf, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { getAnalyticsSummary, type AnalyticsSummary } from '@/api/statisticsApi';

const TRANSPORT_COLORS: Record<string, string> = {
  Bicycle: 'bg-green-500',
  PublicBus: 'bg-blue-500',
  Walking: 'bg-purple-500',
  Carpool: 'bg-orange-500',
  EScooter: 'bg-yellow-500',
  default: 'bg-gray-400',
};

export function DataAnalytics() {
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [timePeriod, setTimePeriod] = useState('7d');

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await getAnalyticsSummary();
        setSummary(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load analytics data.');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const activeUsers = timePeriod === '7d' ? summary?.activeUsers7d : summary?.activeUsers30d;

  const transportData = summary ? Object.entries(summary.transportDistribution).map(([mode, count]) => ({
    mode,
    count,
    percentage: (count / Object.values(summary.transportDistribution).reduce((a, b) => a + b, 0)) * 100,
  })).sort((a,b) => b.count - a.count) : [];

  if (loading) {
    return <div className="h-full flex items-center justify-center"><Loader2 className="size-8 animate-spin text-blue-600"/></div>;
  }

  if (error) {
    return <div className="h-full flex items-center justify-center text-red-600">Error: {error}</div>;
  }

  if (!summary) {
    return <div className="h-full flex items-center justify-center">No data available.</div>;
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div><h2 className="text-2xl font-bold text-red-500">TESTING: IF YOU SEE THIS, IT IS THE CORRECT FILE</h2><p className="text-gray-600 mt-1">Please confirm if the title has changed.</p></div>
        <div className="flex items-center gap-2">
          <Select value={timePeriod} onValueChange={setTimePeriod}>
            <SelectTrigger className="w-40"><SelectValue /></SelectTrigger>
            <SelectContent><SelectItem value="7d">Last 7 Days</SelectItem><SelectItem value="30d">Last 30 Days</SelectItem></SelectContent>
          </Select>
          <Button variant="outline">Export Report</Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card><CardContent className="pt-6"><div className="flex items-center justify-between"><div><p className="text-sm text-gray-600">Active Users</p><p className="text-2xl font-bold mt-1">{activeUsers?.toLocaleString()}</p></div><div className="size-12 bg-blue-100 rounded-lg flex items-center justify-center"><Users className="size-6 text-blue-600" /></div></div></CardContent></Card>
        <Card><CardContent className="pt-6"><div className="flex items-center justify-between"><div><p className="text-sm text-gray-600">Total Trips</p><p className="text-2xl font-bold mt-1">{summary.totalTrips.toLocaleString()}</p></div><div className="size-12 bg-purple-100 rounded-lg flex items-center justify-center"><TrendingUp className="size-6 text-purple-600" /></div></div></CardContent></Card>
        <Card><CardContent className="pt-6"><div className="flex items-center justify-between"><div><p className="text-sm text-gray-600">Carbon Saved</p><p className="text-2xl font-bold mt-1">{summary.totalCarbonSaved.toLocaleString()} kg</p></div><div className="size-12 bg-green-100 rounded-lg flex items-center justify-center"><Leaf className="size-6 text-green-600" /></div></div></CardContent></Card>
        <Card><CardContent className="pt-6"><div className="flex items-center justify-between"><div><p className="text-sm text-gray-600">Redemptions</p><p className="text-2xl font-bold mt-1">{summary.totalRedemptions.toLocaleString()}</p></div><div className="size-12 bg-orange-100 rounded-lg flex items-center justify-center"><BarChart3 className="size-6 text-orange-600" /></div></div></CardContent></Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader><CardTitle>Transport Mode Distribution</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            {transportData.map((item) => (
              <div key={item.mode}>
                <div className="flex items-center justify-between mb-1"><span className="text-sm font-medium">{item.mode}</span><span className="text-sm text-gray-600">{item.percentage.toFixed(1)}%</span></div>
                <div className="w-full bg-gray-200 rounded-full h-2"><div className={`${TRANSPORT_COLORS[item.mode] || TRANSPORT_COLORS.default} h-2 rounded-full`} style={{ width: `${item.percentage}%` }}/></div>
              </div>
            ))}
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Top 5 Performing Users</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            {summary.topUsers.map((user, index) => (
              <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center gap-3"><div className="size-8 bg-blue-600 text-white rounded-full flex items-center justify-center font-semibold text-sm">{index + 1}</div><div><p className="font-medium">{user.nickname}</p><p className="text-xs text-gray-600">{user.trips} trips</p></div></div>
                <div className="text-right"><p className="font-medium text-green-600">{user.carbonSaved.toLocaleString()} kg</p><p className="text-xs text-gray-600">saved</p></div>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
