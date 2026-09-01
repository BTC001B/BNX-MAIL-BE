const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

console.log("=== VIEWING FLOATINGCOMPOSE.JSX LINES 270-310 ===");
lines.slice(269, 310).forEach((line, idx) => {
  console.log(`${270 + idx}: ${line}`);
});
