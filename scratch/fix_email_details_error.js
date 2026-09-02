const fs = require('fs');

const path = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/components/EmailDetails.jsx';

if (fs.existsSync(path)) {
    let content = fs.readFileSync(path, 'utf8');

    const targetCodeBlock = `  const [showBlockModal, setShowBlockModal] = useState(false);
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

    const replacementCodeBlock = `  const [showBlockModal, setShowBlockModal] = useState(false);
  const [blockedContacts, setBlockedContacts] = useState([]);

  useEffect(() => {
    let isMounted = true;
    blockedContactsAPI.getBlockedContacts()
      .then(res => {
        if (isMounted && res.data && res.data.data) {
          const list = res.data.data.map(c => typeof c === 'string' ? c : c.email);
          setBlockedContacts(list);
        }
      })
      .catch(err => {
        console.error("Failed to load blocked contacts from backend", err);
      });
    return () => { isMounted = false; };
  }, [normalizedSender]);

  const isBlocked = normalizedSender ? blockedContacts.some(c => c.toLowerCase() === normalizedSender.toLowerCase()) : false;`;

    if (content.includes(targetCodeBlock)) {
        content = content.replace(targetCodeBlock, replacementCodeBlock);
    } else {
        // Fallback replacement if targetCodeBlock differs slightly
        content = content.replace(
            /const \[showBlockModal, setShowBlockModal\] = useState\(false\);\s*const \[isBlocked, setIsBlocked\] = useState\(false\);[\s\S]*?\}, \[normalizedSender\]\);/,
            replacementCodeBlock
        );
    }

    // Also update handleToggleBlock to update blockedContacts state
    const oldToggleBlock = `  const handleToggleBlock = async () => {
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

    const newToggleBlock = `  const handleToggleBlock = async () => {
    if (!normalizedSender) return;
    try {
      if (isBlocked) {
        toast.loading("Unblocking sender...", { id: "block-toggle" });
        await blockedContactsAPI.unblockSender(normalizedSender);
        setBlockedContacts(prev => prev.filter(c => c.toLowerCase() !== normalizedSender.toLowerCase()));
        toast.success("Sender unblocked successfully.", { id: "block-toggle" });
      } else {
        toast.loading("Blocking sender...", { id: "block-toggle" });
        await blockedContactsAPI.blockSender(normalizedSender);
        setBlockedContacts(prev => [...prev, normalizedSender]);
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

    if (content.includes(oldToggleBlock)) {
        content = content.replace(oldToggleBlock, newToggleBlock);
    }

    fs.writeFileSync(path, content, 'utf8');
    console.log("Successfully fixed EmailDetails.jsx!");
} else {
    console.log("EmailDetails.jsx not found.");
}
