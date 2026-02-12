import { useState } from 'react';
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
  Award,
  Edit,
  Trophy,
  Target,
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
  Mountain
} from 'lucide-react';

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
  rarity: 'common' | 'rare' | 'epic' | 'legendary';
}

export function BadgeManagement() {
  const [badges, setBadges] = useState<BadgeItem[]>([
    {
      id: 'B001',
      name: '环保新人',
      description: '完成第一次步行记录的新手徽章',
      icon: <Footprints className="size-8 text-green-600" />,
      acquisitionMethod: 'free',
      price: 0,
      requirementDescription: '记录第一次步行',
      ownedBy: 2456,
      category: '入门徽章',
      rarity: 'common'
    },
    {
      id: 'B002',
      name: '步行达人',
      description: '累计步行10万步的成就徽章',
      icon: <Target className="size-8 text-blue-600" />,
      acquisitionMethod: 'achievement',
      price: 0,
      requirementDescription: '累计步行达到10万步',
      ownedBy: 1234,
      category: '成就徽章',
      rarity: 'rare'
    },
    {
      id: 'B003',
      name: '黄金会员',
      description: 'VIP会员专属尊贵徽章',
      icon: <Crown className="size-8 text-yellow-600" />,
      acquisitionMethod: 'vip',
      price: 0,
      requirementDescription: '成为VIP会员自动获得',
      ownedBy: 856,
      category: 'VIP徽章',
      rarity: 'epic'
    },
    {
      id: 'B004',
      name: '绿色守护者',
      description: '参与植树活动的环保徽章',
      icon: <Leaf className="size-8 text-green-500" />,
      acquisitionMethod: 'task',
      price: 0,
      requirementDescription: '参与至少一次植树活动',
      ownedBy: 567,
      category: '活动徽章',
      rarity: 'rare'
    },
    {
      id: 'B005',
      name: '星耀徽章',
      description: '限量版精美徽章，可用积分购买',
      icon: <Star className="size-8 text-purple-600" />,
      acquisitionMethod: 'purchase',
      price: 500,
      requirementDescription: '花费500积分购买',
      ownedBy: 432,
      category: '商店徽章',
      rarity: 'rare'
    },
    {
      id: 'B006',
      name: '闪电勇士',
      description: '连续7天打卡的毅力徽章',
      icon: <Zap className="size-8 text-yellow-500" />,
      acquisitionMethod: 'achievement',
      price: 0,
      requirementDescription: '连续7天完成步行任务',
      ownedBy: 789,
      category: '成就徽章',
      rarity: 'rare'
    },
    {
      id: 'B007',
      name: '爱心使者',
      description: '捐赠积分给公益项目的慈善徽章',
      icon: <Heart className="size-8 text-red-500" />,
      acquisitionMethod: 'task',
      price: 0,
      requirementDescription: '捐赠至少1000积分',
      ownedBy: 345,
      category: '公益徽章',
      rarity: 'epic'
    },
    {
      id: 'B008',
      name: '钻石护盾',
      description: '高级VIP专属顶级徽章',
      icon: <Shield className="size-8 text-blue-400" />,
      acquisitionMethod: 'vip',
      price: 0,
      requirementDescription: 'VIP年度会员专属',
      ownedBy: 234,
      category: 'VIP徽章',
      rarity: 'legendary'
    },
    {
      id: 'B009',
      name: '热情火焰',
      description: '限时活动专属纪念徽章',
      icon: <Flame className="size-8 text-orange-500" />,
      acquisitionMethod: 'event',
      price: 0,
      requirementDescription: '参与2026春节特别活动',
      ownedBy: 678,
      category: '活动徽章',
      rarity: 'epic'
    },
    {
      id: 'B010',
      name: '社交达人',
      description: '邀请10位好友加入的推广徽章',
      icon: <Users className="size-8 text-indigo-600" />,
      acquisitionMethod: 'achievement',
      price: 0,
      requirementDescription: '成功邀请10位好友注册',
      ownedBy: 456,
      category: '成就徽章',
      rarity: 'rare'
    },
    {
      id: 'B011',
      name: '荣耀奖章',
      description: '豪华版徽章，积分商店限量发售',
      icon: <Medal className="size-8 text-amber-600" />,
      acquisitionMethod: 'purchase',
      price: 1200,
      requirementDescription: '花费1200积分购买',
      ownedBy: 198,
      category: '商店徽章',
      rarity: 'epic'
    },
    {
      id: 'B012',
      name: '璀璨之星',
      description: '顶级收藏版徽章，价格不菲',
      icon: <Sparkles className="size-8 text-pink-500" />,
      acquisitionMethod: 'purchase',
      price: 2000,
      requirementDescription: '花费2000积分购买',
      ownedBy: 89,
      category: '商店徽章',
      rarity: 'legendary'
    },
    {
      id: 'B013',
      name: '勇攀高峰',
      description: '完成年度步行目标的成就徽章',
      icon: <Mountain className="size-8 text-gray-700" />,
      acquisitionMethod: 'achievement',
      price: 0,
      requirementDescription: '年度累计步行500万步',
      ownedBy: 123,
      category: '成就徽章',
      rarity: 'legendary'
    },
    {
      id: 'B014',
      name: '冠军奖杯',
      description: '排行榜第一名的荣誉徽章',
      icon: <Trophy className="size-8 text-yellow-500" />,
      acquisitionMethod: 'achievement',
      price: 0,
      requirementDescription: '获得月度排行榜第一名',
      ownedBy: 45,
      category: '竞技徽章',
      rarity: 'legendary'
    },
    {
      id: 'B015',
      name: '荣耀之光',
      description: '特殊活动限定高级徽章',
      icon: <Award className="size-8 text-cyan-500" />,
      acquisitionMethod: 'event',
      price: 0,
      requirementDescription: '参与周年庆典活动',
      ownedBy: 567,
      category: '活动徽章',
      rarity: 'epic'
    }
  ]);

  const [editingBadge, setEditingBadge] = useState<BadgeItem | null>(null);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [filterMethod, setFilterMethod] = useState<string>('all');
  const [filterRarity, setFilterRarity] = useState<string>('all');

  const handleEditBadge = (badge: BadgeItem) => {
    setEditingBadge({ ...badge });
    setIsEditDialogOpen(true);
  };

  const handleSaveEdit = () => {
    if (editingBadge) {
      setBadges(badges.map(b => b.id === editingBadge.id ? editingBadge : b));
      setIsEditDialogOpen(false);
      setEditingBadge(null);
    }
  };

  const getMethodBadge = (method: AcquisitionMethod) => {
    switch (method) {
      case 'purchase':
        return <Badge className="bg-blue-100 text-blue-700">积分购买</Badge>;
      case 'achievement':
        return <Badge className="bg-green-100 text-green-700">成就解锁</Badge>;
      case 'task':
        return <Badge className="bg-orange-100 text-orange-700">任务奖励</Badge>;
      case 'vip':
        return <Badge className="bg-purple-100 text-purple-700">VIP专属</Badge>;
      case 'event':
        return <Badge className="bg-pink-100 text-pink-700">活动限定</Badge>;
      case 'free':
        return <Badge className="bg-gray-100 text-gray-700">免费获取</Badge>;
      default:
        return null;
    }
  };

  const getRarityBadge = (rarity: string) => {
    switch (rarity) {
      case 'common':
        return <Badge variant="outline" className="border-gray-400 text-gray-700">普通</Badge>;
      case 'rare':
        return <Badge variant="outline" className="border-blue-400 text-blue-700">稀有</Badge>;
      case 'epic':
        return <Badge variant="outline" className="border-purple-400 text-purple-700">史诗</Badge>;
      case 'legendary':
        return <Badge variant="outline" className="border-yellow-400 text-yellow-700">传说</Badge>;
      default:
        return null;
    }
  };

  const getMethodLabel = (method: AcquisitionMethod) => {
    const labels = {
      purchase: '积分购买',
      achievement: '成就解锁',
      task: '任务奖励',
      vip: 'VIP专属',
      event: '活动限定',
      free: '免费获取'
    };
    return labels[method];
  };

  const filteredBadges = badges.filter(badge => {
    const methodMatch = filterMethod === 'all' || badge.acquisitionMethod === filterMethod;
    const rarityMatch = filterRarity === 'all' || badge.rarity === filterRarity;
    return methodMatch && rarityMatch;
  });

  const totalBadges = badges.length;
  const totalOwners = badges.reduce((sum, b) => sum + b.ownedBy, 0);
  const purchasableBadges = badges.filter(b => b.acquisitionMethod === 'purchase').length;
  const achievementBadges = badges.filter(b => b.acquisitionMethod === 'achievement').length;
  const vipBadges = badges.filter(b => b.acquisitionMethod === 'vip').length;
  const totalRevenue = badges
    .filter(b => b.acquisitionMethod === 'purchase')
    .reduce((sum, b) => sum + (b.price * b.ownedBy), 0);

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Header */}
      <div className="p-6 bg-white border-b">
        <h2 className="text-2xl font-bold text-gray-900">徽章管理</h2>
        <p className="text-gray-600 mt-1">管理徽章定价和获取方式</p>
      </div>

      {/* Statistics Cards */}
      <div className="p-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 bg-gradient-to-br from-blue-500 to-blue-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <Award className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">徽章总数</p>
          <p className="text-3xl font-bold">{totalBadges}</p>
          <p className="text-xs opacity-75 mt-1">可购买: {purchasableBadges} | 成就: {achievementBadges}</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-green-500 to-green-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <Users className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">徽章持有总数</p>
          <p className="text-3xl font-bold">{totalOwners.toLocaleString()}</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-purple-500 to-purple-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <Crown className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">VIP专属徽章</p>
          <p className="text-3xl font-bold">{vipBadges}</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-yellow-500 to-yellow-600 text-white">
          <div className="flex items-center justify-between mb-2">
            <Trophy className="size-8" />
          </div>
          <p className="text-sm opacity-90 mb-1">徽章销售收入</p>
          <p className="text-3xl font-bold">{totalRevenue.toLocaleString()}</p>
          <p className="text-xs opacity-75 mt-1">积分</p>
        </Card>
      </div>

      {/* Filters */}
      <div className="px-6 pb-4">
        <Card className="p-4">
          <div className="flex flex-wrap gap-4 items-end">
            <div className="flex-1 min-w-[200px]">
              <Label>获取方式</Label>
              <Select value={filterMethod} onValueChange={setFilterMethod}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部方式</SelectItem>
                  <SelectItem value="purchase">积分购买</SelectItem>
                  <SelectItem value="achievement">成就解锁</SelectItem>
                  <SelectItem value="task">任务奖励</SelectItem>
                  <SelectItem value="vip">VIP专属</SelectItem>
                  <SelectItem value="event">活动限定</SelectItem>
                  <SelectItem value="free">免费获取</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="flex-1 min-w-[200px]">
              <Label>稀有度</Label>
              <Select value={filterRarity} onValueChange={setFilterRarity}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部稀有度</SelectItem>
                  <SelectItem value="common">普通</SelectItem>
                  <SelectItem value="rare">稀有</SelectItem>
                  <SelectItem value="epic">史诗</SelectItem>
                  <SelectItem value="legendary">传说</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <Button className="bg-blue-600 hover:bg-blue-700 text-white gap-2">
              <Award className="size-4" />
              添加徽章
            </Button>
          </div>
        </Card>
      </div>

      {/* Badges Grid */}
      <div className="flex-1 overflow-hidden px-6 pb-6">
        <div className="h-full overflow-y-auto">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {filteredBadges.map((badge) => (
              <Card key={badge.id} className="p-5 hover:shadow-lg transition-shadow">
                {/* Badge Icon */}
                <div className="flex items-center justify-center mb-4">
                  <div className={`p-4 rounded-full ${
                    badge.rarity === 'legendary' ? 'bg-gradient-to-br from-yellow-100 to-yellow-200' :
                    badge.rarity === 'epic' ? 'bg-gradient-to-br from-purple-100 to-purple-200' :
                    badge.rarity === 'rare' ? 'bg-gradient-to-br from-blue-100 to-blue-200' :
                    'bg-gray-100'
                  }`}>
                    {badge.icon}
                  </div>
                </div>

                {/* Badge Info */}
                <div className="text-center mb-3">
                  <h3 className="font-bold text-gray-900 mb-1">{badge.name}</h3>
                  <div className="flex items-center justify-center gap-2 mb-2">
                    {getRarityBadge(badge.rarity)}
                    {getMethodBadge(badge.acquisitionMethod)}
                  </div>
                  <p className="text-sm text-gray-600 mb-2">
                    {badge.description}
                  </p>
                  <Badge variant="outline" className="text-xs">
                    {badge.category}
                  </Badge>
                </div>

                {/* Requirement & Price */}
                <div className="space-y-2 mb-4 p-3 bg-gray-50 rounded-lg">
                  <div className="text-sm">
                    <p className="text-gray-600 mb-1">获取条件:</p>
                    <p className="font-medium text-gray-900">{badge.requirementDescription}</p>
                  </div>
                  
                  {badge.acquisitionMethod === 'purchase' && (
                    <div className="flex items-center justify-between pt-2 border-t">
                      <span className="text-sm text-gray-600">价格:</span>
                      <span className="font-bold text-blue-600">{badge.price} 积分</span>
                    </div>
                  )}
                  
                  <div className="flex items-center justify-between pt-2 border-t">
                    <span className="text-sm text-gray-600">拥有人数:</span>
                    <span className="font-semibold text-gray-900">{badge.ownedBy}</span>
                  </div>
                </div>

                {/* Action Button */}
                <Button
                  className="w-full gap-2"
                  variant="outline"
                  onClick={() => handleEditBadge(badge)}
                >
                  <Edit className="size-4" />
                  编辑设置
                </Button>
              </Card>
            ))}
          </div>
        </div>
      </div>

      {/* Edit Dialog */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="max-w-md max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>编辑徽章设置</DialogTitle>
            <DialogDescription>修改徽章的获取方式和价格</DialogDescription>
          </DialogHeader>
          
          {editingBadge && (
            <div className="space-y-4">
              {/* Badge Preview */}
              <div className="p-4 bg-gray-50 rounded-lg text-center">
                <div className="flex items-center justify-center mb-2">
                  {editingBadge.icon}
                </div>
                <h3 className="font-bold text-gray-900">{editingBadge.name}</h3>
              </div>

              <div>
                <Label>徽章名称</Label>
                <Input
                  value={editingBadge.name}
                  onChange={(e) => setEditingBadge({
                    ...editingBadge,
                    name: e.target.value
                  })}
                />
              </div>

              <div>
                <Label>徽章描述</Label>
                <Textarea
                  value={editingBadge.description}
                  onChange={(e) => setEditingBadge({
                    ...editingBadge,
                    description: e.target.value
                  })}
                  rows={2}
                />
              </div>

              <div>
                <Label>获取方式</Label>
                <Select 
                  value={editingBadge.acquisitionMethod} 
                  onValueChange={(value: AcquisitionMethod) => setEditingBadge({
                    ...editingBadge,
                    acquisitionMethod: value,
                    price: value === 'purchase' ? editingBadge.price : 0
                  })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="purchase">积分购买</SelectItem>
                    <SelectItem value="achievement">成就解锁</SelectItem>
                    <SelectItem value="task">任务奖励</SelectItem>
                    <SelectItem value="vip">VIP专属</SelectItem>
                    <SelectItem value="event">活动限定</SelectItem>
                    <SelectItem value="free">免费获取</SelectItem>
                  </SelectContent>
                </Select>
                <p className="text-xs text-gray-500 mt-1">
                  当前: {getMethodLabel(editingBadge.acquisitionMethod)}
                </p>
              </div>

              {editingBadge.acquisitionMethod === 'purchase' && (
                <div>
                  <Label>购买价格（积分）</Label>
                  <Input
                    type="number"
                    value={editingBadge.price}
                    onChange={(e) => setEditingBadge({
                      ...editingBadge,
                      price: parseInt(e.target.value) || 0
                    })}
                  />
                  <p className="text-xs text-gray-500 mt-1">
                    设置为0表示免费，仅在"积分购买"方式下生效
                  </p>
                </div>
              )}

              <div>
                <Label>获取条件描述</Label>
                <Textarea
                  value={editingBadge.requirementDescription}
                  onChange={(e) => setEditingBadge({
                    ...editingBadge,
                    requirementDescription: e.target.value
                  })}
                  rows={2}
                  placeholder="例如：完成10次步行任务"
                />
              </div>

              <div>
                <Label>徽章分类</Label>
                <Input
                  value={editingBadge.category}
                  onChange={(e) => setEditingBadge({
                    ...editingBadge,
                    category: e.target.value
                  })}
                />
              </div>

              <div>
                <Label>稀有度</Label>
                <Select 
                  value={editingBadge.rarity} 
                  onValueChange={(value: 'common' | 'rare' | 'epic' | 'legendary') => setEditingBadge({
                    ...editingBadge,
                    rarity: value
                  })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="common">普通</SelectItem>
                    <SelectItem value="rare">稀有</SelectItem>
                    <SelectItem value="epic">史诗</SelectItem>
                    <SelectItem value="legendary">传说</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {/* Summary */}
              <div className="p-3 bg-blue-50 rounded-lg border border-blue-200">
                <p className="text-sm font-semibold text-gray-900 mb-2">设置摘要</p>
                <div className="space-y-1 text-xs text-gray-700">
                  <p>• 获取方式: <strong>{getMethodLabel(editingBadge.acquisitionMethod)}</strong></p>
                  {editingBadge.acquisitionMethod === 'purchase' && (
                    <p>• 购买价格: <strong>{editingBadge.price} 积分</strong></p>
                  )}
                  <p>• 稀有度: <strong>{editingBadge.rarity === 'common' ? '普通' : editingBadge.rarity === 'rare' ? '稀有' : editingBadge.rarity === 'epic' ? '史诗' : '传说'}</strong></p>
                  <p>• 当前拥有人数: <strong>{editingBadge.ownedBy}</strong></p>
                </div>
              </div>
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsEditDialogOpen(false)}>
              取消
            </Button>
            <Button onClick={handleSaveEdit} className="bg-blue-600 hover:bg-blue-700">
              保存修改
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
