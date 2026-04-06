import React, { useEffect, useState } from 'react';
import axios from 'axios';

const TRANSACTION_URL  = process.env.REACT_APP_TRANSACTION_URL  || 'http://localhost:8082';
const RISK_URL         = process.env.REACT_APP_RISK_URL         || 'http://localhost:8083';
const INTELLIGENCE_URL = process.env.REACT_APP_INTELLIGENCE_URL || 'http://localhost:8084';

const styles = {
  grid:    { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', marginBottom: '24px' },
  card:    { background: 'white', borderRadius: '8px', padding: '20px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' },
  label:   { fontSize: '13px', color: '#718096', marginBottom: '8px' },
  value:   { fontSize: '32px', fontWeight: 'bold', color: '#1e3c6e' },
  badge:   { display: 'inline-block', padding: '2px 8px', borderRadius: '12px', fontSize: '12px', fontWeight: '600' },
  explainBtn: { padding: '4px 10px', background: '#1e3c6e', color: 'white', border: 'none', borderRadius: '4px', fontSize: '12px', cursor: 'pointer' },
};

const riskColor = { HIGH: '#e53e3e', MEDIUM: '#dd6b20', LOW: '#38a169', UNKNOWN: '#718096' };

export default function Dashboard() {
  const [stats, setStats]           = useState(null);
  const [riskStats, setRiskStats]   = useState(null);
  const [flagged, setFlagged]       = useState([]);
  const [loading, setLoading]       = useState(true);
  const [explanations, setExplanations] = useState({});
  const [explaining, setExplaining] = useState({});

  useEffect(() => {
    Promise.all([
      axios.get(`${TRANSACTION_URL}/api/transactions/stats`),
      axios.get(`${RISK_URL}/api/risk/stats`),
      axios.get(`${TRANSACTION_URL}/api/transactions/flagged`),
    ])
      .then(([statsRes, riskRes, flaggedRes]) => {
        setStats(statsRes.data);
        setRiskStats(riskRes.data);
        setFlagged(flaggedRes.data);
      })
      .catch(err => console.error('Dashboard load error:', err))
      .finally(() => setLoading(false));
  }, []);

  const handleExplain = async (txId) => {
    if (explanations[txId]) return;
    setExplaining(prev => ({ ...prev, [txId]: true }));
    try {
      const res = await axios.get(`${INTELLIGENCE_URL}/api/intelligence/explain/${txId}`);
      setExplanations(prev => ({ ...prev, [txId]: res.data.explanation }));
    } catch {
      setExplanations(prev => ({ ...prev, [txId]: 'AI explanation unavailable.' }));
    } finally {
      setExplaining(prev => ({ ...prev, [txId]: false }));
    }
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center', color: '#718096' }}>Loading dashboard...</div>;

  return (
    <div>
      <div style={styles.grid}>
        <div style={styles.card}>
          <div style={styles.label}>Total Transactions</div>
          <div style={styles.value}>{stats?.total ?? '—'}</div>
        </div>
        <div style={styles.card}>
          <div style={styles.label}>Completed</div>
          <div style={{ ...styles.value, color: '#38a169' }}>{stats?.completed ?? '—'}</div>
        </div>
        <div style={styles.card}>
          <div style={styles.label}>Flagged (High Risk)</div>
          <div style={{ ...styles.value, color: '#e53e3e' }}>{stats?.flagged ?? '—'}</div>
        </div>
        <div style={styles.card}>
          <div style={styles.label}>Risk Assessments Run</div>
          <div style={styles.value}>{riskStats?.total_assessments ?? '—'}</div>
        </div>
      </div>

      {flagged.length > 0 && (
        <div style={styles.card}>
          <div style={{ fontSize: '16px', fontWeight: '600', marginBottom: '4px', color: '#e53e3e' }}>
            Flagged Transactions
          </div>
          <div style={{ fontSize: '13px', color: '#718096', marginBottom: '16px' }}>
            Click <strong>Explain</strong> to get an AI-generated explanation of why the transaction was flagged.
          </div>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #e2e8f0' }}>
                {['ID', 'Sender', 'Amount (SGD)', 'Type', 'Risk Level', 'Score', 'AI Explain'].map(h => (
                  <th key={h} style={{ padding: '8px', textAlign: 'left', color: '#718096' }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {flagged.map(tx => (
                <React.Fragment key={tx.id}>
                  <tr style={{ borderBottom: explanations[tx.id] ? 'none' : '1px solid #f0f0f0' }}>
                    <td style={{ padding: '10px 8px', fontFamily: 'monospace', fontSize: '12px' }}>{tx.id?.slice(-8)}</td>
                    <td style={{ padding: '10px 8px' }}>{tx.senderId}</td>
                    <td style={{ padding: '10px 8px', fontWeight: '600' }}>{tx.amount?.toFixed(2)}</td>
                    <td style={{ padding: '10px 8px' }}>{tx.type}</td>
                    <td style={{ padding: '10px 8px' }}>
                      <span style={{ ...styles.badge, background: riskColor[tx.riskLevel] + '20', color: riskColor[tx.riskLevel] }}>
                        {tx.riskLevel}
                      </span>
                    </td>
                    <td style={{ padding: '10px 8px' }}>{tx.riskScore?.toFixed(3)}</td>
                    <td style={{ padding: '10px 8px' }}>
                      <button style={styles.explainBtn} onClick={() => handleExplain(tx.id)} disabled={explaining[tx.id]}>
                        {explaining[tx.id] ? '...' : explanations[tx.id] ? 'Explained' : 'Explain'}
                      </button>
                    </td>
                  </tr>
                  {explanations[tx.id] && (
                    <tr style={{ borderBottom: '1px solid #f0f0f0' }}>
                      <td colSpan={7} style={{ padding: '0 8px 12px 8px' }}>
                        <div style={{ background: '#f0f4ff', borderLeft: '3px solid #1e3c6e', padding: '10px 14px', borderRadius: '4px', fontSize: '13px', color: '#2d3748', lineHeight: '1.6' }}>
                          <strong style={{ color: '#1e3c6e' }}>AI Explanation: </strong>{explanations[tx.id]}
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
