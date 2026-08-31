const fs = require('fs');
const settingsPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/Settings.jsx';
let content = fs.readFileSync(settingsPath, 'utf8');

// 1. Add useTranslation import
if (!content.includes("useTranslation")) {
  content = content.replace(
    'import React, { useState, useEffect, useRef } from "react";',
    'import React, { useState, useEffect, useRef } from "react";\nimport { useTranslation } from "../context/LanguageContext";'
  );
}

// 2. Add useTranslation inside component
if (!content.includes("const { t, applyLanguage, currentLanguage } = useTranslation()")) {
  content = content.replace(
    'const Settings = () => {',
    'const Settings = () => {\n  const { t, applyLanguage, currentLanguage } = useTranslation();'
  );
}

// 3. Update handleSaveComposingSettings to call applyLanguage(language)
if (!content.includes("applyLanguage(language)")) {
  content = content.replace(
    'localStorage.setItem("bnx_setting_language", language);',
    'localStorage.setItem("bnx_setting_language", language);\n        await applyLanguage(language);'
  );
}

// 4. Update dropdown options to include clean labels and values
const oldSelectBlock = `<select
                      value={language}
                      onChange={e => setLanguage(e.target.value)}
                      className="w-full p-3 text-sm rounded-xl border outline-none cursor-pointer focus:ring-2 focus:border-transparent transition-all"
                      style={{ background: theme.mode === 'dark' ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)', borderColor: theme.border, color: theme.text }}
                    >
                      <option value="en_US">English</option>
                      <option value="hi_IN">Hindi (??????)</option>
                      <option value="ta_IN">Tamil (?????)</option>
                      <option value="te_IN">Telugu (??????)</option>
                      <option value="ml_IN">Malayalam (??????)</option>
                      <option value="kn_IN">Kannada (?????)</option>
                    </select>`;

const newSelectBlock = `<select
                      value={language}
                      onChange={e => setLanguage(e.target.value)}
                      className="w-full p-3 text-sm rounded-xl border outline-none cursor-pointer focus:ring-2 focus:border-transparent transition-all"
                      style={{ background: theme.mode === 'dark' ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)', borderColor: theme.border, color: theme.text }}
                    >
                      <option value="en">English</option>
                      <option value="ta">Tamil (தமிழ்)</option>
                      <option value="hi">Hindi (हिन्दी)</option>
                      <option value="te">Telugu (తెలుగు)</option>
                      <option value="ml">Malayalam (മലയാളം)</option>
                      <option value="kn">Kannada (ಕನ್ನಡ)</option>
                    </select>`;

if (content.includes('value={language}')) {
  // Replace the select block safely using regex or index
  const selectRegex = /<select\s+value=\{language\}[\s\S]*?<\/select>/;
  content = content.replace(selectRegex, newSelectBlock);
}

fs.writeFileSync(settingsPath, content, 'utf8');
console.log("Settings.jsx updated successfully!");
