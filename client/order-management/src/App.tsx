import React, { useState } from 'react';
import { useAuth, AuthProviderWrapper } from './context/AuthContext';
import { Auth0ProviderWrapper } from './context/Auth0ProviderWrapper';
import { Navbar } from './components/Navbar';
import { BottomNavigation } from './components/BottomNavigation';
import type { ActiveTab } from './components/BottomNavigation';
import { LoginView } from './views/LoginView';
import { RoutesView } from './views/RoutesView';
import { CatalogOrderView } from './views/CatalogOrderView';
import { RetailersView } from './views/RetailersView';
import { ProfileView } from './views/ProfileView';
import type { RouteStop, PointOfSale } from './types/models';

const MainApp: React.FC = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<ActiveTab>('routes');
  const [targetPOS, setTargetPOS] = useState<PointOfSale | null>(null);

  if (!user) {
    return <LoginView />;
  }

  const handleStartOrderForStop = (stop: RouteStop) => {
    setTargetPOS(stop.pointOfSale);
    setActiveTab('catalog');
  };

  return (
    <div className="mobile-app-shell">
      <Navbar />

      <main className="mobile-main-content">
        {activeTab === 'routes' && (
          <RoutesView onStartOrderForStop={handleStartOrderForStop} />
        )}
        {activeTab === 'catalog' && (
          <CatalogOrderView
            initialPOS={targetPOS}
            onOrderSuccess={() => setActiveTab('routes')}
          />
        )}
        {activeTab === 'retailers' && <RetailersView />}
        {activeTab === 'profile' && <ProfileView />}
      </main>

      <BottomNavigation
        activeTab={activeTab}
        onTabChange={setActiveTab}
        cartCount={0}
      />
    </div>
  );
};

export default function App() {
  return (
    <Auth0ProviderWrapper>
      <AuthProviderWrapper>
        <MainApp />
      </AuthProviderWrapper>
    </Auth0ProviderWrapper>
  );
}
