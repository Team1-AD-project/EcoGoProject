import { Bell, Search, Settings, Menu, Maximize } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';

export function Header() {
  return (
    <header className="h-14 bg-white border-b border-gray-200 flex items-center justify-between px-4">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" className="text-gray-600">
          <Menu className="size-5" />
        </Button>
        <Button variant="ghost" size="icon" className="text-gray-600">
          <Maximize className="size-5" />
        </Button>
      </div>

      <div className="flex items-center gap-2">
        <Button variant="ghost" size="icon" className="text-gray-600">
          <Search className="size-5" />
        </Button>

        <Button variant="ghost" size="icon" className="text-gray-600">
          <Maximize className="size-5" />
        </Button>

        <Button variant="ghost" size="icon" className="text-gray-600">
          <Settings className="size-5" />
        </Button>

        <Button variant="ghost" size="icon" className="relative text-gray-600">
          <Bell className="size-5" />
          <span className="absolute top-1 right-1 size-2 bg-red-500 rounded-full" />
        </Button>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="gap-2 h-9">
              <Avatar className="size-7">
                <AvatarFallback className="bg-blue-600 text-white text-xs">
                  Ad
                </AvatarFallback>
              </Avatar>
              <span className="text-sm">Admin</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-48">
            <DropdownMenuLabel>我的账户</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem>个人设置</DropdownMenuItem>
            <DropdownMenuItem>活动日志</DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem className="text-red-600">
              退出登录
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
