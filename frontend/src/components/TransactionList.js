import React, { useEffect, useState } from 'react';
import axios from 'axios';

const TRANSACTION_URL = process.env.REACT_APP_TRANSACTION_URL || 'http://localhost:8082';

const styles = {
  card: { background: 'white', borderRadius: '8px', padding: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' },
  badge: { display: 'inline-block', padding: '2px 8px', borderRadius: '12px', fontSize: '12px', fontWeight: '600' },
};

const riskColor  = { HIGH: '#e53e3e', MEDIUM: '#dd6b20', LOW: '#38a169', UNKNOWN: '#718096' };
const statusColor = { COMPLETED: '#38a169', FLAGGED: '#e53e3e', PENDING: '#dd6b20', REJECTED: '#718096' };

export default function TransactionList() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    axios.get(`${TRANSACTION_URL}/api/transactions`)
      .then(res => setTransactions(res.data))
      .catch(err => console.error(err))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div style={{ padding: '40px', textAlign: 'center', color: '#718096' }}>Loading transactions...</div>;

  return (
    <div style={styles.card}>
      <div style={{ fontSize: '16px', fontWeight: '600', marginBottom: '16px', color: '#1e3c6e' }}>
        All Transactions ({transactions.length})
      </div>
      {transactions.length === 0 ? (
        <div style={{ color: '#718096', textAlign: 'center', padding: '40px' }}>No transactions yet.</div>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #e2e8f0' }}>
              {['ID', 'Sender', 'Receiver', 'Amount', 'Type', 'Status', 'Risk', 'Score'].map(h => (
                <th key={h} style={{ padding: '8px', textAlign: 'left', color: '#718096', fontWeight: '600' }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {transactions.map(tx => (
              <tr key={tx.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                <td style={{ padding: '10px 8px', fontFamily: 'monospace', fontSize: '11px' }}>{tx.id?.slice(-8)}</td>
                <td style={{ padding: '10px 8px' }}>{tx.senderId}</td>
                <td style={{ padding: '10px 8px' }}>{tx.receiverId}</td>
                <td style={{ padding: '10px 8px', fontWeight: '600' }}>{tx.amount?.toFixed(2)} {tx.currency}</td>
                <td style={{ padding: '10px 8px' }}>{tx.type}</td>
                <td style={{ padding: '10px 8px' }}>
                  <span style={{ ...styles.badge, background: (statusColor[tx.status] || '#718096') + '20', color: statusColor[tx.status] || '#718096' }}>
                    {tx.status}
                  </span>
                </td>
                <td style={{ padding: '10px 8px' }}>
                  <span style={{ ...styles.badge, background: (riskColor[tx.riskLevel] || '#718096') + '20', color: riskColor[tx.riskLevel] || '#718096' }}>
                    {tx.riskLevel}
                  </span>
                </td>
                <td style={{ padding: '10px 8px' }}>{tx.riskScore?.toFixed(3)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
