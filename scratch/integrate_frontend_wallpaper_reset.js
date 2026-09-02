const fs = require('fs');

const apiPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/services/api.js';
const themeContextPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/context/ThemeContext.jsx';
const settingsPath = 'C:/Users/ASHWIN/Downloads/BNX-MAIL-FE/src/pages/Settings.jsx';

// 1. Update api.js
if (fs.existsSync(apiPath)) {
    let apiContent = fs.readFileSync(apiPath, 'utf8');
    const oldSettingsAPI = `export const settingsAPI = {
    getComposing: () => api.get(API_ENDPOINTS.SETTINGS?.COMPOSING || '/api/settings/composing'),
    updateComposing: (data) => api.put(API_ENDPOINTS.SETTINGS?.COMPOSING || '/api/settings/composing', data),
    getLanguage: () => api.get(API_ENDPOINTS.SETTINGS?.LANGUAGE || '/api/settings/language'),
    updateLanguage: (language) => api.put(API_ENDPOINTS.SETTINGS?.LANGUAGE || '/api/settings/language', { language }),
    getTextStyle: () => api.get(API_ENDPOINTS.SETTINGS?.TEXT_STYLE || '/api/settings/text-style'),
    updateTextStyle: (data) => api.put(API_ENDPOINTS.SETTINGS?.TEXT_STYLE || '/api/settings/text-style', data),
};`;

    const newSettingsAPI = `export const settingsAPI = {
    getComposing: () => api.get(API_ENDPOINTS.SETTINGS?.COMPOSING || '/api/settings/composing'),
    updateComposing: (data) => api.put(API_ENDPOINTS.SETTINGS?.COMPOSING || '/api/settings/composing', data),
    getLanguage: () => api.get(API_ENDPOINTS.SETTINGS?.LANGUAGE || '/api/settings/language'),
    updateLanguage: (language) => api.put(API_ENDPOINTS.SETTINGS?.LANGUAGE || '/api/settings/language', { language }),
    getTextStyle: () => api.get(API_ENDPOINTS.SETTINGS?.TEXT_STYLE || '/api/settings/text-style'),
    updateTextStyle: (data) => api.put(API_ENDPOINTS.SETTINGS?.TEXT_STYLE || '/api/settings/text-style', data),
    getWallpaper: () => api.get('/api/settings/wallpaper'),
    updateWallpaper: (wallpaper) => api.put('/api/settings/wallpaper', { wallpaper }),
    resetWallpaper: () => api.post('/api/settings/wallpaper/reset'),
};`;

    if (apiContent.includes(oldSettingsAPI)) {
        apiContent = apiContent.replace(oldSettingsAPI, newSettingsAPI);
        fs.writeFileSync(apiPath, apiContent, 'utf8');
        console.log("✓ Updated settingsAPI in api.js");
    } else if (!apiContent.includes('resetWallpaper')) {
        apiContent = apiContent.replace(
            /updateTextStyle:\s*\(data\)\s*=>\s*api\.put\([^)]+\),?/,
            `updateTextStyle: (data) => api.put(API_ENDPOINTS.SETTINGS?.TEXT_STYLE || '/api/settings/text-style', data),\n    getWallpaper: () => api.get('/api/settings/wallpaper'),\n    updateWallpaper: (wallpaper) => api.put('/api/settings/wallpaper', { wallpaper }),\n    resetWallpaper: () => api.post('/api/settings/wallpaper/reset'),`
        );
        fs.writeFileSync(apiPath, apiContent, 'utf8');
        console.log("✓ Added wallpaper methods to settingsAPI in api.js");
    }
}

// 2. Update ThemeContext.jsx
if (fs.existsSync(themeContextPath)) {
    let themeContent = fs.readFileSync(themeContextPath, 'utf8');

    // Add import if missing
    if (!themeContent.includes('settingsAPI')) {
        themeContent = `import { settingsAPI } from "../services/api";\n` + themeContent;
    }

    // Add backend fetch to mount useEffect
    const mountTarget = `const savedBg = localStorage.getItem("bnx_bg_image");
    if (savedBg) {
      setBackgroundImageState(savedBg);
    }`;

    const mountReplacement = `const savedBg = localStorage.getItem("bnx_bg_image");
    if (savedBg) {
      setBackgroundImageState(savedBg);
    }
    settingsAPI.getWallpaper()
      .then(res => {
        if (res.data && res.data.wallpaper) {
          if (res.data.wallpaper === 'default') {
            setBackgroundImageState(null);
            localStorage.removeItem("bnx_bg_image");
          } else {
            setBackgroundImageState(res.data.wallpaper);
            localStorage.setItem("bnx_bg_image", res.data.wallpaper);
          }
        }
      })
      .catch(() => {});`;

    if (themeContent.includes(mountTarget) && !themeContent.includes('settingsAPI.getWallpaper()')) {
        themeContent = themeContent.replace(mountTarget, mountReplacement);
    }

    // Update clearBackgroundImage
    const oldClearBg = `  const clearBackgroundImage = () => {
    setBackgroundImageState(null);
    localStorage.removeItem("bnx_bg_image");
  };`;

    const newClearBg = `  const clearBackgroundImage = async () => {
    setBackgroundImageState(null);
    localStorage.removeItem("bnx_bg_image");
    try {
      await settingsAPI.resetWallpaper();
    } catch (e) {
      console.error("Failed to reset wallpaper on backend", e);
    }
  };`;

    if (themeContent.includes(oldClearBg)) {
        themeContent = themeContent.replace(oldClearBg, newClearBg);
    }

    // Update setBackgroundImage
    const oldSetBg = `  const setBackgroundImage = (url) => {
    setBackgroundImageState(url);
    if (url) {
      localStorage.setItem("bnx_bg_image", url);
    } else {
      localStorage.removeItem("bnx_bg_image");
    }
  };`;

    const newSetBg = `  const setBackgroundImage = (url) => {
    setBackgroundImageState(url);
    if (url) {
      localStorage.setItem("bnx_bg_image", url);
      settingsAPI.updateWallpaper(url).catch(e => console.error("Failed to sync wallpaper to backend", e));
    } else {
      localStorage.removeItem("bnx_bg_image");
      settingsAPI.resetWallpaper().catch(e => console.error("Failed to reset wallpaper on backend", e));
    }
  };`;

    if (themeContent.includes(oldSetBg)) {
        themeContent = themeContent.replace(oldSetBg, newSetBg);
    }

    fs.writeFileSync(themeContextPath, themeContent, 'utf8');
    console.log("✓ Updated ThemeContext.jsx with backend sync for wallpaper reset");
}

// 3. Update Settings.jsx
if (fs.existsSync(settingsPath)) {
    let settingsContent = fs.readFileSync(settingsPath, 'utf8');

    const oldResetBtn = `onClick={() => {
                        setSelectedWallpaper(null);
                        clearBackgroundImage();
                        toast.success("Background reset to default", { id: "wallpaper-toast", duration: 3000 });
                      }}`;

    const newResetBtn = `onClick={async () => {
                        setSelectedWallpaper(null);
                        await clearBackgroundImage();
                        toast.success("Background reset to default", { id: "wallpaper-toast", duration: 3000 });
                      }}`;

    if (settingsContent.includes(oldResetBtn)) {
        settingsContent = settingsContent.replace(oldResetBtn, newResetBtn);
        fs.writeFileSync(settingsPath, settingsContent, 'utf8');
        console.log("✓ Updated Settings.jsx Reset to Default button handler");
    }
}
