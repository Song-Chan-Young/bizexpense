import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import RequireAuth from './auth/RequireAuth'
import Layout from './components/Layout'
import DashboardPage from './pages/DashboardPage'
import LoginPage from './pages/LoginPage'
import ApprovalPage from './pages/approval/ApprovalPage'
import SchedulePage from './pages/schedule/SchedulePage'
import TripDetailPage from './pages/trip/TripDetailPage'
import TripFormPage from './pages/trip/TripFormPage'
import TripListPage from './pages/trip/TripListPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            element={
              <RequireAuth>
                <Layout />
              </RequireAuth>
            }
          >
            <Route index element={<DashboardPage />} />
            <Route path="schedules" element={<SchedulePage />} />
            <Route path="trips" element={<TripListPage />} />
            <Route path="trips/new" element={<TripFormPage />} />
            <Route path="trips/:id" element={<TripDetailPage />} />
            <Route path="trips/:id/edit" element={<TripFormPage />} />
            <Route
              path="approvals"
              element={
                <RequireAuth roles={['MANAGER', 'ADMIN']}>
                  <ApprovalPage />
                </RequireAuth>
              }
            />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
