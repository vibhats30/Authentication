import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Login from '../components/Login';
import Signup from '../components/Signup';

const AuthPage = () => {
  const [isLogin, setIsLogin] = useState(true);
  const navigate = useNavigate();

  const handleSuccess = () => {
    navigate('/dashboard');
  };

  return (
    <>
      {isLogin ? (
        <Login
          onSuccess={handleSuccess}
          onSwitchToSignup={() => setIsLogin(false)}
        />
      ) : (
        <Signup
          onSuccess={handleSuccess}
          onSwitchToLogin={() => setIsLogin(true)}
        />
      )}
    </>
  );
};

export default AuthPage;
