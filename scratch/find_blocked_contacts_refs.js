const fs = require('fs');

const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/EmailDetails.jsx';

if (fs.existsSync(path)) {
    const content = fs.readFileSync(path, 'utf8');
    const lines = content.split('\n');
    lines.forEach((line, index) => {
        if (line.includes('blockedContacts')) {
            console.log(`L${index + 1}: ${line.trim()}`);
        }
    });
}
