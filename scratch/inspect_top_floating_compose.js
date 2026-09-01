const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
let content = fs.readFileSync(composePath, 'utf8');

console.log("=== INSPECTING FLOATINGCOMPOSE SYNTAX ===");

// Check where eventListenerCode was inserted
const lines = content.split('\n');
lines.slice(0, 70).forEach((l, idx) => {
  console.log(`${idx + 1}: ${l}`);
});
