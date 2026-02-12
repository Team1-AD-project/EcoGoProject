import { useState, useEffect } from 'react';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Switch } from '@/components/ui/switch';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
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
  Crown,
  Package,
  Edit,
  Trash2,
  Plus,
  TrendingUp,
  ShoppingBag,
  AlertCircle,
  Loader2,
  User,
  Ticket
} from 'lucide-react';
import { ImageWithFallback } from '@/components/figma/ImageWithFallback';
import {
  fetchRewards,
  fetchOrders,
  createReward,
  updateReward,
  deleteReward,
  fetchVouchers,
  createVoucher,
  updateVoucher,
  deleteVoucher,
  fetchCategories,
  type Reward,
  type Order,
  type CreateRewardRequest
} from '@/services/rewardService';
import { toast } from 'sonner';

export function RewardStoreManagement() {
  // Products State
  const [rewards, setRewards] = useState<Reward[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);

  // Orders State
  const [orders, setOrders] = useState<Order[]>([]);
  const [isLoadingOrders, setIsLoadingOrders] = useState(false);
  const [ordersPage, setOrdersPage] = useState(1);
  const [ordersTotalPages, setOrdersTotalPages] = useState(1);
  const [totalOrders, setTotalOrders] = useState(0);

  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [selectedReward, setSelectedReward] = useState<Reward | null>(null);


  const [vouchers, setVouchers] = useState<Reward[]>([]);
  const [isLoadingVouchers, setIsLoadingVouchers] = useState(false);
  const [vouchersPage, setVouchersPage] = useState(1);
  const [vouchersTotalPages, setVouchersTotalPages] = useState(1);


  const [categories, setCategories] = useState<string[]>([]);

  const [activeTab, setActiveTab] = useState('products');

  // Real-time Sold Counts from Orders
  const [soldCounts, setSoldCounts] = useState<Record<string, number>>({});
  const [totalPointsRedeemed, setTotalPointsRedeemed] = useState(0);

  useEffect(() => {
    loadRewards();
    loadCategories();
    loadSalesData();
  }, [page]);

  const loadSalesData = async () => {
    try {
      // Fetch a large batch of orders to aggregate sales
      const response = await fetchOrders(1, 1000);
      if (response && response.code === 200) {
        const allOrders = response.data.orders || [];
        const counts: Record<string, number> = {};
        let totalPoints = 0;

        allOrders.forEach(order => {
          if (order.status === 'COMPLETED') {
            totalPoints += (order.pointsUsed || 0);
            if (order.items) {
              order.items.forEach(item => {
                counts[item.goodsId] = (counts[item.goodsId] || 0) + item.quantity;
              });
            }
          }
        });
        setSoldCounts(counts);
        setTotalPointsRedeemed(totalPoints);
      }
    } catch (e) {
      console.error("Failed to load sales data", e);
    }
  };

  useEffect(() => {
    loadVouchers();
  }, [vouchersPage]);

  useEffect(() => {
    loadOrders();
  }, [ordersPage]);

  const loadCategories = async () => {
    try {
      const response = await fetchCategories();
      if (response && response.code === 200 && response.data?.categories) {
        setCategories(response.data.categories);
      }
    } catch (error) {
      console.error('Failed to load categories', error);
      toast.error('Failed to load categories');
    }
  };

  const loadVouchers = async () => {
    setIsLoadingVouchers(true);
    try {
      const response = await fetchVouchers(vouchersPage, 20);
      if (response && response.code === 200) {
        let list: Reward[] = [];
        const responseData = response.data as any;
        if (Array.isArray(responseData)) {
          list = responseData;
        } else if (responseData && typeof responseData === 'object') {
          list = responseData.list || responseData.goods || responseData.records || responseData.items || [];
          if (responseData.pagination) {
            setVouchersTotalPages(responseData.pagination.totalPages || 1);
          } else if (responseData.total) {
            setVouchersTotalPages(responseData.totalPages || 1);
          }
        }



        // Ensure ID mapping is robust
        const mappedList = list.map((item: any) => ({
          ...item,
          id: item.id || item.goodsId || item._id || '',
          // Ensure other fields are present to avoid UI issues
          name: item.name || 'Unnamed Voucher',
          stock: item.stock || 0
        }));
        setVouchers(mappedList);
      } else {
        toast.error(response.message || 'Failed to fetch vouchers');
      }
    } catch (error) {
      console.error(error);
      toast.error('Error loading vouchers');
    } finally {
      setIsLoadingVouchers(false);
    }
  };

  const loadRewards = async () => {
    setIsLoading(true);
    try {
      const response = await fetchRewards(page, 20); // Default size 20
      console.log('Rewards API Response:', response);
      if (response && response.code === 200) {
        let list: Reward[] = [];
        const responseData = response.data as any;

        if (Array.isArray(responseData)) {
          list = responseData;
        } else if (responseData && typeof responseData === 'object') {
          // Handle pagination wrapper or named list
          list = responseData.list || responseData.goods || responseData.records || responseData.items || [];

          // Update pagination if available in data
          if (responseData.pagination) {
            setTotalPages(responseData.pagination.totalPages || 1);
            setTotalItems(responseData.pagination.total || 0);
          } else if (responseData.total) { // user list style
            setTotalItems(responseData.total || 0);
            setTotalPages(responseData.totalPages || 1);
          }
        }


        // Ensure ID mapping is robust
        const mappedList = list.map((item: any) => ({
          ...item,
          id: item.id || item.goodsId || item._id || '',
          // Ensure other fields are present to avoid UI issues
          name: item.name || 'Unnamed Product',
          stock: item.stock || 0
        }));
        setRewards(mappedList);

        // Fallback or explicit pagination from root if logical
        if (response.pagination) {
          setTotalPages(response.pagination.totalPages);
          setTotalItems(response.pagination.total);
        }
      } else {
        toast.error('Failed to fetch rewards');
      }
    } catch (error) {
      console.error(error);
      toast.error('Error loading rewards');
      setRewards([]); // Ensure array on error
    } finally {
      setIsLoading(false);
    }
  };

  const loadOrders = async () => {
    setIsLoadingOrders(true);
    try {
      const response = await fetchOrders(ordersPage, 10);
      if (response && response.code === 200) {
        setOrders(response.data.orders || []);
        if (response.data.pagination) {
          setOrdersTotalPages(response.data.pagination.totalPages);
          setTotalOrders(response.data.pagination.total);
        }
      } else {
        toast.error('Failed to fetch orders');
      }
    } catch (error) {
      console.error(error);
      toast.error('Error loading orders');
    } finally {
      setIsLoadingOrders(false);
    }
  };

  const handleAddProduct = () => {
    setSelectedReward({
      id: '',
      name: '',
      description: '',
      price: 0,
      stock: 0,
      category: 'Lifestyle',
      brand: 'EcoBrand',
      imageUrl: '',
      isActive: true,
      isForRedemption: true,
      redemptionPoints: 0,
      vipLevelRequired: 0,
      redemptionLimit: -1,
      totalRedemptionCount: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    });
    setIsEditDialogOpen(true);
  };

  const handleEdit = (reward: Reward) => {
    setSelectedReward({ ...reward });
    setIsEditDialogOpen(true);
  };

  const handleSaveEdit = async () => {
    if (selectedReward) {
      try {
        const payload: CreateRewardRequest = {
          name: selectedReward.name || '',
          description: selectedReward.description || '',
          price: selectedReward.price || 0,
          stock: selectedReward.stock,
          category: selectedReward.category || 'Lifestyle',
          brand: selectedReward.brand || 'EcoBrand',
          imageUrl: selectedReward.imageUrl || '',
          isActive: selectedReward.isActive,
          isForRedemption: selectedReward.isForRedemption,
          redemptionPoints: selectedReward.redemptionPoints,
          vipLevelRequired: selectedReward.vipLevelRequired,
          redemptionLimit: selectedReward.redemptionLimit
        };

        if (activeTab === 'vouchers') {
          if (selectedReward.id) {
            await updateVoucher(selectedReward.id, payload);
            toast.success('Voucher updated successfully');
          } else {
            await createVoucher(payload);
            toast.success('Voucher created successfully');
          }
          loadVouchers();
        } else {
          if (selectedReward.id) {
            await updateReward(selectedReward.id, payload);
            toast.success('Reward updated successfully');
          } else {
            await createReward(payload);
            toast.success('Reward created successfully');
          }
          loadRewards();
        }

        setIsEditDialogOpen(false);
        setSelectedReward(null);
      } catch (error: any) {
        console.error(error);
        const errorMessage = error.response?.data?.message || error.message || 'Failed to save item';
        toast.error(`Error: ${errorMessage}`);
      }
    }
  };

  const handleDeleteClick = (reward: Reward) => {
    setSelectedReward(reward);
    setIsDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (selectedReward) {
      try {
        if (activeTab === 'vouchers') {
          await deleteVoucher(selectedReward.id);
          toast.success('Voucher deleted successfully');
          setVouchers(vouchers.filter(r => r.id !== selectedReward.id));
          loadVouchers();
        } else {
          await deleteReward(selectedReward.id);
          toast.success('Reward deleted successfully');
          setRewards(rewards.filter(r => r.id !== selectedReward.id));
          loadRewards();
        }
        setIsDeleteDialogOpen(false);
        setSelectedReward(null);
      } catch (error: any) {
        console.error(error);
        const errorMessage = error.response?.data?.message || error.message || 'Failed to delete item';
        toast.error(`Error: ${errorMessage}`);
      }
    }
  };

  // Stats
  const totalRedemptions = Object.values(soldCounts).reduce((sum, count) => sum + count, 0);
  const lowStockCount = (rewards || []).filter(r => r.stock < 10).length;


  const getOrderStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING': return <Badge variant="outline" className="bg-yellow-50 text-yellow-700 border-yellow-200">Pending</Badge>;
      case 'PAID': return <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">Paid</Badge>;
      case 'SHIPPED': return <Badge variant="outline" className="bg-purple-50 text-purple-700 border-purple-200">Shipped</Badge>;
      case 'COMPLETED': return <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200">Completed</Badge>;
      case 'CANCELLED': return <Badge variant="outline" className="bg-red-50 text-red-700 border-red-200">Cancelled</Badge>;
      default: return <Badge variant="outline">{status}</Badge>;
    }
  };

  return (
    <div className="min-h-full bg-gray-50 flex flex-col h-full">
      {/* Header */}
      <div className="p-6 bg-white border-b flex-shrink-0">
        <h2 className="text-2xl font-bold text-gray-900">Reward Store Management</h2>
        <p className="text-gray-600 mt-1">Manage product inventory, redemption points, and VIP rewards</p>
      </div>

      <div className="flex-1 overflow-y-auto p-6">
        {/* Statistics - Shared or specific? Keeping shared for now */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <Card className="p-4 bg-gradient-to-br from-blue-500 to-blue-600 text-white">
            <div className="flex items-center gap-3 mb-2">
              <Package className="size-8" />
            </div>
            <p className="text-sm opacity-90 mb-1">Total Products</p>
            <p className="text-3xl font-bold">{totalItems}</p>
          </Card>

          <Card className="p-4 bg-gradient-to-br from-green-500 to-green-600 text-white">
            <div className="flex items-center gap-3 mb-2">
              <ShoppingBag className="size-8" />
            </div>
            <p className="text-sm opacity-90 mb-1">Total Redemptions</p>
            <p className="text-3xl font-bold">{totalRedemptions}</p>
          </Card>

          <Card className="p-4 bg-gradient-to-br from-purple-500 to-purple-600 text-white">
            <div className="flex items-center gap-3 mb-2">
              <TrendingUp className="size-8" />
            </div>
            <p className="text-sm opacity-90 mb-1">Total Points Redeemed</p>
            <p className="text-3xl font-bold">{totalPointsRedeemed.toLocaleString()}</p>
            <p className="text-xs opacity-75 mt-1">consumed by users</p>
          </Card>

          <Card className="p-4 bg-gradient-to-br from-orange-500 to-orange-600 text-white">
            <div className="flex items-center gap-3 mb-2">
              <AlertCircle className="size-8" />
            </div>
            <p className="text-sm opacity-90 mb-1">Low Stock Alert</p>
            <p className="text-3xl font-bold">{lowStockCount}</p>
            <p className="text-xs opacity-75 mt-1">items &lt; 10 stock</p>
          </Card>
        </div>

        <Tabs defaultValue="products" value={activeTab} onValueChange={setActiveTab} className="space-y-4">
          <TabsList className="grid w-full max-w-2xl grid-cols-3">
            <TabsTrigger value="products" className="flex items-center gap-2">
              <Package className="size-4" />
              Products
            </TabsTrigger>
            <TabsTrigger value="vouchers" className="flex items-center gap-2">
              <Ticket className="size-4" />
              Vouchers
            </TabsTrigger>
            <TabsTrigger value="orders" className="flex items-center gap-2">
              <ShoppingBag className="size-4" />
              Redemption Orders
            </TabsTrigger>
          </TabsList>

          {/* Products Tab */}
          <TabsContent value="products">
            {/* Filters & Actions */}
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-3">
                {/* Category Filter could be re-enabled if API supports it or client-side filter */}
              </div>
              <Button className="gap-2" onClick={handleAddProduct}>
                <Plus className="size-4" />
                Add Product
              </Button>
            </div>

            {/* Products Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {isLoading ? (
                <div className="col-span-full text-center py-12 text-gray-500">Loading rewards...</div>
              ) : rewards.length === 0 ? (
                <div className="col-span-full text-center py-12 text-gray-500">No rewards found. Add your first product!</div>
              ) : (
                rewards.map((reward) => (
                  <Card key={reward.id} className="overflow-hidden bg-white hover:shadow-lg transition-all duration-300 border-gray-100 group">
                    <div className="relative aspect-[4/3] bg-gray-100 overflow-hidden">
                      <ImageWithFallback
                        src={reward.imageUrl || ''}
                        alt={reward.name || 'Product'}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                        fallbackText={reward.name ? reward.name.charAt(0).toUpperCase() : 'P'}
                      />
                      {/* Status Badges */}
                      <div className="absolute top-2 right-2 flex flex-col gap-1">
                        {!reward.isActive && <Badge variant="destructive" className="bg-red-500/90 hover:bg-red-500">Inactive</Badge>}
                        {reward.vipLevelRequired > 0 && <Badge className="bg-purple-500/90 hover:bg-purple-500">VIP {reward.vipLevelRequired}+</Badge>}
                        {!reward.isForRedemption && <Badge className="bg-gray-500/90 hover:bg-gray-500">Unavailable</Badge>}
                      </div>
                    </div>

                    <div className="p-4 flex-1 flex flex-col">
                      <h3 className="text-xl font-bold text-gray-900 mb-1 line-clamp-1" title={reward.name || ''}>{reward.name || 'Unnamed Product'}</h3>
                      <p className="text-sm text-gray-500 mb-4 line-clamp-2 h-10">{reward.description || 'No description available'}</p>

                      <div className="space-y-2">
                        <div className="flex items-center justify-between text-base">
                          <span className="text-gray-600">Points:</span>
                          <span className="font-bold text-blue-600">{reward.redemptionPoints} pts</span>
                        </div>
                        <div className="flex items-center justify-between text-base">
                          <span className="text-gray-600">Value:</span>
                          <span className="font-medium text-gray-500">${reward.price || 0}</span>
                        </div>
                        <div className="flex items-center justify-between text-base">
                          <span className="text-gray-600">Stock:</span>
                          <span className="font-bold text-gray-900">{reward.stock}</span>
                        </div>
                        <div className="flex items-center justify-between text-base">
                          <span className="text-gray-600">Sold:</span>
                          <span className="font-bold text-green-600">{soldCounts[reward.id] || 0}</span>
                        </div>
                        <div className="flex items-center justify-between pt-3 mt-2 border-t text-base">
                          <span className="text-gray-600">Category:</span>
                          <Badge variant="secondary" className="font-normal capitalize">{reward.category || 'General'}</Badge>
                        </div>
                      </div>

                      <div className="flex gap-3 mt-6">
                        <Button
                          variant="outline"
                          className="flex-1"
                          onClick={() => handleEdit(reward)}
                        >
                          <Edit className="size-4 mr-2" />
                          Edit
                        </Button>
                        <Button
                          variant="outline"
                          size="icon"
                          className="text-red-600 hover:text-red-700 hover:bg-red-50"
                          onClick={() => handleDeleteClick(reward)}
                          aria-label="Delete"
                        >
                          <Trash2 className="size-4" />
                        </Button>
                      </div>
                    </div>
                  </Card>
                ))
              )}
            </div>

            {/* Pagination Footer */}
            <div className="mt-8 flex items-center justify-center gap-2 border-t pt-4">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(p => Math.max(1, p - 1))}
                disabled={page === 1 || isLoading}
              >
                Previous
              </Button>

              <div className="flex items-center gap-1">
                {totalPages > 0 && Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  let p = i + 1;
                  if (totalPages > 5) {
                    if (page <= 3) p = i + 1;
                    else if (page >= totalPages - 2) p = totalPages - 4 + i;
                    else p = page - 2 + i;
                  }

                  return (
                    <Button
                      key={p}
                      variant={p === page ? "default" : "outline"}
                      size="sm"
                      className="w-8 h-8 p-0"
                      onClick={() => setPage(p)}
                    >
                      {p}
                    </Button>
                  );
                })}
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(p => Math.min(totalPages, p + 1))}
                disabled={page === totalPages || isLoading}
              >
                Next
              </Button>
            </div>
          </TabsContent>

          {/* Vouchers Tab */}
          <TabsContent value="vouchers">
            {/* Filters & Actions */}
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-3">
                {/* Category Filter could be re-enabled if API supports it or client-side filter */}
              </div>
              <Button className="gap-2" onClick={handleAddProduct}> {/* Re-using handleAddProduct for vouchers too */}
                <Plus className="size-4" />
                Add Voucher
              </Button>
            </div>

            {/* Vouchers Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {isLoadingVouchers ? (
                <div className="col-span-full text-center py-12 text-gray-500">Loading vouchers...</div>
              ) : vouchers.length === 0 ? (
                <div className="col-span-full text-center py-12 text-gray-500">No vouchers found. Add your first voucher!</div>
              ) : (
                vouchers.map((voucher) => (
                  <Card key={voucher.id} className="overflow-hidden bg-white hover:shadow-lg transition-all duration-300 border-gray-100 group">
                    <div className="relative aspect-[4/3] bg-gray-100 overflow-hidden">
                      <ImageWithFallback
                        src={voucher.imageUrl || ''}
                        alt={voucher.name || 'Voucher'}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                        fallbackText={voucher.name ? voucher.name.charAt(0).toUpperCase() : 'V'}
                      />
                      <div className="absolute top-2 right-2 flex flex-col gap-1">
                        {!voucher.isActive && <Badge variant="destructive" className="bg-red-500/90 hover:bg-red-500">Inactive</Badge>}
                        {voucher.vipLevelRequired > 0 && <Badge className="bg-purple-500/90 hover:bg-purple-500">VIP {voucher.vipLevelRequired}+</Badge>}
                        {!voucher.isForRedemption && <Badge className="bg-gray-500/90 hover:bg-gray-500">Unavailable</Badge>}
                      </div>
                    </div>

                    <div className="p-4 flex-1 flex flex-col">
                      <h3 className="text-xl font-bold text-gray-900 mb-1 line-clamp-1" title={voucher.name || ''}>{voucher.name || 'Unnamed Voucher'}</h3>
                      <p className="text-sm text-gray-500 mb-4 line-clamp-2 h-10">{voucher.description || 'No description available'}</p>

                      <div className="space-y-2">
                        <div className="flex items-center justify-between text-base">
                          <span className="text-gray-600">Points:</span>
                          <span className="font-bold text-blue-600">{voucher.redemptionPoints} pts</span>
                        </div>
                        <div className="flex items-center justify-between text-base">
                          <span className="text-gray-600">Value:</span>
                          <span className="font-medium text-gray-500">${voucher.price || 0}</span>
                        </div>
                        <div className="flex items-center justify-between text-base">
                          <span className="text-gray-600">Stock:</span>
                          <span className="font-bold text-gray-900">{voucher.stock}</span>
                        </div>
                        <div className="flex items-center justify-between text-base">
                          <span className="text-gray-600">Sold:</span>
                          <span className="font-bold text-green-600">{soldCounts[voucher.id] || 0}</span>
                        </div>
                        <div className="flex items-center justify-between pt-3 mt-2 border-t text-base">
                          <span className="text-gray-600">Category:</span>
                          <Badge variant="secondary" className="font-normal capitalize">{voucher.category || 'Voucher'}</Badge>
                        </div>
                      </div>

                      <div className="flex gap-3 mt-6">
                        <Button
                          variant="outline"
                          className="flex-1"
                          onClick={() => handleEdit(voucher)}
                        >
                          <Edit className="size-4 mr-2" />
                          Edit
                        </Button>
                        <Button
                          variant="outline"
                          size="icon"
                          className="text-red-600 hover:text-red-700 hover:bg-red-50"
                          onClick={() => handleDeleteClick(voucher)}
                          aria-label="Delete"
                        >
                          <Trash2 className="size-4" />
                        </Button>
                      </div>
                    </div>
                  </Card>
                ))
              )}
            </div>

            {/* Vouchers Pagination Footer */}
            <div className="mt-8 flex items-center justify-center gap-2 border-t pt-4">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setVouchersPage(p => Math.max(1, p - 1))}
                disabled={vouchersPage === 1 || isLoadingVouchers}
              >
                Previous
              </Button>

              <div className="flex items-center gap-1">
                {vouchersTotalPages > 0 && Array.from({ length: Math.min(5, vouchersTotalPages) }, (_, i) => {
                  let p = i + 1;
                  if (vouchersTotalPages > 5) {
                    if (vouchersPage <= 3) p = i + 1;
                    else if (vouchersPage >= vouchersTotalPages - 2) p = vouchersTotalPages - 4 + i;
                    else p = vouchersPage - 2 + i;
                  }

                  return (
                    <Button
                      key={p}
                      variant={p === vouchersPage ? "default" : "outline"}
                      size="sm"
                      className="w-8 h-8 p-0"
                      onClick={() => setVouchersPage(p)}
                    >
                      {p}
                    </Button>
                  );
                })}
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={() => setVouchersPage(p => Math.min(vouchersTotalPages, p + 1))}
                disabled={vouchersPage === vouchersTotalPages || isLoadingVouchers}
              >
                Next
              </Button>
            </div>
          </TabsContent>

          {/* Orders Tab */}
          <TabsContent value="orders">
            <Card>
              <div className="p-4 border-b bg-gray-50 flex justify-between items-center">
                <div>
                  <h3 className="font-semibold text-gray-900">Redemption Orders</h3>
                  <p className="text-xs text-gray-600 mt-1">View and manage user redemptions</p>
                </div>
                <Badge variant="secondary">Total: {totalOrders}</Badge>
              </div>

              {isLoadingOrders ? (
                <div className="text-center py-12 text-gray-500">Loading orders...</div>
              ) : orders.length === 0 ? (
                <div className="text-center py-12 text-gray-500">No orders found.</div>
              ) : (
                <div className="divide-y">
                  {orders.map((order) => (
                    <div key={order.id} className="p-4 hover:bg-gray-50 transition-colors">
                      <div className="flex items-start justify-between mb-2">
                        <div className="flex items-center gap-3">
                          <span className="font-mono text-sm font-medium text-gray-900">{order.orderNumber}</span>
                          {getOrderStatusBadge(order.status)}
                        </div>
                        <span className="text-xs text-gray-500">{new Date(order.createdAt).toLocaleString()}</span>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {/* Order Info */}
                        <div className="space-y-1">
                          <div className="flex items-center gap-2 text-sm text-gray-700">
                            <User className="size-3.5 text-gray-400" />
                            <span className="font-medium">{order.userId}</span>
                          </div>
                          <div className="text-sm text-gray-600 pl-5.5">
                            {(order.items || []).map((item, idx) => (
                              <div key={idx} className="flex items-center gap-2">
                                <span>{item.quantity}x {item.goodsName}</span>
                                <span className="text-xs text-gray-400">({item.subtotal} pts)</span>
                              </div>
                            ))}
                          </div>
                        </div>

                        {/* Points and Actions */}
                        <div className="flex flex-col items-end gap-1">
                          <div className="text-sm font-bold text-blue-600">-{order.pointsUsed} Points</div>
                          <div className="text-xs text-gray-500">
                            {order.paymentMethod} • {order.paymentStatus}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Orders Pagination */}
              <div className="p-4 border-t bg-gray-50 flex items-center justify-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setOrdersPage(p => Math.max(1, p - 1))}
                  disabled={ordersPage === 1 || isLoadingOrders}
                >
                  Previous
                </Button>

                <div className="flex items-center gap-1">
                  {ordersTotalPages > 0 && Array.from({ length: Math.min(5, ordersTotalPages) }, (_, i) => {
                    let p = i + 1;
                    if (ordersTotalPages > 5) {
                      if (ordersPage <= 3) p = i + 1;
                      else if (ordersPage >= ordersTotalPages - 2) p = ordersTotalPages - 4 + i;
                      else p = ordersPage - 2 + i;
                    }

                    return (
                      <Button
                        key={p}
                        variant={p === ordersPage ? "default" : "outline"}
                        size="sm"
                        className="w-8 h-8 p-0"
                        onClick={() => setOrdersPage(p)}
                      >
                        {p}
                      </Button>
                    );
                  })}
                </div>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setOrdersPage(p => Math.min(ordersTotalPages, p + 1))}
                  disabled={ordersPage === ordersTotalPages || isLoadingOrders}
                >
                  Next
                </Button>
              </div>
            </Card>
          </TabsContent>
        </Tabs>
      </div>

      {/* Edit Dialog (Simplified for brevity, using existing logic but new fields) */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="sm:max-w-[800px] overflow-y-auto max-h-[90vh]">
          <DialogHeader>
            <DialogTitle>{selectedReward?.id ? 'Edit Reward' : 'Add Product'}</DialogTitle>
            <DialogDescription>
              {selectedReward?.id ? 'Update reward details' : 'Create a new reward product'}
            </DialogDescription>
          </DialogHeader>
          {selectedReward && (
            <div className="space-y-6 py-4">
              <div className="grid grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label htmlFor="reward-name">Product Name</Label>
                  <Input id="reward-name" value={selectedReward.name || ''} onChange={e => setSelectedReward({ ...selectedReward, name: e.target.value })} placeholder="e.g. Eco Water Bottle" />
                </div>
                <div className="space-y-2">
                  <Label>Category</Label>
                  <Select
                    value={selectedReward.category || 'Lifestyle'}
                    onValueChange={v => setSelectedReward({ ...selectedReward, category: v })}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select Category" />
                    </SelectTrigger>
                    <SelectContent>
                      {categories.length > 0 ? (
                        categories.map((cat) => (
                          <SelectItem key={cat} value={cat}>
                            {cat.charAt(0).toUpperCase() + cat.slice(1)}
                          </SelectItem>
                        ))
                      ) : (
                        <div className="p-2 text-sm text-gray-500">No categories available</div>
                      )}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-2">
                <Label>Description</Label>
                <Textarea
                  value={selectedReward.description || ''}
                  onChange={e => setSelectedReward({ ...selectedReward, description: e.target.value })}
                  placeholder="Product description..."
                  className="min-h-[100px]"
                />
              </div>

              <div className="grid grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label>Brand</Label>
                  <Input value={selectedReward.brand || ''} onChange={e => setSelectedReward({ ...selectedReward, brand: e.target.value })} placeholder="e.g. EcoBrand" />
                </div>
                <div className="space-y-2">
                  <Label>Image URL</Label>
                  <Input value={selectedReward.imageUrl || ''} onChange={e => setSelectedReward({ ...selectedReward, imageUrl: e.target.value })} placeholder="/images/example.jpg" />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-6">
                <div className="space-y-2">
                  <Label>Points Cost (Redemption)</Label>
                  <Input type="number" value={selectedReward.redemptionPoints} onChange={e => setSelectedReward({ ...selectedReward, redemptionPoints: parseInt(e.target.value) || 0 })} />
                </div>
                <div className="space-y-2">
                  <Label>Price (Reference)</Label>
                  <Input type="number" value={selectedReward.price || 0} onChange={e => setSelectedReward({ ...selectedReward, price: parseFloat(e.target.value) || 0 })} />
                </div>
                <div className="space-y-2">
                  <Label>Stock</Label>
                  <Input type="number" value={selectedReward.stock} onChange={e => setSelectedReward({ ...selectedReward, stock: parseInt(e.target.value) || 0 })} />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label>VIP Level Required</Label>
                  <Input type="number" min="0" value={selectedReward.vipLevelRequired} onChange={e => setSelectedReward({ ...selectedReward, vipLevelRequired: parseInt(e.target.value) || 0 })} />
                </div>
                <div className="space-y-2">
                  <Label>Redemption Limit (-1 for unlimited)</Label>
                  <Input type="number" value={selectedReward.redemptionLimit} onChange={e => setSelectedReward({ ...selectedReward, redemptionLimit: parseInt(e.target.value) || -1 })} />
                </div>
              </div>

              <div className="flex items-center gap-6">
                <div className="flex items-center space-x-2">
                  <Switch checked={selectedReward.isActive} onCheckedChange={c => setSelectedReward({ ...selectedReward, isActive: c })} />
                  <Label>Active</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch checked={selectedReward.isForRedemption} onCheckedChange={c => setSelectedReward({ ...selectedReward, isForRedemption: c })} />
                  <Label>Available for Redemption</Label>
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsEditDialogOpen(false)}>Cancel</Button>
            <Button onClick={handleSaveEdit}>Save Changes</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Dialog */}
      <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>Delete Product</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete <span className="font-semibold text-gray-900">{selectedReward?.name}</span>?
              This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsDeleteDialogOpen(false)}>
              Cancel
            </Button>
            <Button variant="destructive" onClick={handleDeleteConfirm}>
              Delete Product
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
