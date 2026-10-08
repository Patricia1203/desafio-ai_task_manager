import { createBrowserRouter } from 'react-router-dom';
import AppLayout from './components/layout/AppLayout';
import DashboardPage from './pages/DashboardPage';
import TasksPage from './pages/TasksPage';
import AssistantPage from './pages/AssistantPage';
import AreasPage from './pages/AreasPage';
import AreaDetailPage from './pages/AreaDetailPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'tasks', element: <TasksPage /> },
      { path: 'areas', element: <AreasPage /> },
      { path: 'areas/:areaId', element: <AreaDetailPage /> },
      { path: 'assistente', element: <AssistantPage /> },
    ],
  },
]);