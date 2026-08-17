# Gaming & Maximum Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the HP Laptop 15-dy1xxx (Intel i5-1035G1, 8GB RAM, Intel Iris Plus/UHD Graphics, Ubuntu 26.04) into a low-latency, zero-microstutter gaming and performance machine by configuring high-speed zRAM, CPU performance governor, Vulkan/Mesa pipeline caching, GameMode, TCP BBR, and kernel virtual memory tuning.

**Architecture:** A multi-layered Linux optimization suite addressing memory constraints (zstd zRAM swap), CPU responsiveness (intel_pstate EPP lock + irqbalance), 3D driver acceleration (Mesa Vulkan ANV shader cache), automated game prioritization (Feral GameMode), and low-latency TCP BBR networking.

**Tech Stack:** Ubuntu 26.04 LTS, Kernel 7.0 PREEMPT_DYNAMIC, `zram-tools`, `systemd`, `gamemode`, `powerprofilesctl`, `irqbalance`, `mesa-vulkan-drivers`, `intel-media-va-driver`, `sysctl`.

## Global Constraints
- Target Hardware: Intel Core i5-1035G1 (4C/8T), Intel Iris Plus / UHD Graphics G1, 8GB RAM, NVMe SSD.
- Distro: Ubuntu 26.04 LTS (Resolute Raccoon) x86_64.
- All system configurations must be persistent across reboots.
- Zero data loss or breaking system stability.

---

### Task 1: High-Speed zRAM and Virtual Memory (Sysctl) Tuning

**Files:**
- Create/Modify: `/etc/default/zramswap`
- Create: `/etc/sysctl.d/99-gaming-performance.conf`

- [ ] **Step 1: Install zram-tools**
Run apt to install `zram-tools`.
- [ ] **Step 2: Configure `/etc/default/zramswap`**
Configure ALGO=zstd, PERCENT=100, PRIORITY=32767.
- [ ] **Step 3: Write `/etc/sysctl.d/99-gaming-performance.conf`**
Configure `vm.swappiness=180`, `vm.vfs_cache_pressure=50`, `vm.max_map_count=2147483642`, `vm.dirty_ratio=10`, `vm.dirty_background_ratio=5`, `vm.watermark_boost_factor=0`.
- [ ] **Step 4: Restart zramswap and apply sysctl**
Restart `zramswap.service` and apply `sysctl --system`.
- [ ] **Step 5: Verify zRAM and Sysctl**
Check `zramctl`, `swapon --show`, and `sysctl vm.swappiness vm.max_map_count`.

---

### Task 2: CPU Governor, Energy Performance Preference (EPP), and IRQ Balancing

**Files:**
- Create: `/etc/systemd/system/cpu-performance-boost.service`
- System config: `powerprofilesctl`

- [ ] **Step 1: Install and enable `irqbalance`**
Install `irqbalance` and enable `irqbalance.service`.
- [ ] **Step 2: Set Power Profile to Performance**
Run `powerprofilesctl set performance`.
- [ ] **Step 3: Create persistent CPU boost systemd service**
Create `/etc/systemd/system/cpu-performance-boost.service` to lock all CPU scaling governors to `performance` and energy_perf_preference to `performance`.
- [ ] **Step 4: Enable and start `cpu-performance-boost.service`**
Run `systemctl daemon-reload && systemctl enable --now cpu-performance-boost.service`.
- [ ] **Step 5: Verify CPU Governor and EPP**
Check `/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor` and `energy_perf_preference`.

---

### Task 3: Intel Ice Lake Vulkan & Mesa Graphics Acceleration

**Files:**
- Create: `/etc/environment.d/99-gaming-graphics.conf`

- [ ] **Step 1: Ensure Vulkan and Intel Media Acceleration packages are installed**
Install `mesa-vulkan-drivers`, `vulkan-tools`, `libvulkan1`, `intel-media-va-driver-non-free` (or `intel-media-va-driver`).
- [ ] **Step 2: Create `/etc/environment.d/99-gaming-graphics.conf`**
Set `MESA_SHADER_CACHE_MAX_SIZE=4G`, `ANV_ENABLE_PIPELINE_CACHE=1`, `mesa_glthread=true`.
- [ ] **Step 3: Verify Vulkan & OpenGL acceleration**
Check `vulkaninfo --summary` and `glxinfo -B`.

---

### Task 4: Feral GameMode Configuration & Process Renicing

**Files:**
- Create/Modify: `/etc/gamemode.ini`
- Create: `/etc/security/limits.d/99-gaming.conf`

- [ ] **Step 1: Configure `/etc/gamemode.ini`**
Set desired governor to performance, softrealtime=auto, renice=-10, ioprio=1, inhibit_screensaver=1.
- [ ] **Step 2: Configure `/etc/security/limits.d/99-gaming.conf`**
Set `nofile` to 1048576, `memlock` to unlimited, `nice` priority to -20 for all users.
- [ ] **Step 3: Enable and start gamemoded**
Ensure `systemctl --user enable --now gamemoded` is active.
- [ ] **Step 4: Test GameMode status**
Run `gamemoded -s` and verify daemon responds properly.

---

### Task 5: Low-Latency Networking (TCP BBR + FQ) & Kernel Buffers

**Files:**
- Append: `/etc/sysctl.d/99-gaming-performance.conf`
- Create: `/etc/modules-load.d/tcp_bbr.conf`

- [ ] **Step 1: Enable `tcp_bbr` kernel module**
Write `tcp_bbr` into `/etc/modules-load.d/tcp_bbr.conf` and `modprobe tcp_bbr`.
- [ ] **Step 2: Add network tweaks to `/etc/sysctl.d/99-gaming-performance.conf`**
Add `net.core.default_qdisc = fq`, `net.ipv4.tcp_congestion_control = bbr`, `net.ipv4.tcp_fastopen = 3`, `net.ipv4.tcp_low_latency = 1`, `net.core.netdev_max_backlog = 16384`.
- [ ] **Step 3: Apply and verify sysctl**
Run `sysctl -p /etc/sysctl.d/99-gaming-performance.conf` and verify `sysctl net.ipv4.tcp_congestion_control`.

---

### Task 6: Universal `game-boost` CLI Launcher & End-to-End System Verification

**Files:**
- Create: `/usr/local/bin/game-boost` (chmod +x)

- [ ] **Step 1: Create `/usr/local/bin/game-boost`**
Create a wrapper script that launches any game/command with `gamemoderun` and all Mesa/Vulkan Ice Lake performance environment variables preloaded.
- [ ] **Step 2: Make executable**
Set permissions `chmod 755 /usr/local/bin/game-boost`.
- [ ] **Step 3: Execute comprehensive diagnostic verification**
Run a full verification script checking CPU governor, zRAM size/compression, Mesa Vulkan driver, GameMode daemon, sysctl parameters, and TCP BBR.
