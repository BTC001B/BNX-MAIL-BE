const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

console.log("=== EXACT LINES 810-855 ===");
lines.slice(809, 855).forEach((l, idx) => {
  console.log(`${810 + idx}: ${JSON.stringify(l)}`);
});
