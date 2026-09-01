const fs = require('fs');

console.log("=== INSPECTING FLOATINGCOMPOSE.JSX ===");
const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
if (fs.existsSync(composePath)) {
  const content = fs.readFileSync(composePath, 'utf8');
  const lines = content.split('\n');
  console.log(`FloatingCompose.jsx total lines: ${lines.length}`);
  
  lines.forEach((line, idx) => {
    const l = line.toLowerCase();
    if (l.includes('font') || 
        l.includes('color') || 
        l.includes('size') || 
        l.includes('contenteditable') || 
        l.includes('execcommand') ||
        l.includes('editor') || 
        l.includes('textstyle') ||
        l.includes('bnx_setting') ||
        l.includes('gettextstyle') ||
        l.includes('getsettings')) {
      console.log(`L${idx + 1}: ${line.trim()}`);
    }
  });
} else {
  console.log("FloatingCompose.jsx not found!");
}

console.log("\n=== INSPECTING COMPOSEPAGE.JSX ===");
const composePagePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/ComposePage.jsx';
if (fs.existsSync(composePagePath)) {
  const content = fs.readFileSync(composePagePath, 'utf8');
  const lines = content.split('\n');
  console.log(`ComposePage.jsx total lines: ${lines.length}`);
  
  lines.forEach((line, idx) => {
    const l = line.toLowerCase();
    if (l.includes('font') || 
        l.includes('color') || 
        l.includes('size') || 
        l.includes('contenteditable') || 
        l.includes('execcommand') ||
        l.includes('editor') || 
        l.includes('textstyle') ||
        l.includes('bnx_setting') ||
        l.includes('gettextstyle') ||
        l.includes('getsettings')) {
      console.log(`L${idx + 1}: ${line.trim()}`);
    }
  });
} else {
  console.log("ComposePage.jsx not found!");
}
