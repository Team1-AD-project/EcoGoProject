import { Cloud, Users, ShoppingCart, Package, FileText, Tag, Mail, Bell, Truck } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';

export function Workbench() {
  const quickLinks = [
    { icon: <Users className="size-8 text-blue-500" />, label: '用户', color: 'bg-blue-50' },
    { icon: <ShoppingCart className="size-8 text-green-500" />, label: '分析', color: 'bg-green-50' },
    { icon: <Package className="size-8 text-orange-500" />, label: '商品', color: 'bg-orange-50' },
    { icon: <FileText className="size-8 text-purple-500" />, label: '订单', color: 'bg-purple-50' },
    { icon: <Tag className="size-8 text-yellow-500" />, label: '营销', color: 'bg-yellow-50' },
    { icon: <Mail className="size-8 text-cyan-500" />, label: '消息', color: 'bg-cyan-50' },
    { icon: <Bell className="size-8 text-pink-500" />, label: '检验', color: 'bg-pink-50' },
    { icon: <Truck className="size-8 text-indigo-500" />, label: '配送', color: 'bg-indigo-50' }
  ];

  const recentActivities = [
    { time: '20:30', user: 'SunSmile', action: '解决了 7 issue，提高提醒进度完成度' },
    { time: '19:30', user: 'Jamme', action: '关闭了 7 bug，转移给总后台化主不需' },
    { time: '18:30', user: '项目经理', action: '指定了总后了另别本项目项目一阶bug' },
    { time: '17:30', user: '项目经理', action: '指定了总后了另别本项目项目二阶bug' },
    { time: '16:30', user: '项目经理', action: '指定了总后了另别本项目项目三阶bug' },
    { time: '15:30', user: '项目经理', action: '指定了总后了另别本项目项目四阶bug' },
    { time: '14:30', user: '项目经理', action: '指定了总后了另别本项目项目五阶bug' },
    { time: '12:30', user: '项目经理', action: '指定了总后了另别本项目项目六阶bug' }
  ];

  const myTasks = [
    { id: 1, name: '解决项目一阶bug', user: '任务名称', status: '进行中', color: 'text-blue-600' },
    { id: 2, name: '解决项目二阶bug', user: '任务名称', status: '进行中', color: 'text-blue-600' },
    { id: 3, name: '解决项目三阶bug', user: '任务名称', status: '进行中', color: 'text-blue-600' },
    { id: 4, name: '解决项目四阶bug', user: '任务名称', status: '进行中', color: 'text-blue-600' },
    { id: 5, name: '解决项目五阶bug', user: '任务名称', status: '进行中', color: 'text-blue-600' },
    { id: 6, name: '解决项目六阶bug', user: '任务名称', status: '待开始', color: 'text-gray-500' }
  ];

  const projects = [
    { id: '项目ID00001', name: '项目名称', startDate: '2020-03-01', endDate: '2020-06-01', status: '进行中', progress: 30 }
  ];

  const teamMembers = [
    { name: 'SunSmile', role: '小组计算，交互专家', status: '在线' },
    { name: '何时谷字提好听', role: '', status: '在线' }
  ];

  return (
    <div className="p-6 space-y-6 bg-gray-50">
      {/* Greeting Section */}
      <div className="bg-gradient-to-r from-blue-500 to-cyan-400 rounded-lg p-6 text-white">
        <div className="flex items-center gap-3">
          <Avatar className="size-12">
            <AvatarFallback className="bg-white/20 text-white">早</AvatarFallback>
          </Avatar>
          <div>
            <h2 className="text-xl font-semibold">早安, 管理员，开始您一天的工作吧!</h2>
            <p className="text-sm text-white/80 mt-1">今日多云转晴，18°C - 25°C，即刻提醒外出携带雨伞</p>
          </div>
        </div>
        <div className="flex items-center gap-8 mt-6">
          <div className="text-center">
            <div className="text-sm text-white/80">项目数</div>
            <div className="text-2xl font-bold mt-1">3</div>
          </div>
          <div className="text-center">
            <div className="text-sm text-white/80">待办</div>
            <div className="text-2xl font-bold mt-1">6 / 24</div>
          </div>
          <div className="text-center">
            <div className="text-sm text-white/80">消息</div>
            <div className="text-2xl font-bold mt-1">1,689</div>
          </div>
        </div>
      </div>

      {/* Quick Links */}
      <div className="grid grid-cols-4 md:grid-cols-8 gap-4">
        {quickLinks.map((link, index) => (
          <button
            key={index}
            className="flex flex-col items-center gap-3 p-4 bg-white rounded-lg hover:shadow-md transition-shadow"
          >
            <div className={`size-16 ${link.color} rounded-lg flex items-center justify-center`}>
              {link.icon}
            </div>
            <span className="text-sm text-gray-700">{link.label}</span>
          </button>
        ))}
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Activities */}
        <Card className="lg:col-span-1">
          <CardHeader className="pb-3">
            <CardTitle className="text-base">最新动态</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {recentActivities.map((activity, index) => (
                <div key={index} className="flex items-start gap-3 text-sm">
                  <span className="text-gray-500 flex-shrink-0 w-12">{activity.time}</span>
                  <div className="flex-1">
                    <span className="text-blue-600">{activity.user}</span>
                    <span className="text-gray-600 ml-1">{activity.action}</span>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* My Tasks */}
        <Card className="lg:col-span-1">
          <CardHeader className="pb-3">
            <CardTitle className="text-base">我的任务</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {myTasks.map((task) => (
                <div key={task.id} className="flex items-center gap-3">
                  <div className="flex items-center justify-center size-8 bg-blue-50 rounded text-blue-600 font-medium text-sm flex-shrink-0">
                    {task.id}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className={`text-sm font-medium ${task.color}`}>{task.name}</div>
                  </div>
                  <Badge variant="secondary" className="flex-shrink-0 text-xs">
                    {task.status}
                  </Badge>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Monthly Target */}
        <Card className="lg:col-span-1">
          <CardHeader className="pb-3">
            <CardTitle className="text-base">本月目标</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col items-center justify-center py-8">
              <div className="relative size-40">
                <svg className="size-full -rotate-90" viewBox="0 0 100 100">
                  <circle
                    cx="50"
                    cy="50"
                    r="40"
                    fill="none"
                    stroke="#e5e7eb"
                    strokeWidth="8"
                  />
                  <circle
                    cx="50"
                    cy="50"
                    r="40"
                    fill="none"
                    stroke="#3b82f6"
                    strokeWidth="8"
                    strokeDasharray="251.2"
                    strokeDashoffset="75.36"
                    strokeLinecap="round"
                  />
                </svg>
                <div className="absolute inset-0 flex flex-col items-center justify-center">
                  <Bell className="size-8 text-blue-600 mb-2" />
                  <span className="text-3xl font-bold text-gray-900">285</span>
                </div>
              </div>
              <p className="text-sm text-gray-600 mt-4">作业 本月目标已达标</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Project Progress & Team Members */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Project Progress */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-base">项目进展</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b">
                  <tr className="text-gray-600">
                    <th className="text-left py-3 font-medium">项目名称</th>
                    <th className="text-left py-3 font-medium">开始时间</th>
                    <th className="text-left py-3 font-medium">结束时间</th>
                    <th className="text-left py-3 font-medium">状态</th>
                    <th className="text-left py-3 font-medium">进度</th>
                  </tr>
                </thead>
                <tbody>
                  {projects.map((project) => (
                    <tr key={project.id} className="border-b">
                      <td className="py-3">
                        <div className="text-blue-600">{project.id}</div>
                      </td>
                      <td className="py-3 text-gray-600">{project.startDate}</td>
                      <td className="py-3 text-gray-600">{project.endDate}</td>
                      <td className="py-3">
                        <Badge variant="secondary" className="bg-green-100 text-green-700">
                          {project.status}
                        </Badge>
                      </td>
                      <td className="py-3">
                        <div className="flex items-center gap-2">
                          <Progress value={project.progress} className="flex-1 h-2" />
                          <span className="text-gray-600 text-xs">{project.progress}%</span>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>

        {/* Team Members */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-base">小组成员</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {teamMembers.map((member, index) => (
                <div key={index} className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Avatar className="size-10">
                      <AvatarFallback className="bg-blue-100 text-blue-600">
                        {member.name.charAt(0)}
                      </AvatarFallback>
                    </Avatar>
                    <div>
                      <div className="text-sm font-medium text-gray-900">{member.name}</div>
                      {member.role && (
                        <div className="text-xs text-gray-500">{member.role}</div>
                      )}
                    </div>
                  </div>
                  <Badge className="bg-green-100 text-green-700">
                    {member.status}
                  </Badge>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
