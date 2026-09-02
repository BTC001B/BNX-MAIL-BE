const fs = require('fs');

const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/EmailDetails.jsx';

if (fs.existsSync(path)) {
    let content = fs.readFileSync(path, 'utf8');

    // 1. Ensure blockedContactsAPI is imported
    if (!content.includes('blockedContactsAPI')) {
        content = content.replace(
            'import { mailAPI, reportAPI } from "../services/api";',
            'import { mailAPI, reportAPI, blockedContactsAPI } from "../services/api";'
        );
    }

    // 2. Add useEffect to fetch block status from backend for the normalizedSender
    const stateHookTarget = '  const [showBlockModal, setShowBlockModal] = useState(false);';
    const newBackendStateCode = `  const [showBlockModal, setShowBlockModal] = useState(false);
  const [isBlocked, setIsBlocked] = useState(false);

  useEffect(() => {
    let isMounted = true;
    if (normalizedSender) {
      blockedContactsAPI.checkBlockStatus(normalizedSender)
        .then(res => {
          if (isMounted && res.data) {
            setIsBlocked(!!res.data.blocked);
          }
        })
        .catch(err => {
          console.error("Failed to check block status from backend", err);
        });
    }
    return () => { isMounted = false; };
  }, [normalizedSender]);`;

    if (!content.includes('checkBlockStatus(normalizedSender)')) {
        content = content.replace(
            /const \[blockedContacts, setBlockedContacts\] = useState[\s\S]*?\}\);\s*const \[showBlockModal, setShowBlockModal\] = useState\(false\);\s*const isBlocked = normalizedSender \? blockedContacts\.includes\(normalizedSender\) : false;/,
            newBackendStateCode
        );
    }

    // 3. Update handleToggleBlock to call backend API
    const oldToggleBlockRegex = /const handleToggleBlock = \(\) => \{[\s\S]*?setShowBlockModal\(false\);\s*\};/;
    const newToggleBlockCode = `const handleToggleBlock = async () => {
    if (!normalizedSender) return;
    try {
      if (isBlocked) {
        toast.loading("Unblocking sender...", { id: "block-toggle" });
        await blockedContactsAPI.unblockSender(normalizedSender);
        setIsBlocked(false);
        toast.success("Sender unblocked successfully.", { id: "block-toggle" });
      } else {
        toast.loading("Blocking sender...", { id: "block-toggle" });
        await blockedContactsAPI.blockSender(normalizedSender);
        setIsBlocked(true);
        toast.success("Sender blocked successfully.", { id: "block-toggle" });
      }
      if (fetchEmails) {
        fetchEmails(currentFolder || "inbox");
      }
    } catch (err) {
      console.error("Failed to toggle block status", err);
      toast.error("Failed to update block status", { id: "block-toggle" });
    } finally {
      setShowBlockModal(false);
    }
  };`;

    if (!content.includes('await blockedContactsAPI.blockSender(normalizedSender)')) {
        content = content.replace(oldToggleBlockRegex, newToggleBlockCode);
    }

    fs.writeFileSync(path, content, 'utf8');
    console.log("Successfully updated EmailDetails.jsx with backend blockedContactsAPI!");
} else {
    console.log("EmailDetails.jsx not found.");
}
