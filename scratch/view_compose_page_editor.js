const fs = require('fs');

const composePagePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/ComposePage.jsx';
if (fs.existsSync(composePagePath)) {
  const content = fs.readFileSync(composePagePath, 'utf8');
  const lines = content.split('\n');
  console.log("=== VIEWING COMPOSEPAGE.JSX LINES 330-365 ===");
  lines.slice(330, 365).forEach((line, idx) => {
    console.log(`${331 + idx}: ${line}`);
  });
}
