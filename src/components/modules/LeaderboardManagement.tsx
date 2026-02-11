import { useState, useEffect, useCallback } from 'react';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Trophy,
  Medal,
  Crown,
  TrendingUp,
  Users,
  Gift,
  ChevronLeft,
  ChevronRight,
  Search,
  Loader2,
  RefreshCw,
  Building2
} from 'lucide-react';
import {
  getRankingsByType,
  getFacultyRankings,
  type LeaderboardRankingDto,
  type LeaderboardStatsDto,
  type LeaderboardType,
  type FacultyCarbonResponse
} from '@/api/leaderboardApi';
import { useDebounce } from '@/hooks/useDebounce';

type ViewMode = 'individual' | 'faculty';

interface UserRanking extends LeaderboardRankingDto {
  avatar?: string;
}

const AVATARS = ['👑', '🏃', '🌟', '💪', '🏅', '🌿', '⭐', '🎯', '🔥', '💎'];
const FACULTY_COLORS = [
  'from-yellow-500 to-amber-600',
  'from-gray-400 to-gray-500',
  'from-orange-400 to-orange-500',
  'from-blue-400 to-blue-500',
  'from-green-400 to-green-500',
  'from-purple-400 to-purple-500',
  'from-pink-400 to-pink-500',
  'from-teal-400 to-teal-500',
];

function computeNewDate(current: string, direction: number, type: LeaderboardType): string {
  if (type === 'DAILY') {
    const d = new Date(current);
    d.setDate(d.getDate() + direction);
    return d.toISOString().split('T')[0];
  }
  const [year, month] = current.split('-').map(Number);
  const d = new Date(year, month - 1 + direction, 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function getRankBadge(rank: number) {
  if (rank === 1) return <Crown className="size-6 text-yellow-500" />;
  if (rank === 2) return <Medal className="size-6 text-gray-400" />;
  if (rank === 3) return <Medal className="size-6 text-orange-400" />;
  return <span className="text-xl font-bold text-gray-600">#{rank}</span>;
}

export function LeaderboardManagement() {
  // View mode: individual or faculty
  const [viewMode, setViewMode] = useState<ViewMode>('individual');

  // Individual leaderboard state
  const [selectedType, setSelectedType] = useState<LeaderboardType>('DAILY');
  const [selectedDate, setSelectedDate] = useState('');
  const [stats, setStats] = useState<LeaderboardStatsDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const debouncedSearchQuery = useDebounce(searchQuery, 300);

  // Faculty leaderboard state
  const [facultyData, setFacultyData] = useState<FacultyCarbonResponse[]>([]);
  const [facultyLoading, setFacultyLoading] = useState(false);
  const [facultyError, setFacultyError] = useState<string | null>(null);

  // ===================== Individual logic =====================

  const getDateLabel = () => {
    if (selectedDate) return selectedDate;
    if (selectedType === 'DAILY') return new Date().toISOString().split('T')[0];
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  };

  const navigateDate = (direction: number) => {
    const current = selectedDate || getDateLabel();
    setSelectedDate(computeNewDate(current, direction, selectedType));
    setCurrentPage(0);
  };

  const canGoNext = () => {
    const current = selectedDate || getDateLabel();
    const today = new Date().toISOString().split('T')[0];
    const thisMonth = `${new Date().getFullYear()}-${String(new Date().getMonth() + 1).padStart(2, '0')}`;
    if (selectedType === 'DAILY') return current < today;
    return current < thisMonth;
  };

  const loadRankings = useCallback(async (type: LeaderboardType, date: string, search: string, page: number) => {
    try {
      setLoading(true);
      setError(null);
      const data = await getRankingsByType(type, date, search, page, 10);
      const enrichedContent: UserRanking[] = data.rankingsPage.content.map((ranking) => ({
        ...ranking,
        avatar: AVATARS[(ranking.rank - 1) % AVATARS.length],
      }));
      setStats({
        ...data,
        rankingsPage: { ...data.rankingsPage, content: enrichedContent },
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load rankings');
    } finally {
      setLoading(false);
    }
  }, []);

  const handleTypeChange = (type: LeaderboardType) => {
    setSelectedType(type);
    setSelectedDate('');
    setCurrentPage(0);
  };

  // ===================== Faculty logic =====================

  const loadFacultyRankings = useCallback(async () => {
    try {
      setFacultyLoading(true);
      setFacultyError(null);
      const data = await getFacultyRankings();
      setFacultyData(data);
    } catch (err) {
      setFacultyError(err instanceof Error ? err.message : 'Failed to load faculty rankings');
    } finally {
      setFacultyLoading(false);
    }
  }, []);

  // ===================== Effects =====================

  useEffect(() => {
    if (viewMode === 'individual') {
      loadRankings(selectedType, selectedDate, debouncedSearchQuery, currentPage);
    } else {
      loadFacultyRankings();
    }
  }, [viewMode, selectedType, selectedDate, debouncedSearchQuery, currentPage, loadRankings, loadFacultyRankings]);

  // ===================== Derived values =====================

  const rankingsPage = stats?.rankingsPage;
  const rankings = (rankingsPage?.content || []) as UserRanking[];
  const totalParticipants = stats?.rankingsPage.totalElements || 0;
  const avgCarbonSaved = totalParticipants > 0
    ? Math.round((stats?.totalCarbonSaved || 0) / totalParticipants)
    : 0;

  const facultyTotalCarbon = facultyData.reduce((sum, f) => sum + f.totalCarbon, 0);
  const facultyCount = facultyData.filter(f => f.totalCarbon > 0).length;

  // ===================== Render helpers =====================

  const renderIndividualTableBody = () => {
    if (loading && rankings.length === 0) {
      return <div className="h-full flex items-center justify-center"><Loader2 className="size-6 animate-spin text-blue-600" /></div>;
    }
    if (rankings.length === 0) {
      return <div className="text-center py-12 text-gray-500"><Trophy className="size-12 mx-auto mb-4 opacity-50" /><p>No rankings found</p><p className="text-sm mt-1">No completed trips for this period.</p></div>;
    }
    return (
      <table className="w-full text-sm">
        <thead className="sticky top-0 bg-white border-b z-10">
          <tr className="text-left text-gray-600">
            <th className="p-4 font-medium w-20 text-center">Rank</th>
            <th className="p-4 font-medium">User Info</th>
            <th className="p-4 font-medium text-right">Carbon Saved</th>
            <th className="p-4 font-medium text-right">Reward Points</th>
            <th className="p-4 font-medium text-center">User Type</th>
          </tr>
        </thead>
        <tbody className="divide-y">
          {rankings.map((user) => (
            <tr key={user.userId} className={`hover:bg-gray-50/50 transition-colors ${user.rank <= 3 ? 'bg-yellow-50/50' : ''}`}>
              <td className="p-4"><div className="flex items-center justify-center">{getRankBadge(user.rank)}</div></td>
              <td className="p-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-400 to-purple-400 flex items-center justify-center text-xl shadow-inner text-white">{user.avatar}</div>
                  <div><p className="font-semibold text-gray-900">{user.nickname}</p><p className="text-xs text-gray-500">ID: {user.userId}</p></div>
                </div>
              </td>
              <td className="p-4 text-right"><p className="font-bold text-gray-900">{user.carbonSaved.toFixed(2)} kg</p></td>
              <td className="p-4 text-right">
                {user.rewardPoints > 0
                  ? <p className="font-semibold text-green-600">+{user.rewardPoints.toLocaleString()}</p>
                  : <p className="text-gray-400">—</p>}
              </td>
              <td className="p-4 text-center">
                {user.isVip
                  ? <Badge className="bg-purple-100 text-purple-700">VIP</Badge>
                  : <Badge variant="outline">Regular</Badge>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  };

  const renderFacultyTableBody = () => {
    if (facultyLoading) {
      return <div className="h-full flex items-center justify-center"><Loader2 className="size-6 animate-spin text-blue-600" /></div>;
    }
    if (facultyData.length === 0) {
      return <div className="text-center py-12 text-gray-500"><Building2 className="size-12 mx-auto mb-4 opacity-50" /><p>No faculty data found</p></div>;
    }
    return (
      <table className="w-full text-sm">
        <thead className="sticky top-0 bg-white border-b z-10">
          <tr className="text-left text-gray-600">
            <th className="p-4 font-medium w-20 text-center">Rank</th>
            <th className="p-4 font-medium">Faculty</th>
            <th className="p-4 font-medium text-right">Total Carbon Saved</th>
          </tr>
        </thead>
        <tbody className="divide-y">
          {facultyData.map((faculty, index) => {
            const rank = index + 1;
            const colorClass = FACULTY_COLORS[index % FACULTY_COLORS.length];
            return (
              <tr key={faculty.faculty} className={`hover:bg-gray-50/50 transition-colors ${rank <= 3 ? 'bg-yellow-50/50' : ''}`}>
                <td className="p-4"><div className="flex items-center justify-center">{getRankBadge(rank)}</div></td>
                <td className="p-4">
                  <div className="flex items-center gap-3">
                    <div className={`w-10 h-10 rounded-lg bg-gradient-to-br ${colorClass} flex items-center justify-center text-white font-bold text-sm shadow-sm`}>{faculty.faculty.substring(0, 2).toUpperCase()}</div>
                    <p className="font-semibold text-gray-900">{faculty.faculty}</p>
                  </div>
                </td>
                <td className="p-4 text-right"><p className="font-bold text-gray-900">{faculty.totalCarbon.toFixed(2)} kg</p></td>
              </tr>
            );
          })}
        </tbody>
      </table>
    );
  };

  // ===================== Loading state =====================

  const isInitialLoading = viewMode === 'individual' ? (loading && !stats) : (facultyLoading && facultyData.length === 0);
  if (isInitialLoading) {
    return (
      <div className="h-full flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <Loader2 className="size-8 animate-spin text-blue-600 mx-auto mb-4" />
          <p className="text-gray-600">Loading leaderboard...</p>
        </div>
      </div>
    );
  }

  // ===================== Render =====================

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Header */}
      <div className="p-6 bg-white border-b">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">Leaderboard Management</h2>
            <p className="text-gray-600 mt-1">View real-time user carbon savings rankings and reward distribution</p>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => viewMode === 'individual'
              ? loadRankings(selectedType, selectedDate, debouncedSearchQuery, currentPage)
              : loadFacultyRankings()
            }
            className="gap-2"
            disabled={viewMode === 'individual' ? loading : facultyLoading}
          >
            <RefreshCw className="size-4" />
            Refresh
          </Button>
        </div>

        {/* Individual / Faculty top-level toggle */}
        <div className="flex items-center gap-1 mt-4 bg-gray-100 rounded-lg p-1 w-fit">
          <Button
            variant={viewMode === 'individual' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setViewMode('individual')}
            className="gap-2"
          >
            <Users className="size-4" />
            Individual
          </Button>
          <Button
            variant={viewMode === 'faculty' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setViewMode('faculty')}
            className="gap-2"
          >
            <Building2 className="size-4" />
            Faculty
          </Button>
        </div>

        {(error || facultyError) && (
          <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
            {error || facultyError}
          </div>
        )}
      </div>

      {/* =================== Individual View =================== */}
      {viewMode === 'individual' && (
        <>
          {/* Statistics Cards */}
          <div className="p-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <Card className="p-4 bg-gradient-to-br from-yellow-500 to-yellow-600 text-white">
              <Trophy className="size-8 mb-2" />
              <p className="text-sm opacity-90 mb-1">Total Participants</p>
              <p className="text-3xl font-bold">{totalParticipants}</p>
              <p className="text-xs opacity-75 mt-1">{selectedType === 'DAILY' ? 'Today' : 'This month'}</p>
            </Card>
            <Card className="p-4 bg-gradient-to-br from-blue-500 to-blue-600 text-white">
              <Users className="size-8 mb-2" />
              <p className="text-sm opacity-90 mb-1">VIP Users</p>
              <p className="text-3xl font-bold">{stats?.totalVipUsers || 0}</p>
              <p className="text-xs opacity-75 mt-1">In this period</p>
            </Card>
            <Card className="p-4 bg-gradient-to-br from-green-500 to-green-600 text-white">
              <TrendingUp className="size-8 mb-2" />
              <p className="text-sm opacity-90 mb-1">Average Carbon Saved</p>
              <p className="text-3xl font-bold">{avgCarbonSaved.toLocaleString()} kg</p>
              <p className="text-xs opacity-75 mt-1">Per user</p>
            </Card>
            <Card className="p-4 bg-gradient-to-br from-purple-500 to-purple-600 text-white">
              <Gift className="size-8 mb-2" />
              <p className="text-sm opacity-90 mb-1">Rewards Distributed</p>
              <p className="text-3xl font-bold">{stats?.totalRewardsDistributed || 0}</p>
              <p className="text-xs opacity-75 mt-1">Top 10 rewarded</p>
            </Card>
          </div>

          {/* Type Toggle & Date Navigation & Search */}
          <div className="px-6 pb-4">
            <Card className="p-4">
              <div className="flex flex-wrap gap-4 items-center">
                <div className="flex items-center gap-1 bg-gray-100 rounded-lg p-1">
                  <Button
                    variant={selectedType === 'DAILY' ? 'default' : 'ghost'}
                    size="sm"
                    onClick={() => handleTypeChange('DAILY')}
                  >
                    Daily
                  </Button>
                  <Button
                    variant={selectedType === 'MONTHLY' ? 'default' : 'ghost'}
                    size="sm"
                    onClick={() => handleTypeChange('MONTHLY')}
                  >
                    Monthly
                  </Button>
                </div>
                <div className="flex items-center gap-2">
                  <Button variant="outline" size="sm" onClick={() => navigateDate(-1)}>
                    <ChevronLeft className="size-4" />
                  </Button>
                  <span className="text-sm font-medium text-gray-700 min-w-[110px] text-center">
                    {getDateLabel()}
                  </span>
                  <Button variant="outline" size="sm" onClick={() => navigateDate(1)} disabled={!canGoNext()}>
                    <ChevronRight className="size-4" />
                  </Button>
                </div>
                <div className="flex-1" />
                <div className="relative w-full sm:w-[250px]">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-gray-400" />
                  <Input
                    placeholder="Search by nickname..."
                    value={searchQuery}
                    onChange={(e) => { setSearchQuery(e.target.value); setCurrentPage(0); }}
                    className="pl-9"
                  />
                </div>
              </div>
            </Card>
          </div>

          {/* Individual Rankings Table */}
          <div className="px-6 pb-6 flex-1 flex flex-col">
            <Card className="flex-1 flex flex-col">
              <div className="p-4 border-b bg-gray-50">
                <h3 className="font-bold text-gray-900">
                  {selectedType === 'DAILY' ? 'Daily' : 'Monthly'} Rankings
                </h3>
                <p className="text-sm text-gray-600">{totalParticipants} users found</p>
              </div>
              <div className="flex-1 overflow-y-auto">
                {renderIndividualTableBody()}
              </div>
              {rankingsPage && rankingsPage.totalPages > 1 && (
                <div className="p-4 border-t flex items-center justify-between">
                  <p className="text-sm text-gray-600">
                    Page {rankingsPage.number + 1} of {rankingsPage.totalPages}
                  </p>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCurrentPage(currentPage - 1)}
                      disabled={currentPage === 0}
                    >
                      <ChevronLeft className="size-4 mr-1" />
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCurrentPage(currentPage + 1)}
                      disabled={currentPage >= rankingsPage.totalPages - 1}
                    >
                      Next
                      <ChevronRight className="size-4 ml-1" />
                    </Button>
                  </div>
                </div>
              )}
            </Card>
          </div>
        </>
      )}

      {/* =================== Faculty View =================== */}
      {viewMode === 'faculty' && (
        <>
          {/* Faculty Statistics Cards */}
          <div className="p-6 grid grid-cols-1 md:grid-cols-3 gap-4">
            <Card className="p-4 bg-gradient-to-br from-indigo-500 to-indigo-600 text-white">
              <Building2 className="size-8 mb-2" />
              <p className="text-sm opacity-90 mb-1">Total Faculties</p>
              <p className="text-3xl font-bold">{facultyData.length}</p>
              <p className="text-xs opacity-75 mt-1">Registered</p>
            </Card>
            <Card className="p-4 bg-gradient-to-br from-green-500 to-green-600 text-white">
              <TrendingUp className="size-8 mb-2" />
              <p className="text-sm opacity-90 mb-1">Total Carbon Saved</p>
              <p className="text-3xl font-bold">{facultyTotalCarbon.toFixed(2)} kg</p>
              <p className="text-xs opacity-75 mt-1">This month</p>
            </Card>
            <Card className="p-4 bg-gradient-to-br from-amber-500 to-amber-600 text-white">
              <Trophy className="size-8 mb-2" />
              <p className="text-sm opacity-90 mb-1">Active Faculties</p>
              <p className="text-3xl font-bold">{facultyCount}</p>
              <p className="text-xs opacity-75 mt-1">With carbon savings</p>
            </Card>
          </div>

          {/* Faculty Rankings Table */}
          <div className="px-6 pb-6 flex-1 flex flex-col">
            <Card className="flex-1 flex flex-col">
              <div className="p-4 border-b bg-gray-50">
                <h3 className="font-bold text-gray-900">Faculty Monthly Rankings</h3>
                <p className="text-sm text-gray-600">
                  {new Date().toLocaleString('en-US', { month: 'long', year: 'numeric' })} — {facultyData.length} faculties
                </p>
              </div>
              <div className="flex-1 overflow-y-auto">
                {renderFacultyTableBody()}
              </div>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}
