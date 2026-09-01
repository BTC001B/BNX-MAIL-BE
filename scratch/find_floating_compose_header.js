const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

lines.forEach((l, idx) => {
  if (l.includes('FloatingCompose')) {
    console.log(`L${idx + 1}: ${l}`);
    lines.slice(idx - 2, idx + 25).forEach((line, i) => console.log(`  ${idx - 1 + i}: ${line}`));
  }
});
