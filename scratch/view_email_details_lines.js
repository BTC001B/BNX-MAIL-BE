const fs = require('fs');

const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/EmailDetails.jsx';
const content = fs.readFileSync(path, 'utf8');
const lines = content.split('\n');

for (let i = 148; i < 195; i++) {
    if (lines[i]) console.log(`L${i+1}: ${lines[i]}`);
}
