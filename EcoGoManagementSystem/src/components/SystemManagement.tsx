import { useState } from 'react';
import { Users, Shield, Settings, Database, FileText, Bell } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';

export function SystemManagement() {
  return (
    <div className="p-6 space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-gray-900">系统管理</h2>
        <p className="text-gray-600 mt-1">管理系统配置、用户权限和其他设置</p>
      </div>

      <Tabs defaultValue="users" className="w-full">
        <TabsList>
          <TabsTrigger value="users">用户管理</TabsTrigger>
          <TabsTrigger value="roles">角色权限</TabsTrigger>
          <TabsTrigger value="settings">系统设置</TabsTrigger>
          <TabsTrigger value="logs">操作日志</TabsTrigger>
        </TabsList>

        <TabsContent value="users" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">用户列表</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex gap-3">
                  <Input placeholder="搜索用户..." className="max-w-sm" />
                  <Button>添加用户</Button>
                </div>
                <div className="border rounded-lg">
                  <table className="w-full text-sm">
                    <thead className="border-b bg-gray-50">
                      <tr>
                        <th className="text-left py-3 px-4 font-medium">用户名</th>
                        <th className="text-left py-3 px-4 font-medium">角色</th>
                        <th className="text-left py-3 px-4 font-medium">状态</th>
                        <th className="text-left py-3 px-4 font-medium">最后登录</th>
                        <th className="text-left py-3 px-4 font-medium">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr className="border-b">
                        <td className="py-3 px-4">管理员</td>
                        <td className="py-3 px-4">超级管理员</td>
                        <td className="py-3 px-4">
                          <span className="inline-block px-2 py-1 bg-green-100 text-green-700 rounded text-xs">
                            在线
                          </span>
                        </td>
                        <td className="py-3 px-4 text-gray-600">2026-01-24 10:30</td>
                        <td className="py-3 px-4">
                          <Button variant="ghost" size="sm">编辑</Button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="roles" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">角色权限管理</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <Button>添加角色</Button>
                <div className="grid gap-4">
                  {['超级管理员', '管理员', '普通用户'].map((role) => (
                    <Card key={role}>
                      <CardContent className="pt-6">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-3">
                            <Shield className="size-8 text-blue-600" />
                            <div>
                              <h3 className="font-medium">{role}</h3>
                              <p className="text-sm text-gray-600">完整的系统权限</p>
                            </div>
                          </div>
                          <Button variant="outline" size="sm">配置权限</Button>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="settings" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">系统配置</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-6">
                <div className="flex items-center justify-between">
                  <div className="space-y-0.5">
                    <Label>邮件通知</Label>
                    <p className="text-sm text-gray-600">接收系统邮件通知</p>
                  </div>
                  <Switch defaultChecked />
                </div>
                <div className="flex items-center justify-between">
                  <div className="space-y-0.5">
                    <Label>短信通知</Label>
                    <p className="text-sm text-gray-600">接收系统短信通知</p>
                  </div>
                  <Switch />
                </div>
                <div className="flex items-center justify-between">
                  <div className="space-y-0.5">
                    <Label>数据备份</Label>
                    <p className="text-sm text-gray-600">自动备份系统数据</p>
                  </div>
                  <Switch defaultChecked />
                </div>
                <div className="space-y-2">
                  <Label>系统名称</Label>
                  <Input defaultValue="Ele Admin Pro" />
                </div>
                <div className="space-y-2">
                  <Label>系统描述</Label>
                  <Input defaultValue="企业级管理系统" />
                </div>
                <Button>保存设置</Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="logs" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">操作日志</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {[
                  { time: '2026-01-24 10:30:25', user: '管理员', action: '登录系统' },
                  { time: '2026-01-24 10:15:12', user: '管理员', action: '修改用户权限' },
                  { time: '2026-01-24 09:45:33', user: '管理员', action: '添加新用户' },
                  { time: '2026-01-24 09:20:18', user: '管理员', action: '导出数据' },
                  { time: '2026-01-24 08:55:42', user: '管理员', action: '更新系统配置' }
                ].map((log, index) => (
                  <div key={index} className="flex items-center gap-4 p-3 bg-gray-50 rounded-lg">
                    <FileText className="size-5 text-gray-400 flex-shrink-0" />
                    <div className="flex-1">
                      <p className="text-sm font-medium">{log.action}</p>
                      <p className="text-xs text-gray-600">{log.user} · {log.time}</p>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
