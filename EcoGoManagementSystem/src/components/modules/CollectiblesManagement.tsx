import { useState, useEffect, useCallback } from 'react';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  getAllBadgeItems,
  getAllClothItems,
  getBadgePurchaseStats,
  updateBadge,
  deleteBadge,
  createBadge,
} from '@/api/collectiblesApi';
import type { Badge as ApiBadge, BadgePurchaseStat } from '@/api/collectiblesApi';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Switch } from '@/components/ui/switch';
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@/components/ui/tabs';
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
  Award,
  Edit,
  Trophy,
  Crown,
  Leaf,
  Footprints,
  Star,
  Zap,
  Heart,
  Shield,
  Flame,
  Users,
  Medal,
  Sparkles,
  Mountain,
  Dog,
  Cat,
  Shirt,
  CircleDot,
  Candy,
  Palette,
  Trash2,
  ChevronLeft,
  ChevronRight,
  Search
} from 'lucide-react';
import { ImageWithFallback } from '@/components/figma/ImageWithFallback';

type AcquisitionMethod = 'purchase' | 'achievement' | 'task' | 'vip' | 'event' | 'free';

interface BadgeItem {
  id: string;
  name: string;
  description: string;
  icon: React.ReactNode;
  acquisitionMethod: AcquisitionMethod;
  price: number;
  requirementDescription: string;
  ownedBy: number;
  category: string;
}

interface PetAccessory {
  id: string;
  name: string;
  description: string;
  imageUrl: string;
  category: string; // 直接使用 DB 的 subCategory 值: "head", "body", "face" 等
  acquisitionMethod: AcquisitionMethod;
  price: number;
  requirementDescription: string;
  ownedBy: number;
}

// 根据 subCategory 获取图标
const getIconBySubCategory = (subCategory: string, colorScheme: string) => {
  const color = colorScheme || '#666';
  const style = { color };

  if (subCategory?.includes('VIP')) return <Crown className="size-8" style={style} />;
  if (subCategory?.includes('rank')) return <Trophy className="size-8" style={style} />;
  if (subCategory?.includes('normal')) return <Award className="size-8" style={style} />;
  if (subCategory?.includes('Hat')) return <Crown className="size-8" style={style} />;
  if (subCategory?.includes('clothing')) return <Shirt className="size-8" style={style} />;
  if (subCategory?.includes('shoes')) return <Footprints className="size-8" style={style} />;
  return <Star className="size-8" style={style} />;
};

// 将后端 Badge 转换为前端 BadgeItem
const mapApiBadgeToBadgeItem = (apiBadge: ApiBadge, ownedCount: number): BadgeItem => ({
  id: apiBadge.badgeId,
  name: apiBadge.name?.en || apiBadge.name?.zh || 'Unknown',
  description: apiBadge.description?.en || apiBadge.description?.zh || '',
  icon: getIconBySubCategory(apiBadge.subCategory, apiBadge.icon?.colorScheme),
  acquisitionMethod: (apiBadge.acquisitionMethod || 'free') as AcquisitionMethod,
  price: apiBadge.purchaseCost || 0,
  requirementDescription: apiBadge.description?.en || apiBadge.description?.zh || '',
  ownedBy: ownedCount,
  category: apiBadge.subCategory || 'General'
});

// 将后端 Badge (cloth) 转换为前端 PetAccessory
const mapApiBadgeToPetAccessory = (apiBadge: ApiBadge, ownedCount: number): PetAccessory => {
  return {
    id: apiBadge.badgeId,
    name: apiBadge.name?.en || apiBadge.name?.zh || 'Unknown',
    description: apiBadge.description?.en || apiBadge.description?.zh || '',
    imageUrl: apiBadge.icon?.url || `/clothes/${(apiBadge.name?.en || '').toLowerCase().replace(/ /g, '_')}.png`,
    category: apiBadge.subCategory || 'other', // 直接使用 DB 的 subCategory: "head", "body", "face"
    acquisitionMethod: (apiBadge.acquisitionMethod || 'free') as AcquisitionMethod,
    price: apiBadge.purchaseCost || 0,
    requirementDescription: apiBadge.description?.en || apiBadge.description?.zh || '',
    ownedBy: ownedCount
  };
};

export function CollectiblesManagement() {
  const [badges, setBadges] = useState<BadgeItem[]>([]);
  const [accessories, setAccessories] = useState<PetAccessory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [purchaseStats, setPurchaseStats] = useState<BadgePurchaseStat[]>([]);

  // 加载数据
  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      // 并行获取数据
      const [badgeData, clothData, stats] = await Promise.all([
        getAllBadgeItems(),
        getAllClothItems(),
        getBadgePurchaseStats()
      ]);

      setPurchaseStats(stats);

      // DEBUG: 查看 API 返回的原始数据
      console.log('[Collectibles] Purchase stats from API:', JSON.stringify(stats, null, 2));
      console.log('[Collectibles] Cloth items badgeIds:', clothData.map(c => c.badgeId));

      // 创建 ownedBy 映射
      const ownedMap = new Map<string, number>();
      stats.forEach(stat => {
        console.log(`[Collectibles] stat: badgeId=${stat.badgeId}, purchaseCount=${stat.purchaseCount}`);
        ownedMap.set(stat.badgeId, stat.purchaseCount);
      });

      // 转换徽章数据
      const mappedBadges = badgeData.map(b =>
        mapApiBadgeToBadgeItem(b, ownedMap.get(b.badgeId) || 0)
      );
      setBadges(mappedBadges);

      // 转换服饰数据
      const mappedAccessories = clothData.map(c => {
        const count = ownedMap.get(c.badgeId) || 0;
        console.log(`[Collectibles] cloth ${c.badgeId} → sold count: ${count}`);
        return mapApiBadgeToPetAccessory(c, count);
      });
      setAccessories(mappedAccessories);

    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load data');
      console.error('Failed to load collectibles:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);


  const [editingBadge, setEditingBadge] = useState<BadgeItem | null>(null);
  const [editingAccessory, setEditingAccessory] = useState<PetAccessory | null>(null);
  const [isBadgeEditOpen, setIsBadgeEditOpen] = useState(false);
  const [isAccessoryEditOpen, setIsAccessoryEditOpen] = useState(false);
  const [filterMethod, setFilterMethod] = useState<string>('all');
  const [filterCategory, setFilterCategory] = useState<string>('all');
  const [filterBadgeCategory, setFilterBadgeCategory] = useState<string>('all');
  const [badgePage, setBadgePage] = useState(1);
  const [accessoryPage, setAccessoryPage] = useState(1);
  const ITEMS_PER_PAGE = 8;
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<{ id: string; type: 'badge' | 'accessory'; name: string } | null>(null);
  const [isCreateBadgeOpen, setIsCreateBadgeOpen] = useState(false);
  const [isCreateAccessoryOpen, setIsCreateAccessoryOpen] = useState(false);
  const [searchBadgeId, setSearchBadgeId] = useState('');
  const [searchAccessoryId, setSearchAccessoryId] = useState('');
  const [newBadge, setNewBadge] = useState({
    badgeId: '',
    nameEn: '',
    nameZh: '',
    descriptionEn: '',
    descriptionZh: '',
    category: 'badge',
    subCategory: 'normal badge',
    acquisitionMethod: 'purchase',
    purchaseCost: 0,
    carbonThreshold: 0,
    iconUrl: '',
    iconColorScheme: '#4CAF50',
    isActive: true
  });
  const [newAccessory, setNewAccessory] = useState({
    badgeId: '',
    nameEn: '',
    nameZh: '',
    descriptionEn: '',
    descriptionZh: '',
    category: 'cloth',
    subCategory: 'clothes_Hat',
    acquisitionMethod: 'purchase',
    purchaseCost: 0,
    carbonThreshold: 0,
    iconUrl: '',
    iconColorScheme: '#9C27B0',
    isActive: true
  });

  const handleEditBadge = (badge: BadgeItem) => {
    setEditingBadge({ ...badge });
    setIsBadgeEditOpen(true);
  };

  const handleSaveBadgeEdit = () => {
    if (editingBadge) {
      setBadges(badges.map(b => b.id === editingBadge.id ? editingBadge : b));
      setIsBadgeEditOpen(false);
      setEditingBadge(null);
    }
  };

  const handleEditAccessory = (accessory: PetAccessory) => {
    setEditingAccessory({ ...accessory });
    setIsAccessoryEditOpen(true);
  };

  const handleSaveAccessoryEdit = () => {
    if (editingAccessory) {
      setAccessories(accessories.map(a => a.id === editingAccessory.id ? editingAccessory : a));
      setIsAccessoryEditOpen(false);
      setEditingAccessory(null);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!itemToDelete) return;

    try {
      await deleteBadge(itemToDelete.id);

      if (itemToDelete.type === 'badge') {
        setBadges(badges.filter(b => b.id !== itemToDelete.id));
      } else {
        setAccessories(accessories.filter(a => a.id !== itemToDelete.id));
      }

      setDeleteConfirmOpen(false);
      setItemToDelete(null);
    } catch (err) {
      console.error('Failed to delete:', err);
      alert('Failed to delete item. Please try again.');
    }
  };

  const handleCreateBadge = async () => {
    try {
      const badgeData = {
        badgeId: newBadge.badgeId,
        name: { en: newBadge.nameEn, zh: newBadge.nameZh },
        description: { en: newBadge.descriptionEn, zh: newBadge.descriptionZh },
        category: newBadge.category,
        subCategory: newBadge.subCategory,
        acquisitionMethod: newBadge.acquisitionMethod,
        purchaseCost: newBadge.acquisitionMethod === 'purchase' ? newBadge.purchaseCost : null,
        carbonThreshold: newBadge.acquisitionMethod === 'achievement' ? newBadge.carbonThreshold : null,
        icon: { url: newBadge.iconUrl, colorScheme: newBadge.iconColorScheme },
        isActive: newBadge.isActive,
        createdAt: new Date().toISOString()
      };

      await createBadge(badgeData);

      // Reload data
      await loadData();

      setIsCreateBadgeOpen(false);
      // Reset form
      setNewBadge({
        badgeId: '',
        nameEn: '',
        nameZh: '',
        descriptionEn: '',
        descriptionZh: '',
        category: 'badge',
        subCategory: 'normal badge',
        acquisitionMethod: 'purchase',
        purchaseCost: 0,
        carbonThreshold: 0,
        iconUrl: '',
        iconColorScheme: '#4CAF50',
        isActive: true
      });
    } catch (err) {
      console.error('Failed to create badge:', err);
      alert('Failed to create badge. Please try again.');
    }
  };

  const handleCreateAccessory = async () => {
    try {
      const accessoryData = {
        badgeId: newAccessory.badgeId,
        name: { en: newAccessory.nameEn, zh: newAccessory.nameZh },
        description: { en: newAccessory.descriptionEn, zh: newAccessory.descriptionZh },
        category: newAccessory.category,
        subCategory: newAccessory.subCategory,
        acquisitionMethod: newAccessory.acquisitionMethod,
        purchaseCost: newAccessory.acquisitionMethod === 'purchase' ? newAccessory.purchaseCost : null,
        carbonThreshold: newAccessory.acquisitionMethod === 'achievement' ? newAccessory.carbonThreshold : null,
        icon: { url: newAccessory.iconUrl, colorScheme: newAccessory.iconColorScheme },
        isActive: newAccessory.isActive,
        createdAt: new Date().toISOString()
      };

      await createBadge(accessoryData);

      // Reload data
      await loadData();

      setIsCreateAccessoryOpen(false);
      // Reset form
      setNewAccessory({
        badgeId: '',
        nameEn: '',
        nameZh: '',
        descriptionEn: '',
        descriptionZh: '',
        category: 'cloth',
        subCategory: 'clothes_Hat',
        acquisitionMethod: 'purchase',
        purchaseCost: 0,
        carbonThreshold: 0,
        iconUrl: '',
        iconColorScheme: '#9C27B0',
        isActive: true
      });
    } catch (err) {
      console.error('Failed to create accessory:', err);
      alert('Failed to create accessory. Please try again.');
    }
  };

  const getMethodBadge = (method: AcquisitionMethod) => {
    switch (method) {
      case 'purchase':
        return <Badge className="bg-blue-100 text-blue-700">Points Purchase</Badge>;
      case 'achievement':
        return <Badge className="bg-green-100 text-green-700">Achievement Unlock</Badge>;
      case 'task':
        return <Badge className="bg-orange-100 text-orange-700">Task Reward</Badge>;
      case 'vip':
        return <Badge className="bg-purple-100 text-purple-700">VIP Exclusive</Badge>;
      case 'event':
        return <Badge className="bg-pink-100 text-pink-700">Event Limited</Badge>;
      case 'free':
        return <Badge className="bg-gray-100 text-gray-700">Free</Badge>;
      default:
        return null;
    }
  };

  const getCategoryIcon = (category: string) => {
    switch (category.toLowerCase()) {
      case 'head':
        return <Crown className="size-4" />;
      case 'body':
        return <Shirt className="size-4" />;
      case 'face':
        return <Sparkles className="size-4" />;
      default:
        return <CircleDot className="size-4" />;
    }
  };

  const getCategoryLabel = (category: string) => {
    const labels: Record<string, string> = {
      head: 'Head',
      body: 'Body',
      face: 'Face',
    };
    return labels[category.toLowerCase()] || category.charAt(0).toUpperCase() + category.slice(1);
  };

  const getMethodLabel = (method: AcquisitionMethod) => {
    const labels = {
      purchase: 'Points Purchase',
      achievement: 'Achievement Unlock',
      task: 'Task Reward',
      vip: 'VIP Exclusive',
      event: 'Event Limited',
      free: 'Free'
    };
    return labels[method];
  };

  // 获取 badge 的唯一 subCategory 列表
  const badgeCategories = [...new Set(badges.map(b => b.category))];

  const filteredBadges = badges.filter(badge => {
    const methodMatch = filterMethod === 'all' || badge.acquisitionMethod === filterMethod;
    const categoryMatch = filterBadgeCategory === 'all' || badge.category === filterBadgeCategory;
    const searchMatch = !searchBadgeId || badge.id.toLowerCase().includes(searchBadgeId.toLowerCase());
    return methodMatch && categoryMatch && searchMatch;
  });

  // Badge 分页计算
  const totalBadgePages = Math.ceil(filteredBadges.length / ITEMS_PER_PAGE);
  const paginatedBadges = filteredBadges.slice(
    (badgePage - 1) * ITEMS_PER_PAGE,
    badgePage * ITEMS_PER_PAGE
  );

  // 当过滤器改变时重置页码
  const handleFilterMethodChange = (value: string) => {
    setFilterMethod(value);
    setBadgePage(1);
    setAccessoryPage(1);
  };

  const handleFilterBadgeCategoryChange = (value: string) => {
    setFilterBadgeCategory(value);
    setBadgePage(1);
  };

  const handleFilterCategoryChange = (value: string) => {
    setFilterCategory(value);
    setAccessoryPage(1);
  };

  const handleSearchBadgeIdChange = (value: string) => {
    setSearchBadgeId(value);
    setBadgePage(1);
  };

  const handleSearchAccessoryIdChange = (value: string) => {
    setSearchAccessoryId(value);
    setAccessoryPage(1);
  };

  const filteredAccessories = accessories.filter(accessory => {
    const methodMatch = filterMethod === 'all' || accessory.acquisitionMethod === filterMethod;
    const categoryMatch = filterCategory === 'all' || accessory.category === filterCategory;
    const searchMatch = !searchAccessoryId || accessory.id.toLowerCase().includes(searchAccessoryId.toLowerCase());
    return methodMatch && categoryMatch && searchMatch;
  });

  // Accessory 分页计算
  const totalAccessoryPages = Math.ceil(filteredAccessories.length / ITEMS_PER_PAGE);
  const paginatedAccessories = filteredAccessories.slice(
    (accessoryPage - 1) * ITEMS_PER_PAGE,
    accessoryPage * ITEMS_PER_PAGE
  );

  const totalBadges = badges.length;
  const totalAccessories = accessories.length;
  const badgeOwners = badges.reduce((sum, b) => sum + b.ownedBy, 0);
  const accessoryOwners = accessories.reduce((sum, a) => sum + a.ownedBy, 0);

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Header */}
      <div className="p-6 bg-white border-b">
        <h2 className="text-2xl font-bold text-gray-900">Collectibles Management</h2>
        <p className="text-gray-600 mt-1">Manage badges and pet clothes store</p>
      </div>

      {error && (
        <div className="px-6 pt-6">
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded relative">
            <span className="block sm:inline">{error}</span>
          </div>
        </div>
      )}

      {/* Statistics Cards */}
      <div className="p-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 bg-gradient-to-br from-blue-500 to-blue-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <Award className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">Total Badges</p>
          <p className="text-3xl font-bold">{totalBadges}</p>
          <p className="text-xs opacity-75 mt-1">Total Ownership: {badgeOwners.toLocaleString()}</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-purple-500 to-purple-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <Dog className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">Total Pet Clothes</p>
          <p className="text-3xl font-bold">{totalAccessories}</p>
          <p className="text-xs opacity-75 mt-1">Total Sold: {accessoryOwners.toLocaleString()}</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-green-500 to-green-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <Trophy className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">Purchasable Badges</p>
          <p className="text-3xl font-bold">{badges.filter(b => b.acquisitionMethod === 'purchase').length}</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-pink-500 to-pink-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <Sparkles className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">Purchasable Clothes</p>
          <p className="text-3xl font-bold">{accessories.filter(a => a.acquisitionMethod === 'purchase').length}</p>
        </Card>
      </div>

      {/* Tabs */}
      <div className="flex-1 overflow-hidden px-6 pb-6">
        <Tabs defaultValue="badges" className="h-full flex flex-col">
          <TabsList className="bg-white border w-fit">
            <TabsTrigger value="badges" className="gap-2">
              <Award className="size-4" />
              Badge Management
            </TabsTrigger>
            <TabsTrigger value="accessories" className="gap-2">
              <Dog className="size-4" />
              Pet Clothes Store
            </TabsTrigger>
          </TabsList>

          {/* Badges Tab */}
          <TabsContent value="badges" className="flex-1 overflow-hidden mt-4 data-[state=active]:flex data-[state=active]:flex-col">
            {/* Filters */}
            <Card className="p-4 mb-4 flex-shrink-0">
              <div className="flex flex-wrap gap-4 items-end">
                <div className="flex-1 min-w-[200px]">
                  <Label>Search by Badge ID</Label>
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-gray-400" />
                    <Input
                      placeholder="Enter badge ID..."
                      value={searchBadgeId}
                      onChange={(e) => handleSearchBadgeIdChange(e.target.value)}
                      className="pl-9"
                    />
                  </div>
                </div>

                <div className="flex-1 min-w-[180px]">
                  <Label>Category</Label>
                  <Select value={filterBadgeCategory} onValueChange={handleFilterBadgeCategoryChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Categories</SelectItem>
                      {badgeCategories.map(cat => (
                        <SelectItem key={cat} value={cat}>{cat}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="flex-1 min-w-[180px]">
                  <Label>Acquisition Method</Label>
                  <Select value={filterMethod} onValueChange={handleFilterMethodChange}>
                    <SelectTrigger className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Methods</SelectItem>
                      <SelectItem value="purchase">Points Purchase</SelectItem>
                      <SelectItem value="achievement">Achievement Unlock</SelectItem>
                      <SelectItem value="task">Task Reward</SelectItem>
                      <SelectItem value="vip">VIP Exclusive</SelectItem>
                      <SelectItem value="event">Event Limited</SelectItem>
                      <SelectItem value="free">Free</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <Button
                  className="bg-blue-600 hover:bg-blue-700 text-white gap-2"
                  onClick={() => setIsCreateBadgeOpen(true)}
                >
                  <Award className="size-4" />
                  Add Badge
                </Button>
              </div>
            </Card>

            {/* Badges Grid */}
            <div className="flex-1 overflow-y-auto">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                {paginatedBadges.map((badge) => (
                  <Card key={badge.id} className="p-5 hover:shadow-lg transition-shadow">
                    <div className="flex items-center justify-center mb-4">
                      <div className="p-4 rounded-full bg-gray-100">
                        {badge.icon}
                      </div>
                    </div>

                    <div className="text-center mb-3">
                      <h3 className="font-bold text-gray-900 mb-1">{badge.name}</h3>
                      <div className="flex items-center justify-center gap-2 mb-2">
                        {getMethodBadge(badge.acquisitionMethod)}
                      </div>
                      <p className="text-sm text-gray-600 mb-2">
                        {badge.description}
                      </p>
                      <Badge variant="outline" className="text-xs">
                        {badge.category}
                      </Badge>
                    </div>

                    <div className="space-y-2 mb-4 p-3 bg-gray-50 rounded-lg">
                      <div className="text-sm">
                        <p className="text-gray-600 mb-1">Requirement:</p>
                        <p className="font-medium text-gray-900">{badge.requirementDescription}</p>
                      </div>

                      {badge.acquisitionMethod === 'purchase' && (
                        <div className="flex items-center justify-between pt-2 border-t">
                          <span className="text-sm text-gray-600">Price:</span>
                          <span className="font-bold text-blue-600">{badge.price} points</span>
                        </div>
                      )}

                      <div className="flex items-center justify-between pt-2 border-t">
                        <span className="text-sm text-gray-600">Own:</span>
                        <span className="font-semibold text-gray-900">{badge.ownedBy}</span>
                      </div>
                    </div>

                    <div className="flex gap-2">
                      <Button
                        className="flex-1 gap-2"
                        variant="outline"
                        onClick={() => handleEditBadge(badge)}
                      >
                        <Edit className="size-4" />
                        Edit
                      </Button>
                      <Button
                        className="gap-2"
                        variant="outline"
                        onClick={() => {
                          setItemToDelete({ id: badge.id, type: 'badge', name: badge.name });
                          setDeleteConfirmOpen(true);
                        }}
                      >
                        <Trash2 className="size-4 text-red-500" />
                      </Button>
                    </div>
                  </Card>
                ))}
              </div>

              {/* Pagination */}
              {totalBadgePages > 1 && (
                <div className="flex items-center justify-center gap-4 mt-6 py-4">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setBadgePage(p => Math.max(1, p - 1))}
                    disabled={badgePage === 1}
                  >
                    <ChevronLeft className="size-4 mr-1" />
                    Previous
                  </Button>

                  <div className="flex items-center gap-2">
                    {Array.from({ length: totalBadgePages }, (_, i) => i + 1).map(page => (
                      <Button
                        key={page}
                        variant={page === badgePage ? "default" : "outline"}
                        size="sm"
                        className="w-8 h-8 p-0"
                        onClick={() => setBadgePage(page)}
                      >
                        {page}
                      </Button>
                    ))}
                  </div>

                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setBadgePage(p => Math.min(totalBadgePages, p + 1))}
                    disabled={badgePage === totalBadgePages}
                  >
                    Next
                    <ChevronRight className="size-4 ml-1" />
                  </Button>
                </div>
              )}

              {/* Results info */}
              <div className="text-center text-sm text-gray-500 mt-2">
                Showing {paginatedBadges.length} of {filteredBadges.length} badges
                {filteredBadges.length !== badges.length && ` (filtered from ${badges.length} total)`}
              </div>
            </div>
          </TabsContent>

          {/* Pet Accessories Tab */}
          <TabsContent value="accessories" className="flex-1 overflow-hidden mt-4 data-[state=active]:flex data-[state=active]:flex-col">
            {/* Filters */}
            <Card className="p-4 mb-4 flex-shrink-0">
              <div className="flex flex-wrap gap-4 items-end">
                <div className="flex-1 min-w-[200px]">
                  <Label>Search by Accessory ID</Label>
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-gray-400" />
                    <Input
                      placeholder="Enter accessory ID..."
                      value={searchAccessoryId}
                      onChange={(e) => handleSearchAccessoryIdChange(e.target.value)}
                      className="pl-9"
                    />
                  </div>
                </div>

                <div className="flex-1 min-w-[180px]">
                  <Label>Category</Label>
                  <Select value={filterCategory} onValueChange={handleFilterCategoryChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Categories</SelectItem>
                      {[...new Set(accessories.map(a => a.category))].sort((a, b) => a.localeCompare(b)).map(cat => (
                        <SelectItem key={cat} value={cat}>{cat.charAt(0).toUpperCase() + cat.slice(1)}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="flex-1 min-w-[180px]">
                  <Label>Acquisition Method</Label>
                  <Select value={filterMethod} onValueChange={handleFilterMethodChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Methods</SelectItem>
                      <SelectItem value="purchase">Points Purchase</SelectItem>
                      <SelectItem value="achievement">Achievement Unlock</SelectItem>
                      <SelectItem value="task">Task Reward</SelectItem>
                      <SelectItem value="vip">VIP Exclusive</SelectItem>
                      <SelectItem value="event">Event Limited</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <Button
                  className="bg-purple-600 hover:bg-purple-700 text-white gap-2"
                  onClick={() => setIsCreateAccessoryOpen(true)}
                >
                  <Dog className="size-4" />
                  Add Accessory
                </Button>
              </div>
            </Card>

            {/* Accessories Grid */}
            <div className="flex-1 overflow-y-auto">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                {paginatedAccessories.map((accessory) => (
                  <Card key={accessory.id} className="overflow-hidden hover:shadow-lg transition-shadow">
                    <div className="relative h-48 bg-gray-100">
                      <ImageWithFallback
                        src={accessory.imageUrl}
                        alt={accessory.name}
                        className="w-full h-full object-cover"
                      />
                      <div className="absolute top-2 left-2 flex items-center gap-1 bg-white/90 backdrop-blur-sm rounded-full px-2 py-1">
                        {getCategoryIcon(accessory.category)}
                        <span className="text-xs font-medium">{getCategoryLabel(accessory.category)}</span>
                      </div>
                    </div>

                    <div className="p-4">
                      <h3 className="font-bold text-gray-900 mb-2">{accessory.name}</h3>
                      <div className="mb-3">
                        {getMethodBadge(accessory.acquisitionMethod)}
                      </div>
                      <p className="text-sm text-gray-600 mb-3 line-clamp-2">
                        {accessory.description}
                      </p>

                      <div className="space-y-2 mb-4 p-3 bg-gray-50 rounded-lg">
                        <div className="text-sm">
                          <p className="text-gray-600 mb-1">Requirement:</p>
                          <p className="font-medium text-gray-900">{accessory.requirementDescription}</p>
                        </div>

                        {accessory.acquisitionMethod === 'purchase' && (
                          <div className="flex items-center justify-between pt-2 border-t">
                            <span className="text-sm text-gray-600">Price:</span>
                            <span className="font-bold text-blue-600">{accessory.price} points</span>
                          </div>
                        )}

                        <div className="flex items-center justify-between pt-2 border-t">
                          <span className="text-sm text-gray-600">Sold:</span>
                          <span className="font-semibold text-gray-900">{accessory.ownedBy}</span>
                        </div>
                      </div>

                      <div className="flex gap-2">
                        <Button
                          className="flex-1 gap-2"
                          variant="outline"
                          onClick={() => handleEditAccessory(accessory)}
                        >
                          <Edit className="size-4" />
                          Edit
                        </Button>
                        <Button
                          className="gap-2"
                          variant="outline"
                          onClick={() => {
                            setItemToDelete({ id: accessory.id, type: 'accessory', name: accessory.name });
                            setDeleteConfirmOpen(true);
                          }}
                        >
                          <Trash2 className="size-4 text-red-500" />
                        </Button>
                      </div>
                    </div>
                  </Card>
                ))}
              </div>

              {/* Pagination */}
              {totalAccessoryPages > 1 && (
                <div className="flex items-center justify-center gap-4 mt-6 py-4">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setAccessoryPage(p => Math.max(1, p - 1))}
                    disabled={accessoryPage === 1}
                  >
                    <ChevronLeft className="size-4 mr-1" />
                    Previous
                  </Button>

                  <div className="flex items-center gap-2">
                    {Array.from({ length: totalAccessoryPages }, (_, i) => i + 1).map(page => (
                      <Button
                        key={page}
                        variant={page === accessoryPage ? "default" : "outline"}
                        size="sm"
                        className="w-8 h-8 p-0"
                        onClick={() => setAccessoryPage(page)}
                      >
                        {page}
                      </Button>
                    ))}
                  </div>

                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setAccessoryPage(p => Math.min(totalAccessoryPages, p + 1))}
                    disabled={accessoryPage === totalAccessoryPages}
                  >
                    Next
                    <ChevronRight className="size-4 ml-1" />
                  </Button>
                </div>
              )}

              {/* Results info */}
              <div className="text-center text-sm text-gray-500 mt-2">
                Showing {paginatedAccessories.length} of {filteredAccessories.length} accessories
                {filteredAccessories.length !== accessories.length && ` (filtered from ${accessories.length} total)`}
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </div>

      {/* Badge Edit Dialog */}
      <Dialog open={isBadgeEditOpen} onOpenChange={setIsBadgeEditOpen}>
        <DialogContent className="sm:max-w-[600px]">
          <DialogHeader>
            <DialogTitle>Edit Badge</DialogTitle>
            <DialogDescription>
              Update badge information and settings
            </DialogDescription>
          </DialogHeader>
          {editingBadge && (
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="badge-name">Badge Name</Label>
                <Input
                  id="badge-name"
                  value={editingBadge.name}
                  onChange={(e) => setEditingBadge({ ...editingBadge, name: e.target.value })}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="badge-description">Description</Label>
                <Textarea
                  id="badge-description"
                  value={editingBadge.description}
                  onChange={(e) => setEditingBadge({ ...editingBadge, description: e.target.value })}
                  rows={3}
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="badge-price">Price (points)</Label>
                  <Input
                    id="badge-price"
                    type="number"
                    value={editingBadge.price}
                    onChange={(e) => setEditingBadge({ ...editingBadge, price: parseInt(e.target.value) })}
                    disabled={editingBadge.acquisitionMethod !== 'purchase'}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="badge-category">Category</Label>
                  <Input
                    id="badge-category"
                    value={editingBadge.category}
                    onChange={(e) => setEditingBadge({ ...editingBadge, category: e.target.value })}
                  />
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsBadgeEditOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleSaveBadgeEdit}>
              Save Changes
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteConfirmOpen} onOpenChange={setDeleteConfirmOpen}>
        <DialogContent className="sm:max-w-[400px]">
          <DialogHeader>
            <DialogTitle>Confirm Delete</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete "{itemToDelete?.name}"? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteConfirmOpen(false)}>
              Cancel
            </Button>
            <Button variant="destructive" onClick={handleDeleteConfirm}>
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Create Badge Dialog */}
      <Dialog open={isCreateBadgeOpen} onOpenChange={setIsCreateBadgeOpen}>
        <DialogContent className="sm:max-w-[700px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Create New Badge</DialogTitle>
            <DialogDescription>
              Add a new badge or accessory to the system
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="new-badge-id">Badge ID *</Label>
                <Input
                  id="new-badge-id"
                  placeholder="e.g., eco_champion"
                  value={newBadge.badgeId}
                  onChange={(e) => setNewBadge({ ...newBadge, badgeId: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-badge-category">Type</Label>
                <Select
                  value={newBadge.category}
                  onValueChange={(value) => setNewBadge({ ...newBadge, category: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="badge">Badge</SelectItem>
                    <SelectItem value="cloth">Cloth/Accessory</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="new-badge-name-en">Name (English) *</Label>
                <Input
                  id="new-badge-name-en"
                  placeholder="Badge name in English"
                  value={newBadge.nameEn}
                  onChange={(e) => setNewBadge({ ...newBadge, nameEn: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-badge-name-zh">Name (Chinese)</Label>
                <Input
                  id="new-badge-name-zh"
                  placeholder="徽章中文名称"
                  value={newBadge.nameZh}
                  onChange={(e) => setNewBadge({ ...newBadge, nameZh: e.target.value })}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="new-badge-desc-en">Description (English)</Label>
                <Textarea
                  id="new-badge-desc-en"
                  placeholder="Description in English"
                  value={newBadge.descriptionEn}
                  onChange={(e) => setNewBadge({ ...newBadge, descriptionEn: e.target.value })}
                  rows={2}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-badge-desc-zh">Description (Chinese)</Label>
                <Textarea
                  id="new-badge-desc-zh"
                  placeholder="中文描述"
                  value={newBadge.descriptionZh}
                  onChange={(e) => setNewBadge({ ...newBadge, descriptionZh: e.target.value })}
                  rows={2}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="new-badge-subcategory">Sub Category</Label>
                <Select
                  value={newBadge.subCategory}
                  onValueChange={(value) => setNewBadge({ ...newBadge, subCategory: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="normal badge">Normal Badge</SelectItem>
                    <SelectItem value="VIP badge">VIP Badge</SelectItem>
                    <SelectItem value="rank badge">Rank Badge</SelectItem>
                    <SelectItem value="clothes_Hat">Hat</SelectItem>
                    <SelectItem value="clothes_clothing">Clothing</SelectItem>
                    <SelectItem value="clothes_shoes">Shoes</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-badge-method">Acquisition Method</Label>
                <Select
                  value={newBadge.acquisitionMethod}
                  onValueChange={(value) => setNewBadge({ ...newBadge, acquisitionMethod: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="purchase">Points Purchase</SelectItem>
                    <SelectItem value="achievement">Achievement Unlock</SelectItem>
                    <SelectItem value="task">Task Reward</SelectItem>
                    <SelectItem value="vip">VIP Exclusive</SelectItem>
                    <SelectItem value="event">Event Limited</SelectItem>
                    <SelectItem value="free">Free</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              {newBadge.acquisitionMethod === 'purchase' && (
                <div className="space-y-2">
                  <Label htmlFor="new-badge-price">Price (points)</Label>
                  <Input
                    id="new-badge-price"
                    type="number"
                    value={newBadge.purchaseCost}
                    onChange={(e) => setNewBadge({ ...newBadge, purchaseCost: parseInt(e.target.value) || 0 })}
                  />
                </div>
              )}
              {newBadge.acquisitionMethod === 'achievement' && (
                <div className="space-y-2">
                  <Label htmlFor="new-badge-threshold">Carbon Threshold (g)</Label>
                  <Input
                    id="new-badge-threshold"
                    type="number"
                    value={newBadge.carbonThreshold}
                    onChange={(e) => setNewBadge({ ...newBadge, carbonThreshold: parseInt(e.target.value) || 0 })}
                  />
                </div>
              )}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="new-badge-icon-url">Icon URL</Label>
                <Input
                  id="new-badge-icon-url"
                  placeholder="/icons/badges/..."
                  value={newBadge.iconUrl}
                  onChange={(e) => setNewBadge({ ...newBadge, iconUrl: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-badge-icon-color">Icon Color</Label>
                <Input
                  id="new-badge-icon-color"
                  type="color"
                  value={newBadge.iconColorScheme}
                  onChange={(e) => setNewBadge({ ...newBadge, iconColorScheme: e.target.value })}
                />
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsCreateBadgeOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleCreateBadge}
              disabled={!newBadge.badgeId || !newBadge.nameEn}
            >
              Create Badge
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Create Accessory Dialog */}
      <Dialog open={isCreateAccessoryOpen} onOpenChange={setIsCreateAccessoryOpen}>
        <DialogContent className="sm:max-w-[700px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Create New Accessory</DialogTitle>
            <DialogDescription>
              Add a new pet accessory to the system
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="new-accessory-id">Accessory ID *</Label>
                <Input
                  id="new-accessory-id"
                  placeholder="e.g., cool_hat_01"
                  value={newAccessory.badgeId}
                  onChange={(e) => setNewAccessory({ ...newAccessory, badgeId: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-accessory-subcategory">Sub Category</Label>
                <Select
                  value={newAccessory.subCategory}
                  onValueChange={(value) => setNewAccessory({ ...newAccessory, subCategory: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="clothes_Hat">Hat</SelectItem>
                    <SelectItem value="clothes_clothing">Clothing</SelectItem>
                    <SelectItem value="clothes_shoes">Shoes</SelectItem>
                    <SelectItem value="clothes_accessory">Accessory</SelectItem>
                    <SelectItem value="clothes_background">Background</SelectItem>
                    <SelectItem value="clothes_effect">Effect</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="new-accessory-name-en">Name (English) *</Label>
                <Input
                  id="new-accessory-name-en"
                  placeholder="Accessory name in English"
                  value={newAccessory.nameEn}
                  onChange={(e) => setNewAccessory({ ...newAccessory, nameEn: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-accessory-name-zh">Name (Chinese)</Label>
                <Input
                  id="new-accessory-name-zh"
                  placeholder="配饰中文名称"
                  value={newAccessory.nameZh}
                  onChange={(e) => setNewAccessory({ ...newAccessory, nameZh: e.target.value })}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="new-accessory-desc-en">Description (English)</Label>
                <Textarea
                  id="new-accessory-desc-en"
                  placeholder="Description in English"
                  value={newAccessory.descriptionEn}
                  onChange={(e) => setNewAccessory({ ...newAccessory, descriptionEn: e.target.value })}
                  rows={2}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-accessory-desc-zh">Description (Chinese)</Label>
                <Textarea
                  id="new-accessory-desc-zh"
                  placeholder="中文描述"
                  value={newAccessory.descriptionZh}
                  onChange={(e) => setNewAccessory({ ...newAccessory, descriptionZh: e.target.value })}
                  rows={2}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="new-accessory-method">Acquisition Method</Label>
                <Select
                  value={newAccessory.acquisitionMethod}
                  onValueChange={(value) => setNewAccessory({ ...newAccessory, acquisitionMethod: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="purchase">Points Purchase</SelectItem>
                    <SelectItem value="achievement">Achievement Unlock</SelectItem>
                    <SelectItem value="task">Task Reward</SelectItem>
                    <SelectItem value="vip">VIP Exclusive</SelectItem>
                    <SelectItem value="event">Event Limited</SelectItem>
                    <SelectItem value="free">Free</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              {newAccessory.acquisitionMethod === 'purchase' && (
                <div className="space-y-2">
                  <Label htmlFor="new-accessory-price">Price (points)</Label>
                  <Input
                    id="new-accessory-price"
                    type="number"
                    value={newAccessory.purchaseCost}
                    onChange={(e) => setNewAccessory({ ...newAccessory, purchaseCost: parseInt(e.target.value) || 0 })}
                  />
                </div>
              )}
              {newAccessory.acquisitionMethod === 'achievement' && (
                <div className="space-y-2">
                  <Label htmlFor="new-accessory-threshold">Carbon Threshold (g)</Label>
                  <Input
                    id="new-accessory-threshold"
                    type="number"
                    value={newAccessory.carbonThreshold}
                    onChange={(e) => setNewAccessory({ ...newAccessory, carbonThreshold: parseInt(e.target.value) || 0 })}
                  />
                </div>
              )}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="new-accessory-icon-url">Image URL</Label>
                <Input
                  id="new-accessory-icon-url"
                  placeholder="/images/accessories/..."
                  value={newAccessory.iconUrl}
                  onChange={(e) => setNewAccessory({ ...newAccessory, iconUrl: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="new-accessory-icon-color">Theme Color</Label>
                <Input
                  id="new-accessory-icon-color"
                  type="color"
                  value={newAccessory.iconColorScheme}
                  onChange={(e) => setNewAccessory({ ...newAccessory, iconColorScheme: e.target.value })}
                />
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsCreateAccessoryOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleCreateAccessory}
              disabled={!newAccessory.badgeId || !newAccessory.nameEn}
            >
              Create Accessory
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Accessory Edit Dialog */}
      <Dialog open={isAccessoryEditOpen} onOpenChange={setIsAccessoryEditOpen}>
        <DialogContent className="sm:max-w-[600px]">
          <DialogHeader>
            <DialogTitle>Edit Pet Accessory</DialogTitle>
            <DialogDescription>
              Update accessory information and settings
            </DialogDescription>
          </DialogHeader>
          {editingAccessory && (
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="accessory-name">Accessory Name</Label>
                <Input
                  id="accessory-name"
                  value={editingAccessory.name}
                  onChange={(e) => setEditingAccessory({ ...editingAccessory, name: e.target.value })}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="accessory-description">Description</Label>
                <Textarea
                  id="accessory-description"
                  value={editingAccessory.description}
                  onChange={(e) => setEditingAccessory({ ...editingAccessory, description: e.target.value })}
                  rows={3}
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="accessory-price">Price (points)</Label>
                  <Input
                    id="accessory-price"
                    type="number"
                    value={editingAccessory.price}
                    onChange={(e) => setEditingAccessory({ ...editingAccessory, price: parseInt(e.target.value) })}
                    disabled={editingAccessory.acquisitionMethod !== 'purchase'}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="accessory-category">Category</Label>
                  <Select
                    value={editingAccessory.category}
                    onValueChange={(value: any) => setEditingAccessory({ ...editingAccessory, category: value })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="hat">Hat</SelectItem>
                      <SelectItem value="clothing">Clothing</SelectItem>
                      <SelectItem value="accessory">Accessory</SelectItem>
                      <SelectItem value="background">Background</SelectItem>
                      <SelectItem value="effect">Effect</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsAccessoryEditOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleSaveAccessoryEdit}>
              Save Changes
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
