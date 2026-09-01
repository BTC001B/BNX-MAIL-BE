const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

console.log("=== VIEWING FLOATINGCOMPOSE.JSX LINES 240-285 ===");
lines.slice(239, 285).forEach((line, idx) => {
  console.log(`${240 + idx}: ${line}`);
});
