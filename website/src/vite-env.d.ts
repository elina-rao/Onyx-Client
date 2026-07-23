/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SERVER_IP?: string;
  readonly VITE_DISCORD_INVITE?: string;
  readonly VITE_DOWNLOAD_MAC?: string;
  readonly VITE_DOWNLOAD_WIN?: string;
  readonly VITE_TRAILER_URL?: string;
  readonly VITE_DISCORD_ONLINE?: string;
  readonly VITE_DISCORD_MEMBERS?: string;
  readonly VITE_YOUTUBE_URL?: string;
  readonly VITE_TIKTOK_URL?: string;
  readonly VITE_INSTAGRAM_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
