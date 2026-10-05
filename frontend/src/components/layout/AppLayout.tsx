import { NavLink, Outlet } from 'react-router-dom';

export default function AppLayout() {
  return (
    <div className="app">
      <header className="app__header">
        <h1 className="app__title">AI Task Manager</h1>
        <nav className="app__nav" aria-label="Navegacao principal">
          <NavLink to="/">Dashboard</NavLink>
          <NavLink to="/tasks">Tarefas</NavLink>
        </nav>
      </header>
      <main className="app__main">
        <Outlet />
      </main>
    </div>
  );
}