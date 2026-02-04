import { useState, useEffect } from 'react';
import { Card } from '@/components/ui/card';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
  Search,
  ArrowUpCircle,
  Trophy,
  ShoppingBag,
  Award,
  Footprints,
  Settings,
  Info
} from 'lucide-react';
import { fetchUserList, type User } from '@/services/userService';
import { fetchUserTransactions, type PointsTransaction } from '@/services/pointsService';
import { toast } from 'sonner';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";

export function PointsTransactionManagement() {
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [transactions, setTransactions] = useState<PointsTransaction[]>([]);
  const [isLoadingUsers, setIsLoadingUsers] = useState(false);
  const [isLoadingTransactions, setIsLoadingTransactions] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const pageSize = 10;

  // Load users on mount or page change
  useEffect(() => {
    const loadUsers = async () => {
      setIsLoadingUsers(true);
      try {
        const response = await fetchUserList(page, pageSize);
        if (response.code === 200) {
          setUsers(response.data?.list || []);
          setTotalPages(response.data?.totalPages || 1);
        } else {
          toast.error(response.message || 'Failed to fetch users');
        }
      } catch (error) {
        console.error(error);
        toast.error('Error loading users');
      } finally {
        setIsLoadingUsers(false);
      }
    };
    loadUsers();
  }, [page]);

  // Load transactions when user selected
  useEffect(() => {
    if (!selectedUser) return;

    const loadTransactions = async () => {
      setIsLoadingTransactions(true);
      setTransactions([]);
      try {
        const response = await fetchUserTransactions(selectedUser.userid); // Note: using userid (e.g. user001) not uuid if that's what API expects
        if (response.code === 200) {
          setTransactions(response.data || []);
        } else {
          toast.error(response.message || 'Failed to fetch transactions');
        }
      } catch (error) {
        console.error(error);
        toast.error('Error loading transactions');
      } finally {
        setIsLoadingTransactions(false);
      }
    };
    loadTransactions();
  }, [selectedUser]);

  const getTransactionIcon = (source: string) => {
    switch (source) {
      case 'trip':
        return <Footprints className="size-5 text-green-600" />;
      case 'vip':
        return <Trophy className="size-5 text-purple-600" />;
      case 'store':
        return <ShoppingBag className="size-5 text-blue-600" />;
      case 'badge':
        return <Award className="size-5 text-orange-600" />;
      case 'admin':
        return <Settings className="size-5 text-gray-600" />;
      default:
        return <Info className="size-5 text-gray-400" />;
    }
  };

  const getTransactionLabel = (source: string) => {
    switch (source) {
      case 'trip': return 'Walk Earnings';
      case 'vip': return 'VIP Membership';
      case 'store': return 'Item Purchase';
      case 'badge': return 'Badge Purchase';
      case 'admin': return 'System Adjustment';
      default: return 'Transaction';
    }
  };



  const filteredUsers = users.filter(user =>
    (user.nickname || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (user.userid || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Header */}
      <div className="p-6 bg-white border-b flex justify-between items-start">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Points Transaction Management</h2>
          <p className="text-gray-600 mt-1">View user points income and expenditure records and transaction details</p>
        </div>
        <div className="relative w-64">
          <Search className="absolute left-2 top-2.5 h-4 w-4 text-gray-500" />
          <Input
            placeholder="Search by name or ID..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-8"
          />
        </div>
      </div>

      {/* Content Container */}
      <div className="flex-1 flex overflow-hidden">
        {/* User List Section */}
        <div className="w-80 bg-white border-r flex flex-col">
          <div className="p-4 border-b bg-gray-50 flex-shrink-0 space-y-3">
            <div>
              <h3 className="font-semibold text-gray-900">User List</h3>
              <p className="text-xs text-gray-600 mt-1">Click to view transaction records</p>
            </div>
            <div className="relative">
              <Search className="absolute left-2 top-2.5 h-4 w-4 text-gray-500" />
              <Input
                placeholder="Search users..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-8 bg-white h-9 text-sm"
              />
            </div>
          </div>
          <div className="flex-1 overflow-y-auto p-2 space-y-2">
            {!isLoadingUsers && filteredUsers.length === 0 && (
              <div className="text-center py-4 text-gray-500 text-sm">No users found</div>
            )}
            {filteredUsers.map((user) => (
              <button
                key={user.id}
                onClick={() => setSelectedUser(user)}
                className={`w-full p-3 rounded-lg text-left transition-all border ${selectedUser?.id === user.id
                  ? 'bg-blue-50 border-blue-500 shadow-sm'
                  : 'bg-white border-transparent hover:bg-gray-50'
                  }`}
              >
                <div className="flex items-center gap-3">
                  <Avatar className="size-10 flex-shrink-0">
                    <AvatarFallback
                      className={user.vip?.active ? 'bg-purple-600 text-white text-xs' : 'bg-blue-600 text-white text-xs'}
                    >
                      {user.nickname?.substring(0, 2).toUpperCase() || 'U'}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-semibold text-gray-900 truncate text-sm">
                        {user.nickname}
                      </span>
                      {user.vip?.active && (
                        <Badge variant="secondary" className="bg-purple-100 text-purple-700 text-[10px] h-5 px-1.5">VIP</Badge>
                      )}
                    </div>
                    <div className="flex items-center justify-between text-xs">
                      <span className="text-gray-500">Balance:</span>
                      <span className="font-bold text-blue-600">{user.currentPoints} pts</span>
                    </div>
                  </div>
                </div>
              </button>
            ))}
          </div>
          <div className="p-4 border-t bg-gray-50 flex items-center justify-between flex-shrink-0">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage(p => Math.max(1, p - 1))}
              disabled={page === 1 || isLoadingUsers}
            >
              Previous
            </Button>
            <span className="text-xs text-gray-500">
              Page {page} of {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage(p => Math.min(totalPages, p + 1))}
              disabled={page === totalPages || isLoadingUsers}
            >
              Next
            </Button>
          </div>
        </div>

        {/* Transaction Details Section */}
        <div className="flex-1 bg-white overflow-hidden flex flex-col">
          {selectedUser ? (
            <>
              {/* Selected User Header */}
              {/* Selected User Header */}
              <div className="flex-shrink-0 p-6 border-b bg-gradient-to-r from-blue-50/50 to-purple-50/50">
                <div className="flex items-start gap-5">
                  <Avatar className="size-16 border-4 border-white shadow-sm mt-1">
                    <AvatarFallback
                      className={selectedUser.vip?.active ? 'bg-purple-600 text-white text-xl font-bold' : 'bg-blue-600 text-white text-xl font-bold'}
                    >
                      {selectedUser.nickname?.substring(0, 2).toUpperCase() || 'U'}
                    </AvatarFallback>
                  </Avatar>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-3 mb-4">
                      <h3 className="text-2xl font-bold text-gray-900 tracking-tight">{selectedUser.nickname}</h3>
                      <Badge className={`px-2.5 py-0.5 text-xs font-medium ${selectedUser.vip?.active ? 'bg-purple-600 hover:bg-purple-700' : 'bg-blue-600 hover:bg-blue-700'}`}>
                        {selectedUser.vip?.active ? 'VIP USER' : 'NORMAL USER'}
                      </Badge>
                    </div>

                    <div className="grid grid-cols-3 gap-4 max-w-3xl">
                      <Card className="p-4 shadow-sm border-blue-100 bg-white">
                        <p className="text-xs font-medium text-gray-500 mb-1">Current Balance</p>
                        <p className="text-2xl font-bold text-blue-600">{selectedUser.currentPoints}</p>
                      </Card>

                      <Card className="p-4 shadow-sm border-green-100 bg-white">
                        <p className="text-xs font-medium text-gray-500 mb-1">Total Earned</p>
                        <p className="text-2xl font-bold text-green-600">+{selectedUser.totalPoints}</p>
                      </Card>

                      <Card className="p-4 shadow-sm border-red-100 bg-white">
                        <p className="text-xs font-medium text-gray-500 mb-1">Total Spent</p>
                        <p className="text-2xl font-bold text-red-600">
                          -{Math.max(0, selectedUser.totalPoints - selectedUser.currentPoints)}
                        </p>
                      </Card>
                    </div>
                  </div>
                </div>
              </div>

              {/* Transaction List */}
              <div className="flex-1 overflow-hidden">
                <div className="p-4 border-b bg-gray-50">
                  <h4 className="font-semibold text-gray-900">Transaction Records</h4>
                  <p className="text-xs text-gray-600 mt-1">Total {transactions.length} records</p>
                </div>
                <ScrollArea className="h-[calc(100%-4rem)]">
                  <div className="p-4 space-y-3">
                    {isLoadingTransactions ? (
                      <div className="text-center py-8 text-gray-500">Loading transactions...</div>
                    ) : transactions.length === 0 ? (
                      <div className="text-center py-8 text-gray-500">No transactions found.</div>
                    ) : (
                      transactions.map((transaction) => (
                        <Card key={transaction.id} className="p-4 hover:shadow-md transition-shadow">
                          <div className="flex items-start gap-3">
                            <div className="flex-shrink-0 mt-1">
                              {getTransactionIcon(transaction.source)}
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-start justify-between gap-2 mb-1">
                                <div className="flex-1">
                                  <div className="flex items-center gap-2 mb-1">
                                    <Badge variant="outline" className="text-xs">
                                      {getTransactionLabel(transaction.source)}
                                    </Badge>
                                    {/* Admin Action Tooltip */}
                                    {transaction.source === 'admin' && transaction.admin_action && (
                                      <TooltipProvider>
                                        <Tooltip>
                                          <TooltipTrigger>
                                            <Info className="size-4 text-gray-400 hover:text-gray-600 cursor-help" />
                                          </TooltipTrigger>
                                          <TooltipContent>
                                            <p className="font-medium">Admin Action</p>
                                            <p className="text-xs">Operator: {transaction.admin_action.operator_id}</p>
                                            <p className="text-xs">Reason: {transaction.admin_action.reason}</p>
                                            <p className="text-xs capitalize">Status: {transaction.admin_action.approval_status}</p>
                                          </TooltipContent>
                                        </Tooltip>
                                      </TooltipProvider>
                                    )}
                                  </div>
                                  <p className="text-sm font-medium text-gray-900">
                                    {transaction.description || getTransactionLabel(transaction.source)}
                                  </p>
                                </div>
                                <div className="text-right flex-shrink-0">
                                  <p className={`text-lg font-bold ${transaction.change_type === 'gain' ? 'text-green-600' : 'text-red-600'
                                    }`}>
                                    {transaction.change_type === 'gain' ? '+' : '-'}{Math.abs(transaction.points)}
                                  </p>
                                </div>
                              </div>
                              <div className="flex items-center justify-between text-xs text-gray-500 mt-2">
                                <span>{new Date(transaction.created_at).toLocaleString()}</span>
                                <span className="font-medium">Balance: {transaction.balance_after} pts</span>
                              </div>
                            </div>
                          </div>
                        </Card>
                      ))
                    )}
                  </div>
                </ScrollArea>
              </div>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center">
              <div className="text-center text-gray-400">
                <ArrowUpCircle className="size-16 mx-auto mb-4 opacity-50" />
                <p className="text-lg font-medium">Please Select a User</p>
                <p className="text-sm mt-1">Click a user on the left to view transaction records</p>
              </div>
            </div>
          )}
        </div>

      </div>
    </div >
  );
}
