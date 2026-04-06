import React, { useState } from 'react';
import Dashboard from './components/Dashboard';
import TransactionForm from './components/TransactionForm';
import TransactionList from './components/TransactionList';
import QueryInterface from './components/QueryInterface';

const styles = {
  app:      { maxWidth: '1200px', margin: '0 auto', padding: '24px' },
  header:   { background: '#1e3c6e', color: 'white', padding: '20px 24px', borderRadius: '8px', marginBottom: '24px' },
  title:    { fontSize: '24px', fontWeight: 'bold' },
  subtitle: { fontSize: '14px', opacity: 0.8, marginTop: '4px' },
  nav:      { display: 'flex', gap: '12px', marginBottom: '24px' },
  navBtn:   { padding: '10px 20px', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '14px', fontWeight: '500' },
};

export default function App() {
  const [activeTab, setActiveTab]   = useState('dashboard');
  const [refreshKey, setRefreshKey] = useState(0);

  const tabs = [
    { id: 'dashboard',     label: 'Dashboard' },
    { id: 'new',           label: 'New Transaction' },
    { id: 'transactions',  label: 'All Transactions' },
    { id: 'ai-query',      label: 'AI Query' },
  ];

  return (
    <div style={styles.app}>
      <div style={styles.header}>
        <div style={styles.title}>Payment Risk Dashboard</div>
        <div style={styles.subtitle}>Real-time transaction monitoring · ML risk scoring · Claude-powered AI insights</div>
      </div>

      <div style={styles.nav}>
        {tabs.map(tab => (
          <button
            key={tab.id}
            style={{
              ...styles.navBtn,
              background: activeTab === tab.id ? '#1e3c6e' : '#e2e8f0',
              color:      activeTab === tab.id ? 'white'   : '#4a5568',
            }}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'dashboard'    && <Dashboard />}
      {activeTab === 'new'          && (
        <TransactionForm onSuccess={() => { setRefreshKey(k => k + 1); setActiveTab('transactions'); }} />
      )}
      {activeTab === 'transactions' && <TransactionList key={refreshKey} />}
      {activeTab === 'ai-query'     && <QueryInterface />}
    </div>
  );
}
