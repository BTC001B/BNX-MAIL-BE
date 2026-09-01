const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

console.log("=== VIEWING FLOATINGCOMPOSE.JSX LINES 975-1020 ===");
lines.slice(974, 1020).forEach((line, idx) => {
  console.log(`${975 + idx}: ${line}`);
});
