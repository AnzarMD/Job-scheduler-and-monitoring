import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider, useQueryClient } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import toast from 'react-hot-toast';
import { useState, useEffect, useCallback } from 'react';
import ProtectedRoute from './components/shared/ProtectedRoute';
import Sidebar from './components/layout/Sidebar';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import JobDetailPage from './pages/JobDetailPage';
import AlertsPage from './pages/AlertsPage';
import { useWebSocket } from './hooks/useWebSocket';
import { ErrorBoundary } from 'react-error-boundary';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 30_000 },
  },
});

// AppLayout is the shell around all authenticated pages.
// It holds the WebSocket connection so it persists across page navigation.
// If the WebSocket was inside DashboardPage, it would disconnect every time
// the user navigated to JobDetailPage and reconnect when they came back.
function AppLayout({ children }) {
  const qc = useQueryClient();
  const [isLive, setIsLive] = useState(false);

  // Memoize the WebSocket message handler so it doesn't change on every render
  // This prevents unnecessary WebSocket reconnections
  const handleWsMessage = useCallback((data) => {
    // Show a toast notification
    if (data.status === 'SUCCESS') {
      toast.success(`Job completed ✓ (${data.durationMs}ms)`, { duration: 3000 });
    } else if (data.status === 'FAILED' || data.status === 'TIMEOUT') {
      toast.error(`Job ${data.status.toLowerCase()} after ${data.durationMs}ms`, { duration: 4000 });
    }

    // Invalidate React Query cache so the jobs list and execution history refresh
    // This triggers a background refetch of all queries tagged 'jobs' and 'executions'
    qc.invalidateQueries({ queryKey: ['jobs'] });
    qc.invalidateQueries({ queryKey: ['recent-executions'] });
    qc.invalidateQueries({ queryKey: ['alerts'] });
    if (data.jobId) {
      qc.invalidateQueries({ queryKey: ['executions', data.jobId] });
    }
  }, [qc]);

  // useWebSocket connects on mount and returns the connection status
  const { connected } = useWebSocket(handleWsMessage);

  // Sync connected state to isLive for the Sidebar indicator
  // We use useEffect to avoid infinite render loops from setting state in the component body
  useEffect(() => {
    setIsLive(connected);
  }, [connected]);

  return (
    <div className="flex min-h-screen bg-gray-50">
      <Sidebar isLive={isLive} />
      <main className="flex-1 overflow-auto">{children}</main>
    </div>
  );
}
function ErrorFallback({ error, resetErrorBoundary }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center p-8">
        <div className="text-6xl mb-4">⚠️</div>
        <h2 className="text-xl font-bold text-gray-800 mb-2">Something went wrong</h2>
        <p className="text-gray-500 mb-4 font-mono text-sm">{error.message}</p>
        <button
          onClick={resetErrorBoundary}
          className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700"
        >
          Try again
        </button>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Toaster position="top-right" />
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <AppLayout><DashboardPage /></AppLayout>
            </ProtectedRoute>
          } />
          <Route path="/jobs" element={
            <ProtectedRoute>
              <AppLayout><DashboardPage /></AppLayout>
            </ProtectedRoute>
          } />
          <Route path="/jobs/:id" element={
            <ProtectedRoute>
              <AppLayout><JobDetailPage /></AppLayout>
            </ProtectedRoute>
          } />
          <Route path="/alerts" element={
            <ProtectedRoute>
              <AppLayout><AlertsPage /></AppLayout>
            </ProtectedRoute>
          } />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
    </ErrorBoundary>
  );
}