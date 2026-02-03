import { useState, useEffect } from 'react';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Megaphone,
  Edit,
  Trash2,
  Plus,
  Eye,
  MousePointerClick,
  TrendingUp,
  Calendar,
  MapPin,
  Link as LinkIcon,
  AlertCircle,
  Power,
  Loader2,
  RefreshCw
} from 'lucide-react';
import {
  getAllAdvertisements,
  createAdvertisement,
  updateAdvertisement,
  deleteAdvertisement,
  updateAdvertisementStatus,
  type Advertisement as ApiAdvertisement
} from '@/api/advertisementApi';

// 前端扩展的广告类型（包含 UI 所需的额外字段）
interface Advertisement extends ApiAdvertisement {
  description?: string;
  imageUrl?: string;
  linkUrl?: string;
  position?: 'banner' | 'sidebar' | 'popup' | 'feed';
  impressions?: number;
  clicks?: number;
  clickRate?: number;
  budget?: number;
  spent?: number;
  targetAudience?: string;
}

// 默认图片
const DEFAULT_IMAGES = [
  'https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?w=800',
  'https://images.unsplash.com/photo-1607083206869-4c7672e72a8a?w=800',
  'https://images.unsplash.com/photo-1580870069867-74c57ee1bb07?w=800',
  'https://images.unsplash.com/photo-1579547621700-1d0ca0f38e8a?w=800',
];

export function AdManagement() {
  const [ads, setAds] = useState<Advertisement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingAd, setEditingAd] = useState<Advertisement | null>(null);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [adToDelete, setAdToDelete] = useState<Advertisement | null>(null);
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [filterPosition, setFilterPosition] = useState<string>('all');
  const [saving, setSaving] = useState(false);

  const [newAd, setNewAd] = useState<Partial<Advertisement>>({
    name: '',
    description: '',
    imageUrl: '',
    linkUrl: '',
    position: 'banner',
    status: 'Active',
    startDate: '',
    endDate: '',
    budget: 0,
    targetAudience: 'All Users'
  });

  // 加载广告数据
  const loadAds = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAllAdvertisements();
      // 为每个广告添加前端默认值
      const enrichedAds: Advertisement[] = data.map((ad, index) => ({
        ...ad,
        description: ad.name + ' - Advertisement',
        imageUrl: DEFAULT_IMAGES[index % DEFAULT_IMAGES.length],
        linkUrl: '#',
        position: (['banner', 'sidebar', 'popup', 'feed'] as const)[index % 4],
        impressions: Math.floor(Math.random() * 50000) + 10000,
        clicks: Math.floor(Math.random() * 5000) + 1000,
        clickRate: parseFloat((Math.random() * 10 + 2).toFixed(2)),
        budget: Math.floor(Math.random() * 100000) + 20000,
        spent: Math.floor(Math.random() * 50000) + 5000,
        targetAudience: 'All Users'
      }));
      setAds(enrichedAds);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load advertisements');
      console.error('Error loading ads:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAds();
  }, []);

  const handleEditAd = (ad: Advertisement) => {
    setEditingAd({ ...ad });
    setIsEditDialogOpen(true);
  };

  const handleSaveEdit = async () => {
    if (editingAd) {
      try {
        setSaving(true);
        await updateAdvertisement(editingAd.id, {
          name: editingAd.name,
          status: editingAd.status,
          startDate: editingAd.startDate,
          endDate: editingAd.endDate
        });
        setAds(ads.map(a => a.id === editingAd.id ? editingAd : a));
        setIsEditDialogOpen(false);
        setEditingAd(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to update advertisement');
      } finally {
        setSaving(false);
      }
    }
  };

  const handleAddAd = async () => {
    try {
      setSaving(true);
      const createdAd = await createAdvertisement({
        name: newAd.name || '',
        status: newAd.status || 'Active',
        startDate: newAd.startDate || '',
        endDate: newAd.endDate || ''
      });

      // 添加前端字段
      const enrichedAd: Advertisement = {
        ...createdAd,
        description: newAd.description || '',
        imageUrl: newAd.imageUrl || DEFAULT_IMAGES[0],
        linkUrl: newAd.linkUrl || '#',
        position: newAd.position || 'banner',
        impressions: 0,
        clicks: 0,
        clickRate: 0,
        budget: newAd.budget || 0,
        spent: 0,
        targetAudience: newAd.targetAudience || 'All Users'
      };

      setAds([...ads, enrichedAd]);
      setIsAddDialogOpen(false);
      setNewAd({
        name: '',
        description: '',
        imageUrl: '',
        linkUrl: '',
        position: 'banner',
        status: 'Active',
        startDate: '',
        endDate: '',
        budget: 0,
        targetAudience: 'All Users'
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create advertisement');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteAd = (ad: Advertisement) => {
    setAdToDelete(ad);
    setIsDeleteDialogOpen(true);
  };

  const confirmDelete = async () => {
    if (adToDelete) {
      try {
        setSaving(true);
        await deleteAdvertisement(adToDelete.id);
        setAds(ads.filter(a => a.id !== adToDelete.id));
        setIsDeleteDialogOpen(false);
        setAdToDelete(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to delete advertisement');
      } finally {
        setSaving(false);
      }
    }
  };

  const toggleAdStatus = async (adId: string) => {
    const ad = ads.find(a => a.id === adId);
    if (!ad) return;

    const newStatus = ad.status === 'Active' ? 'Inactive' : 'Active';
    try {
      await updateAdvertisementStatus(adId, newStatus);
      setAds(ads.map(a => {
        if (a.id === adId) {
          return { ...a, status: newStatus };
        }
        return a;
      }));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update status');
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status.toLowerCase()) {
      case 'active':
        return <Badge className="bg-green-100 text-green-700">Active</Badge>;
      case 'inactive':
        return <Badge className="bg-gray-100 text-gray-700">Inactive</Badge>;
      case 'paused':
        return <Badge className="bg-yellow-100 text-yellow-700">Paused</Badge>;
      case 'scheduled':
        return <Badge className="bg-blue-100 text-blue-700">Scheduled</Badge>;
      default:
        return <Badge className="bg-gray-100 text-gray-700">{status}</Badge>;
    }
  };

  const getPositionLabel = (position: string | undefined) => {
    const labels: Record<string, string> = {
      banner: 'Banner',
      sidebar: 'Sidebar',
      popup: 'Popup',
      feed: 'Feed'
    };
    return labels[position || 'banner'] || 'Banner';
  };

  const filteredAds = ads.filter(ad => {
    const statusMatch = filterStatus === 'all' || ad.status.toLowerCase() === filterStatus.toLowerCase();
    const positionMatch = filterPosition === 'all' || ad.position === filterPosition;
    return statusMatch && positionMatch;
  });

  const totalAds = ads.length;
  const activeAds = ads.filter(a => a.status.toLowerCase() === 'active').length;
  const totalImpressions = ads.reduce((sum, a) => sum + (a.impressions || 0), 0);
  const totalClicks = ads.reduce((sum, a) => sum + (a.clicks || 0), 0);
  const avgClickRate = totalImpressions > 0 ? (totalClicks / totalImpressions) * 100 : 0;
  const totalBudget = ads.reduce((sum, a) => sum + (a.budget || 0), 0);
  const totalSpent = ads.reduce((sum, a) => sum + (a.spent || 0), 0);

  if (loading) {
    return (
      <div className="h-full flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <Loader2 className="size-8 animate-spin text-blue-600 mx-auto mb-4" />
          <p className="text-gray-600">Loading advertisements...</p>
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
            <h2 className="text-2xl font-bold text-gray-900">Advertisement Management</h2>
            <p className="text-gray-600 mt-1">Manage platform ad publishing, editing, and deployment</p>
          </div>
          <Button variant="outline" size="sm" onClick={loadAds} className="gap-2">
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
        <Card className="p-4 bg-gradient-to-br from-blue-500 to-blue-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <Megaphone className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">Total Ads</p>
          <p className="text-3xl font-bold">{totalAds}</p>
          <p className="text-xs opacity-75 mt-1">Active: {activeAds}</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-purple-500 to-purple-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <Eye className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">Total Impressions</p>
          <p className="text-3xl font-bold">{totalImpressions.toLocaleString()}</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-green-500 to-green-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <MousePointerClick className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">Total Clicks</p>
          <p className="text-3xl font-bold">{totalClicks.toLocaleString()}</p>
          <p className="text-xs opacity-75 mt-1">Avg Click Rate: {avgClickRate.toFixed(2)}%</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-yellow-500 to-yellow-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <TrendingUp className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">Budget Usage</p>
          <p className="text-3xl font-bold">{totalBudget > 0 ? ((totalSpent / totalBudget) * 100).toFixed(1) : 0}%</p>
          <p className="text-xs opacity-75 mt-1">{totalSpent.toLocaleString()} / {totalBudget.toLocaleString()}</p>
        </Card>
      </div>

      {/* Filters and Actions */}
      <div className="px-6 pb-4">
        <Card className="p-4">
          <div className="flex flex-wrap gap-4 items-end">
            <div className="flex-1 min-w-[200px]">
              <Label>Ad Status</Label>
              <Select value={filterStatus} onValueChange={setFilterStatus}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Status</SelectItem>
                  <SelectItem value="active">Active</SelectItem>
                  <SelectItem value="inactive">Inactive</SelectItem>
                  <SelectItem value="paused">Paused</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="flex-1 min-w-[200px]">
              <Label>Display Position</Label>
              <Select value={filterPosition} onValueChange={setFilterPosition}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Positions</SelectItem>
                  <SelectItem value="banner">Banner</SelectItem>
                  <SelectItem value="sidebar">Sidebar</SelectItem>
                  <SelectItem value="popup">Popup</SelectItem>
                  <SelectItem value="feed">Feed</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <Button
              className="bg-blue-600 hover:bg-blue-700 text-white gap-2"
              onClick={() => setIsAddDialogOpen(true)}
            >
              <Plus className="size-4" />
              Add Advertisement
            </Button>
          </div>
        </Card>
      </div>

      {/* Ads Grid */}
      <div className="flex-1 overflow-hidden px-6 pb-6">
        <div className="h-full overflow-y-auto">
          {filteredAds.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              <Megaphone className="size-12 mx-auto mb-4 opacity-50" />
              <p>No advertisements found</p>
              <p className="text-sm mt-1">Click "Add Advertisement" to create one</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              {filteredAds.map((ad) => (
                <Card key={ad.id} className="overflow-hidden hover:shadow-lg transition-shadow">
                  {/* Ad Image */}
                  <div className="relative h-48 bg-gray-100">
                    <img
                      src={ad.imageUrl}
                      alt={ad.name}
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        (e.target as HTMLImageElement).src = DEFAULT_IMAGES[0];
                      }}
                    />
                    <div className="absolute top-2 right-2">
                      {getStatusBadge(ad.status)}
                    </div>
                    <div className="absolute top-2 left-2">
                      <Badge variant="outline" className="bg-white/90">
                        <MapPin className="size-3 mr-1" />
                        {getPositionLabel(ad.position)}
                      </Badge>
                    </div>
                  </div>

                  {/* Ad Info */}
                  <div className="p-4">
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex-1">
                        <h3 className="font-bold text-gray-900 mb-1">{ad.name}</h3>
                        <p className="text-sm text-gray-600 mb-2">{ad.description}</p>
                        <div className="flex items-center gap-2 text-xs text-gray-500">
                          <Calendar className="size-3" />
                          <span>{ad.startDate} to {ad.endDate}</span>
                        </div>
                      </div>
                    </div>

                    {/* Stats */}
                    <div className="grid grid-cols-3 gap-3 mb-4 p-3 bg-gray-50 rounded-lg">
                      <div>
                        <p className="text-xs text-gray-600 mb-1">Impressions</p>
                        <p className="font-bold text-gray-900">{(ad.impressions || 0).toLocaleString()}</p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-600 mb-1">Clicks</p>
                        <p className="font-bold text-gray-900">{(ad.clicks || 0).toLocaleString()}</p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-600 mb-1">Click Rate</p>
                        <p className="font-bold text-green-600">{(ad.clickRate || 0).toFixed(2)}%</p>
                      </div>
                    </div>

                    {/* Action Buttons */}
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        variant={ad.status.toLowerCase() === 'active' ? 'default' : 'outline'}
                        className={`flex-1 gap-1 ${
                          ad.status.toLowerCase() === 'active'
                            ? 'bg-green-600 hover:bg-green-700'
                            : ''
                        }`}
                        onClick={() => toggleAdStatus(ad.id)}
                      >
                        <Power className="size-3" />
                        {ad.status.toLowerCase() === 'active' ? 'Deactivate' : 'Activate'}
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        className="flex-1 gap-1"
                        onClick={() => handleEditAd(ad)}
                      >
                        <Edit className="size-3" />
                        Edit
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        className="flex-1 gap-1 text-red-600 border-red-200 hover:bg-red-50"
                        onClick={() => handleDeleteAd(ad)}
                      >
                        <Trash2 className="size-3" />
                        Delete
                      </Button>
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Add Dialog */}
      <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Add New Advertisement</DialogTitle>
            <DialogDescription>Create a new ad campaign</DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2">
                <Label>Ad Name *</Label>
                <Input
                  value={newAd.name}
                  onChange={(e) => setNewAd({ ...newAd, name: e.target.value })}
                  placeholder="e.g., Spring Eco Event"
                />
              </div>

              <div>
                <Label>Ad Status *</Label>
                <Select
                  value={newAd.status}
                  onValueChange={(value) => setNewAd({ ...newAd, status: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="Active">Active</SelectItem>
                    <SelectItem value="Inactive">Inactive</SelectItem>
                    <SelectItem value="Paused">Paused</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label>Display Position</Label>
                <Select
                  value={newAd.position}
                  onValueChange={(value) => setNewAd({ ...newAd, position: value as any })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="banner">Banner</SelectItem>
                    <SelectItem value="sidebar">Sidebar</SelectItem>
                    <SelectItem value="popup">Popup</SelectItem>
                    <SelectItem value="feed">Feed</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label>Start Date *</Label>
                <Input
                  type="date"
                  value={newAd.startDate}
                  onChange={(e) => setNewAd({ ...newAd, startDate: e.target.value })}
                />
              </div>

              <div>
                <Label>End Date *</Label>
                <Input
                  type="date"
                  value={newAd.endDate}
                  onChange={(e) => setNewAd({ ...newAd, endDate: e.target.value })}
                />
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsAddDialogOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleAddAd}
              className="bg-blue-600 hover:bg-blue-700"
              disabled={saving || !newAd.name || !newAd.startDate || !newAd.endDate}
            >
              {saving ? <Loader2 className="size-4 animate-spin mr-2" /> : null}
              Create Ad
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Edit Advertisement</DialogTitle>
            <DialogDescription>Modify ad information and settings</DialogDescription>
          </DialogHeader>

          {editingAd && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <Label>Ad Name</Label>
                  <Input
                    value={editingAd.name}
                    onChange={(e) => setEditingAd({ ...editingAd, name: e.target.value })}
                  />
                </div>

                <div>
                  <Label>Ad Status</Label>
                  <Select
                    value={editingAd.status}
                    onValueChange={(value) => setEditingAd({ ...editingAd, status: value })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="Active">Active</SelectItem>
                      <SelectItem value="Inactive">Inactive</SelectItem>
                      <SelectItem value="Paused">Paused</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div>
                  <Label>Display Position</Label>
                  <Select
                    value={editingAd.position || 'banner'}
                    onValueChange={(value: any) => setEditingAd({ ...editingAd, position: value })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="banner">Banner</SelectItem>
                      <SelectItem value="sidebar">Sidebar</SelectItem>
                      <SelectItem value="popup">Popup</SelectItem>
                      <SelectItem value="feed">Feed</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div>
                  <Label>Start Date</Label>
                  <Input
                    type="date"
                    value={editingAd.startDate}
                    onChange={(e) => setEditingAd({ ...editingAd, startDate: e.target.value })}
                  />
                </div>

                <div>
                  <Label>End Date</Label>
                  <Input
                    type="date"
                    value={editingAd.endDate}
                    onChange={(e) => setEditingAd({ ...editingAd, endDate: e.target.value })}
                  />
                </div>
              </div>
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsEditDialogOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleSaveEdit}
              className="bg-blue-600 hover:bg-blue-700"
              disabled={saving}
            >
              {saving ? <Loader2 className="size-4 animate-spin mr-2" /> : null}
              Save Changes
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-red-600">
              <AlertCircle className="size-5" />
              Confirm Deletion
            </DialogTitle>
            <DialogDescription>
              Are you sure you want to delete this advertisement? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          {adToDelete && (
            <div className="p-4 bg-gray-50 rounded-lg">
              <p className="text-sm font-semibold text-gray-900">{adToDelete.name}</p>
              <p className="text-xs text-gray-600 mt-1">ID: {adToDelete.id}</p>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsDeleteDialogOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={confirmDelete}
              className="bg-red-600 hover:bg-red-700 text-white"
              disabled={saving}
            >
              {saving ? <Loader2 className="size-4 animate-spin mr-2" /> : null}
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
