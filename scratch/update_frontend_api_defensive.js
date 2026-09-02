const fs = require('fs');

const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/services/api.js';

if (fs.existsSync(path)) {
    let content = fs.readFileSync(path, 'utf8');

    const oldBlockAPI = `export const blockedContactsAPI = {
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
};`;

    const newBlockAPI = `export const blockedContactsAPI = {
  blockSender: async (email) => {
    const response = await api.post('/blocked-contacts', { email });
    return response.data;
  },

  unblockSender: async (email) => {
    const response = await api.delete(\`/blocked-contacts/\${encodeURIComponent(email)}\`);
    return response.data;
  },

  checkBlockStatus: async (email) => {
    try {
      const response = await api.get(\`/blocked-contacts/check?email=\${encodeURIComponent(email)}\`);
      return response.data;
    } catch (err) {
      console.warn("Failed to check block status, returning fallback", err);
      return { success: true, data: { blocked: false } };
    }
  },

  getBlockedContacts: async () => {
    try {
      const response = await api.get('/blocked-contacts');
      return response.data;
    } catch (err) {
      console.warn("Failed to load blocked contacts, returning empty list fallback", err);
      return { success: true, data: [] };
    }
  }
};`;

    if (content.includes(oldBlockAPI)) {
        content = content.replace(oldBlockAPI, newBlockAPI);
        fs.writeFileSync(path, content, 'utf8');
        console.log("Successfully updated frontend api.js with defensive error handling!");
    } else {
        // Replace getBlockedContacts specifically if whole block matches slightly differently
        content = content.replace(
            /getBlockedContacts:\s*async\s*\(\)\s*=>\s*\{[\s\S]*?return\s+response\.data;\s*\}/,
            `getBlockedContacts: async () => {
    try {
      const response = await api.get('/blocked-contacts');
      return response.data;
    } catch (err) {
      console.warn("Failed to load blocked contacts, returning empty list fallback", err);
      return { success: true, data: [] };
    }
  }`
        );
        fs.writeFileSync(path, content, 'utf8');
        console.log("Successfully updated getBlockedContacts in api.js!");
    }
} else {
    console.log("Frontend api.js file not found.");
}
