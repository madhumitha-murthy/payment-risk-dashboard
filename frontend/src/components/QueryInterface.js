import React, { useState } from 'react';
import axios from 'axios';

const INTELLIGENCE_URL = process.env.REACT_APP_INTELLIGENCE_URL || 'http://localhost:8084';

const styles = {
  card:    { background: 'white', borderRadius: '8px', padding: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' },
  label:   { fontSize: '13px', color: '#718096', marginBottom: '8px' },
  input:   { width: '100%', padding: '12px', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' },
  btn:     { marginTop: '12px', padding: '10px 20px', background: '#1e3c6e', color: 'white', border: 'none', borderRadius: '6px', fontSize: '14px', fontWeight: '600', cursor: 'pointer' },
  badge:   { display: 'inline-block', padding: '2px 8px', borderRadius: '12px', fontSize: '12px', fontWeight: '600' },
  filters: { marginTop: '12px', padding: '10px 14px', background: '#f0f4ff', borderRadius: '6px', fontSize: '13px', color: '#4a5568', fontFamily: 'monospace' },
};

const riskColor   = { HIGH: '#e53e3e', MEDIUM: '#dd6b20', LOW: '#38a169', UNKNOWN: '#718096' };
const statusColor = { COMPLETED: '#38a169', FLAGGED: '#e53e3e', PENDING: '#dd6b20' };

const EXAMPLES = [
  'Show all flagged transactions',
  'Show high risk refunds',
  'Show transfers over SGD 5000',
  'Show completed low risk payments',
];

export default function QueryInterface() {
  const [query, setQuery]     = useState('');
  const [result, setResult]   = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState(null);

  const handleSubmit = async (q) => {
    const text = q || query;
    if (!text.trim()) return;
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await axios.post(`${INTELLIGENCE_URL}/api/intelligence/query`, { query: text });
      setResult(res.data);
      setQuery(text);
    } catch (err) {
      setError(err.response?.data?.error || 'Query failed. Is the intelligence service running?');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.card}>
      <div style={{ fontSize: '16px', fontWeight: '600', color: '#1e3c6e', marginBottom: '4px' }}>
        AI Transaction Query
      </div>
      <div style={{ fontSize: '13px', color: '#718096', marginBottom: '16px' }}>
        Ask in plain English — Claude parses your query into filters and searches transactions.
      </div>

      <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '16px' }}>
        {EXAMPLES.map(ex => (
          <button key={ex}
            style={{ padding: '4px 10px', background: '#edf2f7', border: '1px solid #e2e8f0', borderRadius: '12px', fontSize: '12px', cursor: 'pointer', color: '#4a5568' }}
            onClick={() => { setQuery(ex); handleSubmit(ex); }}>
            {ex}
          </button>
        ))}
      </div>

      <div style={styles.label}>Your query</div>
      <input
        style={styles.input}
        placeholder='e.g. "Show all high risk flagged transactions"'
        value={query}
        onChange={e => setQuery(e.target.value)}
        onKeyDown={e => e.key === 'Enter' && handleSubmit()}
      />
      <button style={styles.btn} onClick={() => handleSubmit()} disabled={loading}>
        {loading ? 'Querying...' : 'Search'}
      </button>

      {error && (
        <div style={{ marginTop: '16px', padding: '12px', background: '#fed7d7', borderRadius: '6px', color: '#c53030', fontSize: '14px' }}>
          {error}
        </div>
      )}

      {result && (
        <div style={{ marginTop: '20px' }}>
          <div style={styles.filters}>
            Parsed filters: <strong>{result.parsedFilters}</strong>
          </div>
          <div style={{ margin: '12px 0 8px', fontSize: '14px', color: '#4a5568' }}>
            {result.totalFound} transaction{result.totalFound !== 1 ? 's' : ''} found
          </div>

          {result.results.length === 0 ? (
            <div style={{ color: '#718096', padding: '20px', textAlign: 'center' }}>No transactions match your query.</div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #e2e8f0' }}>
                  {['ID', 'Sender', 'Amount', 'Type', 'Status', 'Risk'].map(h => (
                    <th key={h} style={{ padding: '8px', textAlign: 'left', color: '#718096' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {result.results.map(tx => (
                  <tr key={tx.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                    <td style={{ padding: '10px 8px', fontFamily: 'monospace', fontSize: '11px' }}>{tx.id?.slice(-8)}</td>
                    <td style={{ padding: '10px 8px' }}>{tx.senderId}</td>
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
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
