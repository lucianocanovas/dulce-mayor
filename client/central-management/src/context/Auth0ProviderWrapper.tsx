import React, { createContext, useContext } from 'react';
import { Auth0Provider } from '@auth0/auth0-react';

interface Auth0ConfigContextType {
  isConfigured: boolean;
  domain: string;
  clientId: string;
}

const Auth0ConfigContext = createContext<Auth0ConfigContextType>({
  isConfigured: false,
  domain: '',
  clientId: '',
});

export const useAuth0Config = () => useContext(Auth0ConfigContext);

export const Auth0ProviderWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const domain = (import.meta.env.VITE_AUTH0_DOMAIN as string) || '';
  const clientId = (import.meta.env.VITE_AUTH0_CLIENT_ID as string) || '';

  const isConfigured = Boolean(
    domain &&
    clientId &&
    !domain.includes('your_') &&
    !domain.includes('your-tenant') &&
    domain.includes('.')
  );

  const contextValue: Auth0ConfigContextType = {
    isConfigured,
    domain,
    clientId,
  };

  if (!isConfigured) {
    return (
      <Auth0ConfigContext.Provider value={contextValue}>
        {children}
      </Auth0ConfigContext.Provider>
    );
  }

  return (
    <Auth0ConfigContext.Provider value={contextValue}>
      <Auth0Provider
        domain={domain}
        clientId={clientId}
        authorizationParams={{
          redirect_uri: window.location.origin,
        }}
      >
        {children}
      </Auth0Provider>
    </Auth0ConfigContext.Provider>
  );
};
