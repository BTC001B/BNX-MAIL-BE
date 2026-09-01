const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

console.log("=== SEARCHING FLOATINGCOMPOSE.JSX FOR SETTINGS & USEEFFECT ===");

lines.forEach((line, idx) => {
  const l = line.toLowerCase();
  if (l.includes('useeffect') || 
      l.includes('localstorage') || 
      l.includes('settingsapi') || 
      l.includes('userapi') ||
      l.includes('gettextstyle') ||
      l.includes('textstyle') ||
      l.includes('fontfamily')) {
    console.log(`L${idx + 1}: ${line.trim()}`);
  }
});
