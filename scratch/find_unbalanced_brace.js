const fs = require('fs');

const composePath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/FloatingCompose.jsx';
const content = fs.readFileSync(composePath, 'utf8');
const lines = content.split('\n');

let balance = 0;
lines.forEach((line, idx) => {
  for (let char of line) {
    if (char === '{') balance++;
    if (char === '}') balance--;
  }
  if (balance !== 0) {
    // console.log(`Line ${idx + 1}: balance ${balance} | ${line}`);
  }
});
console.log(`Final balance at line ${lines.length}: ${balance}`);
