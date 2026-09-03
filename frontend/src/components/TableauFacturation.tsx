import React, { useState, useEffect } from 'react';
import { FacturationServiceAPI } from '../services/api';
import { StatutFacturation } from '../types/facturation';

interface Props {
  idEntreprise: string;
}

const getInitialAnnee = () => {
  const saved = localStorage.getItem('facturation_annee');
  return saved ? parseInt(saved, 10) : new Date().getFullYear();
};

const getInitialMois = () => {
  const saved = localStorage.getItem('facturation_mois');
  return saved ? parseInt(saved, 10) : new Date().getMonth() + 1;
};

export const TableauFacturation: React.FC<Props> = ({ idEntreprise }) => {
  const [annee, setAnnee] = useState<number>(getInitialAnnee);
  const [mois, setMois] = useState<number>(getInitialMois);
  const [statut, setStatut] = useState<StatutFacturation | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [message, setMessage] = useState<string>('');

  const moisNoms = [
    'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
    'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'
  ];

  useEffect(() => {
    localStorage.setItem('facturation_annee', annee.toString());
    localStorage.setItem('facturation_mois', mois.toString());
  }, [annee, mois]);

  const handleInitialiserMois = async () => {
    setLoading(true);
    setMessage('');
    try {
      const facturation = await FacturationServiceAPI.initialiserMois(idEntreprise, annee, mois);
      setStatut(facturation.statut);
      setMessage(`Mois de ${moisNoms[mois - 1]} ${annee} initialisé avec succès !`);
    } catch (err: any) {
      setMessage(err.response?.data?.message || "Erreur lors de l'initialisation du mois.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif' }}>
      <h2>📋 Facturation Mensuelle</h2>

      <div style={{ display: 'flex', gap: '15px', alignItems: 'center', marginBottom: '20px' }}>
        <label>
          <strong>Mois :</strong>
          <select value={mois} onChange={(e) => setMois(Number(e.target.value))} style={{ marginLeft: '8px', padding: '5px' }}>
            {moisNoms.map((m, idx) => (
              <option key={idx + 1} value={idx + 1}>{m}</option>
            ))}
          </select>
        </label>

        <label>
          <strong>Année :</strong>
          <input 
            type="number" 
            value={annee} 
            onChange={(e) => setAnnee(Number(e.target.value))} 
            style={{ marginLeft: '8px', padding: '5px', width: '80px' }} 
          />
        </label>

        <button 
          onClick={handleInitialiserMois} 
          disabled={loading}
          style={{ padding: '6px 16px', backgroundColor: '#007bff', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
        >
          {loading ? 'Chargement...' : 'Ouvrir ce mois'}
        </button>
      </div>

      {message && (
        <div style={{ padding: '10px', marginBottom: '15px', backgroundColor: '#e9ecef', borderRadius: '4px' }}>
          {message}
        </div>
      )}

      {statut && (
        <div style={{ marginBottom: '15px' }}>
          <span>Statut du mois : </span>
          <strong style={{ color: statut === 'BROUILLON' ? '#ffc107' : '#28a745' }}>
            {statut}
          </strong>
        </div>
      )}
    </div>
  );
};