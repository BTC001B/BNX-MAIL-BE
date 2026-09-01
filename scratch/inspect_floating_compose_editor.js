const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

console.log("=== SEARCHING FLOATINGCOMPOSE.JSX FOR EDITOR & BODY ===");

lines.forEach((line, idx) => {
  const l = line.toLowerCase();
  if (l.includes('body') || 
      l.includes('editor') || 
      l.includes('contenteditable') || 
      l.includes('textarea') || 
      l.includes('ref=') ||
      l.includes('formatting') ||
      l.includes('fontfamily') ||
      l.includes('fontsize') ||
      l.includes('color')) {
    console.log(`L${idx + 1}: ${line.trim()}`);
  }
});
