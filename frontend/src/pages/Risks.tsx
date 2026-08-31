import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { RefreshCcw, TrendingUp, CheckCircle, ShieldAlert } from 'lucide-react';
import { riskApi, ProjectRisk, predictiveRiskApi, PredictiveRiskReport } from '../services/api';
import LogoLoader from '../components/LogoLoader';

export default function Risks() {
  const { id } = useParams();
  const projectId = Number(id);

  const [risks, setRisks] = useState<ProjectRisk[]>([]);
  const [predictions, setPredictions] = useState<PredictiveRiskReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [error, setError] = useState('');

  const loadRisks = async () => {
    setLoading(true);
    try {
      const [riskData, predData] = await Promise.all([
        riskApi.list(projectId),
        predictiveRiskApi.getPredictions(projectId).catch(() => null),
      ]);
      setRisks(riskData);
      setPredictions(predData);
    } catch (err) {
      setError('Failed to load risks.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRisks();
  }, [projectId]);

  const handleAnalyze = async () => {
    setAnalyzing(true);
    try {
      await riskApi.analyze(projectId);
      await loadRisks();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to analyze risks');
    } finally {
      setAnalyzing(false);
    }
  };

  const getSeverityBadge = (s: string) => {
    switch (s) {
      case 'CRITICAL': return <span className="badge badge-critical" style={{ padding: '4px 10px' }}>Critical</span>;
      case 'HIGH': return <span className="badge badge-secondary" style={{ padding: '4px 10px' }}>High</span>;
      case 'MEDIUM': return <span className="badge badge-primary" style={{ padding: '4px 10px' }}>Medium</span>;
      default: return <span className="badge" style={{ padding: '4px 10px', background: 'rgba(255,255,255,0.1)' }}>Low</span>;
    }
  };

  const renderEvidence = (jsonStr?: string) => {
    if (!jsonStr) return null;
    try {
      const evidence = JSON.parse(jsonStr);
      return (
        <div style={{ background: 'rgba(0,0,0,0.2)', padding: '12px', borderRadius: '8px', fontSize: '0.8rem', marginTop: 12, border: '1px solid var(--border-light)' }}>
          <strong style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>Evidence Data:</strong>
          <ul style={{ margin: 0, paddingLeft: 16, color: 'var(--text-muted)' }}>
            {Object.entries(evidence).map(([key, val]) => (
              <li key={key}>
                <span style={{ color: 'var(--primary-light)' }}>{key}</span>: {String(val)}
              </li>
            ))}
          </ul>
        </div>
      );
    } catch (e) {
      return null;
    }
  };

  if (loading) return <LogoLoader message="Loading risk prediction engine..." />;

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <Link to={`/projects/${projectId}`} style={{ color: 'var(--primary-light)', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: '0.9rem', fontWeight: 500 }}>
        ← Back to Project
      </Link>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <div style={{ display: 'inline-block', padding: '4px 12px', background: 'rgba(250, 177, 160, 0.1)', border: '1px solid rgba(250, 177, 160, 0.2)', borderRadius: 'var(--radius-full)', color: '#fab1a0', fontSize: '0.78rem', fontWeight: 600, letterSpacing: '0.5px', marginBottom: '0.5rem' }}>
            PROACTIVE ML & MONTE CARLO RISK PREDICTION
          </div>
          <h2 style={{ fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.5px', color: 'var(--text-primary)', margin: 0 }}>
            Risk Assessment & Delay Probability
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.92rem', marginTop: '0.2rem' }}>
            Predictive machine learning and 1,000-run Monte Carlo simulation for project milestones and budget.
          </p>
        </div>
        <button onClick={handleAnalyze} disabled={analyzing} className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <RefreshCcw size={16} className={analyzing ? 'spin' : ''} />
          {analyzing ? 'Analyzing...' : 'Run Risk Analysis'}
        </button>
      </div>

      {/* ── Probabilistic Risk & Monte Carlo Prediction Hero ── */}
      {predictions && (
        <div style={{
          padding: '22px 24px', borderRadius: 'var(--radius-md)',
          background: 'linear-gradient(135deg, rgba(235, 87, 87, 0.08), rgba(242, 153, 74, 0.05))',
          border: '1px solid rgba(235, 87, 87, 0.3)', boxShadow: '0 8px 30px rgba(0,0,0,0.06)',
          display: 'flex', flexDirection: 'column', gap: 16,
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 14 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{
                width: 44, height: 44, borderRadius: 10, background: 'linear-gradient(135deg, #eb5757, #f2994a)',
                display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff',
                boxShadow: '0 4px 12px rgba(235, 87, 87, 0.35)',
              }}>
                <TrendingUp size={24} />
              </div>
              <div>
                <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#f2994a', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                  Probabilistic Delay Likelihood
                </div>
                <h3 style={{ margin: 0, fontSize: '1.45rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                  This project has a <span style={{ color: predictions.delayProbabilityPercentage > 40 ? '#eb5757' : '#27ae60' }}>{predictions.delayProbabilityPercentage}% chance of delay</span>
                </h3>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: 2 }}>
                  {predictions.similarityAssessment} · {predictions.predictedDelayDays > 0 ? `Estimated slippage: +${predictions.predictedDelayDays} days` : 'On track with positive buffer'}
                </div>
              </div>
            </div>
          </div>

          {/* Monte Carlo Percentile Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 12 }}>
            <div style={{ padding: 12, borderRadius: 'var(--radius-sm)', background: 'var(--bg-card)', border: '1px solid var(--border-light)' }}>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontWeight: 700, textTransform: 'uppercase' }}>
                🟢 Optimistic (P10)
              </div>
              <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)', marginTop: 4 }}>
                {predictions.p10FinishDate}
              </div>
              <div style={{ fontSize: '0.74rem', color: 'var(--text-secondary)' }}>Best case accelerated timeline</div>
            </div>

            <div style={{ padding: 12, borderRadius: 'var(--radius-sm)', background: 'var(--bg-card)', border: '1px solid var(--border-light)' }}>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontWeight: 700, textTransform: 'uppercase' }}>
                🟡 Expected (P50)
              </div>
              <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)', marginTop: 4 }}>
                {predictions.p50FinishDate}
              </div>
              <div style={{ fontSize: '0.74rem', color: 'var(--text-secondary)' }}>Most likely completion date</div>
            </div>

            <div style={{ padding: 12, borderRadius: 'var(--radius-sm)', background: 'var(--bg-card)', border: '1px solid var(--border-light)' }}>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontWeight: 700, textTransform: 'uppercase' }}>
                🔴 Pessimistic Buffer (P90)
              </div>
              <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)', marginTop: 4 }}>
                {predictions.p90FinishDate}
              </div>
              <div style={{ fontSize: '0.74rem', color: 'var(--text-secondary)' }}>90% confidence contingency date</div>
            </div>
          </div>

          {/* Top Risk Drivers */}
          {predictions.topRiskDrivers && predictions.topRiskDrivers.length > 0 && (
            <div style={{ fontSize: '0.82rem', color: 'var(--text-primary)', background: 'var(--bg-card)', padding: '10px 14px', borderRadius: 6, border: '1px solid var(--border-light)' }}>
              <strong style={{ color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>Key Predictive Risk Factors:</strong>
              <ul style={{ margin: 0, paddingLeft: 18, color: 'var(--text-muted)' }}>
                {predictions.topRiskDrivers.map((d, idx) => (
                  <li key={idx} style={{ color: 'var(--text-secondary)' }}>{d}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {error && <div className="glass-panel" style={{ padding: 16, color: '#ff7675', borderColor: '#ff7675' }}>{error}</div>}


      {risks.length === 0 ? (
        <div className="glass-panel" style={{ padding: '3rem 2rem', textAlign: 'center' }}>
          <div style={{ background: 'rgba(85, 239, 196, 0.1)', color: '#55efc4', width: '56px', height: '56px', borderRadius: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1.25rem', boxShadow: '0 4px 20px rgba(85, 239, 196, 0.1)' }}>
            <CheckCircle size={28} />
          </div>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>Project is Healthy</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.92rem', maxWidth: '460px', margin: '0 auto' }}>
            No active risks were detected during the last analysis. Run the risk analysis engine to check for new risks.
          </p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: 20 }}>
          {risks.map(risk => (
            <div key={risk.id} className="glass-panel" style={{ padding: 24, borderTop: `4px solid ${risk.severity === 'CRITICAL' ? 'var(--accent-crimson)' : risk.severity === 'HIGH' ? 'var(--secondary)' : 'var(--primary-light)'}` }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
                  <ShieldAlert size={18} color="var(--text-secondary)" />
                  {risk.title}
                </h3>
                {getSeverityBadge(risk.severity)}
              </div>
              <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: 12, display: 'flex', gap: 12 }}>
                <span>Type: <strong>{risk.riskType}</strong></span>
                <span>Detected: <strong>{new Date(risk.detectedAt).toLocaleDateString()}</strong></span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.5, marginBottom: 16 }}>
                {risk.description}
              </p>
              
              {renderEvidence(risk.evidenceJson)}

              {risk.suggestedAction && (
                <div style={{ marginTop: 16, background: 'rgba(85, 239, 196, 0.05)', padding: '12px', borderRadius: '8px', border: '1px solid rgba(85, 239, 196, 0.2)' }}>
                  <strong style={{ color: '#55efc4', fontSize: '0.85rem', display: 'block', marginBottom: 4 }}>Recommended Action:</strong>
                  <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)' }}>{risk.suggestedAction}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
