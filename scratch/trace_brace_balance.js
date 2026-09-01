const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

let balance = 0;
lines.forEach((line, idx) => {
  const lineNo = idx + 1;
  for (let char of line) {
    if (char === '{') balance++;
    if (char === '}') balance--;
  }
  if (line.includes('const FloatingCompose') || line.includes('<style>') || line.includes('</style>')) {
    console.log(`L${lineNo}: balance=${balance} | ${line.trim()}`);
  }
});
