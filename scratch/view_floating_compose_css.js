const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

console.log("=== VIEWING FLOATINGCOMPOSE.JSX LINES 760-810 ===");
lines.slice(759, 810).forEach((line, idx) => {
  console.log(`${760 + idx}: ${line}`);
});
