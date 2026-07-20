const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('onyx', {
  authStatus: () => ipcRenderer.invoke('auth:status'),
  tryRefresh: () => ipcRenderer.invoke('auth:try-refresh'),
  startMicrosoft: () => ipcRenderer.invoke('auth:start-microsoft'),
  cancelMicrosoft: () => ipcRenderer.invoke('auth:cancel-microsoft'),
  playAsGuest: (username) => ipcRenderer.invoke('auth:guest', username),
  signOut: () => ipcRenderer.invoke('auth:sign-out'),
  getSettings: () => ipcRenderer.invoke('settings:get'),
  setSettings: (partial) => ipcRenderer.invoke('settings:set', partial),
  pickJava: () => ipcRenderer.invoke('dialog:pick-java'),
  pickGameDir: () => ipcRenderer.invoke('dialog:pick-game-dir'),
  fetchStats: () => ipcRenderer.invoke('stats:fetch'),
  getReadiness: () => ipcRenderer.invoke('launch:readiness'),
  installForge: () => ipcRenderer.invoke('forge:install'),
  play: () => ipcRenderer.invoke('launch:play'),
  checkUpdates: () => ipcRenderer.invoke('updater:check'),
  openExternal: (url) => ipcRenderer.invoke('shell:open-external', url),
  getVersion: () => ipcRenderer.invoke('app:get-version'),
  onLaunchProgress: (cb) => {
    const handler = (_e, data) => cb(data);
    ipcRenderer.on('launch:progress', handler);
    return () => ipcRenderer.removeListener('launch:progress', handler);
  },
  onUpdaterProgress: (cb) => {
    const handler = (_e, data) => cb(data);
    ipcRenderer.on('updater:progress', handler);
    return () => ipcRenderer.removeListener('updater:progress', handler);
  }
});
