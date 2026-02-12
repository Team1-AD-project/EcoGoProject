import {
  Users,
  Map,
  Coins,
  Crown,
  Gift,
  Award,
  BarChart3,
  Megaphone,
  Trophy,
  MessageSquare,
  LogOut,
  Activity,
  Target
} from 'lucide-react';
import { cn } from '@/components/ui/utils';

interface MenuItem {
  id: string;
  label: string;
  icon: React.ReactNode;
}

interface SidebarProps {
  selectedModule: string;
  onModuleSelect: (moduleId: string) => void;
  onLogout?: () => void;
}

export function Sidebar({ selectedModule, onModuleSelect, onLogout }: SidebarProps) {
  const menuItems: MenuItem[] = [
    {
      id: 'dashboard',
      label: 'Dashboard',
      icon: <BarChart3 className="size-5" />
    },
    {
      id: 'user-management',
      label: 'User Management',
      icon: <Users className="size-5" />
    },
    {
      id: 'trip-management',
      label: 'Trip Data',
      icon: <Map className="size-5" />
    },
    {
      id: 'points-management',
      label: 'Point Transactions',
      icon: <Coins className="size-5" />
    },
    {
      id: 'vip-management',
      label: 'VIP Subscriptions',
      icon: <Crown className="size-5" />
    },
    {
      id: 'rewards-management',
      label: 'Reward Store',
      icon: <Gift className="size-5" />
    },
    {
      id: 'collectibles-management',
      label: 'Collectibles',
      icon: <Award className="size-5" />
    },
    {
      id: 'ad-management',
      label: 'Ad Management',
      icon: <Megaphone className="size-5" />
    },
    {
      id: 'leaderboard-management',
      label: 'Leaderboard',
      icon: <Trophy className="size-5" />
    },
    {
      id: 'chat-management',
      label: 'Chat Management',
      icon: <MessageSquare className="size-5" />
    },
    {
      id: 'activity-management',
      label: 'Activity Logs',
      icon: <Activity className="size-5" />
    },
    {
      id: 'challenge-management',
      label: 'Challenges',
      icon: <Target className="size-5" />
    }
  ];

  return (
    <div className="w-56 bg-[#001529] h-full overflow-y-auto">
      <div className="p-4">
        <div className="flex items-center gap-2 mb-8">
          <div className="size-8 bg-gradient-to-br from-blue-500 to-cyan-400 rounded-lg flex items-center justify-center">
            <span className="text-white font-bold text-lg">E</span>
          </div>
          <span className="text-white font-semibold text-base">Ele Admin Pro</span>
        </div>
        <nav className="space-y-1">
          {menuItems.map((item) => (
            <button
              key={item.id}
              onClick={() => onModuleSelect(item.id)}
              className={cn(
                'w-full flex items-center gap-3 px-4 py-2.5 rounded-md text-sm transition-all',
                selectedModule === item.id
                  ? 'bg-blue-600 text-white shadow-lg'
                  : 'text-gray-300 hover:bg-white/10'
              )}
            >
              {item.icon}
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        {/* Logout Button */}
        {onLogout && (
          <div className="mt-6 pt-6 border-t border-gray-700">
            <button
              onClick={onLogout}
              className="w-full flex items-center gap-3 px-4 py-2.5 rounded-md text-sm transition-all text-gray-300 hover:bg-red-600/20 hover:text-red-400"
            >
              <LogOut className="size-5" />
              <span>Logout</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}