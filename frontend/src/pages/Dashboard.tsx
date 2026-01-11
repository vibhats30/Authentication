import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import './Dashboard.css';

const Dashboard = () => {
  const navigate = useNavigate();
  const { user, isAuthenticated, logout, loadUser } = useAuthStore();

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    if (!user) {
      loadUser();
    }
  }, [isAuthenticated, user, navigate, loadUser]);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  if (!user) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <div className="dashboard">
      <div className="dashboard-card">
        <h1>Welcome to Your Dashboard</h1>

        <div className="user-info">
          {user.imageUrl && (
            <img src={user.imageUrl} alt={user.name} className="user-avatar" />
          )}

          <div className="user-details">
            <h2>{user.name}</h2>
            <p className="user-email">{user.email}</p>
            <p className="user-provider">
              <strong>Login Provider:</strong> {user.provider}
            </p>
            <p className="user-roles">
              <strong>Roles:</strong> {user.roles.join(', ')}
            </p>
          </div>
        </div>

        <button onClick={handleLogout} className="btn btn-logout">
          Logout
        </button>
      </div>
    </div>
  );
};

export default Dashboard;
