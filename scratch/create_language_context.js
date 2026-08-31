const fs = require('fs');
const path = require('path');

const filePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/context/LanguageContext.jsx';

const content = `import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';
import en from '../locales/en.json';
import ta from '../locales/ta.json';
import hi from '../locales/hi.json';
import te from '../locales/te.json';
import ml from '../locales/ml.json';
import kn from '../locales/kn.json';

const translations = { en, ta, hi, te, ml, kn };

const normalizeLang = (code) => {
  if (!code) return 'en';
  const c = code.toLowerCase();
  if (c.startsWith('ta')) return 'ta';
  if (c.startsWith('hi')) return 'hi';
  if (c.startsWith('te')) return 'te';
  if (c.startsWith('ml')) return 'ml';
  if (c.startsWith('kn')) return 'kn';
  return 'en';
};

const LanguageContext = createContext();

export const LanguageProvider = ({ children }) => {
  const [currentLanguage, setCurrentLanguage] = useState(() => {
    return normalizeLang(localStorage.getItem('bnx_setting_language') || 'en');
  });

  const fetchUserLanguage = async () => {
    const token = localStorage.getItem('accessToken');
    if (!token) return;

    try {
      const baseUrl = import.meta.env.VITE_API_URL || '';
      const response = await axios.get(\`\${baseUrl}/api/settings/language\`, {
        headers: { Authorization: \`Bearer \${token}\` }
      });
      if (response.data && response.data.language) {
        const lang = normalizeLang(response.data.language);
        setCurrentLanguage(lang);
        localStorage.setItem('bnx_setting_language', lang);
      }
    } catch (err) {
      console.warn("Could not fetch user language preference from backend:", err);
    }
  };

  useEffect(() => {
    fetchUserLanguage();
  }, []);

  const applyLanguage = async (newLang) => {
    const lang = normalizeLang(newLang);
    const token = localStorage.getItem('accessToken');
    
    setCurrentLanguage(lang);
    localStorage.setItem('bnx_setting_language', lang);

    if (token) {
      try {
        const baseUrl = import.meta.env.VITE_API_URL || '';
        await axios.put(\`\${baseUrl}/api/settings/language\`, 
          { language: lang },
          { headers: { Authorization: \`Bearer \${token}\` } }
        );
      } catch (err) {
        console.error("Failed to save language preference to backend:", err);
      }
    }
  };

  const t = (keyPath, fallback = '') => {
    if (!keyPath) return fallback;
    const keys = keyPath.split('.');
    
    let activeDict = translations[currentLanguage] || translations['en'];
    let val = activeDict;
    for (const k of keys) {
      val = val ? val[k] : undefined;
    }
    if (val !== undefined && typeof val === 'string') {
      return val;
    }

    // Fallback to English
    let enDict = translations['en'];
    let enVal = enDict;
    for (const k of keys) {
      enVal = enVal ? enVal[k] : undefined;
    }
    if (enVal !== undefined && typeof enVal === 'string') {
      return enVal;
    }

    return fallback || keyPath;
  };

  return (
    <LanguageContext.Provider value={{ currentLanguage, applyLanguage, fetchUserLanguage, t }}>
      {children}
    </LanguageContext.Provider>
  );
};

export const useLanguage = () => {
  const ctx = useContext(LanguageContext);
  if (!ctx) {
    throw new Error('useLanguage must be used within a LanguageProvider');
  }
  return ctx;
};

export const useTranslation = () => {
  const { t, currentLanguage, applyLanguage } = useLanguage();
  return { t, currentLanguage, applyLanguage };
};
`;

fs.writeFileSync(filePath, content, 'utf8');
console.log("Created LanguageContext.jsx successfully!");
