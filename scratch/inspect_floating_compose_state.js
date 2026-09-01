const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

console.log("=== INSPECTING FLOATINGCOMPOSE.JSX STATE & FETCH ===");

lines.forEach((line, idx) => {
  if (line.includes('defaultFontFamily') || 
      line.includes('defaultFontSize') || 
      line.includes('defaultTextColor') || 
      line.includes('fetchSettings') ||
      line.includes('getTextStyle') ||
      line.includes('getSettings')) {
    console.log(`L${idx + 1}: ${line.trim()}`);
  }
});
