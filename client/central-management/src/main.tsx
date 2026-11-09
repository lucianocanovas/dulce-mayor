import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.tsx';
import { Auth0ProviderWrapper } from './context/Auth0ProviderWrapper.tsx';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Auth0ProviderWrapper>
      <App />
    </Auth0ProviderWrapper>
  </StrictMode>,
);
