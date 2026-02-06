import { useParams, useNavigate } from 'react-router-dom';
import { Header } from '@/components/Header';
import { Sidebar } from '@/components/Sidebar';
import { Dashboard } from '@/components/modules/Dashboard';
import { UserManagement } from '@/components/modules/UserManagement';
import { TripDataManagement } from '@/components/modules/TripDataManagement';
import { PointsTransactionManagement } from '@/components/modules/PointsTransactionManagement';
import { VIPManagement } from '@/components/modules/VIPManagement';
import { RewardStoreManagement } from '@/components/modules/RewardStoreManagement';
import { CollectiblesManagement } from '@/components/modules/CollectiblesManagement';
import { AnalyticsManagement } from '@/components/modules/AnalyticsManagement';
import { AdManagement } from '@/components/modules/AdManagement';
import { LeaderboardManagement } from '@/components/modules/LeaderboardManagement';
import { ChatManagement } from '@/components/modules/ChatManagement';
import { ActivityManagement } from '@/components/modules/ActivityManagement';
import { ChallengeManagement } from '@/components/modules/ChallengeManagement';

export function AdminLayout() {
  const { module } = useParams<{ module: string }>();
  const navigate = useNavigate();

  const handleModuleSelect = (newModule: string) => {
    navigate(`/admin/${newModule}`);
  };

  const handleLogout = () => {
    localStorage.removeItem('adminToken');
    localStorage.removeItem('adminInfo');
    navigate('/admin');
  };

  const renderContent = () => {
    switch (module) {
      case 'dashboard':
        return <Dashboard onModuleSelect={handleModuleSelect} />;
      case 'user-management':
        return <UserManagement />;
      case 'trip-management':
        return <TripDataManagement />;
      case 'points-management':
        return <PointsTransactionManagement />;
      case 'vip-management':
        return <VIPManagement />;
      case 'rewards-management':
        return <RewardStoreManagement />;
      case 'collectibles-management':
        return <CollectiblesManagement />;
      case 'analytics-management':
        return <AnalyticsManagement />;
      case 'ad-management':
        return <AdManagement />;
      case 'leaderboard-management':
        return <LeaderboardManagement />;
      case 'chat-management':
        return <ChatManagement />;
      case 'activity-management':
        return <ActivityManagement />;
      case 'challenge-management':
        return <ChallengeManagement />;
      default:
        return <Dashboard onModuleSelect={handleModuleSelect} />;
    }
  };

  return (
    <div className="size-full flex flex-col bg-gray-50">
      <Header />
      <div className="flex-1 flex overflow-hidden">
        <Sidebar
          selectedModule={module || 'dashboard'}
          onModuleSelect={handleModuleSelect}
          onLogout={handleLogout}
        />
        <main className="flex-1 overflow-y-auto">
          {renderContent()}
        </main>
      </div>
    </div>
  );
}
