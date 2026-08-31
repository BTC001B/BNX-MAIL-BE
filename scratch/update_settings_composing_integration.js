const fs = require('fs');

const settingsPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/Settings.jsx';
let content = fs.readFileSync(settingsPath, 'utf8');

// Ensure settingsAPI is imported
if (!content.includes('settingsAPI')) {
  content = content.replace(
    'import { userAPI, signatureAPI, casboxAPI } from "../services/api";',
    'import { userAPI, signatureAPI, casboxAPI, settingsAPI } from "../services/api";'
  );
}

// 1. Add fetchComposing logic inside fetchBackendSettings
const oldFetchComposingMarker = 'const res = await userAPI.getSettings();';
const newFetchComposingCode = `const res = await userAPI.getSettings();
      try {
        const compRes = await settingsAPI.getComposing();
        if (compRes.data) {
          const cd = compRes.data;
          if (cd.spellingCheckEnabled !== undefined) setSpellingCheck(cd.spellingCheckEnabled);
          if (cd.grammarCheckEnabled !== undefined) setGrammarCheck(cd.grammarCheckEnabled);
          if (cd.autoCorrectEnabled !== undefined) setAutoCorrect(cd.autoCorrectEnabled);
          if (cd.smartComposeEnabled !== undefined) setWritingSuggestions(cd.smartComposeEnabled);
        }
      } catch (err) {
        console.warn("Error fetching composing preferences:", err);
      }`;

if (!content.includes('compRes.data')) {
  content = content.replace(oldFetchComposingMarker, newFetchComposingCode);
}

// 2. Add updateComposing call inside handleSaveComposingSettings
const oldSaveComposingMarker = 'await applyLanguage(targetLang);';
const newSaveComposingCode = `await applyLanguage(targetLang);

        try {
          await settingsAPI.updateComposing({
            spellingCheckEnabled: spellingCheck,
            grammarCheckEnabled: grammarCheck,
            autoCorrectEnabled: autoCorrect,
            smartComposeEnabled: writingSuggestions
          });
        } catch (err) {
          console.warn("Failed to save composing preferences to endpoint:", err);
        }`;

if (!content.includes('settingsAPI.updateComposing')) {
  content = content.replace(oldSaveComposingMarker, newSaveComposingCode);
}

fs.writeFileSync(settingsPath, content, 'utf8');
console.log("Updated Settings.jsx with composing settings persistence!");
