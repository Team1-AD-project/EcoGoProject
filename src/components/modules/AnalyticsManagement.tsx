import { useState, useEffect, useMemo } from 'react';
import { Card } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select';
import {
  LineChart, Line, BarChart, Bar, PieChart, Pie, AreaChart, Area,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, Cell,
} from 'recharts';
import {
  TrendingUp, TrendingDown, Users, Leaf, Crown, Coins,
  ShoppingBag, Award, Activity, Calendar, Loader2,
  TreePine,
} from 'lucide-react';
import { HeatMapView } from './HeatMapView';
import { getManagementAnalytics, type ManagementAnalyticsData, type Metric } from '@/api/statisticsApi';
import { fetchAllTrips, type TripSummary } from '@/services/tripService';
import { getFacultyRankings, getRankingsByType, type FacultyCarbonResponse, type LeaderboardStatsDto } from '@/api/leaderboardApi';
import { fetchRewards, fetchOrders, type Reward, type Order } from '@/services/rewardService';
import { getBadgePurchaseStats, getAllBadges, type BadgePurchaseStat, type Badge } from '@/api/collectiblesApi';
import { fetchPointsSummary, type PointsSummary } from '@/services/pointsService';
import { challengeApi, type Challenge } from '@/api/challengeApi';

type TimeRange = 'daily' | 'weekly' | 'monthly' | 'yearly';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16'];

function formatNum(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return n.toLocaleString();
}

function KpiCard({ title, value, unit, growth, icon, color }: {
  title: string; value: string; unit?: string; growth?: number; icon: React.ReactNode; color: string;
}) {
  return (
    <Card className={`p-4 bg-gradient-to-br ${color} text-white`}>
      <div className="flex items-center justify-between mb-3">
        {icon}
        {growth !== undefined && (
          <div className={`flex items-center gap-1 ${growth >= 0 ? 'text-green-200' : 'text-red-200'}`}>
            {growth >= 0 ? <TrendingUp className="size-4" /> : <TrendingDown className="size-4" />}
            <span className="text-sm font-semibold">{growth.toFixed(1)}%</span>
          </div>
        )}
      </div>
      <p className="text-sm opacity-90 mb-1">{title}</p>
      <p className="text-3xl font-bold">{value}</p>
      {unit && <p className="text-xs opacity-75 mt-1">{unit}</p>}
    </Card>
  );
}

function ChartCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Card className="p-6">
      <h3 className="text-lg font-semibold mb-4">{title}</h3>
      {children}
    </Card>
  );
}

const tooltipStyle = { backgroundColor: 'white', border: '1px solid #e5e7eb', borderRadius: '8px' };

export function AnalyticsManagement() {
  const [timeRange, setTimeRange] = useState<TimeRange>('monthly');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [analyticsData, setAnalyticsData] = useState<ManagementAnalyticsData | null>(null);
  const [trips, setTrips] = useState<TripSummary[]>([]);
  const [facultyRankings, setFacultyRankings] = useState<FacultyCarbonResponse[]>([]);
  const [leaderboardData, setLeaderboardData] = useState<LeaderboardStatsDto | null>(null);
  const [rewards, setRewards] = useState<Reward[]>([]);
  const [badgeStats, setBadgeStats] = useState<BadgePurchaseStat[]>([]);
  const [badges, setBadges] = useState<Badge[]>([]);
  const [pointsSummary, setPointsSummary] = useState<PointsSummary[]>([]);
  const [challenges, setChallenges] = useState<Challenge[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        setLoading(true);
        setError(null);
        const toArr = <T,>(v: unknown): T[] => Array.isArray(v) ? v : [];
        const [ana, trp, fac, lb, rew, bs, bg, ps, ch, ord] = await Promise.all([
          getManagementAnalytics(timeRange).catch(() => null),
          fetchAllTrips().catch(() => [] as TripSummary[]),
          getFacultyRankings().catch(() => [] as FacultyCarbonResponse[]),
          getRankingsByType('MONTHLY', '', '', 0, 10).catch(() => null),
          fetchRewards(1, 100).then(r => toArr<Reward>(r?.data)).catch(() => [] as Reward[]),
          getBadgePurchaseStats().then(r => toArr<BadgePurchaseStat>(r)).catch(() => [] as BadgePurchaseStat[]),
          getAllBadges().then(r => toArr<Badge>(r)).catch(() => [] as Badge[]),
          fetchPointsSummary().then(r => toArr<PointsSummary>(r?.data)).catch(() => [] as PointsSummary[]),
          challengeApi.getAllChallenges().then(r => toArr<Challenge>(r)).catch(() => [] as Challenge[]),
          fetchOrders(1, 200).then(r => toArr<Order>(r?.data?.orders)).catch(() => [] as Order[]),
        ]);
        setAnalyticsData(ana);
        setTrips(toArr<TripSummary>(trp));
        setFacultyRankings(toArr<FacultyCarbonResponse>(fac));
        setLeaderboardData(lb);
        setRewards(rew);
        setBadgeStats(bs);
        setBadges(bg);
        setPointsSummary(ps);
        setChallenges(ch);
        setOrders(ord);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load analytics');
      } finally {
        setLoading(false);
      }
    };
    fetchAll();
  }, [timeRange]);

  // ─── Derived data ───

  const heatmapData = useMemo<Array<[number, number, number]>>(() => {
    const points: Array<[number, number, number]> = [];
    trips.forEach(t => {
      if (t.startPoint) points.push([t.startPoint.lat, t.startPoint.lng, 1.0]);
      if (t.endPoint) points.push([t.endPoint.lat, t.endPoint.lng, 1.0]);
    });
    return points;
  }, [trips]);

  const transportDist = useMemo(() => {
    const map: Record<string, number> = {};
    trips.forEach(t => { if (t.detectedMode) map[t.detectedMode] = (map[t.detectedMode] || 0) + 1; });
    return Object.entries(map).map(([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value);
  }, [trips]);

  const greenData = useMemo(() => {
    let green = 0, nonGreen = 0;
    trips.forEach(t => { if (t.isGreenTrip) green++; else nonGreen++; });
    return [{ name: 'Green Trips', value: green }, { name: 'Non-Green', value: nonGreen }];
  }, [trips]);

  const carbonByMode = useMemo(() => {
    const map: Record<string, number> = {};
    trips.forEach(t => { if (t.detectedMode) map[t.detectedMode] = (map[t.detectedMode] || 0) + t.carbonSaved; });
    return Object.entries(map).map(([mode, carbon]) => ({ mode, carbon: Math.round(carbon * 100) / 100 })).sort((a, b) => b.carbon - a.carbon);
  }, [trips]);

  const tripVolumeTrend = useMemo(() => {
    const map: Record<string, { total: number; green: number }> = {};
    trips.forEach(t => {
      if (!t.startTime) return;
      const date = t.startTime.substring(0, 10);
      if (!map[date]) map[date] = { total: 0, green: 0 };
      map[date].total++;
      if (t.isGreenTrip) map[date].green++;
    });
    return Object.entries(map).sort(([a], [b]) => a.localeCompare(b)).map(([date, v]) => ({ date, ...v }));
  }, [trips]);

  const topProducts = useMemo(() =>
    [...rewards].filter(r => r.totalRedemptionCount > 0).sort((a, b) => b.totalRedemptionCount - a.totalRedemptionCount).slice(0, 10)
      .map(r => ({ name: (r.name || 'Unknown').substring(0, 15), count: r.totalRedemptionCount })),
    [rewards]);

  const topBadges = useMemo(() => {
    const badgeMap = new Map(badges.map(b => [b.badgeId, b.name?.en || b.name?.zh || b.badgeId]));
    return [...badgeStats].sort((a, b) => b.count - a.count).slice(0, 10)
      .map(s => ({ name: (badgeMap.get(s.badgeId) || s.badgeId || 'Unknown').substring(0, 15), count: s.count }));
  }, [badgeStats, badges]);

  const categoryDist = useMemo(() => {
    const map: Record<string, number> = {};
    rewards.forEach(r => { const cat = r.category || 'Other'; map[cat] = (map[cat] || 0) + 1; });
    return Object.entries(map).map(([name, value]) => ({ name, value }));
  }, [rewards]);

  const acquisitionDist = useMemo(() => {
    const map: Record<string, number> = {};
    badges.forEach(b => { map[b.acquisitionMethod] = (map[b.acquisitionMethod] || 0) + 1; });
    return Object.entries(map).map(([name, value]) => ({ name, value }));
  }, [badges]);

  const challengeParticipation = useMemo(() =>
    challenges.map(c => ({ title: (c.title || '').substring(0, 20), participants: c.participants || 0 })),
    [challenges]);

  const challengeTypeDist = useMemo(() => {
    const map: Record<string, number> = {};
    challenges.forEach(c => { map[c.type] = (map[c.type] || 0) + 1; });
    return Object.entries(map).map(([name, value]) => ({ name: name.replace(/_/g, ' '), value }));
  }, [challenges]);

  const pointsEconomy = useMemo(() => {
    let earned = 0, remaining = 0;
    pointsSummary.forEach(p => { earned += p.totalPoints; remaining += p.currentPoints; });
    return [
      { name: 'Total Earned', value: earned },
      { name: 'Remaining', value: remaining },
      { name: 'Spent', value: earned - remaining },
    ];
  }, [pointsSummary]);

  const vipPenetration = useMemo(() => {
    if (!analyticsData?.vipDistribution) return 0;
    const total = analyticsData.vipDistribution.reduce((s, d) => s + d.value, 0);
    const vip = analyticsData.vipDistribution.find(d => d.name.toLowerCase().includes('vip') || d.name.toLowerCase().includes('active'));
    return total > 0 && vip ? Math.round((vip.value / total) * 100) : 0;
  }, [analyticsData]);

  const orderStatusDist = useMemo(() => {
    const map: Record<string, number> = {};
    orders.forEach(o => { map[o.status] = (map[o.status] || 0) + 1; });
    return Object.entries(map).map(([name, value]) => ({ name, value }));
  }, [orders]);

  const greenTripRate = trips.length > 0 ? Math.round((trips.filter(t => t.isGreenTrip).length / trips.length) * 100) : 0;
  const totalPointsSpent = pointsEconomy.find(p => p.name === 'Spent')?.value || 0;
  const bestSellerCount = rewards.length > 0 ? Math.max(...rewards.map(r => r.totalRedemptionCount), 0) : 0;
  const totalBadgesSold = badgeStats.reduce((s, b) => s + b.count, 0);

  const top10Users = useMemo(() =>
    (leaderboardData?.rankingsPage?.content || []).slice(0, 10).map(u => ({
      name: (u.nickname || u.userId).substring(0, 12), carbon: Math.round(u.carbonSaved * 100) / 100,
    })),
    [leaderboardData]);

  // ─── Render ───

  if (loading) {
    return <div className="h-full flex items-center justify-center"><Loader2 className="size-8 animate-spin text-blue-600" /></div>;
  }

  if (error) {
    return <div className="h-full flex items-center justify-center text-red-500">{error}</div>;
  }

  const m = (metric?: Metric) => metric ? { val: formatNum(metric.currentValue), growth: metric.growthRate } : { val: 'N/A', growth: undefined };

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Header */}
      <div className="p-6 bg-white border-b">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold">Data Analytics</h2>
            <p className="text-gray-600 mt-1">Comprehensive overview of platform performance, user engagement, and environmental impact</p>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <Calendar className="size-5 text-gray-600" />
              <Label className="text-sm">Time Range:</Label>
            </div>
            <Select value={timeRange} onValueChange={(v: TimeRange) => setTimeRange(v)}>
              <SelectTrigger className="w-32"><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="daily">Daily</SelectItem>
                <SelectItem value="weekly">Weekly</SelectItem>
                <SelectItem value="monthly">Monthly</SelectItem>
                <SelectItem value="yearly">Yearly</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-6 space-y-6">

        {/* Row 2: KPI Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <KpiCard title="Total Users" value={m(analyticsData?.totalUsers).val} unit="users" growth={m(analyticsData?.totalUsers).growth} icon={<Users className="size-6" />} color="from-blue-500 to-blue-600" />
          <KpiCard title="Active Users" value={m(analyticsData?.activeUsers).val} unit="users" growth={m(analyticsData?.activeUsers).growth} icon={<Activity className="size-6" />} color="from-purple-500 to-purple-600" />
          <KpiCard title="Total Carbon Saved" value={m(analyticsData?.totalCarbonSaved).val} unit="kg CO2" growth={m(analyticsData?.totalCarbonSaved).growth} icon={<Leaf className="size-6" />} color="from-green-500 to-green-600" />
          <KpiCard title="Green Trip Rate" value={`${greenTripRate}%`} unit={`${trips.filter(t => t.isGreenTrip).length} / ${trips.length} trips`} icon={<TreePine className="size-6" />} color="from-emerald-500 to-emerald-600" />
          <KpiCard title="VIP Penetration" value={`${vipPenetration}%`} unit="of all users" icon={<Crown className="size-6" />} color="from-amber-500 to-amber-600" />
          <KpiCard title="Total Points Spent" value={formatNum(totalPointsSpent)} unit="points consumed" icon={<Coins className="size-6" />} color="from-orange-500 to-orange-600" />
          <KpiCard title="Best Seller Count" value={formatNum(bestSellerCount)} unit="top product redemptions" icon={<ShoppingBag className="size-6" />} color="from-pink-500 to-pink-600" />
          <KpiCard title="Total Badges Sold" value={formatNum(totalBadgesSold)} unit="badges purchased" icon={<Award className="size-6" />} color="from-indigo-500 to-indigo-600" />
        </div>

        {/* Row 3: User Growth + Carbon Saved Trends */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ChartCard title="User Growth Trends">
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={analyticsData?.userGrowthTrend || []}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="date" stroke="#6b7280" tick={{ fontSize: 12 }} />
                <YAxis stroke="#6b7280" />
                <Tooltip contentStyle={tooltipStyle} />
                <Legend />
                <Line type="monotone" dataKey="users" stroke="#3b82f6" strokeWidth={2} name="Total Users" dot={false} />
                <Line type="monotone" dataKey="newUsers" stroke="#10b981" strokeWidth={2} name="New Users" dot={false} />
                <Line type="monotone" dataKey="activeUsers" stroke="#8b5cf6" strokeWidth={2} name="Active Users" dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </ChartCard>

          <ChartCard title="Carbon Saved Trends">
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={analyticsData?.carbonGrowthTrend || []}>
                <defs>
                  <linearGradient id="colorCarbon" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.8} />
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0.1} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="date" stroke="#6b7280" tick={{ fontSize: 12 }} />
                <YAxis stroke="#6b7280" />
                <Tooltip contentStyle={tooltipStyle} formatter={(v: number) => [`${v.toLocaleString()} kg`, '']} />
                <Legend />
                <Area type="monotone" dataKey="carbonSaved" stroke="#10b981" fill="url(#colorCarbon)" name="Carbon Saved (kg)" />
              </AreaChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>

        {/* Row 4: Trip Volume Trend + Carbon by Mode */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ChartCard title="Trip Volume Trend">
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={tripVolumeTrend}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="date" stroke="#6b7280" tick={{ fontSize: 12 }} />
                <YAxis stroke="#6b7280" />
                <Tooltip contentStyle={tooltipStyle} />
                <Legend />
                <Line type="monotone" dataKey="total" stroke="#3b82f6" strokeWidth={2} name="Total Trips" dot={false} />
                <Line type="monotone" dataKey="green" stroke="#10b981" strokeWidth={2} name="Green Trips" dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </ChartCard>

          <ChartCard title="Carbon Saved by Transport Mode">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={carbonByMode}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="mode" stroke="#6b7280" tick={{ fontSize: 11 }} />
                <YAxis stroke="#6b7280" />
                <Tooltip contentStyle={tooltipStyle} formatter={(v: number) => [`${v} kg`, 'Carbon Saved']} />
                <Bar dataKey="carbon" name="Carbon Saved (kg)" radius={[4, 4, 0, 0]}>
                  {carbonByMode.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>

        {/* Row 5: Transport Mode Pie + Green vs Non-Green Donut */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ChartCard title="Transport Mode Distribution">
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie data={transportDist} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={100} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}>
                  {transportDist.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>

          <ChartCard title="Green vs Non-Green Trips">
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie data={greenData} dataKey="value" nameKey="name" cx="50%" cy="50%" innerRadius={60} outerRadius={100} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}>
                  <Cell fill="#10b981" />
                  <Cell fill="#ef4444" />
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>

        {/* Row 6: Faculty Rankings + Top 10 Users */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ChartCard title="Faculty Carbon Rankings (Monthly)">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={[...facultyRankings].sort((a, b) => b.totalCarbon - a.totalCarbon)} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis type="number" stroke="#6b7280" />
                <YAxis dataKey="faculty" type="category" stroke="#6b7280" width={120} tick={{ fontSize: 11 }} />
                <Tooltip contentStyle={tooltipStyle} formatter={(v: number) => [`${v.toFixed(2)} kg`, 'Carbon Saved']} />
                <Bar dataKey="totalCarbon" name="Carbon Saved (kg)" radius={[0, 4, 4, 0]}>
                  {facultyRankings.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>

          <ChartCard title="Top 10 Users by Carbon Saved">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={top10Users} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis type="number" stroke="#6b7280" />
                <YAxis dataKey="name" type="category" stroke="#6b7280" width={100} tick={{ fontSize: 11 }} />
                <Tooltip contentStyle={tooltipStyle} formatter={(v: number) => [`${v} kg`, 'Carbon Saved']} />
                <Bar dataKey="carbon" name="Carbon Saved (kg)" radius={[0, 4, 4, 0]}>
                  {top10Users.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>

        {/* Row 7: Heatmap */}
        <Card className="p-6">
          <HeatMapView title="Trip Activity Heatmap" height="550px" heatmapData={heatmapData} />
        </Card>

        {/* Row 8: Top Products + Top Badges */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ChartCard title="Top 10 Selling Products">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={topProducts}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="name" stroke="#6b7280" tick={{ fontSize: 10 }} angle={-20} textAnchor="end" height={60} />
                <YAxis stroke="#6b7280" />
                <Tooltip contentStyle={tooltipStyle} />
                <Bar dataKey="count" name="Redemptions" radius={[4, 4, 0, 0]}>
                  {topProducts.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>

          <ChartCard title="Top 10 Popular Badges">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={topBadges}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="name" stroke="#6b7280" tick={{ fontSize: 10 }} angle={-20} textAnchor="end" height={60} />
                <YAxis stroke="#6b7280" />
                <Tooltip contentStyle={tooltipStyle} />
                <Bar dataKey="count" name="Purchases" radius={[4, 4, 0, 0]}>
                  {topBadges.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>

        {/* Row 9: Product Category + Badge Acquisition Method */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ChartCard title="Product Category Distribution">
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie data={categoryDist} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={100} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}>
                  {categoryDist.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>

          <ChartCard title="Badge Acquisition Method">
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie data={acquisitionDist} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={100} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}>
                  {acquisitionDist.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>

        {/* Row 10: Challenge Participation + Type */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ChartCard title="Challenge Participation">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={challengeParticipation}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="title" stroke="#6b7280" tick={{ fontSize: 10 }} angle={-15} textAnchor="end" height={60} />
                <YAxis stroke="#6b7280" />
                <Tooltip contentStyle={tooltipStyle} />
                <Bar dataKey="participants" name="Participants" radius={[4, 4, 0, 0]}>
                  {challengeParticipation.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>

          <ChartCard title="Challenge Type Distribution">
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie data={challengeTypeDist} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={100} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}>
                  {challengeTypeDist.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>

        {/* Row 11: VIP Distribution + Points Economy */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ChartCard title="VIP Membership Distribution">
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie data={analyticsData?.vipDistribution || []} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={100} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}>
                  {(analyticsData?.vipDistribution || []).map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>

          <ChartCard title="Points Economy Overview">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={pointsEconomy}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="name" stroke="#6b7280" />
                <YAxis stroke="#6b7280" tickFormatter={(v) => formatNum(v)} />
                <Tooltip contentStyle={tooltipStyle} formatter={(v: number) => [formatNum(v), 'Points']} />
                <Bar dataKey="value" name="Points" radius={[4, 4, 0, 0]}>
                  <Cell fill="#10b981" />
                  <Cell fill="#3b82f6" />
                  <Cell fill="#f59e0b" />
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>

        {/* Row 12: Order Status + Revenue Trends */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <ChartCard title="Order Status Distribution">
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie data={orderStatusDist} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={100} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}>
                  {orderStatusDist.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>

          <ChartCard title="Revenue Trends (Points-based)">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={analyticsData?.revenueGrowthTrend || []}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="date" stroke="#6b7280" tick={{ fontSize: 12 }} />
                <YAxis stroke="#6b7280" />
                <Tooltip contentStyle={tooltipStyle} />
                <Legend />
                <Bar dataKey="vipRevenue" fill="#8b5cf6" name="VIP Points" radius={[4, 4, 0, 0]} />
                <Bar dataKey="shopRevenue" fill="#ec4899" name="Shop Points" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </div>

      </div>
    </div>
  );
}
