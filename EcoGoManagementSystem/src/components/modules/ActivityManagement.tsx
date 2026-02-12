import { useState, useEffect } from 'react';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Activity as ActivityIcon,
  Plus,
  Edit2,
  Trash2,
  Users,
  Calendar,
  Award,
  TrendingUp,
  CheckCircle,
  Clock,
  XCircle,
  FileText,
  Eye,
  Filter,
  Send,
  Loader2,
  Globe,
  MapPin
} from 'lucide-react';
import type {
  Activity,
  ActivityStatus,
  ActivityType,
  CreateActivityRequest,
  UpdateActivityRequest
} from '@/services/activityService';
import {
  fetchActivities,
  createActivity,
  updateActivity,
  deleteActivity,
  publishActivity,
} from '@/services/activityService';

interface NewActivityForm {
  title: string;
  description: string;
  type: ActivityType;
  status: ActivityStatus;
  rewardCredits: number;
  maxParticipants: number;
  startTime: string;
  endTime: string;
  latitude: string;
  longitude: string;
  locationName: string;
}

export function ActivityManagement() {
  const [activities, setActivities] = useState<Activity[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const [filterStatus, setFilterStatus] = useState<ActivityStatus | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isViewDialogOpen, setIsViewDialogOpen] = useState(false);
  const [selectedActivity, setSelectedActivity] = useState<Activity | null>(null);

  const [newActivity, setNewActivity] = useState<NewActivityForm>({
    title: '',
    description: '',
    type: 'OFFLINE',
    status: 'DRAFT',
    rewardCredits: 100,
    maxParticipants: 50,
    startTime: '',
    endTime: '',
    latitude: '',
    longitude: '',
    locationName: '',
  });

  // Fetch activities on mount
  useEffect(() => {
    loadActivities();
  }, []);

  const loadActivities = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetchActivities();
      if (response.code === 200) {
        setActivities(response.data || []);
      } else {
        setError(response.message || 'Failed to load activities');
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load activities');
    } finally {
      setLoading(false);
    }
  };

  // Filter activities
  const filteredActivities = activities.filter(activity => {
    const matchesStatus = filterStatus === 'ALL' || activity.status === filterStatus;
    const matchesSearch =
      activity.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      activity.description?.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesStatus && matchesSearch;
  });

  // Statistics
  const stats = {
    total: activities.length,
    published: activities.filter(a => a.status === 'PUBLISHED').length,
    ongoing: activities.filter(a => a.status === 'ONGOING').length,
    ended: activities.filter(a => a.status === 'ENDED').length,
    draft: activities.filter(a => a.status === 'DRAFT').length,
    totalParticipants: activities.reduce((sum, a) => sum + (a.currentParticipants || 0), 0),
    totalRewards: activities.reduce((sum, a) => sum + ((a.rewardCredits || 0) * (a.currentParticipants || 0)), 0),
  };

  const getStatusBadge = (status: ActivityStatus) => {
    const styles: Record<ActivityStatus, string> = {
      PUBLISHED: 'bg-blue-100 text-blue-700',
      ONGOING: 'bg-green-100 text-green-700',
      ENDED: 'bg-gray-100 text-gray-700',
      DRAFT: 'bg-orange-100 text-orange-700',
    };
    return <Badge className={styles[status]}>{status}</Badge>;
  };

  const getTypeBadge = (type: ActivityType) => {
    return type === 'ONLINE'
      ? <Badge className="bg-cyan-100 text-cyan-700"><Globe className="size-3 mr-1" />Online</Badge>
      : <Badge className="bg-amber-100 text-amber-700"><MapPin className="size-3 mr-1" />Offline</Badge>;
  };

  const getStatusIcon = (status: ActivityStatus) => {
    switch (status) {
      case 'PUBLISHED':
        return <CheckCircle className="size-4 text-blue-600" />;
      case 'ONGOING':
        return <Clock className="size-4 text-green-600" />;
      case 'ENDED':
        return <XCircle className="size-4 text-gray-600" />;
      case 'DRAFT':
        return <FileText className="size-4 text-orange-600" />;
    }
  };

  const formatDateTime = (dateString: string) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
  };

  const handleAddActivity = async () => {
    if (!newActivity.title || !newActivity.startTime || !newActivity.endTime) {
      alert('Please fill in title, start time and end time');
      return;
    }

    try {
      setActionLoading(true);
      const request: CreateActivityRequest = {
        title: newActivity.title,
        description: newActivity.description,
        type: newActivity.type,
        status: newActivity.status,
        rewardCredits: newActivity.rewardCredits,
        maxParticipants: newActivity.maxParticipants,
        startTime: new Date(newActivity.startTime).toISOString(),
        endTime: new Date(newActivity.endTime).toISOString(),
        latitude: newActivity.latitude ? parseFloat(newActivity.latitude) : null,
        longitude: newActivity.longitude ? parseFloat(newActivity.longitude) : null,
        locationName: newActivity.locationName || null,
      };

      const response = await createActivity(request);
      if (response.code === 200) {
        await loadActivities();
        setIsAddDialogOpen(false);
        resetNewActivity();
      } else {
        alert(response.message || 'Failed to create activity');
      }
    } catch (err: any) {
      alert(err.message || 'Failed to create activity');
    } finally {
      setActionLoading(false);
    }
  };

  const handleEditActivity = async () => {
    if (!selectedActivity) return;

    try {
      setActionLoading(true);
      const request: UpdateActivityRequest = {
        title: selectedActivity.title,
        description: selectedActivity.description,
        type: selectedActivity.type,
        status: selectedActivity.status,
        rewardCredits: selectedActivity.rewardCredits,
        maxParticipants: selectedActivity.maxParticipants,
        latitude: selectedActivity.latitude,
        longitude: selectedActivity.longitude,
        locationName: selectedActivity.locationName,
      };

      const response = await updateActivity(selectedActivity.id, request);
      if (response.code === 200) {
        await loadActivities();
        setIsEditDialogOpen(false);
        setSelectedActivity(null);
      } else {
        alert(response.message || 'Failed to update activity');
      }
    } catch (err: any) {
      alert(err.message || 'Failed to update activity');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteActivity = async (id: string) => {
    if (!confirm('Are you sure you want to delete this activity?')) return;

    try {
      setActionLoading(true);
      await deleteActivity(id);
      await loadActivities();
    } catch (err: any) {
      alert(err.message || 'Failed to delete activity');
    } finally {
      setActionLoading(false);
    }
  };

  const handlePublishActivity = async (id: string) => {
    try {
      setActionLoading(true);
      const response = await publishActivity(id);
      if (response.code === 200) {
        await loadActivities();
      } else {
        alert(response.message || 'Failed to publish activity');
      }
    } catch (err: any) {
      alert(err.message || 'Failed to publish activity');
    } finally {
      setActionLoading(false);
    }
  };

  const handleViewParticipants = (activity: Activity) => {
    setSelectedActivity(activity);
    setIsViewDialogOpen(true);
  };

  const resetNewActivity = () => {
    setNewActivity({
      title: '',
      description: '',
      type: 'OFFLINE',
      status: 'DRAFT',
      rewardCredits: 100,
      maxParticipants: 50,
      startTime: '',
      endTime: '',
      latitude: '',
      longitude: '',
      locationName: '',
    });
  };

  if (loading) {
    return (
      <div className="h-full flex items-center justify-center bg-gray-50">
        <div className="flex items-center gap-2 text-gray-500">
          <Loader2 className="size-6 animate-spin" />
          <span>Loading activities...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="h-full flex flex-col items-center justify-center bg-gray-50 gap-4">
        <p className="text-red-500">{error}</p>
        <Button onClick={loadActivities}>Retry</Button>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Header */}
      <div className="p-6 bg-white border-b">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">Activity Management</h2>
            <p className="text-gray-600 mt-1">Manage eco-friendly activities and events</p>
          </div>
          <Button onClick={() => setIsAddDialogOpen(true)} className="bg-green-600 hover:bg-green-700">
            <Plus className="size-4 mr-2" />
            Add New Activity
          </Button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        {/* Statistics Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="p-5 bg-gradient-to-br from-blue-500 to-blue-600 text-white">
            <div className="flex items-center justify-between mb-3">
              <ActivityIcon className="size-8 opacity-80" />
              <Badge className="bg-white/20 text-white">Total</Badge>
            </div>
            <p className="text-sm opacity-90 mb-1">Total Activities</p>
            <p className="text-3xl font-bold">{stats.total}</p>
            <div className="mt-3 pt-3 border-t border-white/20">
              <div className="flex items-center justify-between text-sm">
                <span>Published: {stats.published}</span>
                <span>Ongoing: {stats.ongoing}</span>
              </div>
            </div>
          </Card>

          <Card className="p-5 bg-gradient-to-br from-green-500 to-green-600 text-white">
            <div className="flex items-center justify-between mb-3">
              <Users className="size-8 opacity-80" />
              <Badge className="bg-white/20 text-white">Participants</Badge>
            </div>
            <p className="text-sm opacity-90 mb-1">Total Participants</p>
            <p className="text-3xl font-bold">{stats.totalParticipants}</p>
            <div className="mt-3 pt-3 border-t border-white/20 flex items-center gap-2 text-sm">
              <TrendingUp className="size-4" />
              <span>Active engagement</span>
            </div>
          </Card>

          <Card className="p-5 bg-gradient-to-br from-purple-500 to-purple-600 text-white">
            <div className="flex items-center justify-between mb-3">
              <Award className="size-8 opacity-80" />
              <Badge className="bg-white/20 text-white">Rewards</Badge>
            </div>
            <p className="text-sm opacity-90 mb-1">Total Rewards Issued</p>
            <p className="text-3xl font-bold">{stats.totalRewards.toLocaleString()}</p>
            <div className="mt-3 pt-3 border-t border-white/20">
              <p className="text-xs opacity-75">Points distributed to participants</p>
            </div>
          </Card>

          <Card className="p-5 bg-gradient-to-br from-orange-500 to-orange-600 text-white">
            <div className="flex items-center justify-between mb-3">
              <Clock className="size-8 opacity-80" />
              <Badge className="bg-white/20 text-white">Status</Badge>
            </div>
            <p className="text-sm opacity-90 mb-1">Ongoing Activities</p>
            <p className="text-3xl font-bold">{stats.ongoing}</p>
            <div className="mt-3 pt-3 border-t border-white/20">
              <div className="flex items-center justify-between text-sm">
                <span>Draft: {stats.draft}</span>
                <span>Ended: {stats.ended}</span>
              </div>
            </div>
          </Card>
        </div>

        {/* Filters */}
        <Card className="p-4">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1">
              <div className="relative">
                <Input
                  placeholder="Search by title or description..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10"
                />
                <Filter className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-gray-400" />
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Label>Status:</Label>
              <Select value={filterStatus} onValueChange={(value: ActivityStatus | 'ALL') => setFilterStatus(value)}>
                <SelectTrigger className="w-40">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Status</SelectItem>
                  <SelectItem value="PUBLISHED">Published</SelectItem>
                  <SelectItem value="ONGOING">Ongoing</SelectItem>
                  <SelectItem value="ENDED">Ended</SelectItem>
                  <SelectItem value="DRAFT">Draft</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </Card>

        {/* Activities Table */}
        <Card className="overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow className="bg-gray-50">
                <TableHead className="w-[50px]">Status</TableHead>
                <TableHead>Title / Description</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Location</TableHead>
                <TableHead className="text-center">Rewards</TableHead>
                <TableHead className="text-center">Participants</TableHead>
                <TableHead>Start Time</TableHead>
                <TableHead>End Time</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredActivities.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={9} className="text-center py-8 text-gray-500">
                    No activities found
                  </TableCell>
                </TableRow>
              ) : (
                filteredActivities.map((activity) => (
                  <TableRow key={activity.id} className="hover:bg-gray-50">
                    <TableCell>
                      <div className="flex items-center gap-2">
                        {getStatusIcon(activity.status)}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div>
                        <p className="font-medium text-gray-900">{activity.title || 'Untitled'}</p>
                        <p className="text-sm text-gray-500 line-clamp-1">{activity.description}</p>
                        <div className="flex items-center gap-2 mt-1">
                          {getStatusBadge(activity.status)}
                          <span className="text-xs text-gray-400">ID: {activity.id?.slice(0, 12)}...</span>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      {getTypeBadge(activity.type)}
                    </TableCell>
                    <TableCell>
                      {activity.locationName ? (
                        <div className="flex items-center gap-1 text-sm">
                          <MapPin className="size-3 text-red-500 shrink-0" />
                          <span className="text-gray-700 truncate max-w-[140px]" title={activity.locationName}>{activity.locationName}</span>
                        </div>
                      ) : (
                        <span className="text-xs text-gray-400">-</span>
                      )}
                    </TableCell>
                    <TableCell className="text-center">
                      <div className="flex items-center justify-center gap-1">
                        <Award className="size-4 text-orange-500" />
                        <span className="font-semibold text-gray-900">{activity.rewardCredits}</span>
                      </div>
                    </TableCell>
                    <TableCell className="text-center">
                      <div>
                        <p className="font-semibold text-gray-900">
                          {activity.currentParticipants || 0} / {activity.maxParticipants}
                        </p>
                        <div className="w-full bg-gray-200 rounded-full h-1.5 mt-1">
                          <div
                            className="bg-green-600 h-1.5 rounded-full"
                            style={{ width: `${((activity.currentParticipants || 0) / activity.maxParticipants) * 100}%` }}
                          />
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1 text-sm text-gray-600">
                        <Calendar className="size-3" />
                        <span className="text-xs">{formatDateTime(activity.startTime)}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1 text-sm text-gray-600">
                        <Calendar className="size-3" />
                        <span className="text-xs">{formatDateTime(activity.endTime)}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center justify-end gap-1">
                        {activity.status === 'DRAFT' && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handlePublishActivity(activity.id)}
                            className="hover:bg-blue-50 text-blue-600"
                            disabled={actionLoading}
                            title="Publish"
                          >
                            <Send className="size-4" />
                          </Button>
                        )}
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleViewParticipants(activity)}
                          className="hover:bg-blue-50"
                          title="View Participants"
                        >
                          <Eye className="size-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setSelectedActivity(activity);
                            setIsEditDialogOpen(true);
                          }}
                          className="hover:bg-green-50"
                          title="Edit"
                        >
                          <Edit2 className="size-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDeleteActivity(activity.id)}
                          className="hover:bg-red-50 text-red-600"
                          disabled={actionLoading}
                          title="Delete"
                        >
                          <Trash2 className="size-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </Card>
      </div>

      {/* Add Activity Dialog */}
      <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Add New Activity</DialogTitle>
            <DialogDescription>Create a new eco-friendly activity for users to participate in.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <Label>Title *</Label>
              <Input
                placeholder="Enter activity title..."
                value={newActivity.title}
                onChange={(e) => setNewActivity({ ...newActivity, title: e.target.value })}
              />
            </div>
            <div>
              <Label>Description</Label>
              <Textarea
                placeholder="Enter activity description..."
                value={newActivity.description}
                onChange={(e) => setNewActivity({ ...newActivity, description: e.target.value })}
                rows={3}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>Type</Label>
                <Select value={newActivity.type} onValueChange={(value: ActivityType) => setNewActivity({ ...newActivity, type: value })}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ONLINE">Online</SelectItem>
                    <SelectItem value="OFFLINE">Offline</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label>Status</Label>
                <Select value={newActivity.status} onValueChange={(value: ActivityStatus) => setNewActivity({ ...newActivity, status: value })}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="DRAFT">Draft</SelectItem>
                    <SelectItem value="PUBLISHED">Published</SelectItem>
                    <SelectItem value="ONGOING">Ongoing</SelectItem>
                    <SelectItem value="ENDED">Ended</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>Reward Credits</Label>
                <Input
                  type="number"
                  value={newActivity.rewardCredits}
                  onChange={(e) => setNewActivity({ ...newActivity, rewardCredits: Number(e.target.value) })}
                />
              </div>
              <div>
                <Label>Max Participants</Label>
                <Input
                  type="number"
                  value={newActivity.maxParticipants}
                  onChange={(e) => setNewActivity({ ...newActivity, maxParticipants: Number(e.target.value) })}
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>Start Time *</Label>
                <Input
                  type="datetime-local"
                  value={newActivity.startTime}
                  onChange={(e) => setNewActivity({ ...newActivity, startTime: e.target.value })}
                />
              </div>
              <div>
                <Label>End Time *</Label>
                <Input
                  type="datetime-local"
                  value={newActivity.endTime}
                  onChange={(e) => setNewActivity({ ...newActivity, endTime: e.target.value })}
                />
              </div>
            </div>
            <div>
              <Label>Location Name</Label>
              <Input
                placeholder="e.g. UCD Student Centre"
                value={newActivity.locationName}
                onChange={(e) => setNewActivity({ ...newActivity, locationName: e.target.value })}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>Latitude</Label>
                <Input
                  type="number"
                  step="any"
                  placeholder="e.g. 53.3066"
                  value={newActivity.latitude}
                  onChange={(e) => setNewActivity({ ...newActivity, latitude: e.target.value })}
                />
              </div>
              <div>
                <Label>Longitude</Label>
                <Input
                  type="number"
                  step="any"
                  placeholder="e.g. -6.2186"
                  value={newActivity.longitude}
                  onChange={(e) => setNewActivity({ ...newActivity, longitude: e.target.value })}
                />
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsAddDialogOpen(false)}>Cancel</Button>
            <Button onClick={handleAddActivity} className="bg-green-600 hover:bg-green-700" disabled={actionLoading}>
              {actionLoading ? <Loader2 className="size-4 mr-2 animate-spin" /> : null}
              Add Activity
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Activity Dialog */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Edit Activity</DialogTitle>
            <DialogDescription>Update activity information.</DialogDescription>
          </DialogHeader>
          {selectedActivity && (
            <div className="space-y-4">
              <div>
                <Label>Title</Label>
                <Input
                  value={selectedActivity.title || ''}
                  onChange={(e) => setSelectedActivity({ ...selectedActivity, title: e.target.value })}
                />
              </div>
              <div>
                <Label>Description</Label>
                <Textarea
                  value={selectedActivity.description || ''}
                  onChange={(e) => setSelectedActivity({ ...selectedActivity, description: e.target.value })}
                  rows={3}
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label>Type</Label>
                  <Select value={selectedActivity.type} onValueChange={(value: ActivityType) => setSelectedActivity({ ...selectedActivity, type: value })}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ONLINE">Online</SelectItem>
                      <SelectItem value="OFFLINE">Offline</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label>Status</Label>
                  <Select value={selectedActivity.status} onValueChange={(value: ActivityStatus) => setSelectedActivity({ ...selectedActivity, status: value })}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="DRAFT">Draft</SelectItem>
                      <SelectItem value="PUBLISHED">Published</SelectItem>
                      <SelectItem value="ONGOING">Ongoing</SelectItem>
                      <SelectItem value="ENDED">Ended</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label>Reward Credits</Label>
                  <Input
                    type="number"
                    value={selectedActivity.rewardCredits}
                    onChange={(e) => setSelectedActivity({ ...selectedActivity, rewardCredits: Number(e.target.value) })}
                  />
                </div>
                <div>
                  <Label>Max Participants</Label>
                  <Input
                    type="number"
                    value={selectedActivity.maxParticipants}
                    onChange={(e) => setSelectedActivity({ ...selectedActivity, maxParticipants: Number(e.target.value) })}
                  />
                </div>
              </div>
              <div>
                <Label>Location Name</Label>
                <Input
                  placeholder="e.g. UCD Student Centre"
                  value={selectedActivity.locationName || ''}
                  onChange={(e) => setSelectedActivity({ ...selectedActivity, locationName: e.target.value || null })}
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label>Latitude</Label>
                  <Input
                    type="number"
                    step="any"
                    placeholder="e.g. 53.3066"
                    value={selectedActivity.latitude ?? ''}
                    onChange={(e) => setSelectedActivity({ ...selectedActivity, latitude: e.target.value ? parseFloat(e.target.value) : null })}
                  />
                </div>
                <div>
                  <Label>Longitude</Label>
                  <Input
                    type="number"
                    step="any"
                    placeholder="e.g. -6.2186"
                    value={selectedActivity.longitude ?? ''}
                    onChange={(e) => setSelectedActivity({ ...selectedActivity, longitude: e.target.value ? parseFloat(e.target.value) : null })}
                  />
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsEditDialogOpen(false)}>Cancel</Button>
            <Button onClick={handleEditActivity} className="bg-green-600 hover:bg-green-700" disabled={actionLoading}>
              {actionLoading ? <Loader2 className="size-4 mr-2 animate-spin" /> : null}
              Save Changes
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* View Participants Dialog */}
      <Dialog open={isViewDialogOpen} onOpenChange={setIsViewDialogOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Activity Participants</DialogTitle>
            <DialogDescription>
              {selectedActivity?.currentParticipants || 0} / {selectedActivity?.maxParticipants} participants
            </DialogDescription>
          </DialogHeader>
          {selectedActivity && (
            <div className="space-y-4">
              <div className="bg-gray-50 p-4 rounded-lg">
                <p className="text-sm font-medium text-gray-700 mb-1">{selectedActivity.title || 'Untitled'}</p>
                <p className="text-xs text-gray-600 line-clamp-3">{selectedActivity.description}</p>
                <div className="flex items-center gap-4 mt-3">
                  {getStatusBadge(selectedActivity.status)}
                  {getTypeBadge(selectedActivity.type)}
                  <div className="flex items-center gap-1 text-sm">
                    <Award className="size-4 text-orange-500" />
                    <span>{selectedActivity.rewardCredits} credits</span>
                  </div>
                </div>
              </div>

              <div>
                <p className="text-sm font-medium text-gray-700 mb-2">Participant IDs</p>
                {selectedActivity.participantIds && selectedActivity.participantIds.length > 0 ? (
                  <div className="space-y-2 max-h-60 overflow-y-auto">
                    {selectedActivity.participantIds.map((id, index) => (
                      <div key={index} className="flex items-center gap-2 p-2 bg-blue-50 rounded">
                        <Users className="size-4 text-blue-600" />
                        <span className="text-sm font-mono text-gray-700">{id}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-gray-500 italic">No participants yet</p>
                )}
              </div>
            </div>
          )}
          <DialogFooter>
            <Button onClick={() => setIsViewDialogOpen(false)}>Close</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
