const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

console.log("=== INSPECTING REACTQUILL IN FLOATINGCOMPOSE.JSX ===");

lines.forEach((line, idx) => {
  const l = line.toLowerCase();
  if (l.includes('quill') || 
      l.includes('font') || 
      l.includes('ref=') ||
      l.includes('module') ||
      l.includes('format')) {
    console.log(`L${idx + 1}: ${line.trim()}`);
  }
});
