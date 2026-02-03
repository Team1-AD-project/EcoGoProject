import { useState, useEffect } from 'react';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Trophy,
  Medal,
  Crown,
  TrendingUp,
  TrendingDown,
  Minus,
  Calendar,
  Users,
  Gift,
  ChevronLeft,
  ChevronRight,
  Search,
  ChevronDown,
  ChevronUp,
  Loader2,
  RefreshCw
} from 'lucide-react';
import {
  getLeaderboardPeriods,
  getRankingsByPeriod,
  type Ranking
} from '@/api/leaderboardApi';

// 前端扩展的排名类型
interface UserRanking extends Ranking {
  username?: string;
  avatar?: string;
  points?: number;
  distance?: number;
  previousRank?: number | null;
  rewards?: string[];
  avgDailySteps?: number;
  activeDays?: number;
  longestStreak?: number;
  favoriteRoute?: string;
}

// 默认头像列表
const AVATARS = ['👑', '🏃', '🌟', '💪', '🏅', '🌿', '⭐', '🎯', '🔥', '💎'];

export function LeaderboardManagement() {
  const [periods, setPeriods] = useState<string[]>([]);
  const [selectedPeriod, setSelectedPeriod] = useState<string>('');
  const [rankings, setRankings] = useState<UserRanking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterUserType, setFilterUserType] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());
  const [showFullRankings, setShowFullRankings] = useState(true);

  // 加载周期列表
  const loadPeriods = async () => {
    try {
      const data = await getLeaderboardPeriods();
      setPeriods(data);
      if (data.length > 0 && !selectedPeriod) {
        setSelectedPeriod(data[0]);
      }
    } catch (err) {
      console.error('Error loading periods:', err);
    }
  };

  // 加载排行榜数据
  const loadRankings = async (period: string) => {
    if (!period) return;

    try {
      setLoading(true);
      setError(null);
      const data = await getRankingsByPeriod(period);

      // 为每个排名添加前端默认值
      const enrichedRankings: UserRanking[] = data.map((ranking, index) => ({
        ...ranking,
        username: ranking.nickname || `User ${ranking.userId}`,
        avatar: AVATARS[index % AVATARS.length],
        points: ranking.steps ? Math.floor(ranking.steps / 10) : 0,
        distance: ranking.steps ? parseFloat((ranking.steps * 0.0007).toFixed(1)) : 0,
        previousRank: index > 0 ? ranking.rank + (Math.random() > 0.5 ? 1 : -1) : null,
        rewards: ranking.rank <= 3 ? ['Top 3 Badge', `${(4 - ranking.rank) * 300} Points`] : ranking.rank <= 10 ? ['Top 10 Badge'] : [],
        avgDailySteps: ranking.steps ? Math.floor(ranking.steps / 7) : 0,
        activeDays: 7,
        longestStreak: Math.floor(Math.random() * 30) + 10,
        favoriteRoute: ['Central Park', 'Riverside Trail', 'City Greenway', 'Forest Park'][index % 4]
      }));

      setRankings(enrichedRankings);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load rankings');
      console.error('Error loading rankings:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPeriods();
  }, []);

  useEffect(() => {
    if (selectedPeriod) {
      loadRankings(selectedPeriod);
    }
  }, [selectedPeriod]);

  const toggleRowExpansion = (userId: string) => {
    setExpandedRows(prev => {
      const newSet = new Set(prev);
      if (newSet.has(userId)) {
        newSet.delete(userId);
      } else {
        newSet.add(userId);
      }
      return newSet;
    });
  };

  const getRankTrendIcon = (current: number, previous: number | null | undefined) => {
    if (previous === null || previous === undefined) return <Minus className="size-4 text-gray-400" />;
    if (current < previous) return <TrendingUp className="size-4 text-green-600" />;
    if (current > previous) return <TrendingDown className="size-4 text-red-600" />;
    return <Minus className="size-4 text-gray-400" />;
  };

  const getRankTrendText = (current: number, previous: number | null | undefined) => {
    if (previous === null || previous === undefined) return 'New';
    const diff = previous - current;
    if (diff > 0) return `↑${diff}`;
    if (diff < 0) return `↓${Math.abs(diff)}`;
    return '—';
  };

  const getRankBadge = (rank: number) => {
    if (rank === 1) return <Crown className="size-6 text-yellow-500" />;
    if (rank === 2) return <Medal className="size-6 text-gray-400" />;
    if (rank === 3) return <Medal className="size-6 text-orange-400" />;
    return <span className="text-xl font-bold text-gray-600">#{rank}</span>;
  };

  const filteredRankings = rankings.filter(user => {
    const typeMatch = filterUserType === 'all' ||
      (filterUserType === 'vip' && user.isVip) ||
      (filterUserType === 'normal' && !user.isVip);
    const searchMatch = searchQuery === '' ||
      (user.username?.toLowerCase().includes(searchQuery.toLowerCase())) ||
      user.userId.toLowerCase().includes(searchQuery.toLowerCase());
    return typeMatch && searchMatch;
  });

  const topThree = rankings.slice(0, 3);
  const vipCount = rankings.filter(u => u.isVip).length;
  const totalSteps = rankings.reduce((sum, u) => sum + (u.steps || 0), 0);
  const avgSteps = Math.round(totalSteps / (rankings.length || 1));

  const currentPeriodIndex = periods.indexOf(selectedPeriod);
  const canGoPrevious = currentPeriodIndex < periods.length - 1;
  const canGoNext = currentPeriodIndex > 0;

  const goToPreviousPeriod = () => {
    if (canGoPrevious) {
      setSelectedPeriod(periods[currentPeriodIndex + 1]);
    }
  };

  const goToNextPeriod = () => {
    if (canGoNext) {
      setSelectedPeriod(periods[currentPeriodIndex - 1]);
    }
  };

  if (loading && rankings.length === 0) {
    return (
      <div className="h-full flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <Loader2 className="size-8 animate-spin text-blue-600 mx-auto mb-4" />
          <p className="text-gray-600">Loading leaderboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Header */}
      <div className="p-6 bg-white border-b">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">Leaderboard Management</h2>
            <p className="text-gray-600 mt-1">View and manage weekly user step rankings and reward distribution</p>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => loadRankings(selectedPeriod)}
            className="gap-2"
          >
            <RefreshCw className="size-4" />
            Refresh
          </Button>
        </div>
        {error && (
          <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
            {error}
          </div>
        )}
      </div>

      {/* Statistics Cards */}
      <div className="p-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 bg-gradient-to-br from-yellow-500 to-yellow-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <Trophy className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">Total Participants</p>
          <p className="text-3xl font-bold">{rankings.length}</p>
          <p className="text-xs opacity-75 mt-1">This period</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-blue-500 to-blue-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <Users className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">VIP Users</p>
          <p className="text-3xl font-bold">{vipCount}</p>
          <p className="text-xs opacity-75 mt-1">{rankings.length > 0 ? Math.round(vipCount / rankings.length * 100) : 0}% of total</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-green-500 to-green-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <TrendingUp className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">Average Steps</p>
          <p className="text-3xl font-bold">{avgSteps.toLocaleString()}</p>
          <p className="text-xs opacity-75 mt-1">Per user</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-purple-500 to-purple-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <Gift className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">Rewards Distributed</p>
          <p className="text-3xl font-bold">{Math.min(rankings.length, 10)}</p>
          <p className="text-xs opacity-75 mt-1">Top 10 rewarded</p>
        </Card>
      </div>

      {/* Period Selector & Filters */}
      <div className="px-6 pb-4">
        <Card className="p-4">
          <div className="flex flex-wrap gap-4 items-center">
            {/* Period Navigation */}
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={goToPreviousPeriod}
                disabled={!canGoPrevious}
              >
                <ChevronLeft className="size-4" />
              </Button>

              <Select value={selectedPeriod} onValueChange={setSelectedPeriod}>
                <SelectTrigger className="w-[200px]">
                  <Calendar className="size-4 mr-2" />
                  <SelectValue placeholder="Select period" />
                </SelectTrigger>
                <SelectContent>
                  {periods.map(period => (
                    <SelectItem key={period} value={period}>
                      {period}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Button
                variant="outline"
                size="sm"
                onClick={goToNextPeriod}
                disabled={!canGoNext}
              >
                <ChevronRight className="size-4" />
              </Button>
            </div>

            <div className="flex-1" />

            {/* User Type Filter */}
            <Select value={filterUserType} onValueChange={setFilterUserType}>
              <SelectTrigger className="w-[150px]">
                <SelectValue placeholder="User Type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Users</SelectItem>
                <SelectItem value="vip">VIP Users</SelectItem>
                <SelectItem value="normal">Regular Users</SelectItem>
              </SelectContent>
            </Select>

            {/* Search */}
            <div className="relative w-[250px]">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-gray-400" />
              <Input
                placeholder="Search username or ID..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9"
              />
            </div>
          </div>
        </Card>
      </div>

      {/* Top 3 Podium */}
      {topThree.length > 0 && (
        <div className="px-6 pb-4">
          <Card className="p-6 bg-gradient-to-br from-yellow-50 to-orange-50">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                <Trophy className="size-5 text-yellow-600" />
                Top 3 This Period
              </h3>
              <Button
                variant="outline"
                onClick={() => setShowFullRankings(!showFullRankings)}
                className="gap-2"
              >
                {showFullRankings ? (
                  <>
                    <ChevronUp className="size-4" />
                    Hide Full Rankings
                  </>
                ) : (
                  <>
                    <ChevronDown className="size-4" />
                    View Full Rankings
                  </>
                )}
              </Button>
            </div>
            <div className="grid grid-cols-3 gap-4">
              {/* 2nd Place */}
              {topThree[1] && (
                <div className="flex flex-col items-center">
                  <div className="w-20 h-20 rounded-full bg-gradient-to-br from-gray-300 to-gray-400 flex items-center justify-center text-3xl mb-2 shadow-lg">
                    {topThree[1].avatar}
                  </div>
                  <Medal className="size-8 text-gray-400 mb-2" />
                  <p className="font-bold text-gray-900">{topThree[1].nickname || topThree[1].username}</p>
                  <p className="text-sm text-gray-600">{(topThree[1].steps || 0).toLocaleString()} steps</p>
                  {topThree[1].isVip && (
                    <Badge className="mt-1 bg-purple-100 text-purple-700">VIP</Badge>
                  )}
                </div>
              )}

              {/* 1st Place */}
              {topThree[0] && (
                <div className="flex flex-col items-center -mt-4">
                  <div className="w-24 h-24 rounded-full bg-gradient-to-br from-yellow-400 to-yellow-500 flex items-center justify-center text-4xl mb-2 shadow-xl ring-4 ring-yellow-300">
                    {topThree[0].avatar}
                  </div>
                  <Crown className="size-10 text-yellow-500 mb-2" />
                  <p className="font-bold text-gray-900 text-lg">{topThree[0].nickname || topThree[0].username}</p>
                  <p className="text-sm text-gray-600">{(topThree[0].steps || 0).toLocaleString()} steps</p>
                  {topThree[0].isVip && (
                    <Badge className="mt-1 bg-purple-100 text-purple-700">VIP</Badge>
                  )}
                </div>
              )}

              {/* 3rd Place */}
              {topThree[2] && (
                <div className="flex flex-col items-center">
                  <div className="w-20 h-20 rounded-full bg-gradient-to-br from-orange-300 to-orange-400 flex items-center justify-center text-3xl mb-2 shadow-lg">
                    {topThree[2].avatar}
                  </div>
                  <Medal className="size-8 text-orange-400 mb-2" />
                  <p className="font-bold text-gray-900">{topThree[2].nickname || topThree[2].username}</p>
                  <p className="text-sm text-gray-600">{(topThree[2].steps || 0).toLocaleString()} steps</p>
                  {topThree[2].isVip && (
                    <Badge className="mt-1 bg-purple-100 text-purple-700">VIP</Badge>
                  )}
                </div>
              )}
            </div>
          </Card>
        </div>
      )}

      {/* Rankings Table */}
      {showFullRankings && (
        <div className="flex-1 overflow-hidden px-6 pb-6">
          <Card className="h-full flex flex-col">
            <div className="p-4 border-b bg-gray-50">
              <h3 className="font-bold text-gray-900">Full Rankings</h3>
              <p className="text-sm text-gray-600">{filteredRankings.length} users</p>
            </div>

            <div className="flex-1 overflow-y-auto">
              {filteredRankings.length === 0 ? (
                <div className="text-center py-12 text-gray-500">
                  <Trophy className="size-12 mx-auto mb-4 opacity-50" />
                  <p>No rankings found</p>
                  <p className="text-sm mt-1">Select a different period or adjust filters</p>
                </div>
              ) : (
                <table className="w-full">
                  <thead className="sticky top-0 bg-white border-b z-10">
                    <tr className="text-left text-sm text-gray-600">
                      <th className="p-4 font-medium w-20">Rank</th>
                      <th className="p-4 font-medium">User Info</th>
                      <th className="p-4 font-medium text-right">Steps</th>
                      <th className="p-4 font-medium text-right">Points</th>
                      <th className="p-4 font-medium text-center">Rank Change</th>
                      <th className="p-4 font-medium text-center">User Type</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredRankings.map((user) => (
                      <tr
                        key={user.id}
                        className={`border-b hover:bg-gray-50 transition-colors ${
                          user.rank <= 3 ? 'bg-yellow-50/50' : ''
                        }`}
                      >
                        <td className="p-4">
                          <div className="flex items-center justify-center">
                            {getRankBadge(user.rank)}
                          </div>
                        </td>
                        <td className="p-4">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-400 to-purple-400 flex items-center justify-center text-xl">
                              {user.avatar}
                            </div>
                            <div>
                              <p className="font-semibold text-gray-900">{user.nickname || user.username}</p>
                              <p className="text-sm text-gray-500">{user.userId}</p>
                            </div>
                          </div>
                        </td>
                        <td className="p-4 text-right">
                          <p className="font-bold text-gray-900">{(user.steps || 0).toLocaleString()}</p>
                          <p className="text-xs text-gray-500">{(user.avgDailySteps || 0).toLocaleString()}/day</p>
                        </td>
                        <td className="p-4 text-right">
                          <p className="font-semibold text-blue-600">{(user.points || 0).toLocaleString()}</p>
                        </td>
                        <td className="p-4">
                          <div className="flex items-center justify-center gap-1">
                            {getRankTrendIcon(user.rank, user.previousRank)}
                            <span className="text-sm font-medium">{getRankTrendText(user.rank, user.previousRank)}</span>
                          </div>
                        </td>
                        <td className="p-4 text-center">
                          {user.isVip ? (
                            <Badge className="bg-purple-100 text-purple-700">VIP</Badge>
                          ) : (
                            <Badge variant="outline">Regular</Badge>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
