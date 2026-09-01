const fs = require('fs');

const settingsPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/Settings.jsx';
const content = fs.readFileSync(settingsPath, 'utf8');
const lines = content.split('\n');

console.log("=== VIEWING SETTINGS.JSX LINES 360-395 ===");
lines.slice(359, 395).forEach((line, idx) => {
  console.log(`${360 + idx}: ${line}`);
});
