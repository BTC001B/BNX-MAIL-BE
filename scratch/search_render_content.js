const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

console.log("=== SEARCHING FLOATINGCOMPOSE.JSX FOR RENDERCONTENT ===");

lines.forEach((line, idx) => {
  if (line.includes('renderContent')) {
    console.log(`L${idx + 1}: ${line.trim()}`);
  }
});
