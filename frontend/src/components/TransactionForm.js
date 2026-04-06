import React, { useState } from 'react';
import axios from 'axios';

const TRANSACTION_URL = process.env.REACT_APP_TRANSACTION_URL || 'http://localhost:8082';

const styles = {
  card: { background: 'white', borderRadius: '8px', padding: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)', maxWidth: '500px' },
  label: { display: 'block', fontSize: '13px', fontWeight: '600', color: '#4a5568', marginBottom: '6px' },
  input: { width: '100%', padding: '10px 12px', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '14px', marginBottom: '16px' },
  btn: { width: '100%', padding: '12px', background: '#1e3c6e', color: 'white', border: 'none', borderRadius: '6px', fontSize: '15px', fontWeight: '600', cursor: 'pointer' },
  result: { marginTop: '20px', padding: '16px', borderRadius: '8px', fontSize: '14px' },
};

const riskColor = { HIGH: '#fed7d7', MEDIUM: '#feebc8', LOW: '#c6f6d5', UNKNOWN: '#e2e8f0' };

export default function TransactionForm({ onSuccess }) {
  const [form, setForm] = useState({ senderId: '', receiverId: '', amount: '', type: 'TRANSFER', currency: 'SGD', description: '' });
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = e => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async e => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const res = await axios.post(`${TRANSACTION_URL}/api/transactions`, {
        ...form,
        amount: parseFloat(form.amount),
      });
      setResult(res.data);
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create transaction');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.card}>
      <div style={{ fontSize: '16px', fontWeight: '600', marginBottom: '20px', color: '#1e3c6e' }}>New Transaction</div>
      <form onSubmit={handleSubmit}>
        {[
          { name: 'senderId', label: 'Sender ID', placeholder: 'user_001' },
          { name: 'receiverId', label: 'Receiver ID', placeholder: 'user_002' },
          { name: 'amount', label: 'Amount (SGD)', placeholder: '500.00', type: 'number' },
          { name: 'description', label: 'Description', placeholder: 'Payment for services' },
        ].map(field => (
          <div key={field.name}>
            <label style={styles.label}>{field.label}</label>
            <input
              style={styles.input}
              name={field.name}
              type={field.type || 'text'}
              placeholder={field.placeholder}
              value={form[field.name]}
              onChange={handleChange}
              required={field.name !== 'description'}
            />
          </div>
        ))}

        <label style={styles.label}>Transaction Type</label>
        <select style={styles.input} name="type" value={form.type} onChange={handleChange}>
          <option value="TRANSFER">Transfer</option>
          <option value="PAYMENT">Payment</option>
          <option value="REFUND">Refund</option>
        </select>

        <button style={styles.btn} type="submit" disabled={loading}>
          {loading ? 'Processing...' : 'Submit Transaction'}
        </button>
      </form>

      {error && <div style={{ ...styles.result, background: '#fed7d7', color: '#c53030' }}>{error}</div>}

      {result && (
        <div style={{ ...styles.result, background: riskColor[result.riskLevel] || '#e2e8f0' }}>
          <div style={{ fontWeight: '600', marginBottom: '8px' }}>Transaction Created</div>
          <div>Status: <strong>{result.status}</strong></div>
          <div>Risk Level: <strong>{result.riskLevel}</strong></div>
          <div>Risk Score: <strong>{result.riskScore?.toFixed(3)}</strong></div>
          <div style={{ marginTop: '6px', fontSize: '12px', color: '#718096' }}>ID: {result.id}</div>
        </div>
      )}
    </div>
  );
}
