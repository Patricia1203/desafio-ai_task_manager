import { NavLink, Outlet } from 'react-router-dom';

export default function AppLayout() {
  return (
    <div className="app">
      <a className="skip-link" href="#conteudo">
        Pular para o conteúdo
      </a>
      <header className="app__header">
        <h1 className="app__title">AI Task Manager</h1>
        <nav className="app__nav" aria-label="Navegacao principal">
          <NavLink to="/">Dashboard</NavLink>
          <NavLink to="/tasks">Tarefas</NavLink>
          <NavLink to="/areas">Quadros</NavLink>
          <NavLink to="/assistente">Assistente</NavLink>
        </nav>
      </header>
      <main className="app__main" id="conteudo" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  );
}