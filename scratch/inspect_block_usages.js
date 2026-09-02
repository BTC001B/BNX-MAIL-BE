const fs = require('fs');

const filesToInspect = [
    'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/EmailDetails.jsx',
    'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/Settings.jsx',
    'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/Subscriptions.jsx',
    'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/context/MailContext.jsx'
];

filesToInspect.forEach(filePath => {
    if (fs.existsSync(filePath)) {
        console.log("==========================================");
        console.log("FILE:", filePath);
        const content = fs.readFileSync(filePath, 'utf8');
        const lines = content.split('\n');
        lines.forEach((line, index) => {
            if (line.toLowerCase().includes('block') || line.toLowerCase().includes('unsubscribe') || line.toLowerCase().includes('subscribe')) {
                console.log(`L${index + 1}: ${line.trim()}`);
            }
        });
    }
});
