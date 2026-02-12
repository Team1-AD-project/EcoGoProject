import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { LandingPage } from '@/components/LandingPage';
import { LoginPage } from '@/components/LoginPage';
import { AdminLayout } from '@/components/AdminLayout';
import { Toaster } from '@/components/ui/sonner';
import { ProtectedRoute } from '@/components/ProtectedRoute';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 门户网站 - 纯展示页面 */}
        <Route path="/" element={<LandingPage />} />

        {/* 管理平台登录 */}
        <Route path="/admin" element={<LoginPage />} />

        {/* 管理后台 - 需要登录 */}
        <Route element={<ProtectedRoute />}>
          <Route path="/admin/:module" element={<AdminLayout />} />
        </Route>

        {/* 其他路径重定向到首页 */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <Toaster position="top-center" />
    </BrowserRouter>
  );
}
