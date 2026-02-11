import { useState, useEffect, useCallback } from 'react';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
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
  Calendar,
  MapPin,
  AlertCircle,
  Power,
  Loader2,
  RefreshCw,
  Search,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';
import {
  getAllAdvertisements,
  createAdvertisement,
  updateAdvertisement,
  deleteAdvertisement,
  updateAdvertisementStatus,
  type Advertisement,
  type Page
} from '@/api/advertisementApi';
import { useDebounce } from '@/hooks/useDebounce';

const DEFAULT_IMAGE = 'https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?w=800';

export function AdManagement() {
  const [adsPage, setAdsPage] = useState<Page<Advertisement> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingAd, setEditingAd] = useState<Advertisement | null>(null);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [adToDelete, setAdToDelete] = useState<Advertisement | null>(null);
  const [saving, setSaving] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const debouncedSearchQuery = useDebounce(searchQuery, 300);

  const [newAd, setNewAd] = useState<Omit<Advertisement, 'id' | 'impressions' | 'clicks' | 'clickRate'> & { startDate: string, endDate: string }>({
    name: '',
    description: '',
    imageUrl: '',
    linkUrl: '',
    position: 'banner',
    status: 'Active',
    startDate: new Date().toISOString().split('T')[0],
    endDate: new Date(new Date().setDate(new Date().getDate() + 7)).toISOString().split('T')[0],
  });

  const loadAds = useCallback(async (search: string, page: number) => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAllAdvertisements(search, page, 6); // Show 6 ads per page
      setAdsPage(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load advertisements');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAds(debouncedSearchQuery, currentPage);
  }, [debouncedSearchQuery, currentPage, loadAds]);

  const handleEditAd = (ad: Advertisement) => {
    setEditingAd({ ...ad, startDate: ad.startDate.split('T')[0], endDate: ad.endDate.split('T')[0] });
    setIsEditDialogOpen(true);
  };

  const handleSaveEdit = async () => {
    if (editingAd) {
      try {
        setSaving(true);
        await updateAdvertisement(editingAd.id, editingAd);
        await loadAds(debouncedSearchQuery, currentPage);
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
      await createAdvertisement({
        ...newAd,
      });
      await loadAds('', 0);
      setCurrentPage(0);
      setSearchQuery('');
      setIsAddDialogOpen(false);
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
        await loadAds(debouncedSearchQuery, currentPage);
        setIsDeleteDialogOpen(false);
        setAdToDelete(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to delete advertisement');
      } finally {
        setSaving(false);
      }
    }
  };

  const toggleAdStatus = async (ad: Advertisement) => {
    const newStatus = ad.status === 'Active' ? 'Inactive' : 'Active';
    try {
      await updateAdvertisementStatus(ad.id, newStatus);
      await loadAds(debouncedSearchQuery, currentPage);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update status');
    }
  };

  const handlePageChange = (newPage: number) => setCurrentPage(newPage);

  const getStatusBadge = (status: string) => {
    switch (status.toLowerCase()) {
      case 'active': return <Badge className="bg-green-100 text-green-700">Active</Badge>;
      case 'inactive': return <Badge className="bg-gray-100 text-gray-700">Inactive</Badge>;
      case 'paused': return <Badge className="bg-yellow-100 text-yellow-700">Paused</Badge>;
      default: return <Badge variant="secondary">{status}</Badge>;
    }
  };

  const getPositionLabel = (position?: string) => ({ banner: 'Banner', sidebar: 'Sidebar', popup: 'Popup', feed: 'Feed' }[position || 'banner'] || 'Banner');
  const calculateClickRate = (impressions = 0, clicks = 0) => (impressions === 0 ? 0 : (clicks / impressions) * 100);

  const ads = adsPage?.content || [];
  const totalAds = adsPage?.totalElements || 0;

  if (loading && !adsPage) {
    return <div className="h-full flex items-center justify-center bg-gray-50"><div className="text-center"><Loader2 className="size-8 animate-spin text-blue-600 mx-auto mb-4" /><p className="text-gray-600">Loading advertisements...</p></div></div>;
  }

  const renderAdListContent = () => {
    if (loading && ads.length === 0) {
      return <div className="flex justify-center items-center h-full"><Loader2 className="size-8 animate-spin text-blue-500" /></div>;
    }
    if (ads.length === 0) {
      return <div className="text-center py-12 text-gray-500"><Megaphone className="size-12 mx-auto mb-4 opacity-50" /><p>No advertisements found</p><p className="text-sm mt-1">Try adjusting your search or click "Add Advertisement".</p></div>;
    }
    return (
      <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-4">{ads.map((ad) => (<Card key={ad.id} className="overflow-hidden hover:shadow-lg transition-shadow flex flex-col"><div className="relative h-40 bg-gray-100"><img src={ad.imageUrl || DEFAULT_IMAGE} alt={ad.name} className="w-full h-full object-cover" onError={(e) => { (e.target as HTMLImageElement).src = DEFAULT_IMAGE; }} /><div className="absolute top-2 right-2">{getStatusBadge(ad.status)}</div><div className="absolute top-2 left-2"><Badge variant="outline" className="bg-white/90"><MapPin className="size-3 mr-1" />{getPositionLabel(ad.position)}</Badge></div></div><div className="p-4 flex flex-col flex-1"><h3 className="font-bold text-gray-900 mb-1 truncate" title={ad.name}>{ad.name}</h3><p className="text-sm text-gray-600 mb-2 h-10 text-ellipsis overflow-hidden">{ad.description}</p><div className="text-xs text-gray-500 mb-4"><Calendar className="size-3 inline-block mr-1" />{ad.startDate.split('T')[0]} to {ad.endDate.split('T')[0]}</div><div className="grid grid-cols-3 gap-3 mb-4 p-3 bg-gray-50 rounded-lg text-center"><div><p className="text-xs text-gray-600">Impressions</p><p className="font-bold text-gray-900">{ad.impressions.toLocaleString()}</p></div><div><p className="text-xs text-gray-600">Clicks</p><p className="font-bold text-gray-900">{ad.clicks.toLocaleString()}</p></div><div><p className="text-xs text-gray-600">CTR</p><p className="font-bold text-green-600">{calculateClickRate(ad.impressions, ad.clicks).toFixed(2)}%</p></div></div><div className="mt-auto flex gap-2"><Button size="sm" variant={ad.status === 'Active' ? 'default' : 'outline'} className={`flex-1 gap-1 ${ad.status === 'Active' ? 'bg-green-600 hover:bg-green-700' : ''}`} onClick={() => toggleAdStatus(ad)}><Power className="size-3" />{ad.status === 'Active' ? 'Deactivate' : 'Activate'}</Button><Button size="sm" variant="outline" className="flex-1 gap-1" onClick={() => handleEditAd(ad)}><Edit className="size-3" />Edit</Button><Button size="sm" variant="outline" className="flex-1 gap-1 text-red-600 hover:text-red-700 border-red-200 hover:bg-red-50" onClick={() => handleDeleteAd(ad)}><Trash2 className="size-3" />Delete</Button></div></div></Card>))}</div>
    );
  };

  return (
    <div className="h-full flex flex-col bg-gray-50">
      <div className="p-6 bg-white border-b"><div className="flex items-center justify-between"><div><h2 className="text-2xl font-bold text-gray-900">Advertisement Management</h2><p className="text-gray-600 mt-1">Manage platform ad publishing, editing, and deployment</p></div><Button variant="outline" size="sm" onClick={() => loadAds(debouncedSearchQuery, currentPage)} className="gap-2" disabled={loading}><RefreshCw className="size-4" />Refresh</Button></div>{error && <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">{error}</div>}</div>

      <div className="px-6 pt-6 pb-4"><Card className="p-4"><div className="flex flex-wrap gap-4 items-center"><div className="relative w-full md:w-[300px]"><Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-gray-400" /><Input placeholder="Search by ad name..." value={searchQuery} onChange={(e) => { setSearchQuery(e.target.value); setCurrentPage(0); }} className="pl-9" /></div><div className="flex-1" /><Button className="bg-blue-600 hover:bg-blue-700 text-white gap-2" onClick={() => setIsAddDialogOpen(true)}><Plus className="size-4" />Add Advertisement</Button></div></Card></div>

      <div className="flex-1 overflow-hidden px-6 pb-6"><div className="h-full flex flex-col"><div className="flex-1 overflow-y-auto pr-2 -mr-2">{renderAdListContent()}</div>{adsPage && adsPage.totalPages > 1 && (<div className="p-4 border-t flex items-center justify-between"><p className="text-sm text-gray-600">Page {currentPage + 1} of {adsPage.totalPages} ({totalAds} ads)</p><div className="flex items-center gap-2"><Button variant="outline" size="sm" onClick={() => handlePageChange(currentPage - 1)} disabled={currentPage === 0}><ChevronLeft className="size-4 mr-1" />Previous</Button><Button variant="outline" size="sm" onClick={() => handlePageChange(currentPage + 1)} disabled={currentPage >= adsPage.totalPages - 1}>Next<ChevronRight className="size-4 ml-1" /></Button></div></div>)}</div></div>

      {/* Add Dialog */}
      <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Add New Advertisement</DialogTitle>
            <DialogDescription>Create a new ad campaign. Fields with * are required.</DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Name *</Label><Input className="col-span-3" value={newAd.name} onChange={(e) => setNewAd({ ...newAd, name: e.target.value })} /></div>
            <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Description</Label><Input className="col-span-3" value={newAd.description} onChange={(e) => setNewAd({ ...newAd, description: e.target.value })} /></div>
            <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Image URL</Label><Input className="col-span-3" value={newAd.imageUrl} onChange={(e) => setNewAd({ ...newAd, imageUrl: e.target.value })} /></div>
            <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Link URL</Label><Input className="col-span-3" value={newAd.linkUrl} onChange={(e) => setNewAd({ ...newAd, linkUrl: e.target.value })} /></div>
            <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Position</Label><Select value={newAd.position} onValueChange={(value: 'banner' | 'sidebar' | 'popup' | 'feed') => setNewAd({ ...newAd, position: value })}><SelectTrigger className="col-span-3"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="banner">Banner</SelectItem><SelectItem value="sidebar">Sidebar</SelectItem><SelectItem value="popup">Popup</SelectItem><SelectItem value="feed">Feed</SelectItem></SelectContent></Select></div>
            <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Status</Label><Select value={newAd.status} onValueChange={(value) => setNewAd({ ...newAd, status: value })}><SelectTrigger className="col-span-3"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="Active">Active</SelectItem><SelectItem value="Inactive">Inactive</SelectItem><SelectItem value="Paused">Paused</SelectItem></SelectContent></Select></div>
            <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Start Date *</Label><Input type="date" className="col-span-3" value={newAd.startDate} onChange={(e) => setNewAd({ ...newAd, startDate: e.target.value })} /></div>
            <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">End Date *</Label><Input type="date" className="col-span-3" value={newAd.endDate} onChange={(e) => setNewAd({ ...newAd, endDate: e.target.value })} /></div>
          </div>
          <DialogFooter><Button variant="outline" onClick={() => setIsAddDialogOpen(false)}>Cancel</Button><Button onClick={handleAddAd} disabled={saving || !newAd.name || !newAd.startDate || !newAd.endDate}>{saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}Create</Button></DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      {editingAd && <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}><DialogContent className="max-w-2xl"><DialogHeader><DialogTitle>Edit Advertisement</DialogTitle></DialogHeader><div className="grid gap-4 py-4">
        <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Name</Label><Input className="col-span-3" value={editingAd.name} onChange={(e) => setEditingAd({ ...editingAd, name: e.target.value })} /></div>
        <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Description</Label><Input className="col-span-3" value={editingAd.description} onChange={(e) => setEditingAd({ ...editingAd, description: e.target.value })} /></div>
        <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Image URL</Label><Input className="col-span-3" value={editingAd.imageUrl} onChange={(e) => setEditingAd({ ...editingAd, imageUrl: e.target.value })} /></div>
        <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Link URL</Label><Input className="col-span-3" value={editingAd.linkUrl} onChange={(e) => setEditingAd({ ...editingAd, linkUrl: e.target.value })} /></div>
        <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Position</Label><Select value={editingAd.position} onValueChange={(value: 'banner' | 'sidebar' | 'popup' | 'feed') => setEditingAd({ ...editingAd, position: value })}><SelectTrigger className="col-span-3"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="banner">Banner</SelectItem><SelectItem value="sidebar">Sidebar</SelectItem><SelectItem value="popup">Popup</SelectItem><SelectItem value="feed">Feed</SelectItem></SelectContent></Select></div>
        <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Status</Label><Select value={editingAd.status} onValueChange={(value) => setEditingAd({ ...editingAd, status: value })}><SelectTrigger className="col-span-3"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="Active">Active</SelectItem><SelectItem value="Inactive">Inactive</SelectItem><SelectItem value="Paused">Paused</SelectItem></SelectContent></Select></div>
        <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">Start Date</Label><Input type="date" className="col-span-3" value={editingAd.startDate} onChange={(e) => setEditingAd({ ...editingAd, startDate: e.target.value })} /></div>
        <div className="grid grid-cols-4 items-center gap-4"><Label className="text-right">End Date</Label><Input type="date" className="col-span-3" value={editingAd.endDate} onChange={(e) => setEditingAd({ ...editingAd, endDate: e.target.value })} /></div>
      </div><DialogFooter><Button variant="outline" onClick={() => setIsEditDialogOpen(false)}>Cancel</Button><Button onClick={handleSaveEdit} disabled={saving}>{saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}Save Changes</Button></DialogFooter></DialogContent></Dialog>}

      {/* Delete Confirmation Dialog */}
      <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}><DialogContent><DialogHeader><DialogTitle className="flex items-center gap-2"><AlertCircle className="text-red-500" />Confirm Deletion</DialogTitle><DialogDescription>Are you sure you want to delete this advertisement? This action cannot be undone.</DialogDescription></DialogHeader>{adToDelete && <div className="py-4"><p className="font-semibold">{adToDelete.name}</p><p className="text-sm text-gray-500">ID: {adToDelete.id}</p></div>}<DialogFooter><Button variant="outline" onClick={() => setIsDeleteDialogOpen(false)}>Cancel</Button><Button variant="destructive" onClick={confirmDelete} disabled={saving}>{saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}Delete</Button></DialogFooter></DialogContent></Dialog>
    </div>
  );
}
