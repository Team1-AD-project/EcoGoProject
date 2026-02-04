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
  Trash2
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
  category: 'hat' | 'clothing' | 'accessory' | 'background' | 'effect';
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
  // 从 subCategory 推断 category
  let category: PetAccessory['category'] = 'accessory';
  if (apiBadge.subCategory?.toLowerCase().includes('hat')) category = 'hat';
  else if (apiBadge.subCategory?.toLowerCase().includes('clothing')) category = 'clothing';
  else if (apiBadge.subCategory?.toLowerCase().includes('shoes')) category = 'accessory';
  else if (apiBadge.subCategory?.toLowerCase().includes('background')) category = 'background';
  else if (apiBadge.subCategory?.toLowerCase().includes('effect')) category = 'effect';

  return {
    id: apiBadge.badgeId,
    name: apiBadge.name?.en || apiBadge.name?.zh || 'Unknown',
    description: apiBadge.description?.en || apiBadge.description?.zh || '',
    imageUrl: apiBadge.icon?.url || '',
    category,
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

      // 创建 ownedBy 映射
      const ownedMap = new Map<string, number>();
      stats.forEach(stat => ownedMap.set(stat.badgeId, stat.count));

      // 转换徽章数据
      const mappedBadges = badgeData.map(b =>
        mapApiBadgeToBadgeItem(b, ownedMap.get(b.badgeId) || 0)
      );
      setBadges(mappedBadges);

      // 转换服饰数据
      const mappedAccessories = clothData.map(c =>
        mapApiBadgeToPetAccessory(c, ownedMap.get(c.badgeId) || 0)
      );
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
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<{ id: string; type: 'badge' | 'accessory'; name: string } | null>(null);
  const [isCreateBadgeOpen, setIsCreateBadgeOpen] = useState(false);
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
    switch (category) {
      case 'hat':
        return <Crown className="size-4" />;
      case 'clothing':
        return <Shirt className="size-4" />;
      case 'accessory':
        return <CircleDot className="size-4" />;
      case 'background':
        return <Palette className="size-4" />;
      case 'effect':
        return <Sparkles className="size-4" />;
      default:
        return null;
    }
  };

  const getCategoryLabel = (category: string) => {
    const labels = {
      hat: 'Hat',
      clothing: 'Clothing',
      accessory: 'Accessory',
      background: 'Background',
      effect: 'Effect'
    };
    return labels[category as keyof typeof labels] || category;
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
    return methodMatch && categoryMatch;
  });

  const filteredAccessories = accessories.filter(accessory => {
    const methodMatch = filterMethod === 'all' || accessory.acquisitionMethod === filterMethod;
    const categoryMatch = filterCategory === 'all' || accessory.category === filterCategory;
    return methodMatch && categoryMatch;
  });

  const totalBadges = badges.length;
  const totalAccessories = accessories.length;
  const badgeOwners = badges.reduce((sum, b) => sum + b.ownedBy, 0);
  const accessoryOwners = accessories.reduce((sum, a) => sum + a.ownedBy, 0);

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Header */}
      <div className="p-6 bg-white border-b">
        <h2 className="text-2xl font-bold text-gray-900">Collectibles Management</h2>
        <p className="text-gray-600 mt-1">Manage badges and pet accessory system</p>
      </div>

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
          <p className="text-sm opacity-90 mb-1">Total Pet Accessories</p>
          <p className="text-3xl font-bold">{totalAccessories}</p>
          <p className="text-xs opacity-75 mt-1">Total Ownership: {accessoryOwners.toLocaleString()}</p>
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
          <p className="text-sm opacity-90 mb-1">Purchasable Accessories</p>
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
              Pet Accessory Management
            </TabsTrigger>
          </TabsList>

          {/* Badges Tab */}
          <TabsContent value="badges" className="flex-1 overflow-hidden mt-4 data-[state=active]:flex data-[state=active]:flex-col">
            {/* Filters */}
            <Card className="p-4 mb-4 flex-shrink-0">
              <div className="flex flex-wrap gap-4 items-end">
                <div className="flex-1 min-w-[180px]">
                  <Label>Category</Label>
                  <Select value={filterBadgeCategory} onValueChange={setFilterBadgeCategory}>
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
                  <Select value={filterMethod} onValueChange={setFilterMethod}>
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
                {filteredBadges.map((badge) => (
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
                        <span className="text-sm text-gray-600">Owners:</span>
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
            </div>
          </TabsContent>

          {/* Pet Accessories Tab */}
          <TabsContent value="accessories" className="flex-1 overflow-hidden mt-4 data-[state=active]:flex data-[state=active]:flex-col">
            {/* Filters */}
            <Card className="p-4 mb-4 flex-shrink-0">
              <div className="flex flex-wrap gap-4 items-end">
                <div className="flex-1 min-w-[180px]">
                  <Label>Category</Label>
                  <Select value={filterCategory} onValueChange={setFilterCategory}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Categories</SelectItem>
                      <SelectItem value="hat">Hat</SelectItem>
                      <SelectItem value="clothing">Clothing</SelectItem>
                      <SelectItem value="accessory">Accessory</SelectItem>
                      <SelectItem value="background">Background</SelectItem>
                      <SelectItem value="effect">Effect</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="flex-1 min-w-[180px]">
                  <Label>Acquisition Method</Label>
                  <Select value={filterMethod} onValueChange={setFilterMethod}>
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

                <Button className="bg-purple-600 hover:bg-purple-700 text-white gap-2">
                  <Dog className="size-4" />
                  Add Accessory
                </Button>
              </div>
            </Card>

            {/* Accessories Grid */}
            <div className="flex-1 overflow-y-auto">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                {filteredAccessories.map((accessory) => (
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
                          <span className="text-sm text-gray-600">Owners:</span>
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
