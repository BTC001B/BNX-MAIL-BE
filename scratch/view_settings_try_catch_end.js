const fs = require('fs');

const settingsPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/Settings.jsx';
const content = fs.readFileSync(settingsPath, 'utf8');
const lines = content.split('\n');

console.log("=== VIEWING SETTINGS.JSX LINES 395-420 ===");
lines.slice(394, 420).forEach((line, idx) => {
  console.log(`${395 + idx}: ${line}`);
});
