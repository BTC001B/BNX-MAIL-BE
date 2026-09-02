const fs = require('fs');
const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/services/api.js';

console.log("Checking if frontend api.js exists at:", path);
if (fs.existsSync(path)) {
    console.log("File exists! Reading content...");
    let content = fs.readFileSync(path, 'utf8');
    console.log("File size:", content.length, "bytes");

    if (!content.includes('blockedContactsAPI')) {
        console.log("Adding blockedContactsAPI to api.js...");
        const blockedAPICode = `

// Blocked Contacts API Service
export const blockedContactsAPI = {
  blockSender: async (email) => {
    const response = await api.post('/blocked-contacts', { email });
    return response.data;
  },

  unblockSender: async (email) => {
    const response = await api.delete(\`/blocked-contacts/\${encodeURIComponent(email)}\`);
    return response.data;
  },

  checkBlockStatus: async (email) => {
    const response = await api.get(\`/blocked-contacts/check?email=\${encodeURIComponent(email)}\`);
    return response.data;
  },

  getBlockedContacts: async () => {
    const response = await api.get('/blocked-contacts');
    return response.data;
  }
};
`;
        content += blockedAPICode;
        fs.writeFileSync(path, content, 'utf8');
        console.log("Successfully updated api.js with blockedContactsAPI!");
    } else {
        console.log("blockedContactsAPI already exists in api.js.");
    }
} else {
    console.log("File does not exist at:", path);
}
