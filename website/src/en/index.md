---
# https://vitepress.dev/reference/default-theme-home-page
layout: home

hero:
  name: 'LunaticChat'
  tagline: A next-generation chat plugin for Paper, Folia and Velocity.
  actions:
    - theme: brand
      text: Download
      link: /download
    - theme: brand
      text: Documentation
      link: /en/docs/getting-started
    - theme: alt
      text: GitHub
      link: https://github.com/m1sk9/LunaticChat

features:
  - title: Channel Chat
    details: Create and manage channels for group conversations between specific players. Includes private channels and moderation features.
    icon: ☎️
  - title: Direct Messages
    details: Send 1-on-1 chats with /tell or /msg commands. Quickly reply to the last sender with /reply.
    icon: ✉️
  - title: Romaji Conversion
    details: Automatically convert romaji input into Japanese. Fast performance powered by caching.
    icon: 🌍
  - title: Velocity Cross-Server Chat
    details: Relay global chat across multiple servers via a Velocity proxy. Join conversations from any server.
    icon: 🔗
  - title: Flexible Configuration
    details: Toggle features on/off with a YAML-based config file. Customize to fit your server's needs.
    icon: ⚙️
  - title: Latest Version Support
    details: Minimal external plugin dependencies, always supporting the latest Minecraft versions.
    icon: ⛏️
---

<hr class="home-divider" />

<!-- Section 1: Channel Chat (text left, image right) -->
<div class="feature-showcase">
  <div class="feature-showcase-text">
    <h2>Organize Conversations with Channel Chat</h2>
    <p>
      Create channels within your server to separate conversations by topic or group.
      Communicate with only the members you need, without flooding the global chat.
    </p>
    <ul>
      <li>Create password-protected private channels</li>
      <li>Per-channel moderation (kick, mute, ban)</li>
      <li>Customizable join/leave notifications</li>
    </ul>
  </div>
  <div class="feature-showcase-image">
    <img src="../assets/features/channel-chat.png" alt="LunaticChat Channel Chat feature" />
  </div>
</div>

<hr class="home-divider" />

<!-- Section 2: Direct Messages (image left, text right) -->
<div class="feature-showcase reverse">
  <div class="feature-showcase-text">
    <h2>Direct Messages & Quick Reply</h2>
    <p>
      Easily send private 1-on-1 chats between players.
      Use the <code>/reply</code> command to instantly respond to the last sender.
    </p>
    <ul>
      <li>Send direct messages with <code>/tell</code> / <code>/msg</code></li>
      <li>Instantly reply to the last sender with <code>/reply</code></li>
      <li>Messages are visible only to the sender and recipient</li>
    </ul>
  </div>
  <div class="feature-showcase-image">
    <img src="../assets/features/dm.png" alt="LunaticChat Direct Message feature" />
  </div>
</div>

<hr class="home-divider" />

<!-- Section 3: Romaji Conversion (text left, image right) -->
<div class="feature-showcase">
  <div class="feature-showcase-text">
    <h2>Automatic Romaji to Japanese Conversion</h2>
    <p>
      Even in environments without Japanese input support, simply type in romaji and it will be automatically converted to Japanese.
      Powered by the Google IME API for natural conversion results.
    </p>
    <ul>
      <li>Real-time romaji-to-Japanese conversion while chatting</li>
      <li>Fast performance with conversion result caching</li>
      <li>Per-player toggle to enable/disable conversion</li>
    </ul>
  </div>
  <div class="feature-showcase-image">
    <img src="../assets/features/romaji.png" alt="LunaticChat Romaji Conversion feature" />
  </div>
</div>

<hr class="home-divider" />

<!-- Section 4: Velocity Integration (image left, text right) -->
<div class="feature-showcase reverse">
  <div class="feature-showcase-text">
    <h2>Cross-Server Chat with Velocity</h2>
    <p>
      Integrate with a Velocity proxy to relay global chat across multiple Paper/Folia servers.
      Players can join the same chat space regardless of which server they're on.
    </p>
    <ul>
      <li>Relay regular chat to all servers in real time</li>
      <li>Fast communication via a custom plugin messaging protocol</li>
      <li>Backward compatibility guaranteed through protocol versioning</li>
    </ul>
  </div>
  <div class="feature-showcase-image">
    <img src="../assets/features/cross-chat.png" alt="LunaticChat Cross-Server Chat feature" />
  </div>
</div>

<hr class="home-divider" />

<!-- Section 5: Platforms -->
<div class="platform-section">
  <h2>Multi-Platform Support</h2>
  <p class="section-desc">Flexibly deploy to match your server setup</p>
  <div class="platform-cards">
    <a class="platform-card" href="https://papermc.io/software/paper/" target="_blank" rel="noopener">
      <div class="platform-icon">
        <img src="../assets/brand/paper.svg" alt="Paper" width="40" height="40" />
      </div>
      <p class="name">Paper</p>
      <p class="desc">The most widely used Minecraft server implementation. Full support for DM, channel chat, romaji conversion, and all features. Maintains compatibility with Bukkit/Spigot plugins.</p>
    </a>
    <a class="platform-card" href="https://papermc.io/software/folia" target="_blank" rel="noopener">
      <div class="platform-icon">
        <img src="../assets/brand/folia.svg" alt="Folia" width="40" height="40" />
      </div>
      <p class="name">Folia</p>
      <p class="desc">A multithreaded server implementation by PaperMC. Provides a stable chat experience even on large-scale servers through region-based parallel processing.</p>
    </a>
    <a class="platform-card" href="https://papermc.io/software/velocity" target="_blank" rel="noopener">
      <div class="platform-icon">
        <img src="../assets/brand/velocity.svg" alt="Velocity" width="40" height="40" />
      </div>
      <p class="name">Velocity</p>
      <p class="desc">A high-performance proxy server. Install the LunaticChat Velocity plugin to enable global chat relay across multiple Paper/Folia servers.</p>
    </a>
  </div>
</div>
