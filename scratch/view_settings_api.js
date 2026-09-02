const fs = require('fs');

const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/services/api.js';

if (fs.existsSync(path)) {
    const content = fs.readFileSync(path, 'utf8');
    const lines = content.split('\n');
    lines.forEach((line, index) => {
        if (line.includes('settingsAPI')) {
            for (let i = index; i < Math.min(lines.length, index + 35); i++) {
                console.log(`L${i + 1}: ${lines[i]}`);
            }
        }
    });
}
