import React, { useState, useEffect } from 'react';
import { useAuth, AuthProviderWrapper } from './context/AuthContext';
import { Auth0ProviderWrapper } from './context/Auth0ProviderWrapper';
import { Navbar } from './components/Navbar';
import { BottomNavigation } from './components/BottomNavigation';
import type { ActiveTab } from './components/BottomNavigation';
import { LoginView } from './views/LoginView';
import { CatalogOrderView } from './views/CatalogOrderView';
import { RetailersView } from './views/RetailersView';
import { PresaleOrdersView } from './views/PresaleOrdersView';
import { DeliveryDashboardView } from './views/DeliveryDashboardView';
import { ProfileView } from './views/ProfileView';
import { UserRole } from './types/enums';
import type { PointOfSale } from './types/models';

const MainApp: React.FC = () => {
  const { user } = useAuth();
  const isDelivery = user?.role === UserRole.DELIVERY_AGENT;

  const [activeTab, setActiveTab] = useState<ActiveTab>(
    isDelivery ? 'delivery-route' : 'retailers'
  );
  const [targetPOS, setTargetPOS] = useState<PointOfSale | null>(null);

  // Sync tab if user role changes (e.g. via profile switcher)
  useEffect(() => {
    if (user?.role === UserRole.DELIVERY_AGENT) {
      setActiveTab('delivery-route');
    } else if (user) {
      setActiveTab('retailers');
    }
  }, [user?.role]);

  if (!user) {
    return <LoginView />;
  }

  return (
    <div className="mobile-app-shell">
      <Navbar />

      <main className="mobile-main-content">
        {/* PREVENTISTA VIEWS */}
        {!isDelivery && (
          <>
            {activeTab === 'order' && (
              <CatalogOrderView
                initialPOS={targetPOS}
                onBackToRetailers={() => setActiveTab('retailers')}
                onOrderSuccess={() => setActiveTab('history')}
              />
            )}
            {activeTab === 'retailers' && (
              <RetailersView
                onSelectPOS={(pos) => {
                  setTargetPOS(pos);
                  setActiveTab('order');
                }}
              />
            )}
            {activeTab === 'history' && <PresaleOrdersView />}
          </>
        )}

        {/* REPARTIDOR VIEWS */}
        {isDelivery && (
          <>
            {activeTab === 'delivery-route' && (
              <DeliveryDashboardView initialSubTab="map" />
            )}
            {activeTab === 'delivery-pending' && (
              <DeliveryDashboardView initialSubTab="pending" />
            )}
            {activeTab === 'delivery-completed' && (
              <DeliveryDashboardView initialSubTab="delivered" />
            )}
          </>
        )}

        {/* SHARED ACCOUNT / PROFILE */}
        {activeTab === 'profile' && <ProfileView />}
      </main>

      {activeTab !== 'retailers' && (
        <BottomNavigation
          activeTab={activeTab}
          onTabChange={setActiveTab}
          role={user.role}
          cartCount={0}
        />
      )}
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
