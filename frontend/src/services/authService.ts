import axios from 'axios';

const API_BASE_URL = '/api/auth';

export interface SignUpData {
  name: string;
  email: string;
  password: string;
}

export interface LoginData {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
}

export interface User {
  id: number;
  name: string;
  email: string;
  imageUrl?: string;
  provider: string;
  roles: string[];
}

class AuthService {
  async signup(data: SignUpData): Promise<AuthResponse> {
    const response = await axios.post(`${API_BASE_URL}/signup`, data);
    return response.data;
  }

  async login(data: LoginData): Promise<AuthResponse> {
    const response = await axios.post(`${API_BASE_URL}/login`, data);
    return response.data;
  }

  async refreshToken(refreshToken: string): Promise<AuthResponse> {
    const response = await axios.post(`${API_BASE_URL}/refresh`, { refreshToken });
    return response.data;
  }

  async logout(refreshToken: string): Promise<void> {
    await axios.post(`${API_BASE_URL}/logout`, { refreshToken });
  }

  async getCurrentUser(): Promise<User> {
    const response = await axios.get('/api/user/me');
    return response.data;
  }

  getOAuth2LoginUrl(provider: 'google' | 'facebook' | 'github' | 'twitter'): string {
    // Use absolute URL to backend for OAuth2 authorization
    const backendUrl = 'http://localhost:8080';
    return `${backendUrl}/oauth2/authorize/${provider}?redirect_uri=${encodeURIComponent(window.location.origin + '/oauth2/redirect')}`;
  }
}

export default new AuthService();
