const fs = require('fs');

const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/context/MailContext.jsx';

if (fs.existsSync(path)) {
    let content = fs.readFileSync(path, 'utf8');

    if (!content.includes('blockedContactsAPI')) {
        content = content.replace(
            'import { mailAPI } from "../services/api";',
            'import { mailAPI, blockedContactsAPI } from "../services/api";'
        );
    }

    if (content.includes('const handleUnsubscribe = async (senderEmail, silent = false) => {')) {
        content = content.replace(
            'await mailAPI.unsubscribe(senderEmail);',
            'await blockedContactsAPI.blockSender(senderEmail);'
        );
        fs.writeFileSync(path, content, 'utf8');
        console.log("Successfully updated MailContext.jsx!");
    } else {
        console.log("handleUnsubscribe signature not found in MailContext.jsx");
    }
} else {
    console.log("MailContext.jsx not found.");
}
