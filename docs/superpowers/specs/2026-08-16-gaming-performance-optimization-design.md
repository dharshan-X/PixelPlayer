# System Performance & Gaming Optimization Specification

**Target Hardware & System**:
- Laptop: HP Laptop 15-dy1xxx
- CPU: Intel(R) Core(TM) i5-1035G1 (4 Cores / 8 Threads @ 1.00 GHz base, 3.60 GHz Turbo)
- GPU: Intel Iris Plus / UHD Graphics G1 (Ice Lake GT1) with `i915` kernel driver
- Memory: 8.0 GB RAM
- Storage: 256 GB Micron NVMe SSD
- OS: Ubuntu 26.04 LTS (Kernel 7.0.0-29-generic x86_64, PREEMPT_DYNAMIC)

---

## 1. Objectives & Success Criteria
1. **Zero Microstutters**: Eliminate frame drops and system freezing caused by heavy disk swap paging and CPU frequency scaling latencies.
2. **Effective Memory Multiplier**: Boost usable memory headroom using high-speed in-RAM zstd zRAM compression (up to ~14GB+ effective headroom) with high priority over SSD disk swap.
3. **Maximum CPU Power & Responsiveness**: Lock CPU governor to `performance` / EPP `performance`, enable irqbalance across all 8 CPU threads, and disable core parking during intensive tasks.
4. **Graphics & Vulkan Driver Optimization**: Maximize Mesa Vulkan (Intel ANV) shader cache, enable multi-threaded OpenGL (`mesa_glthread=true`), and configure hardware video acceleration (`intel-media-va-driver`).
5. **GameMode & Real-time Process Scheduling**: Configure `/etc/gamemode.ini` to auto-elevate game process priority (`nice = -10`, I/O priority 1) and disable sleep/power saving while gaming.
6. **Low Latency Network (TCP BBR)**: Enable FQ packet scheduling with TCP BBR congestion control to reduce ping spikes and packet loss in multiplayer gaming.
7. **Universal Launcher Script**: Provide a system-wide `/usr/local/bin/game-boost` utility to easily launch any game, emulator, or high-performance application with all optimizations pre-loaded.

---

## 2. Technical Architecture & Component Changes

### 2.1 Memory & Virtual Memory (zRAM + sysctl)
- **Package**: `zram-tools`
- **Configuration (`/etc/default/zramswap`)**:
  - `ALGO=zstd`
  - `PERCENT=100` (Creates an 8GB compressed zRAM swap disk in RAM with priority 32767)
  - `PRIORITY=32767`
- **Sysctl Tweaks (`/etc/sysctl.d/99-gaming-performance.conf`)**:
  - `vm.swappiness = 180` (Directs memory management to prefer fast zRAM over physical disk paging)
  - `vm.vfs_cache_pressure = 50` (Preserves directory and inode caches)
  - `vm.watermark_boost_factor = 0` (Prevents kswapd latency spikes)
  - `vm.watermark_scale_factor = 125` (Smoothes memory allocation thresholds)
  - `vm.dirty_ratio = 10`
  - `vm.dirty_background_ratio = 5`
  - `vm.max_map_count = 2147483642` (Required for Steam Proton / Wine / DXVK / Unreal Engine games)

### 2.2 CPU Governor & Interrupts
- **Power Profile**: Set `powerprofilesctl set performance`
- **CPU Scaling**: Set `/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor` to `performance` and energy-performance-preference (`EPP`) to `performance`.
- **System Service (`irqbalance`)**: Enable and start `irqbalance` service to distribute device IRQs across all 8 logical CPU cores.

### 2.3 Mesa & Vulkan Graphics Pipeline
- **Packages**: `mesa-vulkan-drivers`, `vulkan-tools`, `libvulkan1`, `intel-media-va-driver-non-free` (or `intel-media-va-driver`), `vainfo`.
- **Global / Game Environment Profiles (`/etc/environment.d/99-gaming-performance.conf` and wrapper)**:
  - `MESA_SHADER_CACHE_MAX_SIZE=4G` (Avoids repeated runtime shader compilation stutters)
  - `ANV_ENABLE_PIPELINE_CACHE=1` (Intel Vulkan pipeline caching)
  - `mesa_glthread=true` (Multi-threaded OpenGL driver execution)
  - `VK_DRIVER_FILES=/usr/share/vulkan/icd.d/intel_icd.x86_64.json`

### 2.4 Feral GameMode Optimization
- **Configuration (`/etc/gamemode.ini`)**:
  ```ini
  [general]
  desiredgov=performance
  igpu_power_threshold=0.8
  softrealtime=auto
  renice=-10
  ioprio=1
  inhibit_screensaver=1

  [gpu]
  apply_gpu_optimisations=accept-responsibility
  gpu_device=0
  ```
- **Daemon**: Ensure `gamemoded` user service is running and properly authenticated via Polkit / PAM limits.

### 2.5 Low-Latency Networking & File Limits
- **Kernel Networking (`/etc/sysctl.d/99-gaming-performance.conf`)**:
  - `net.core.default_qdisc = fq`
  - `net.ipv4.tcp_congestion_control = bbr`
  - `net.ipv4.tcp_fastopen = 3`
  - `net.ipv4.tcp_low_latency = 1`
  - `net.core.netdev_max_backlog = 16384`
  - `net.core.somaxconn = 8192`
  - `fs.file-max = 2097152`
- **Security Limits (`/etc/security/limits.d/99-gaming.conf`)**:
  - Increase `nofile` (number of open files) soft/hard to `1048576`
  - Set `memlock` to `unlimited`
  - Set `nice` priority to `-20`

### 2.6 Helper CLI Launcher (`game-boost`)
- Script: `/usr/local/bin/game-boost`
- Functionality: Wraps any executed command or game inside `gamemoderun` with Intel Ice Lake optimized Vulkan/OpenGL environment variables pre-exported.

---

## 3. Verification & Testing Plan
1. **zRAM Status**: Run `zramctl` and `swapon --show` to verify 8GB zstd zRAM active with priority 32767.
2. **CPU Governor**: Run `cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor` and verify `performance`.
3. **Sysctl Parameters**: Run `sysctl vm.swappiness vm.max_map_count net.ipv4.tcp_congestion_control` to verify all parameters loaded.
4. **Vulkan & 3D Acceleration**: Run `vulkaninfo --summary` and `glxinfo -B` to verify Intel Vulkan ICD and Mesa OpenGL hardware rendering.
5. **GameMode Status**: Run `gamemoded -s` and `gamemoderun glxgears` or test binary to confirm active optimization hooks.
