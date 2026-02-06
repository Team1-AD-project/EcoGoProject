import { useState, useEffect } from 'react';
import { Card } from '@/components/ui/card';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Crown,
  Users,
  CheckCircle,
  XCircle,
  Calendar,
  TrendingUp,
  Settings,
  Zap,
  Loader2
} from 'lucide-react';
import { fetchUserList, type User } from '@/services/userService';
import { fetchVipSwitches, updateVipSwitch, type VipSwitch } from '@/services/vipService';
import { toast } from 'sonner';

// Helper to map keys to icons
const getFeatureIcon = (key: string) => {
  const lowerKey = key.toLowerCase();
  if (lowerKey.includes('badge')) return <Crown className="size-5 text-purple-600" />;
  if (lowerKey.includes('point')) return <Zap className="size-5 text-yellow-600" />;
  if (lowerKey.includes('support')) return <CheckCircle className="size-5 text-green-600" />;
  if (lowerKey.includes('analytic')) return <TrendingUp className="size-5 text-blue-600" />;
  if (lowerKey.includes('goods')) return <Users className="size-5 text-blue-600" />;
  if (lowerKey.includes('voucher')) return <Calendar className="size-5 text-orange-600" />;
  return <Settings className="size-5 text-gray-600" />;
};

export function VIPManagement() {
  const [users, setUsers] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [vipFeatures, setVipFeatures] = useState<VipSwitch[]>([]);

  useEffect(() => {
    loadUsers();
    loadVipFeatures();
  }, []);

  const loadUsers = async () => {
    setIsLoading(true);
    try {
      // Fetch a larger list to find VIPs. In a real app, backend should support filtering by VIP status.
      // For now, fetching first 100 users.
      const response = await fetchUserList(1, 100);
      if (response && response.code === 200) {
        setUsers(response.data?.list || []);
      } else {
        toast.error('Failed to fetch users');
      }
    } catch (error) {
      console.error(error);
      toast.error('Error loading users');
    } finally {
      setIsLoading(false);
    }
  };

  const loadVipFeatures = async () => {
    try {
      const response = await fetchVipSwitches();
      if (response && response.code === 200 && response.data) {
        setVipFeatures(response.data);
      }
    } catch (error) {
      console.error('Failed to load VIP features', error);
      toast.error('Failed to load VIP features');
    }
  };

  const handleToggleFeature = async (id: string, currentStatus: boolean, switchKey: string) => {
    // Optimistic update
    setVipFeatures(prev => prev.map(f => f.id === id ? { ...f, enabled: !f.enabled } : f));

    try {
      await updateVipSwitch({
        switchKey,
        isEnabled: !currentStatus,
        updatedBy: 'admin'
      });
      toast.success('Feature status updated');
      loadVipFeatures();
    } catch (error) {
      console.error('Failed to update feature', error);
      toast.error('Failed to update feature');
      loadVipFeatures();
    }
  };



  // Filter only users who are VIPs or have a plan
  const vipUsers = users.filter(u => u.vip?.active || u.vip?.plan);

  const activeCount = vipUsers.filter(u => u.vip?.active).length;
  // Calculate expiring soon (e.g., within 7 days)
  const expiringCount = vipUsers.filter(u => {
    if (!u.vip?.expiryDate) return false;
    const days = Math.ceil((new Date(u.vip.expiryDate).getTime() - new Date().getTime()) / (1000 * 3600 * 24));
    return days > 0 && days <= 7;
  }).length;

  // Total Revenue Estimate: Sum of (Total Earned - Current Balance) for VIP users
  // Note: This is an approximation as per user instruction.
  const totalRevenue = vipUsers.reduce((sum, user) => sum + Math.max(0, user.totalPoints - user.currentPoints), 0);

  const getPlanBadgeColor = (plan: string | null) => {
    if (!plan) return 'bg-gray-100 text-gray-700';
    const p = plan.toLowerCase();
    if (p.includes('month')) return 'bg-blue-100 text-blue-700';
    if (p.includes('quarter')) return 'bg-purple-100 text-purple-700';
    if (p.includes('year')) return 'bg-amber-100 text-amber-700';
    return 'bg-gray-100 text-gray-700';
  };

  const getStatusBadge = (user: User) => {
    if (user.vip?.active) {
      // Check if expiring
      if (user.vip.expiryDate) {
        const days = Math.ceil((new Date(user.vip.expiryDate).getTime() - new Date().getTime()) / (1000 * 3600 * 24));
        if (days <= 0) return <Badge className="bg-red-100 text-red-700">Expired</Badge>;
        if (days <= 7) return <Badge className="bg-orange-100 text-orange-700">Expiring Soon</Badge>;
      }
      return <Badge className="bg-green-100 text-green-700">Active</Badge>;
    }
    return <Badge className="bg-red-100 text-red-700">Expired</Badge>;
  };

  return (
    <div className="p-6 space-y-6 bg-gray-50 min-h-full">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900">VIP Subscription Management</h2>
        <p className="text-gray-600 mt-1">Manage VIP user memberships and premium features</p>
      </div>

      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Total VIP Users</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">{vipUsers.length}</p>
            </div>
            <div className="size-12 bg-purple-50 rounded-lg flex items-center justify-center">
              <Crown className="size-6 text-purple-600" />
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Active</p>
              <p className="text-3xl font-bold text-green-600 mt-1">{activeCount}</p>
            </div>
            <div className="size-12 bg-green-50 rounded-lg flex items-center justify-center">
              <CheckCircle className="size-6 text-green-600" />
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Expiring Soon</p>
              <p className="text-3xl font-bold text-orange-600 mt-1">{expiringCount}</p>
            </div>
            <div className="size-12 bg-orange-50 rounded-lg flex items-center justify-center">
              <Calendar className="size-6 text-orange-600" />
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Est. Revenue</p>
              <p className="text-3xl font-bold text-blue-600 mt-1">{totalRevenue}</p>
              <p className="text-xs text-gray-500 mt-1">pts spent</p>
            </div>
            <div className="size-12 bg-blue-50 rounded-lg flex items-center justify-center">
              <TrendingUp className="size-6 text-blue-600" />
            </div>
          </div>
        </Card>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="users" className="w-full">
        <TabsList className="grid w-full max-w-md grid-cols-2">
          <TabsTrigger value="users">
            <Users className="size-4 mr-2" />
            VIP Users
          </TabsTrigger>
          <TabsTrigger value="features">
            <Settings className="size-4 mr-2" />
            Features
          </TabsTrigger>
        </TabsList>

        {/* VIP Users Tab */}
        <TabsContent value="users" className="mt-6">
          <Card>
            <div className="p-4 border-b bg-gray-50">
              <h3 className="font-semibold text-gray-900">VIP User List</h3>
              <p className="text-xs text-gray-600 mt-1">Manage VIP subscriptions and settings</p>
            </div>
            <ScrollArea className="h-[600px]">
              <div className="p-4 space-y-4">
                {isLoading ? (
                  <div className="text-center py-12 flex flex-col items-center justify-center text-gray-500">
                    <Loader2 className="size-8 animate-spin mb-2" />
                    Loading VIP users...
                  </div>
                ) : vipUsers.length === 0 ? (
                  <div className="text-center py-12 text-gray-500">
                    No VIP users found.
                  </div>
                ) : (
                  vipUsers.map((user) => (
                    <Card key={user.id} className="p-4 hover:shadow-md transition-shadow">
                      <div className="flex items-start gap-4">
                        <Avatar className="size-14 flex-shrink-0">
                          <AvatarFallback className="bg-purple-600 text-white text-lg">
                            {user.nickname?.substring(0, 2).toUpperCase() || 'U'}
                          </AvatarFallback>
                        </Avatar>

                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-2">
                            <h4 className="font-semibold text-gray-900">{user.nickname}</h4>
                            <Badge className={getPlanBadgeColor(user.vip?.plan)}>
                              {user.vip?.plan || 'Unknown Plan'}
                            </Badge>
                            {getStatusBadge(user)}
                          </div>

                          <p className="text-sm text-gray-600 mb-3">{user.email}</p>

                          <div className="grid grid-cols-2 gap-4 mb-3">
                            <div>
                              <p className="text-xs text-gray-500">Start Date</p>
                              <p className="text-sm font-medium text-gray-900">
                                {user.vip?.startDate ? new Date(user.vip.startDate).toLocaleDateString() : 'N/A'}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs text-gray-500">End Date</p>
                              <p className="text-sm font-medium text-gray-900">
                                {user.vip?.expiryDate ? new Date(user.vip.expiryDate).toLocaleDateString() : 'N/A'}
                              </p>
                            </div>
                            <div>
                              <p className="text-xs text-gray-500">Total Spent</p>
                              <p className="text-sm font-medium text-blue-600">
                                {Math.max(0, user.totalPoints - user.currentPoints)} pts
                              </p>
                            </div>
                            <div>
                              <p className="text-xs text-gray-500">Days Remaining</p>
                              <p className="text-sm font-medium text-gray-900">
                                {user.vip?.expiryDate
                                  ? Math.max(0, Math.ceil((new Date(user.vip.expiryDate).getTime() - new Date().getTime()) / (1000 * 3600 * 24)))
                                  : 0} days
                              </p>
                            </div>
                          </div>


                        </div>
                      </div>
                    </Card>
                  ))
                )}
              </div>
            </ScrollArea>
          </Card>
        </TabsContent>

        {/* VIP Features Tab */}
        <TabsContent value="features" className="mt-6">
          <Card>
            <div className="p-4 border-b bg-gray-50">
              <h3 className="font-semibold text-gray-900">VIP Feature Management</h3>
              <p className="text-xs text-gray-600 mt-1">Enable or disable premium features</p>
            </div>
            <div className="p-4 space-y-4">
              {vipFeatures.map((feature) => (
                <Card key={feature.id} className="p-4 hover:shadow-md transition-shadow">
                  <div className="flex items-center gap-4">
                    <div className="flex-shrink-0">
                      {getFeatureIcon(feature.switchKey)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <h4 className="font-semibold text-gray-900 mb-1">{feature.displayName}</h4>
                      <p className="text-sm text-gray-600">{feature.description}</p>
                    </div>
                    <div className="flex items-center gap-3 flex-shrink-0">
                      <Badge className={feature.enabled ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'}>
                        {feature.enabled ? 'Active' : 'Disabled'}
                      </Badge>
                      <Switch
                        checked={feature.enabled}
                        onCheckedChange={() => handleToggleFeature(feature.id, feature.enabled, feature.switchKey)}
                      />
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
