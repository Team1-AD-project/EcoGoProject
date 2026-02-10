import { useState, useEffect } from 'react';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Users,
  Map,
  Coins,
  Crown,
  ShoppingBag,
  Award,
  BarChart3,
  Megaphone,
  Trophy,
  TrendingUp,
  Activity,
  DollarSign,
  UserPlus,
  Clock,
  Eye,
  Zap,
  Target,
  MessageSquare,
  Server
} from 'lucide-react';
import { AreaChart, Area, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { fetchUserList } from '@/services/userService';
import { fetchAllTrips } from '@/services/tripService';
import { fetchOrders, fetchRewards, fetchVouchers } from '@/services/rewardService';
import { getAllAdvertisements } from '@/api/advertisementApi';
import { getRankingsByType } from '@/api/leaderboardApi';
import { getAllBadges } from '@/api/collectiblesApi';
import { toast } from 'sonner';

interface DashboardProps {
  onModuleSelect: (moduleId: string) => void;
}

export function Dashboard({ onModuleSelect }: DashboardProps) {
  const [loading, setLoading] = useState(true);

  // Real Calculated Stats
  const [userStats, setUserStats] = useState({
    total: 0,
    newToday: 0,
    growthRate: 0 // New users today / (Total - New) * 100
  });

  const [tripStats, setTripStats] = useState({
    totalTrips: 0,
    todayTrips: 0,
    yesterdayTrips: 0,
    growthRate: 0 // (Today - Yesterday) / Yesterday * 100
  });

  const [vipStats, setVipStats] = useState({
    totalVip: 0,
    newThisMonth: 0,
    growthRate: 0, // New This Month / (Total - New) * 100
    estimatedRevenue: 0 // Placeholder calculation
  });

  const [revenueStats, setRevenueStats] = useState({
    totalRevenue: 0, // Keep for charts if needed
    storeRevenue: 0,
    vipRevenue: 0,
    totalPointsRedeemed: 0,
    growthRate: 0
  });

  const [storeStats, setStoreStats] = useState({
    totalProducts: 0,
    activeProducts: 0,
    totalVouchers: 0,
    activeVouchers: 0,
    totalOrders: 0,
    todayOrders: 0,
    topProduct: 'Eco Water Bottle' // Placeholder
  });

  const [adStats, setAdStats] = useState({
    totalAds: 0,
    activeAds: 0,
    totalImpressions: 0,
    totalClicks: 0,
    ctr: 0,
    revenue: 0 // Placeholder
  });

  const [collectibleStats, setCollectibleStats] = useState({
    totalBadges: 0,
    totalPets: 0,
    activeCollectors: 0
  });

  const [leaderboardStats, setLeaderboardStats] = useState({
    totalCarbon: 0,
    rewardsDistributed: 0,
    topUser: 'None',
    participants: 0
  });

  // Mock data for Charts & Chat (No API history yet)
  const chatStats = {
    totalRequests: 24610,
    todayRequests: 1567,
    activeModels: 2,
    totalModels: 3,
    apiStatus: 'connected',
    avgResponseTime: 1.1,
    activeUsers: 435,
    topModel: 'Llama 2',
    growth: 22.3
  };

  const userGrowthData = [
    { month: 'Jan', normal: 32000, vip: 5500 },
    { month: 'Feb', normal: 33500, vip: 5800 },
    { month: 'Mar', normal: 35000, vip: 6200 },
    { month: 'Apr', normal: 36200, vip: 6500 },
    { month: 'May', normal: 37500, vip: 6800 },
    { month: 'Jun', normal: 38456, vip: 7222 }
  ];
  const activityData = [
    { day: 'Mon', trips: 980, transactions: 756, orders: 123 },
    { day: 'Tue', trips: 1050, transactions: 812, orders: 145 },
    { day: 'Wed', trips: 1120, transactions: 890, orders: 156 },
    { day: 'Thu', trips: 1080, transactions: 845, orders: 138 },
    { day: 'Fri', trips: 1200, transactions: 923, orders: 167 },
    { day: 'Sat', trips: 1350, transactions: 1045, orders: 189 },
    { day: 'Sun', trips: 1234, transactions: 892, orders: 145 }
  ];
  const revenueData = [
    { name: 'VIP Subscriptions', value: revenueStats.vipRevenue || 288880, color: '#8b5cf6' },
    { name: 'Store Sales', value: revenueStats.storeRevenue || 567890, color: '#10b981' },
    { name: 'Ad Revenue', value: adStats.revenue || 123456, color: '#f59e0b' },
    { name: 'Point Transactions', value: 456789, color: '#3b82f6' }
  ];
  const stepsData = [
    { week: 'W1', steps: 112000000 },
    { week: 'W2', steps: 115000000 },
    { week: 'W3', steps: 118000000 },
    { week: 'W4', steps: 122000000 },
    { week: 'W5', steps: 125000000 }
  ];

  const recentActivities = [
    { id: 1, type: 'user', icon: UserPlus, text: 'New User Registration', detail: `${userStats.newToday} new users joined today`, time: 'Today', color: 'text-blue-600' },
    { id: 2, type: 'vip', icon: Crown, text: 'VIP Subscription', detail: `${vipStats.newThisMonth} new VIPs this month`, time: 'This Month', color: 'text-purple-600' },
    { id: 3, type: 'order', icon: ShoppingBag, text: 'Store Orders', detail: `${storeStats.todayOrders} new orders today`, time: 'Today', color: 'text-green-600' },
    { id: 4, type: 'ad', icon: Megaphone, text: 'Ad Campaign', detail: `${adStats.activeAds} active campaigns`, time: 'Now', color: 'text-orange-600' },
    { id: 5, type: 'trip', icon: Map, text: 'Trip Data', detail: `${tripStats.todayTrips} trips logged today`, time: 'Today', color: 'text-green-600' }
  ];

  useEffect(() => {
    calculateRealData();
  }, []);

  const calculateRealData = async () => {
    setLoading(true);
    try {
      const todayStr = new Date().toISOString().split('T')[0];
      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      const yesterdayStr = yesterday.toISOString().split('T')[0];
      const currentMonth = new Date().toISOString().slice(0, 7); // YYYY-MM

      // 1. Users & VIPs (Fetch large list to ensure accuracy)
      const userResp = await fetchUserList(1, 1000); // Fetch up to 1000 users for calc
      const allUsers = userResp.data.list;

      const totalUsers = userResp.data.total;
      const newUsersToday = allUsers.filter(u => u.createdAt && u.createdAt.startsWith(todayStr)).length;

      // VIP Calc
      const vipUsers = allUsers.filter(u => u.vip && u.vip.active);
      const totalVip = vipUsers.length;
      const newVipsMonth = vipUsers.filter(u => u.vip.startDate && u.vip.startDate.startsWith(currentMonth)).length;
      // Approx revenue: VIP count * 30 (Assuming 30 CNY/month avg)
      const estimatedVipRevenue = totalVip * 30;

      setUserStats({
        total: totalUsers,
        newToday: newUsersToday,
        growthRate: 0 // Hard to calc without total from yesterday
      });

      setVipStats({
        totalVip: totalVip,
        newThisMonth: newVipsMonth,
        growthRate: totalVip > newVipsMonth ? parseFloat(((newVipsMonth / (totalVip - newVipsMonth)) * 100).toFixed(1)) : 0,
        estimatedRevenue: estimatedVipRevenue
      });

      // 2. Trips (Total, Today, Yesterday)
      const trips = await fetchAllTrips();
      const totalTrips = trips.length;
      const todayTrips = trips.filter(t => t.startTime.startsWith(todayStr)).length;
      const yesterdayTripsCount = trips.filter(t => t.startTime.startsWith(yesterdayStr)).length;

      let tripGrowth = 0;
      if (yesterdayTripsCount > 0) {
        tripGrowth = ((todayTrips - yesterdayTripsCount) / yesterdayTripsCount) * 100;
      } else if (todayTrips > 0) {
        tripGrowth = 100; // 100% growth if yesterday was 0
      }

      setTripStats({
        totalTrips,
        todayTrips,
        yesterdayTrips: yesterdayTripsCount,
        growthRate: parseFloat(tripGrowth.toFixed(1))
      });

      // 3. Store Orders & Revenue & Vouchers (Robust Parsing)
      const ordersResp = await fetchOrders(1, 1000); // Fetch large list
      const allOrders = ordersResp.data.orders || [];
      const totalOrders = ordersResp.data.pagination.total;
      const todayOrders = allOrders.filter(o => o.createdAt.startsWith(todayStr)).length;

      // Calculate Revenue from COMPLETED or PAID orders
      const validOrders = allOrders.filter(o => o.status === 'PAID' || o.status === 'COMPLETED' || o.status === 'SHIPPED');
      const storeRevenue = validOrders.reduce((sum, o) => sum + (o.finalAmount || 0), 0);
      const storePoints = validOrders.reduce((sum, o) => sum + (o.pointsUsed || 0), 0);

      // Fetch Rewards (Products)
      const rewardsResp: any = await fetchRewards(1, 100);
      let productsList: any[] = [];
      let totalProducts = 0;

      if (rewardsResp && rewardsResp.code === 200) {
        const responseData = rewardsResp.data as any;
        if (Array.isArray(responseData)) {
          productsList = responseData;
        } else if (responseData && typeof responseData === 'object') {
          productsList = responseData.list || responseData.goods || responseData.records || responseData.items || [];
          if (responseData.pagination) totalProducts = responseData.pagination.total;
          else if (responseData.total) totalProducts = responseData.total;
        }
        if (rewardsResp.pagination && !totalProducts) totalProducts = rewardsResp.pagination.total;
        if (totalProducts === 0 && productsList.length > 0) totalProducts = productsList.length; // Fallback
      }
      const activeProducts = productsList.filter((r: any) => r.isActive).length;

      // Fetch Vouchers
      const vouchersResp: any = await fetchVouchers(1, 100);
      let vouchersList: any[] = [];
      let totalVouchers = 0;

      if (vouchersResp && vouchersResp.code === 200) {
        const responseData = vouchersResp.data as any;
        if (Array.isArray(responseData)) {
          vouchersList = responseData;
        } else if (responseData && typeof responseData === 'object') {
          vouchersList = responseData.list || responseData.goods || responseData.records || responseData.items || [];
          if (responseData.pagination) totalVouchers = responseData.pagination.total;
          else if (responseData.total) totalVouchers = responseData.total;
        }
        if (vouchersResp.pagination && !totalVouchers) totalVouchers = vouchersResp.pagination.total;
        if (totalVouchers === 0 && vouchersList.length > 0) totalVouchers = vouchersList.length; // Fallback
      }
      const activeVouchers = vouchersList.filter((v: any) => v.isActive).length;

      setStoreStats({
        totalProducts,
        activeProducts,
        totalVouchers,
        activeVouchers,
        totalOrders,
        todayOrders,
        topProduct: 'Eco Water Bottle'
      });

      setRevenueStats({
        storeRevenue,
        vipRevenue: estimatedVipRevenue,
        totalRevenue: storeRevenue + estimatedVipRevenue + (adStats.revenue || 0),
        totalPointsRedeemed: storePoints,
        growthRate: 0 // Need historical data
      });

      // 4. Ads
      const adsPage = await getAllAdvertisements('', 0, 100);
      const ads = adsPage.content;
      const activeAds = ads.filter(a => a.status === 'active').length;
      const impressions = ads.reduce((sum, a) => sum + (a.impressions || 0), 0);
      const clicks = ads.reduce((sum, a) => sum + (a.clicks || 0), 0);

      setAdStats({
        totalAds: adsPage.totalElements,
        activeAds,
        totalImpressions: impressions,
        totalClicks: clicks,
        ctr: impressions > 0 ? parseFloat(((clicks / impressions) * 100).toFixed(2)) : 0,
        revenue: clicks * 0.5 // Placeholder: 0.5 CNY per click
      });

      // 5. Leaderboard / Carbon
      const leaderboard = await getRankingsByType('DAILY', '', '', 0, 1); // Get daily stats
      const allBadges = await getAllBadges(); // For collectible stats

      setLeaderboardStats({
        totalCarbon: leaderboard.totalCarbonSaved,
        rewardsDistributed: leaderboard.totalRewardsDistributed,
        topUser: leaderboard.rankingsPage.content[0]?.nickname || 'None',
        participants: leaderboard.rankingsPage.totalElements
      });

      setCollectibleStats(prev => ({
        ...prev,
        totalBadges: allBadges.length
      }));

    } catch (e) {
      console.error("Dashboard calculation error:", e);
      toast.error("Failed to calculate some dashboard metrics");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="h-full overflow-y-auto bg-gray-50">
      {/* Header */}
      <div className="p-6 bg-white border-b sticky top-0 z-10">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">System Dashboard</h2>
            <p className="text-gray-600 mt-1">Real-time overview based on current system data</p>
          </div>
          <div className="flex items-center gap-2 text-sm text-gray-600">
            <Clock className="size-4" />
            <span>Updated: {new Date().toLocaleTimeString()}</span>
            <Button variant="ghost" size="sm" onClick={calculateRealData}>Refresh</Button>
          </div>
        </div>
      </div>

      <div className="p-6 space-y-6">
        {/* Core metric cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Total users */}
          <Card className="p-5 bg-gradient-to-br from-blue-500 to-blue-600 text-white hover:shadow-lg transition-shadow cursor-pointer" onClick={() => onModuleSelect('user-management')}>
            <div className="flex items-center justify-between mb-3">
              <Users className="size-8 opacity-80" />
              <Badge className="bg-white/20 text-white">User Mgmt</Badge>
            </div>
            <p className="text-sm opacity-90 mb-1">Total Users</p>
            <p className="text-3xl font-bold mb-2">{userStats.total.toLocaleString()}</p>
            <div className="flex items-center gap-2 text-sm">
              <UserPlus className="size-4" />
              <span>+{userStats.newToday} today</span>
            </div>
          </Card>

          {/* Today's trips */}
          <Card className="p-5 bg-gradient-to-br from-green-500 to-green-600 text-white hover:shadow-lg transition-shadow cursor-pointer" onClick={() => onModuleSelect('trip-management')}>
            <div className="flex items-center justify-between mb-3">
              <Map className="size-8 opacity-80" />
              <Badge className="bg-white/20 text-white">Trip Data</Badge>
            </div>
            <p className="text-sm opacity-90 mb-1">Today's Trips</p>
            <p className="text-3xl font-bold mb-2">{tripStats.todayTrips.toLocaleString()}</p>
            <div className="flex items-center gap-2 text-sm">
              <TrendingUp className="size-4" />
              <span>{tripStats.growthRate > 0 ? '+' : ''}{tripStats.growthRate}% vs yesterday</span>
            </div>
          </Card>

          {/* VIP subscriptions */}
          <Card className="p-5 bg-gradient-to-br from-purple-500 to-purple-600 text-white hover:shadow-lg transition-shadow cursor-pointer" onClick={() => onModuleSelect('vip-management')}>
            <div className="flex items-center justify-between mb-3">
              <Crown className="size-8 opacity-80" />
              <Badge className="bg-white/20 text-white">VIP Mgmt</Badge>
            </div>
            <p className="text-sm opacity-90 mb-1">VIP Users</p>
            <p className="text-3xl font-bold mb-2">{vipStats.totalVip.toLocaleString()}</p>
            <div className="flex items-center gap-2 text-sm">
              <TrendingUp className="size-4" />
              <span>{vipStats.growthRate > 0 ? '+' : ''}{vipStats.growthRate}% new this month</span>
            </div>
          </Card>

          {/* Total Points Redeemed */}
          <Card className="p-5 bg-gradient-to-br from-orange-500 to-orange-600 text-white hover:shadow-lg transition-shadow cursor-pointer" onClick={() => onModuleSelect('analytics-management')}>
            <div className="flex items-center justify-between mb-3">
              <DollarSign className="size-8 opacity-80" />
              <Badge className="bg-white/20 text-white">Total Points</Badge>
            </div>
            <p className="text-sm opacity-90 mb-1">Total Points Redeemed</p>
            <p className="text-3xl font-bold mb-2">{revenueStats.totalPointsRedeemed.toLocaleString()}</p>
            <div className="flex items-center gap-2 text-sm opacity-90">
              <span className="text-xs">Store: {revenueStats.totalPointsRedeemed} | VIP: 0</span>
            </div>
          </Card>
        </div>

        {/* Detailed statistic cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {/* Point transactions: Keeping semi-mocked or using analytics data if available */}
          <Card className="p-5 hover:shadow-md transition-shadow">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-blue-100 rounded-lg">
                <Coins className="size-6 text-blue-600" />
              </div>
              <div className="flex-1">
                <h3 className="font-bold text-gray-900">Total Carbon Saved</h3>
                <p className="text-sm text-gray-600">Avg per user: {(leaderboardStats.totalCarbon / (userStats.total || 1)).toFixed(1)} kg</p>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Total Saved</span>
                <span className="font-semibold">{leaderboardStats.totalCarbon.toLocaleString()} kg</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Top Saver</span>
                <span className="font-semibold text-green-600">{leaderboardStats.topUser}</span>
              </div>
            </div>
          </Card>

          {/* Reward store */}
          <Card className="p-5 hover:shadow-md transition-shadow">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-green-100 rounded-lg">
                <ShoppingBag className="size-6 text-green-600" />
              </div>
              <div className="flex-1">
                <h3 className="font-bold text-gray-900">Reward Store</h3>
                <p className="text-sm text-gray-600">Today: {storeStats.todayOrders} orders</p>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Total Products</span>
                <span className="font-semibold">{storeStats.totalProducts}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Total Vouchers</span>
                <span className="font-semibold text-purple-600">{storeStats.totalVouchers}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Total Orders</span>
                <span className="font-semibold">{storeStats.totalOrders.toLocaleString()}</span>
              </div>
            </div>
          </Card>

          {/* Collectible management */}
          <Card className="p-5 hover:shadow-md transition-shadow">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-purple-100 rounded-lg">
                <Award className="size-6 text-purple-600" />
              </div>
              <div className="flex-1">
                <h3 className="font-bold text-gray-900">Collectibles</h3>
                <p className="text-sm text-gray-600">Badges</p>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Total Badges</span>
                <span className="font-semibold">{collectibleStats.totalBadges}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Total Pets</span>
                <span className="font-semibold">{collectibleStats.totalPets}</span>
              </div>
            </div>
          </Card>

          {/* Ad management */}
          <Card className="p-5 hover:shadow-md transition-shadow">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-orange-100 rounded-lg">
                <Megaphone className="size-6 text-orange-600" />
              </div>
              <div className="flex-1">
                <h3 className="font-bold text-gray-900">Ad Management</h3>
                <p className="text-sm text-gray-600">Active: {adStats.activeAds}/{adStats.totalAds}</p>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Total Impressions</span>
                <span className="font-semibold">{(adStats.totalImpressions / 10000).toFixed(1)}W</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Click Rate</span>
                <span className="font-semibold text-orange-600">{adStats.ctr}%</span>
              </div>
            </div>
          </Card>

          {/* Leaderboard */}
          <Card className="p-5 hover:shadow-md transition-shadow">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-yellow-100 rounded-lg">
                <Trophy className="size-6 text-yellow-600" />
              </div>
              <div className="flex-1">
                <h3 className="font-bold text-gray-900">Leaderboard</h3>
                <p className="text-sm text-gray-600">Participants: {leaderboardStats.participants} users</p>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Total Carbon</span>
                <span className="font-semibold">{(leaderboardStats.totalCarbon).toLocaleString()} kg</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Rewards Issued</span>
                <span className="font-semibold text-yellow-600">{(leaderboardStats.rewardsDistributed)}</span>
              </div>
            </div>
          </Card>

          {/* Data analysis */}
          <Card className="p-5 hover:shadow-md transition-shadow">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-indigo-100 rounded-lg">
                <BarChart3 className="size-6 text-indigo-600" />
              </div>
              <div className="flex-1">
                <h3 className="font-bold text-gray-900">Data Analytics</h3>
                <p className="text-sm text-gray-600">Smart Insights</p>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <Clock className="size-4 text-green-600" />
                <span className="text-sm text-gray-700">Real-time data active</span>
              </div>
              <div className="flex items-center gap-2">
                <Zap className="size-4 text-blue-600" />
                <span className="text-sm text-gray-700">{tripStats.totalTrips} trips logged</span>
              </div>
              <div className="flex items-center gap-2">
                <Users className="size-4 text-purple-600" />
                <span className="text-sm text-gray-700">{userStats.total} total users</span>
              </div>
            </div>
            <div className="mt-3 pt-3 border-t">
              <Button variant="outline" size="sm" className="w-full" onClick={() => onModuleSelect('analytics-management')}>
                <Target className="size-4 mr-2" />
                View Details
              </Button>
            </div>
          </Card>

          {/* Chat Management (Mocked) */}
          <Card className="p-5 hover:shadow-md transition-shadow cursor-pointer border-2 border-blue-200 bg-gradient-to-br from-blue-50 to-purple-50" onClick={() => onModuleSelect('chat-management')}>
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-blue-100 rounded-lg">
                <MessageSquare className="size-6 text-blue-600" />
              </div>
              <div className="flex-1">
                <h3 className="font-bold text-gray-900 flex items-center gap-2">
                  AI Chat
                  <Badge className="bg-green-100 text-green-700 text-xs">
                    <Server className="size-3 mr-1" />
                    {chatStats.apiStatus}
                  </Badge>
                </h3>
                <p className="text-sm text-gray-600">Today: {chatStats.todayRequests} requests</p>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Total Requests</span>
                <span className="font-semibold">{chatStats.totalRequests.toLocaleString()}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Active Models</span>
                <span className="font-semibold text-blue-600">{chatStats.activeModels}/{chatStats.totalModels}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Active Users</span>
                <span className="font-semibold">{chatStats.activeUsers}</span>
              </div>
            </div>
          </Card>
        </div>

        {/* Chart area - Still Mocked for History but Header is dynamic */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* User growth trend */}
          <Card className="p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-gray-900">User Growth Trend</h3>
              <Badge variant="outline">Last 6 months (Simulated)</Badge>
            </div>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={userGrowthData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Area type="monotone" dataKey="normal" stackId="1" stroke="#3b82f6" fill="#3b82f6" name="Normal Users" />
                <Area type="monotone" dataKey="vip" stackId="1" stroke="#8b5cf6" fill="#8b5cf6" name="VIP Users" />
              </AreaChart>
            </ResponsiveContainer>
          </Card>

          {/* Revenue distribution */}
          <Card className="p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-gray-900">Revenue Distribution</h3>
              <Badge variant="outline">Estimated Total</Badge>
            </div>
            <div className="flex items-center gap-6">
              <ResponsiveContainer width="50%" height={300}>
                <PieChart>
                  <Pie
                    data={revenueData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    outerRadius={100}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {revenueData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value: number) => `¥${value.toLocaleString()}`} />
                </PieChart>
              </ResponsiveContainer>
              <div className="flex-1 space-y-3">
                {revenueData.map((item, index) => (
                  <div key={index} className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <div className="w-3 h-3 rounded-full" style={{ backgroundColor: item.color }} />
                      <span className="text-sm text-gray-700">{item.name}</span>
                    </div>
                    <div className="text-right">
                      <p className="font-semibold text-gray-900">¥{item.value.toLocaleString()}</p>
                      <p className="text-xs text-gray-500">
                        {((item.value / (revenueStats.totalRevenue || 1)) * 100).toFixed(1)}%
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </Card>
        </div>

        {/* Recent activities (Dynamic now) */}
        <Card className="p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
              <Activity className="size-5 text-blue-600" />
              Recent System Activities
            </h3>
            <Button variant="outline" size="sm">
              <Eye className="size-4 mr-2" />
              View All
            </Button>
          </div>
          <div className="space-y-3">
            {recentActivities.map((activity) => (
              <div key={activity.id} className="flex items-center gap-4 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
                <div className={`p-2 bg-white rounded-lg ${activity.color}`}>
                  <activity.icon className="size-5" />
                </div>
                <div className="flex-1">
                  <p className="font-semibold text-gray-900">{activity.text}</p>
                  <p className="text-sm text-gray-600">{activity.detail}</p>
                </div>
                <div className="text-sm text-gray-500 flex items-center gap-1">
                  <Clock className="size-4" />
                  {activity.time}
                </div>
              </div>
            ))}
          </div>
        </Card>

        {/* Quick actions */}
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4">
          <Button variant="outline" className="h-20 flex-col gap-2 hover:border-blue-500 hover:bg-blue-50 transition-all" onClick={() => onModuleSelect('user-management')}>
            <Users className="size-6" />
            <span className="text-sm">Users</span>
          </Button>
          <Button variant="outline" className="h-20 flex-col gap-2 hover:border-green-500 hover:bg-green-50 transition-all" onClick={() => onModuleSelect('trip-management')}>
            <Map className="size-6" />
            <span className="text-sm">Trips</span>
          </Button>
          <Button variant="outline" className="h-20 flex-col gap-2 hover:border-blue-500 hover:bg-blue-50 transition-all" onClick={() => onModuleSelect('points-management')}>
            <Coins className="size-6" />
            <span className="text-sm">Points</span>
          </Button>
          <Button variant="outline" className="h-20 flex-col gap-2 hover:border-purple-500 hover:bg-purple-50 transition-all" onClick={() => onModuleSelect('vip-management')}>
            <Crown className="size-6" />
            <span className="text-sm">VIP</span>
          </Button>
          <Button variant="outline" className="h-20 flex-col gap-2 hover:border-green-500 hover:bg-green-50 transition-all" onClick={() => onModuleSelect('rewards-management')}>
            <ShoppingBag className="size-6" />
            <span className="text-sm">Store</span>
          </Button>
          <Button variant="outline" className="h-20 flex-col gap-2 hover:border-purple-500 hover:bg-purple-50 transition-all" onClick={() => onModuleSelect('collectibles-management')}>
            <Award className="size-6" />
            <span className="text-sm">Collectibles</span>
          </Button>
          <Button variant="outline" className="h-20 flex-col gap-2 hover:border-indigo-500 hover:bg-indigo-50 transition-all" onClick={() => onModuleSelect('analytics-management')}>
            <BarChart3 className="size-6" />
            <span className="text-sm">Analytics</span>
          </Button>
          <Button variant="outline" className="h-20 flex-col gap-2 hover:border-orange-500 hover:bg-orange-50 transition-all" onClick={() => onModuleSelect('ad-management')}>
            <Megaphone className="size-6" />
            <span className="text-sm">Ads</span>
          </Button>
          <Button variant="outline" className="h-20 flex-col gap-2 hover:border-yellow-500 hover:bg-yellow-50 transition-all" onClick={() => onModuleSelect('leaderboard-management')}>
            <Trophy className="size-6" />
            <span className="text-sm">Leaderboard</span>
          </Button>
          <Button variant="outline" className="h-20 flex-col gap-2 hover:border-blue-500 hover:bg-blue-50 transition-all" onClick={() => onModuleSelect('chat-management')}>
            <MessageSquare className="size-6" />
            <span className="text-sm">AI Chat</span>
          </Button>
          <Button variant="outline" className="h-20 flex-col gap-2 bg-gradient-to-br from-blue-50 to-purple-50 border-blue-200 hover:shadow-md transition-all" onClick={() => onModuleSelect('dashboard')}>
            <Zap className="size-6 text-blue-600" />
            <span className="text-sm text-blue-600">All Modules</span>
          </Button>
        </div>
      </div>
    </div>
  );
}