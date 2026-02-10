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
import { getManagementAnalytics, type ManagementAnalyticsData } from '@/api/statisticsApi';
import { fetchAllTrips, type TripSummary } from '@/services/tripService';
import { getFacultyRankings, getRankingsByType, type FacultyCarbonResponse, type LeaderboardStatsDto } from '@/api/leaderboardApi';
import { fetchRewards, fetchOrders, type Reward, type Order } from '@/services/rewardService';
import { getBadgePurchaseStats, getAllBadges, type BadgePurchaseStat, type Badge } from '@/api/collectiblesApi';
import { fetchPointsSummary, fetchAllPointsHistory, type PointsSummary, type PointsTransaction } from '@/services/pointsService';
import { challengeApi, type Challenge } from '@/api/challengeApi';
import { fetchUserList, type User } from '@/services/userService';

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

const tooltipStyle = { backgroundColor: 'white', border: '1px solid #e5e7eb', borderRadius: '8px' };

const CHART_NAMES = [
  'User Growth Trends',
  'Carbon Saved Trends',
  'Trip Volume Trend',
  'Carbon Saved by Transport Mode',
  'Transport Mode Distribution',
  'Green vs Non-Green Trips',
  'Faculty Carbon Rankings',
  'Top 10 Users by Carbon Saved',
  'Top 10 Selling Products',
  'Top 10 Popular Badges',
  'Top 10 Popular Clothes',
  'Product Category Distribution',
  'Badge Acquisition Method',
  'Challenge Participation',
  'Challenge Type Distribution',
  'VIP Membership Distribution',
  'Points Economy Overview',
];

const CHART_SECTIONS: { label: string; start: number; end: number }[] = [
  { label: 'User & Carbon', start: 0, end: 2 },
  { label: 'Trip Analysis', start: 2, end: 6 },
  { label: 'Rankings', start: 6, end: 8 },
  { label: 'Products & Collectibles', start: 8, end: 11 },
  { label: 'Distribution', start: 11, end: 13 },
  { label: 'Challenges', start: 13, end: 15 },
  { label: 'Economy & Status', start: 15, end: 17 },
];

export function AnalyticsManagement() {
  const [timeRange, setTimeRange] = useState<TimeRange>('monthly');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedChart, setSelectedChart] = useState(0);
  const [selectedChart2, setSelectedChart2] = useState(1);
  const [chartMonth, setChartMonth] = useState(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  });

  const [, setAnalyticsData] = useState<ManagementAnalyticsData | null>(null);
  const [trips, setTrips] = useState<TripSummary[]>([]);
  const [facultyRankings, setFacultyRankings] = useState<FacultyCarbonResponse[]>([]);
  const [leaderboardData, setLeaderboardData] = useState<LeaderboardStatsDto | null>(null);
  const [rewards, setRewards] = useState<Reward[]>([]);
  const [badgeStats, setBadgeStats] = useState<BadgePurchaseStat[]>([]);
  const [badges, setBadges] = useState<Badge[]>([]);
  const [pointsSummary, setPointsSummary] = useState<PointsSummary[]>([]);
  const [challenges, setChallenges] = useState<Challenge[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [totalUserCount, setTotalUserCount] = useState(0);
  const [pointsLogs, setPointsLogs] = useState<PointsTransaction[]>([]);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        setLoading(true);
        setError(null);
        const toArr = <T,>(v: unknown): T[] => Array.isArray(v) ? v : [];
        const [ana, trp, fac, lb, rew, bs, bg, ps, ch, ord, ul, pl] = await Promise.all([
          getManagementAnalytics(timeRange).catch(e => { console.warn('[Analytics] management-analytics failed:', e); return null; }),
          fetchAllTrips().catch(e => { console.warn('[Analytics] fetchAllTrips failed:', e); return [] as TripSummary[]; }),
          getFacultyRankings().catch(e => { console.warn('[Analytics] facultyRankings failed:', e); return [] as FacultyCarbonResponse[]; }),
          getRankingsByType('MONTHLY', '', '', 0, 10).catch(e => { console.warn('[Analytics] leaderboard failed:', e); return null; }),
          fetchRewards(1, 100).then(r => toArr<Reward>(r?.data)).catch(e => { console.warn('[Analytics] rewards failed:', e); return [] as Reward[]; }),
          getBadgePurchaseStats().then(r => toArr<BadgePurchaseStat>(r)).catch(e => { console.warn('[Analytics] badgeStats failed:', e); return [] as BadgePurchaseStat[]; }),
          getAllBadges().then(r => toArr<Badge>(r)).catch(e => { console.warn('[Analytics] badges failed:', e); return [] as Badge[]; }),
          fetchPointsSummary().then(r => toArr<PointsSummary>(r?.data)).catch(e => { console.warn('[Analytics] points failed:', e); return [] as PointsSummary[]; }),
          challengeApi.getAllChallenges().then(r => toArr<Challenge>(r)).catch(e => { console.warn('[Analytics] challenges failed:', e); return [] as Challenge[]; }),
          fetchOrders(1, 200).then(r => toArr<Order>(r?.data?.orders)).catch(e => { console.warn('[Analytics] orders failed:', e); return [] as Order[]; }),
          fetchUserList(1, 500).catch(e => { console.warn('[Analytics] userList failed:', e); return null; }),
          fetchAllPointsHistory().then(r => toArr<PointsTransaction>(r?.data)).catch(e => { console.warn('[Analytics] pointsLogs failed:', e); return [] as PointsTransaction[]; }),
        ]);
        console.log('[Analytics] Results:', { ana, trp: trp?.length, fac: fac?.length, lb, rew: rew?.length, bs: bs?.length, bg: bg?.length, ps: ps?.length, ch: ch?.length, ord: ord?.length, users: ul?.data?.total, pl: pl?.length });
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
        setUsers(toArr<User>(ul?.data?.list));
        setTotalUserCount(ul?.data?.total || 0);
        setPointsLogs(pl);
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
    // Only use endPoint (destination) to mark where users go
    const grid = new Map<string, { lat: number; lng: number; count: number }>();
    const precision = 3; // ~111m grid cells
    trips.forEach(t => {
      if (!t.endPoint) return;
      const key = `${t.endPoint.lat.toFixed(precision)},${t.endPoint.lng.toFixed(precision)}`;
      const existing = grid.get(key);
      if (existing) { existing.count++; }
      else { grid.set(key, { lat: t.endPoint.lat, lng: t.endPoint.lng, count: 1 }); }
    });
    // Map frequency to 5 tiers: 0.2 / 0.4 / 0.6 / 0.8 / 1.0
    const maxCount = Math.max(1, ...[...grid.values()].map(g => g.count));
    return [...grid.values()].map(g => {
      const ratio = g.count / maxCount;
      let intensity: number;
      if (ratio <= 0.2) intensity = 0.2;
      else if (ratio <= 0.4) intensity = 0.4;
      else if (ratio <= 0.6) intensity = 0.6;
      else if (ratio <= 0.8) intensity = 0.8;
      else intensity = 1.0;
      return [g.lat, g.lng, intensity] as [number, number, number];
    });
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
    const sorted = Object.entries(map).sort(([a], [b]) => a.localeCompare(b));
    let cumTotal = 0, cumGreen = 0;
    return sorted.map(([date, v]) => {
      cumTotal += v.total;
      cumGreen += v.green;
      return { date, total: cumTotal, green: cumGreen };
    });
  }, [trips]);

  const topProducts = useMemo(() => {
    // Count sales per product from orders in the selected month
    const monthOrders = orders.filter(o =>
      o.createdAt?.substring(0, 7) === chartMonth && o.status !== 'CANCELLED'
    );
    const salesMap = new Map<string, { name: string; count: number }>();
    monthOrders.forEach(o => {
      o.items?.forEach(item => {
        const existing = salesMap.get(item.goodsId);
        if (existing) existing.count += item.quantity;
        else salesMap.set(item.goodsId, { name: item.goodsName || 'Unknown', count: item.quantity });
      });
    });
    const sorted = [...salesMap.values()].sort((a, b) => b.count - a.count).slice(0, 10);
    if (sorted.length > 0) {
      return sorted.map(s => ({ name: s.name.substring(0, 15), count: s.count }));
    }
    // No sales this month — show 10 random goods with count 0
    return rewards.slice(0, 10).map(r => ({ name: (r.name || 'Unknown').substring(0, 15), count: 0 }));
  }, [orders, rewards, chartMonth]);

  const topBadges = useMemo(() => {
    const badgeOnly = badges.filter(b => b.category === 'badge');
    const badgeMap = new Map(badgeOnly.map(b => [b.badgeId, b.name?.en || b.name?.zh || b.badgeId]));
    const badgeIds = new Set(badgeOnly.map(b => b.badgeId));
    const withSales = [...badgeStats].filter(s => badgeIds.has(s.badgeId) && s.purchaseCount > 0)
      .sort((a, b) => b.purchaseCount - a.purchaseCount).slice(0, 10)
      .map(s => ({ name: (badgeMap.get(s.badgeId) || s.badgeId || 'Unknown').substring(0, 15), count: s.purchaseCount }));
    if (withSales.length > 0) return withSales;
    // No sales — show 10 random badges with count 0
    return badgeOnly.slice(0, 10).map(b => ({ name: (b.name?.en || b.name?.zh || b.badgeId).substring(0, 15), count: 0 }));
  }, [badgeStats, badges]);

  const topClothes = useMemo(() => {
    const clothOnly = badges.filter(b => b.category === 'cloth');
    const clothMap = new Map(clothOnly.map(b => [b.badgeId, b.name?.en || b.name?.zh || b.badgeId]));
    const clothIds = new Set(clothOnly.map(b => b.badgeId));
    const withSales = [...badgeStats].filter(s => clothIds.has(s.badgeId) && s.purchaseCount > 0)
      .sort((a, b) => b.purchaseCount - a.purchaseCount).slice(0, 10)
      .map(s => ({ name: (clothMap.get(s.badgeId) || s.badgeId || 'Unknown').substring(0, 15), count: s.purchaseCount }));
    if (withSales.length > 0) return withSales;
    // No sales — show 10 random clothes with count 0
    return clothOnly.slice(0, 10).map(b => ({ name: (b.name?.en || b.name?.zh || b.badgeId).substring(0, 15), count: 0 }));
  }, [badgeStats, badges]);

  const categoryDist = useMemo(() => {
    // Map goodsId → category from rewards
    const catMap = new Map(rewards.map(r => [r.id, r.category || 'Other']));
    // Count sales by category from orders in selected month
    const monthOrders = orders.filter(o =>
      o.createdAt?.substring(0, 7) === chartMonth && o.status !== 'CANCELLED'
    );
    const salesByCat = new Map<string, number>();
    monthOrders.forEach(o => {
      o.items?.forEach(item => {
        const cat = catMap.get(item.goodsId) || 'Other';
        salesByCat.set(cat, (salesByCat.get(cat) || 0) + item.quantity);
      });
    });
    if (salesByCat.size > 0) {
      return [...salesByCat.entries()].map(([name, value]) => ({ name, value }));
    }
    return [];
  }, [orders, rewards, chartMonth]);

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
    let earned = 0;
    let spent = 0;

    pointsLogs.forEach(log => {
      if (log.change_type === 'gain') {
        earned += Math.abs(log.points);
      } else if (log.change_type === 'deduct') {
        spent += Math.abs(log.points);
      }
    });

    return [
      { name: 'Total Earned', value: earned },
      { name: 'Remaining', value: earned - spent },
      { name: 'Spent', value: spent },
    ];
  }, [pointsLogs]);

  const nonAdminUsers = useMemo(() =>
    users.filter(u => !u.admin && !u.isAdmin),
    [users]);

  const vipPenetration = useMemo(() => {
    if (nonAdminUsers.length === 0) return 0;
    const vipCount = nonAdminUsers.filter(u => u.vip?.active).length;
    return Math.round((vipCount / nonAdminUsers.length) * 100);
  }, [nonAdminUsers]);

  const totalCarbonFromTrips = useMemo(() =>
    trips.filter(t => t.carbonStatus === 'completed').reduce((s, t) => s + (t.carbonSaved || 0), 0),
    [trips]);

  const activeUserCount = useMemo(() => {
    const now = new Date();
    const daysMap: Record<TimeRange, number> = { daily: 1, weekly: 7, monthly: 30, yearly: 365 };
    const cutoff = new Date(now.getTime() - daysMap[timeRange] * 86400000);
    return nonAdminUsers.filter(u => u.lastLoginAt && new Date(u.lastLoginAt) >= cutoff).length;
  }, [nonAdminUsers, timeRange]);

  const vipDistribution = useMemo(() => {
    const vipActive = nonAdminUsers.filter(u => u.vip?.active).length;
    const nonVip = nonAdminUsers.length - vipActive;
    return [
      { name: 'VIP Active', value: vipActive },
      { name: 'Non-VIP', value: nonVip },
    ];
  }, [nonAdminUsers]);

  const greenTripRate = trips.length > 0 ? Math.round((trips.filter(t => t.isGreenTrip).length / trips.length) * 100) : 0;
  const totalPointsSpent = pointsEconomy.find(p => p.name === 'Spent')?.value || 0;

  // Total Goods Redemptions: 和 Reward Store 一样，从 COMPLETED 订单统计总兑换数量
  const totalGoodsRedemptions = useMemo(() => {
    let count = 0;
    orders.forEach(order => {
      if (order.status === 'COMPLETED' && order.items) {
        order.items.forEach(item => { count += item.quantity; });
      }
    });
    return count;
  }, [orders]);

  // Total Cloth Sold: 从 badgeStats 中仅统计 cloth 类别的总 purchaseCount
  const totalClothSold = useMemo(() => {
    const clothIds = new Set(badges.filter(b => b.category === 'cloth').map(b => b.badgeId));
    return badgeStats
      .filter(s => clothIds.has(s.badgeId))
      .reduce((sum, s) => sum + s.purchaseCount, 0);
  }, [badgeStats, badges]);

  const top10Users = useMemo(() =>
    (leaderboardData?.rankingsPage?.content || []).slice(0, 10).map(u => ({
      name: (u.nickname || u.userId).substring(0, 12), carbon: Math.round(u.carbonSaved * 100) / 100,
    })),
    [leaderboardData]);

  // ─── Month-based chart data ───

  const userGrowthData = useMemo(() => {
    const [year, month] = chartMonth.split('-').map(Number);
    const daysInMonth = new Date(year, month, 0).getDate();
    const today = new Date();
    const monthStart = `${chartMonth}-01`;

    // Users registered before this month (cumulative baseline)
    let cumulative = nonAdminUsers.filter(u => u.createdAt && u.createdAt.substring(0, 10) < monthStart).length;

    const data: { date: string; totalUsers: number; newUsers: number; activeUsers: number }[] = [];
    for (let d = 1; d <= daysInMonth; d++) {
      const dateStr = `${chartMonth}-${String(d).padStart(2, '0')}`;
      if (new Date(dateStr) > today) break;

      const newUsers = nonAdminUsers.filter(u => u.createdAt?.substring(0, 10) === dateStr).length;
      cumulative += newUsers;
      const activeUsers = nonAdminUsers.filter(u => u.lastLoginAt?.substring(0, 10) === dateStr).length;

      data.push({ date: `${month}/${d}`, totalUsers: cumulative, newUsers, activeUsers });
    }
    return data;
  }, [nonAdminUsers, chartMonth]);

  const carbonTrendData = useMemo(() => {
    const [year, month] = chartMonth.split('-').map(Number);
    const daysInMonth = new Date(year, month, 0).getDate();
    const today = new Date();

    let cumulative = 0;
    const data: { date: string; dailyCarbon: number; totalCarbon: number }[] = [];
    for (let d = 1; d <= daysInMonth; d++) {
      const dateStr = `${chartMonth}-${String(d).padStart(2, '0')}`;
      if (new Date(dateStr) > today) break;

      const dailyCarbon = trips
        .filter(t => t.startTime?.substring(0, 10) === dateStr)
        .reduce((sum, t) => sum + (t.carbonSaved || 0), 0);
      cumulative += dailyCarbon;

      data.push({
        date: `${month}/${d}`,
        dailyCarbon: Math.round(dailyCarbon * 100) / 100,
        totalCarbon: Math.round(cumulative * 100) / 100,
      });
    }
    return data;
  }, [trips, chartMonth]);

  const changeMonth = (delta: number) => {
    setChartMonth(prev => {
      const [y, m] = prev.split('-').map(Number);
      const d = new Date(y, m - 1 + delta, 1);
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    });
  };

  const chartMonthLabel = useMemo(() => {
    const [y, m] = chartMonth.split('-').map(Number);
    return new Date(y, m - 1).toLocaleString('en-US', { year: 'numeric', month: 'long' });
  }, [chartMonth]);

  // ─── Chart renderer ───

  const renderChart = (index: number) => {
    const H = 380;
    switch (index) {
      case 0: return (
        <div>
          <div className="flex items-center justify-center gap-3 mb-4">
            <button onClick={() => changeMonth(-1)} className="px-2 py-1 rounded hover:bg-gray-100 text-gray-600 font-bold">&lt;</button>
            <span className="text-sm font-medium text-gray-700 min-w-[140px] text-center">{chartMonthLabel}</span>
            <button onClick={() => changeMonth(1)} className="px-2 py-1 rounded hover:bg-gray-100 text-gray-600 font-bold">&gt;</button>
          </div>
          <ResponsiveContainer width="100%" height={H}>
            <LineChart data={userGrowthData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
              <XAxis dataKey="date" stroke="#6b7280" tick={{ fontSize: 12 }} />
              <YAxis stroke="#6b7280" />
              <Tooltip contentStyle={tooltipStyle} />
              <Legend />
              <Line type="monotone" dataKey="totalUsers" stroke="#3b82f6" strokeWidth={2} name="Total Users" dot={false} />
              <Line type="monotone" dataKey="newUsers" stroke="#10b981" strokeWidth={2} name="New Users" dot={false} />
              <Line type="monotone" dataKey="activeUsers" stroke="#8b5cf6" strokeWidth={2} name="Active Users" dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      );
      case 1: return (
        <div>
          <div className="flex items-center justify-center gap-3 mb-4">
            <button onClick={() => changeMonth(-1)} className="px-2 py-1 rounded hover:bg-gray-100 text-gray-600 font-bold">&lt;</button>
            <span className="text-sm font-medium text-gray-700 min-w-[140px] text-center">{chartMonthLabel}</span>
            <button onClick={() => changeMonth(1)} className="px-2 py-1 rounded hover:bg-gray-100 text-gray-600 font-bold">&gt;</button>
          </div>
          <ResponsiveContainer width="100%" height={H}>
            <AreaChart data={carbonTrendData}>
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
              <Area type="monotone" dataKey="totalCarbon" stroke="#10b981" fill="url(#colorCarbon)" name="Cumulative Carbon (kg)" />
              <Line type="monotone" dataKey="dailyCarbon" stroke="#3b82f6" strokeWidth={2} name="Daily Carbon (kg)" dot={false} />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      );
      case 2: return (
        <ResponsiveContainer width="100%" height={H}>
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
      );
      case 3: return (
        <ResponsiveContainer width="100%" height={H}>
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
      );
      case 4: return (
        <ResponsiveContainer width="100%" height={H}>
          <PieChart>
            <Pie data={transportDist} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={130} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}>
              {transportDist.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
            </Pie>
            <Tooltip />
          </PieChart>
        </ResponsiveContainer>
      );
      case 5: return (
        <ResponsiveContainer width="100%" height={H}>
          <PieChart>
            <Pie data={greenData} dataKey="value" nameKey="name" cx="50%" cy="50%" innerRadius={70} outerRadius={130} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}>
              <Cell fill="#10b981" />
              <Cell fill="#ef4444" />
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      );
      case 6: return (
        <ResponsiveContainer width="100%" height={H}>
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
      );
      case 7: return (
        <ResponsiveContainer width="100%" height={H}>
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
      );
      case 8: return (
        <div>
          <div className="flex items-center justify-center gap-3 mb-4">
            <button onClick={() => changeMonth(-1)} className="px-2 py-1 rounded hover:bg-gray-100 text-gray-600 font-bold">&lt;</button>
            <span className="text-sm font-medium text-gray-700 min-w-[140px] text-center">{chartMonthLabel}</span>
            <button onClick={() => changeMonth(1)} className="px-2 py-1 rounded hover:bg-gray-100 text-gray-600 font-bold">&gt;</button>
          </div>
          <ResponsiveContainer width="100%" height={H}>
            <BarChart data={topProducts} layout="vertical">
              <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
              <XAxis type="number" stroke="#6b7280" />
              <YAxis dataKey="name" type="category" stroke="#6b7280" width={120} tick={{ fontSize: 11 }} />
              <Tooltip contentStyle={tooltipStyle} formatter={(v: number) => [v, 'Sales']} />
              <Bar dataKey="count" name="Sales" radius={[0, 4, 4, 0]}>
                {topProducts.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      );
      case 9: return (
        <ResponsiveContainer width="100%" height={H}>
          <BarChart data={topBadges} layout="vertical">
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis type="number" stroke="#6b7280" />
            <YAxis dataKey="name" type="category" stroke="#6b7280" width={120} tick={{ fontSize: 11 }} />
            <Tooltip contentStyle={tooltipStyle} formatter={(v: number) => [v, 'Purchases']} />
            <Bar dataKey="count" name="Purchases" radius={[0, 4, 4, 0]}>
              {topBadges.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      );
      case 10: return (
        <ResponsiveContainer width="100%" height={H}>
          <BarChart data={topClothes} layout="vertical">
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis type="number" stroke="#6b7280" />
            <YAxis dataKey="name" type="category" stroke="#6b7280" width={120} tick={{ fontSize: 11 }} />
            <Tooltip contentStyle={tooltipStyle} formatter={(v: number) => [v, 'Purchases']} />
            <Bar dataKey="count" name="Purchases" radius={[0, 4, 4, 0]}>
              {topClothes.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      );
      case 11: return (
        categoryDist.length === 0 ? (
          <div className="flex flex-col items-center justify-center" style={{ height: H }}>
            <div className="w-52 h-52 rounded-full bg-gray-200 flex items-center justify-center">
              <span className="text-gray-400 text-sm">No sales data</span>
            </div>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={H}>
            <PieChart>
              <Pie data={categoryDist} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={130} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}>
                {categoryDist.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        )
      );
      case 12: return (
        <ResponsiveContainer width="100%" height={H}>
          <PieChart>
            <Pie data={acquisitionDist} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={130} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}>
              {acquisitionDist.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      );
      case 13: return (
        <ResponsiveContainer width="100%" height={H}>
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
      );
      case 14: return (
        <ResponsiveContainer width="100%" height={H}>
          <PieChart>
            <Pie data={challengeTypeDist} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={130} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}>
              {challengeTypeDist.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      );
      case 15: return (
        <ResponsiveContainer width="100%" height={H}>
          <PieChart>
            <Pie data={vipDistribution} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={130} label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}>
              {vipDistribution.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      );
      case 16: return (
        <ResponsiveContainer width="100%" height={H}>
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
      );
      default: return null;
    }
  };

  // ─── Render ───

  if (loading) {
    return <div className="h-full flex items-center justify-center"><Loader2 className="size-8 animate-spin text-blue-600" /></div>;
  }

  if (error) {
    return <div className="h-full flex items-center justify-center text-red-500">{error}</div>;
  }

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

        {/* KPI Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <KpiCard title="Total Users" value={formatNum(totalUserCount || nonAdminUsers.length)} unit="registered users" icon={<Users className="size-6" />} color="from-blue-500 to-blue-600" />
          <KpiCard title="Active Users" value={formatNum(activeUserCount)} unit={`active in ${timeRange} period`} icon={<Activity className="size-6" />} color="from-purple-500 to-purple-600" />
          <KpiCard title="Total Carbon Saved" value={formatNum(Math.round(totalCarbonFromTrips))} unit="kg CO2" icon={<Leaf className="size-6" />} color="from-green-500 to-green-600" />
          <KpiCard title="Green Trip Rate" value={`${greenTripRate}%`} unit={`${trips.filter(t => t.isGreenTrip).length} / ${trips.length} trips`} icon={<TreePine className="size-6" />} color="from-emerald-500 to-emerald-600" />
          <KpiCard title="VIP Penetration" value={`${vipPenetration}%`} unit="of all users" icon={<Crown className="size-6" />} color="from-amber-500 to-amber-600" />
          <KpiCard title="Total Points Spent" value={formatNum(totalPointsSpent)} unit="points consumed" icon={<Coins className="size-6" />} color="from-orange-500 to-orange-600" />
          <KpiCard title="Total Goods Count" value={formatNum(totalGoodsRedemptions)} unit="products redeemed" icon={<ShoppingBag className="size-6" />} color="from-pink-500 to-pink-600" />
          <KpiCard title="Total Collectible Count" value={formatNum(totalClothSold)} unit="clothes sold" icon={<Award className="size-6" />} color="from-indigo-500 to-indigo-600" />
        </div>

        {/* Charts: Two large charts + Right nav list */}
        <Card className="p-6">
          <div className="flex gap-4" style={{ minHeight: 420 }}>
            {/* Two charts side by side */}
            <div className="flex-1 min-w-0 flex gap-4">
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-semibold mb-2">{CHART_NAMES[selectedChart]}</h3>
                {renderChart(selectedChart)}
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-semibold mb-2">{CHART_NAMES[selectedChart2]}</h3>
                {renderChart(selectedChart2)}
              </div>
            </div>
            {/* Right: Chart navigation list */}
            <div className="w-48 border-l pl-3 overflow-y-auto flex-shrink-0" style={{ maxHeight: 420 }}>
              {CHART_SECTIONS.map((section) => (
                <div key={section.label}>
                  <div className="text-[10px] font-bold text-gray-400 uppercase tracking-wide mt-3 mb-1 px-2 first:mt-0">
                    {section.label}
                  </div>
                  {CHART_NAMES.slice(section.start, section.end).map((name, i) => {
                    const idx = section.start + i;
                    const isLeft = idx === selectedChart;
                    const isRight = idx === selectedChart2;
                    return (
                      <div
                        key={idx}
                        onClick={() => setSelectedChart(idx)}
                        onContextMenu={(e) => { e.preventDefault(); setSelectedChart2(idx); }}
                        className={`px-2 py-1.5 rounded cursor-pointer text-xs mb-0.5 transition-colors flex items-center gap-1 ${
                          isLeft ? 'bg-blue-50 text-blue-600 font-semibold'
                            : isRight ? 'bg-green-50 text-green-600 font-semibold'
                            : 'text-gray-700 hover:bg-gray-100'
                        }`}
                      >
                        {isLeft && <span className="w-1.5 h-1.5 rounded-full bg-blue-500 flex-shrink-0" />}
                        {isRight && <span className="w-1.5 h-1.5 rounded-full bg-green-500 flex-shrink-0" />}
                        {name}
                      </div>
                    );
                  })}
                </div>
              ))}
              <p className="text-[9px] text-gray-400 mt-3 px-2">Click = left chart<br />Right-click = right chart</p>
            </div>
          </div>
        </Card>

        {/* Heatmap */}
        <Card className="p-6">
          <HeatMapView title="Trip Activity Heatmap" height="550px" heatmapData={heatmapData} />
        </Card>

      </div>
    </div>
  );
}
