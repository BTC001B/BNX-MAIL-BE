const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

console.log("=== VIEWING FLOATINGCOMPOSE.JSX LINES 780-840 ===");
lines.slice(779, 840).forEach((line, idx) => {
  console.log(`${780 + idx}: ${line}`);
});
