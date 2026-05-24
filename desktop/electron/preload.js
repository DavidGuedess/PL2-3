const { contextBridge } = require("electron");

contextBridge.exposeInMainWorld("miaugenda", {
  platform: process.platform
});