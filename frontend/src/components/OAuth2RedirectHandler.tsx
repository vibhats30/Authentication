import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

const OAuth2RedirectHandler = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { setTokens, loadUser } = useAuthStore();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const token = params.get('token');
    const refreshToken = params.get('refreshToken');
    const error = params.get('error');

    if (error) {
      console.error('OAuth2 Error:', error);
      navigate('/login', { state: { error } });
      return;
    }

    if (token && refreshToken) {
      setTokens({
        accessToken: token,
        refreshToken: refreshToken,
        tokenType: 'Bearer'
      });

      loadUser().then(() => {
        navigate('/dashboard');
      });
    } else {
      navigate('/login');
    }
  }, [location, navigate, setTokens, loadUser]);

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      height: '100vh',
      fontSize: '18px',
      color: '#667eea'
    }}>
      Processing authentication...
    </div>
  );
};

export default OAuth2RedirectHandler;
