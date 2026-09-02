const fs = require('fs');

const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/Subscriptions.jsx';

if (fs.existsSync(path)) {
    let content = fs.readFileSync(path, 'utf8');

    // 1. Ensure blockedContactsAPI is imported
    if (!content.includes('blockedContactsAPI')) {
        content = content.replace(
            'import { mailAPI } from "../services/api";',
            'import { mailAPI, blockedContactsAPI } from "../services/api";'
        );
    }

    // 2. Update fetchSubscriptionsData to fetch from blockedContactsAPI
    if (content.includes('const blockedRes = await mailAPI.getSubscriptions();')) {
        content = content.replace(
            'const blockedRes = await mailAPI.getSubscriptions();',
            'const blockedRes = await blockedContactsAPI.getBlockedContacts();'
        );
        content = content.replace(
            'const blockedList = blockedRes.data?.data || [];',
            'const blockedList = (blockedRes.data?.data || []).map(item => typeof item === "string" ? item : item.email);'
        );
    }

    // 3. Update handleToggleBlock to call blockedContactsAPI.unblockSender / blockSender
    const oldToggleBlockRegex = /const handleToggleBlock = async \(senderEmail, isBlocked\) => \{[\s\S]*?\}\;\s*const filteredSenders/;
    const newToggleBlockCode = `const handleToggleBlock = async (senderEmail, isBlocked) => {
    const toastMessage = isBlocked ? "Unblocked sender" : "Blocked sender";
    
    try {
      if (isBlocked) {
        await blockedContactsAPI.unblockSender(senderEmail);
      } else {
        await blockedContactsAPI.blockSender(senderEmail);
      }
      toast.success(toastMessage);
      
      // Update local state
      if (isBlocked) {
        setBlockedSenders(prev => prev.filter(e => e.toLowerCase() !== senderEmail.toLowerCase()));
      } else {
        setBlockedSenders(prev => [...prev, senderEmail]);
      }
    } catch (error) {
      console.error("Error toggling subscription:", error);
      toast.error("Failed to update status");
    }
  };

  const filteredSenders`;

    if (!content.includes('await blockedContactsAPI.unblockSender(senderEmail)')) {
        content = content.replace(oldToggleBlockRegex, newToggleBlockCode);
    }

    fs.writeFileSync(path, content, 'utf8');
    console.log("Successfully updated Subscriptions.jsx with backend blockedContactsAPI!");
} else {
    console.log("Subscriptions.jsx not found.");
}
