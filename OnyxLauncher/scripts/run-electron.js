#!/usr/bin/env node
/**
 * Launch Electron with a clean env.
 * Cursor / some IDEs set ELECTRON_RUN_AS_NODE=1 which breaks require('electron').app.
 */
const { spawn } = require('child_process');
const path = require('path');
const electron = require('electron');

const env = { ...process.env };
delete env.ELECTRON_RUN_AS_NODE;

const args = process.argv.slice(2);
const child = spawn(electron, ['.', ...args], {
  cwd: path.join(__dirname, '..'),
  env,
  stdio: 'inherit'
});

child.on('exit', (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
  } else {
    process.exit(code == null ? 0 : code);
  }
});
