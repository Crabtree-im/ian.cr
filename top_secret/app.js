const appState = {
  started: false,
  muted: false,
  sweep: 0,
  beepTimer: null,
  bars: Array.from({ length: 8 }, () => Math.random() * 0.8 + 0.2),
  line: Array.from({ length: 80 }, () => Math.random() * 0.5 + 0.25),
  nodes: [],
  pings: [],
  sound: {
    profile: "stealth",
    masterVolume: 0.45,
    humEnabled: true,
    droneEnabled: true,
    beepsEnabled: true,
    customAmbientEnabled: false,
    customBeepEnabled: false,
    customAmbientUrl: "",
    customBeepUrl: "",
  },
};

const SOUND_PROFILES = {
  stealth: {
    humType: "sine",
    humFreq: 52,
    humLfoRate: 0.25,
    humLfoDepth: 2,
    droneType: "triangle",
    droneFreq: 96,
    droneLfoRate: 0.08,
    droneLfoDepth: 5,
    beepPool: [310, 370, 415, 466],
  },
  cinematic: {
    humType: "sawtooth",
    humFreq: 43,
    humLfoRate: 0.2,
    humLfoDepth: 4,
    droneType: "sine",
    droneFreq: 78,
    droneLfoRate: 0.12,
    droneLfoDepth: 8,
    beepPool: [280, 340, 420, 560],
  },
  retro: {
    humType: "square",
    humFreq: 66,
    humLfoRate: 0.4,
    humLfoDepth: 1.6,
    droneType: "triangle",
    droneFreq: 120,
    droneLfoRate: 0.18,
    droneLfoDepth: 4,
    beepPool: [520, 620, 740, 880],
  },
};

const feeds = [
  "Satellite stream synchronized across 6 relays.",
  "Quantum key verified for channel OMEGA.",
  "Drone cluster rerouted through corridor theta.",
  "Anomaly spike detected in sector V-12.",
  "Signal ghosting present near maritime grid.",
  "Countermeasure simulation running at 93%.",
  "Thermal profile map refreshed with new sweep.",
  "Telemetry burst captured from unknown node.",
  "Handshake accepted: codename NIGHTFALL.",
  "Secure mirror initiated for archive vault.",
];

const radarCanvas = document.getElementById("radarCanvas");
const graphCanvas = document.getElementById("graphCanvas");
const barsCanvas = document.getElementById("barsCanvas");
const lineCanvas = document.getElementById("lineCanvas");
const mapCanvas = document.getElementById("mapCanvas");
const feedEl = document.getElementById("feed");
const bootOverlay = document.getElementById("bootOverlay");
const engageBtn = document.getElementById("engageBtn");
const muteBtn = document.getElementById("muteBtn");
const clockEl = document.getElementById("clock");
const soundProfileEl = document.getElementById("soundProfile");
const masterVolumeEl = document.getElementById("masterVolume");
const layerHumEl = document.getElementById("layerHum");
const layerDroneEl = document.getElementById("layerDrone");
const layerBeepsEl = document.getElementById("layerBeeps");
const customAmbientFileEl = document.getElementById("customAmbientFile");
const customBeepFileEl = document.getElementById("customBeepFile");
const soundFileStatusEl = document.getElementById("soundFileStatus");
const dropOverlayEl = document.getElementById("dropOverlay");
const dropTargetEls = Array.from(document.querySelectorAll("[data-drop-target]"));
const sceneSwitchBtn = document.getElementById("sceneSwitchBtn");
const hackScreenEl = document.getElementById("hackScreen");
const backToHudBtn = document.getElementById("backToHudBtn");
const cycleSceneBtn = document.getElementById("cycleSceneBtn");
const matrixCanvas = document.getElementById("matrixCanvas");
const typeStreamEl = document.getElementById("typeStream");
const hackTitleEl = document.getElementById("hackTitle");
const hackHintEl = document.getElementById("hackHint");
const hackPresetBadgeEl = document.getElementById("hackPresetBadge");
const matrixPanelHeadEl = document.getElementById("matrixPanelHead");
const terminalPanelHeadEl = document.getElementById("terminalPanelHead");

const contexts = [radarCanvas, graphCanvas, barsCanvas, lineCanvas, mapCanvas].map((canvas) =>
  canvas.getContext("2d")
);

let audioCtx = null;
let masterGain = null;
let humOsc = null;
let humLfo = null;
let humLfoGain = null;
let humGain = null;
let droneOsc = null;
let droneLfo = null;
let droneLfoGain = null;
let droneGain = null;
let customAmbientAudio = null;
let dragDepth = 0;
let hackMode = false;
let matrixRafId = 0;
let typewriterTimer = null;
let typeBurstTimer = null;
let matrixDrops = [];
let typeLineBuffer = "";
let hackSceneIndex = 0;

const HACK_SCENES = [
  {
    key: "breach",
    label: "BREACH",
    title: "BLACKSITE TERMINAL",
    matrixHead: "NEURAL STREAM",
    terminalHead: "INJECTION TERMINAL",
    hint: "Type to trigger burst injections. Press J to cycle scenes.",
    matrixChars: "01ABCDEF#%$@?><{}[]",
    matrixFallMin: 0.9,
    matrixFallMax: 1.6,
    matrixGlowMin: 120,
    matrixGlowMax: 230,
    matrixTint: [0.55, 1, 0.75],
    phrases: [
      "proxy.tunnel --region ghost_net --route 6f:11:delta",
      "injector.run --payload mirror_handshake_v9",
      "decrypt --channel sigma --keyfrag 3/7",
      "traceback.open node://atlantic_relay_12 --deep",
      "spoof.identity --profile courier_alpha",
      "reroute uplink --via satcom_shadow --persist",
      "cache.flush --segment blacksite_index --silent",
      "override protocol::sentinel /force /masked",
    ],
  },
  {
    key: "trace",
    label: "TRACE",
    title: "GHOST TRACE CORE",
    matrixHead: "SIGNAL LATTICE",
    terminalHead: "TRACE TERMINAL",
    hint: "Hammer keys for surge traces. Press J to cycle scenes.",
    matrixChars: "TRACE0123456789<>[]{}",
    matrixFallMin: 0.7,
    matrixFallMax: 1.3,
    matrixGlowMin: 130,
    matrixGlowMax: 240,
    matrixTint: [0.45, 0.9, 1],
    phrases: [
      "trace.route --origin relay_09 --depth 6 --mask",
      "scan.echo --target phantom_peer --pattern helix",
      "log.pull --window 00:30 --index spectral",
      "signature.diff --base atlas --candidate shade",
      "reroute.trace --through node:iceline --rapid",
      "obfuscate.route --seed 88af --chain 12",
    ],
  },
  {
    key: "uplink",
    label: "UPLINK",
    title: "ORBITAL UPLINK DECK",
    matrixHead: "TELEMETRY RAIN",
    terminalHead: "SATCOM CONSOLE",
    hint: "Fast typing simulates uplink handshakes. Press J to cycle scenes.",
    matrixChars: "UPLINK6789ABCXYZ<>$%",
    matrixFallMin: 1.1,
    matrixFallMax: 1.9,
    matrixGlowMin: 140,
    matrixGlowMax: 255,
    matrixTint: [1, 0.78, 0.42],
    phrases: [
      "uplink.handshake --band ka --window 4ms",
      "satcom.shift --relay apogee_3 --priority omega",
      "packet.inject --stream ionburst --retry 2",
      "spectrum.scan --beam narrow --noise floor_low",
      "telemetry.patch --target orbital_mesh --apply",
      "channel.lock --constellation horizon_ring --confirm",
    ],
  },
  {
    key: "shadow",
    label: "SHADOW",
    title: "PHANTOM SHADOW GRID",
    matrixHead: "SHADOW VEIL",
    terminalHead: "DECOY ENGINE",
    hint: "Mash keys to flood decoy traffic. Press J to cycle scenes.",
    matrixChars: "SHDW!@#$%^&*()[]{}<>",
    matrixFallMin: 0.85,
    matrixFallMax: 1.7,
    matrixGlowMin: 120,
    matrixGlowMax: 235,
    matrixTint: [1, 0.52, 0.72],
    phrases: [
      "decoy.spawn --count 64 --signature phantom",
      "veil.inject --profile eclipse --duration 45s",
      "mirror.route --source blackglass --fanout 9",
      "noise.forge --channel lavender --density high",
      "identity.rotate --pool ghost_ops --seed c11",
      "footprint.scrub --layer deep --archive null",
    ],
  },
];

function currentHackScene() {
  return HACK_SCENES[hackSceneIndex] ?? HACK_SCENES[0];
}

function applyHackScene() {
  const scene = currentHackScene();
  hackScreenEl.dataset.scene = scene.key;
  hackTitleEl.textContent = `${scene.title} // FICTIONAL VISUALS`;
  hackPresetBadgeEl.textContent = `SCENE: ${scene.label}`;
  matrixPanelHeadEl.textContent = scene.matrixHead;
  terminalPanelHeadEl.textContent = scene.terminalHead;
  hackHintEl.textContent = scene.hint;
}

function cycleHackScene(step = 1) {
  hackSceneIndex = (hackSceneIndex + step + HACK_SCENES.length) % HACK_SCENES.length;
  applyHackScene();
  resizeHackMatrixCanvas();
  if (hackMode) {
    queueFakeCommandBurst(4);
  }
}

function resizeCanvas(canvas) {
  const ratio = window.devicePixelRatio || 1;
  const rect = canvas.getBoundingClientRect();
  canvas.width = Math.max(1, Math.floor(rect.width * ratio));
  canvas.height = Math.max(1, Math.floor(rect.height * ratio));
  const ctx = canvas.getContext("2d");
  ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
}

function resizeHackMatrixCanvas() {
  if (!matrixCanvas) return;
  resizeCanvas(matrixCanvas);
  const width = matrixCanvas.clientWidth;
  const scene = currentHackScene();
  const colWidth = scene.key === "uplink" ? 14 : 16;
  const cols = Math.max(8, Math.floor(width / colWidth));
  matrixDrops = Array.from({ length: cols }, () => Math.random() * 20);
}

function setupNodes() {
  const count = 16;
  appState.nodes = Array.from({ length: count }, (_, idx) => ({
    x: 40 + (idx % 4) * 75 + Math.random() * 35,
    y: 35 + Math.floor(idx / 4) * 55 + Math.random() * 30,
    pulse: Math.random() * Math.PI * 2,
  }));
}

function addTypeLine(text) {
  const line = document.createElement("div");
  line.className = "type-line";
  line.textContent = text;
  typeStreamEl.prepend(line);
  while (typeStreamEl.children.length > 34) {
    typeStreamEl.removeChild(typeStreamEl.lastChild);
  }
}

function queueFakeCommandBurst(intensity = 3) {
  const scene = currentHackScene();
  for (let i = 0; i < intensity; i++) {
    const phrase = scene.phrases[Math.floor(Math.random() * scene.phrases.length)];
    addTypeLine(`> ${phrase}`);
    addTypeLine(`[ok] ${Math.random().toString(16).slice(2, 10)} :: ack`);
  }
}

function runTypewriterLoop() {
  if (typewriterTimer) {
    clearInterval(typewriterTimer);
  }

  let phraseIndex = 0;
  let charIndex = 0;
  typeLineBuffer = "> ";

  typewriterTimer = setInterval(() => {
    if (!hackMode) return;
    const scene = currentHackScene();
    const phrase = scene.phrases[phraseIndex % scene.phrases.length];
    typeLineBuffer = `> ${phrase.slice(0, charIndex)}`;

    if (charIndex >= phrase.length) {
      addTypeLine(typeLineBuffer);
      addTypeLine(`[trace] burst ${Math.floor(Math.random() * 9000 + 1000)} stable`);
      charIndex = 0;
      phraseIndex += 1;
      typeLineBuffer = "> ";
      return;
    }

    charIndex += 2;
  }, 48);
}

function runBurstLoop() {
  if (typeBurstTimer) {
    clearInterval(typeBurstTimer);
  }

  typeBurstTimer = setInterval(() => {
    if (!hackMode) return;
    if (Math.random() > 0.55) {
      queueFakeCommandBurst(2);
    }
  }, 820);
}

function drawMatrixFrame() {
  if (!hackMode) return;
  const scene = currentHackScene();
  const ctx = matrixCanvas.getContext("2d");
  const w = matrixCanvas.clientWidth;
  const h = matrixCanvas.clientHeight;
  const fontSize = scene.key === "uplink" ? 14 : 15;
  const chars = scene.matrixChars;

  ctx.fillStyle = "rgba(2, 10, 12, 0.16)";
  ctx.fillRect(0, 0, w, h);
  ctx.font = `${fontSize}px Share Tech Mono`;

  for (let i = 0; i < matrixDrops.length; i++) {
    const ch = chars[Math.floor(Math.random() * chars.length)];
    const x = i * fontSize;
    const y = matrixDrops[i] * fontSize;
    const glow = scene.matrixGlowMin + Math.floor(Math.random() * (scene.matrixGlowMax - scene.matrixGlowMin));
    const [rTint, gTint, bTint] = scene.matrixTint;
    ctx.fillStyle = `rgb(${Math.floor(glow * rTint)}, ${Math.floor(glow * gTint)}, ${Math.floor(glow * bTint)})`;
    ctx.fillText(ch, x, y);

    if (y > h && Math.random() > 0.975) {
      matrixDrops[i] = 0;
    }
    matrixDrops[i] += scene.matrixFallMin + Math.random() * (scene.matrixFallMax - scene.matrixFallMin);
  }

  matrixRafId = requestAnimationFrame(drawMatrixFrame);
}

function startHackMode() {
  if (hackMode) return;
  hackMode = true;
  applyHackScene();
  resizeHackMatrixCanvas();
  hackScreenEl.classList.remove("hidden");
  hackScreenEl.setAttribute("aria-hidden", "false");
  queueFakeCommandBurst(4);
  runTypewriterLoop();
  runBurstLoop();
  drawMatrixFrame();
}

function stopHackMode() {
  hackMode = false;
  hackScreenEl.classList.add("hidden");
  hackScreenEl.setAttribute("aria-hidden", "true");

  if (matrixRafId) {
    cancelAnimationFrame(matrixRafId);
    matrixRafId = 0;
  }
  if (typewriterTimer) {
    clearInterval(typewriterTimer);
    typewriterTimer = null;
  }
  if (typeBurstTimer) {
    clearInterval(typeBurstTimer);
    typeBurstTimer = null;
  }
}

function updateMasterGain() {
  if (!masterGain || !audioCtx) return;
  const target = appState.muted ? 0 : appState.sound.masterVolume * 0.16;
  masterGain.gain.setTargetAtTime(target, audioCtx.currentTime, 0.08);

  if (customAmbientAudio) {
    customAmbientAudio.volume = appState.muted ? 0 : Math.min(1, appState.sound.masterVolume * 1.2);
  }
}

function applyLayerState() {
  if (!audioCtx) return;

  if (humGain) {
    humGain.gain.setTargetAtTime(
      appState.sound.humEnabled ? 0.42 : 0,
      audioCtx.currentTime,
      0.08
    );
  }

  if (droneGain) {
    droneGain.gain.setTargetAtTime(
      appState.sound.droneEnabled ? 0.36 : 0,
      audioCtx.currentTime,
      0.08
    );
  }

  if (customAmbientAudio) {
    if (appState.sound.customAmbientEnabled && !appState.muted) {
      customAmbientAudio.play().catch(() => {});
    } else {
      customAmbientAudio.pause();
    }
  }
}

function applyProfile() {
  const profile = SOUND_PROFILES[appState.sound.profile] ?? SOUND_PROFILES.stealth;
  if (!audioCtx) return;

  if (humOsc && humLfo && humLfoGain) {
    humOsc.type = profile.humType;
    humOsc.frequency.setTargetAtTime(profile.humFreq, audioCtx.currentTime, 0.2);
    humLfo.frequency.setTargetAtTime(profile.humLfoRate, audioCtx.currentTime, 0.2);
    humLfoGain.gain.setTargetAtTime(profile.humLfoDepth, audioCtx.currentTime, 0.2);
  }

  if (droneOsc && droneLfo && droneLfoGain) {
    droneOsc.type = profile.droneType;
    droneOsc.frequency.setTargetAtTime(profile.droneFreq, audioCtx.currentTime, 0.3);
    droneLfo.frequency.setTargetAtTime(profile.droneLfoRate, audioCtx.currentTime, 0.3);
    droneLfoGain.gain.setTargetAtTime(profile.droneLfoDepth, audioCtx.currentTime, 0.3);
  }
}

function playBeep() {
  if (!audioCtx || !appState.started || appState.muted || !appState.sound.beepsEnabled) return;

  if (appState.sound.customBeepEnabled && appState.sound.customBeepUrl) {
    const clip = new Audio(appState.sound.customBeepUrl);
    clip.volume = Math.min(1, appState.sound.masterVolume);
    clip.play().catch(() => {});
    return;
  }

  const profile = SOUND_PROFILES[appState.sound.profile] ?? SOUND_PROFILES.stealth;
  const freq = profile.beepPool[Math.floor(Math.random() * profile.beepPool.length)];

  const osc = audioCtx.createOscillator();
  const amp = audioCtx.createGain();
  osc.type = "triangle";
  osc.frequency.value = freq;
  amp.gain.value = 0;

  osc.connect(amp);
  amp.connect(masterGain);

  const now = audioCtx.currentTime;
  amp.gain.setValueAtTime(0.0001, now);
  amp.gain.exponentialRampToValueAtTime(0.4, now + 0.015);
  amp.gain.exponentialRampToValueAtTime(0.0001, now + 0.16);

  osc.start(now);
  osc.stop(now + 0.18);
}

function scheduleBeeps() {
  if (appState.beepTimer) {
    clearInterval(appState.beepTimer);
  }

  appState.beepTimer = setInterval(() => {
    if (Math.random() > 0.5) {
      playBeep();
    }
  }, 1200);
}

function updateSoundStatus() {
  const ambientText = appState.sound.customAmbientEnabled ? "ambient: custom" : "ambient: synth";
  const beepText = appState.sound.customBeepEnabled ? "beeps: custom" : "beeps: synth";
  soundFileStatusEl.textContent = `${ambientText} | ${beepText}`;
}

function setCustomAmbientFile(file) {
  if (!file) return;

  if (appState.sound.customAmbientUrl) {
    URL.revokeObjectURL(appState.sound.customAmbientUrl);
  }

  appState.sound.customAmbientUrl = URL.createObjectURL(file);
  appState.sound.customAmbientEnabled = true;

  if (!customAmbientAudio) {
    customAmbientAudio = new Audio();
    customAmbientAudio.loop = true;
  }

  customAmbientAudio.src = appState.sound.customAmbientUrl;
  customAmbientAudio.volume = appState.muted ? 0 : Math.min(1, appState.sound.masterVolume * 1.2);
  if (appState.started && !appState.muted) {
    customAmbientAudio.play().catch(() => {});
  }
  updateSoundStatus();
}

function setCustomBeepFile(file) {
  if (!file) return;

  if (appState.sound.customBeepUrl) {
    URL.revokeObjectURL(appState.sound.customBeepUrl);
  }

  appState.sound.customBeepUrl = URL.createObjectURL(file);
  appState.sound.customBeepEnabled = true;
  updateSoundStatus();
}

function resetDropTargets() {
  dropTargetEls.forEach((el) => el.classList.remove("hot"));
}

function showDropOverlay() {
  dropOverlayEl.classList.add("visible");
}

function hideDropOverlay() {
  dropOverlayEl.classList.remove("visible");
  resetDropTargets();
  dragDepth = 0;
}

function isAudioFile(file) {
  return file && typeof file.type === "string" && file.type.startsWith("audio/");
}

function setupDragAndDrop() {
  window.addEventListener("dragenter", (event) => {
    if (!event.dataTransfer?.types?.includes("Files")) return;
    dragDepth += 1;
    showDropOverlay();
  });

  window.addEventListener("dragover", (event) => {
    if (!event.dataTransfer?.types?.includes("Files")) return;
    event.preventDefault();
  });

  window.addEventListener("dragleave", (event) => {
    if (!event.dataTransfer?.types?.includes("Files")) return;
    dragDepth = Math.max(0, dragDepth - 1);
    if (dragDepth === 0) {
      hideDropOverlay();
    }
  });

  window.addEventListener("drop", (event) => {
    event.preventDefault();
    hideDropOverlay();
  });

  dropTargetEls.forEach((targetEl) => {
    targetEl.addEventListener("dragenter", (event) => {
      if (!event.dataTransfer?.types?.includes("Files")) return;
      event.preventDefault();
      resetDropTargets();
      targetEl.classList.add("hot");
    });

    targetEl.addEventListener("dragover", (event) => {
      if (!event.dataTransfer?.types?.includes("Files")) return;
      event.preventDefault();
      targetEl.classList.add("hot");
    });

    targetEl.addEventListener("dragleave", () => {
      targetEl.classList.remove("hot");
    });

    targetEl.addEventListener("drop", (event) => {
      event.preventDefault();
      const file = event.dataTransfer?.files?.[0];
      if (!isAudioFile(file)) {
        soundFileStatusEl.textContent = "Drop an audio file (.wav, .mp3, .m4a, .ogg).";
        hideDropOverlay();
        return;
      }

      const targetType = targetEl.dataset.dropTarget;
      if (targetType === "ambient") {
        setCustomAmbientFile(file);
      } else {
        setCustomBeepFile(file);
        playBeep();
      }

      hideDropOverlay();
    });
  });
}

function startAudio() {
  if (audioCtx) return;
  audioCtx = new (window.AudioContext || window.webkitAudioContext)();

  masterGain = audioCtx.createGain();
  masterGain.gain.value = 0;
  masterGain.connect(audioCtx.destination);

  humOsc = audioCtx.createOscillator();
  humLfo = audioCtx.createOscillator();
  humLfoGain = audioCtx.createGain();
  humGain = audioCtx.createGain();

  humOsc.connect(humGain);
  humGain.connect(masterGain);
  humLfo.connect(humLfoGain);
  humLfoGain.connect(humOsc.frequency);

  droneOsc = audioCtx.createOscillator();
  droneLfo = audioCtx.createOscillator();
  droneLfoGain = audioCtx.createGain();
  droneGain = audioCtx.createGain();

  droneOsc.connect(droneGain);
  droneGain.connect(masterGain);
  droneLfo.connect(droneLfoGain);
  droneLfoGain.connect(droneOsc.frequency);

  humGain.gain.value = 0;
  droneGain.gain.value = 0;

  applyProfile();
  applyLayerState();
  updateMasterGain();

  humOsc.start();
  humLfo.start();
  droneOsc.start();
  droneLfo.start();
  scheduleBeeps();
}

function toggleMute() {
  appState.muted = !appState.muted;
  muteBtn.textContent = appState.muted ? "SOUND: OFF" : "SOUND: ON";
  updateMasterGain();
}

function syncControlsFromState() {
  soundProfileEl.value = appState.sound.profile;
  masterVolumeEl.value = String(Math.round(appState.sound.masterVolume * 100));
  layerHumEl.checked = appState.sound.humEnabled;
  layerDroneEl.checked = appState.sound.droneEnabled;
  layerBeepsEl.checked = appState.sound.beepsEnabled;
  updateSoundStatus();
}

function setupSoundControlEvents() {
  soundProfileEl.addEventListener("change", () => {
    appState.sound.profile = soundProfileEl.value;
    applyProfile();
    playBeep();
  });

  masterVolumeEl.addEventListener("input", () => {
    appState.sound.masterVolume = Number(masterVolumeEl.value) / 100;
    updateMasterGain();
  });

  layerHumEl.addEventListener("change", () => {
    appState.sound.humEnabled = layerHumEl.checked;
    applyLayerState();
  });

  layerDroneEl.addEventListener("change", () => {
    appState.sound.droneEnabled = layerDroneEl.checked;
    applyLayerState();
  });

  layerBeepsEl.addEventListener("change", () => {
    appState.sound.beepsEnabled = layerBeepsEl.checked;
    if (layerBeepsEl.checked) {
      playBeep();
    }
  });

  customAmbientFileEl.addEventListener("change", () => {
    const [file] = customAmbientFileEl.files;
    if (file) {
      setCustomAmbientFile(file);
    }
  });

  customBeepFileEl.addEventListener("change", () => {
    const [file] = customBeepFileEl.files;
    if (file) {
      setCustomBeepFile(file);
      playBeep();
    }
  });
}

function pushFeedLine() {
  const line = document.createElement("div");
  line.className = "feed-line";
  const now = new Date();
  const stamp = now.toISOString().slice(11, 19);
  const item = feeds[Math.floor(Math.random() * feeds.length)];
  line.textContent = `[${stamp}] ${item}`;
  feedEl.prepend(line);

  while (feedEl.children.length > 18) {
    feedEl.removeChild(feedEl.lastChild);
  }
}

function drawRadar(ctx, w, h, t) {
  const cx = w / 2;
  const cy = h / 2;
  const radius = Math.min(w, h) * 0.42;
  ctx.clearRect(0, 0, w, h);

  ctx.strokeStyle = "rgba(87, 242, 255, 0.35)";
  ctx.lineWidth = 1;
  for (let i = 1; i <= 5; i++) {
    ctx.beginPath();
    ctx.arc(cx, cy, (radius * i) / 5, 0, Math.PI * 2);
    ctx.stroke();
  }

  for (let a = 0; a < 360; a += 30) {
    const rad = (a * Math.PI) / 180;
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(cx + Math.cos(rad) * radius, cy + Math.sin(rad) * radius);
    ctx.stroke();
  }

  appState.sweep = (appState.sweep + 0.012) % (Math.PI * 2);
  const grad = ctx.createRadialGradient(cx, cy, 0, cx, cy, radius);
  grad.addColorStop(0, "rgba(39, 255, 197, 0.25)");
  grad.addColorStop(1, "rgba(39, 255, 197, 0.02)");
  ctx.fillStyle = grad;
  ctx.beginPath();
  ctx.arc(cx, cy, radius, appState.sweep - 0.5, appState.sweep);
  ctx.lineTo(cx, cy);
  ctx.closePath();
  ctx.fill();

  for (let i = 0; i < 8; i++) {
    const a = (i * Math.PI * 2) / 8 + t * 0.0003;
    const r = radius * (0.2 + ((i * 11) % 7) / 10);
    const px = cx + Math.cos(a) * r;
    const py = cy + Math.sin(a) * r;
    const blink = 0.5 + Math.sin(t * 0.005 + i) * 0.5;
    ctx.fillStyle = `rgba(255, 198, 89, ${0.2 + blink * 0.8})`;
    ctx.beginPath();
    ctx.arc(px, py, 2 + blink * 2, 0, Math.PI * 2);
    ctx.fill();
  }
}

function drawGraph(ctx, w, h, t) {
  ctx.clearRect(0, 0, w, h);
  ctx.save();
  ctx.translate(20, 30);

  appState.nodes.forEach((node, i) => {
    for (let j = i + 1; j < appState.nodes.length; j++) {
      if ((i + j) % 4 !== 0) continue;
      const node2 = appState.nodes[j];
      ctx.strokeStyle = "rgba(87, 242, 255, 0.15)";
      ctx.beginPath();
      ctx.moveTo(node.x, node.y);
      ctx.lineTo(node2.x, node2.y);
      ctx.stroke();
    }
  });

  appState.nodes.forEach((node, i) => {
    const pulse = (Math.sin(t * 0.004 + node.pulse + i) + 1) * 0.5;
    ctx.fillStyle = `rgba(39, 255, 197, ${0.4 + pulse * 0.6})`;
    ctx.beginPath();
    ctx.arc(node.x, node.y, 2 + pulse * 4, 0, Math.PI * 2);
    ctx.fill();
  });

  ctx.restore();
}

function drawBars(ctx, w, h, t) {
  ctx.clearRect(0, 0, w, h);
  const pad = 24;
  const innerW = w - pad * 2;
  const innerH = h - pad * 2;
  const bw = innerW / appState.bars.length;

  appState.bars = appState.bars.map((v, i) => {
    const target = 0.2 + ((Math.sin(t * 0.001 + i) + 1) / 2) * 0.8;
    return v + (target - v) * 0.06;
  });

  appState.bars.forEach((v, i) => {
    const x = pad + i * bw + 8;
    const bh = innerH * v;
    const y = h - pad - bh;

    const g = ctx.createLinearGradient(x, y, x, h - pad);
    g.addColorStop(0, "rgba(255, 198, 89, 0.9)");
    g.addColorStop(1, "rgba(255, 77, 103, 0.85)");
    ctx.fillStyle = g;
    ctx.fillRect(x, y, bw - 14, bh);
  });
}

function drawLine(ctx, w, h, t) {
  ctx.clearRect(0, 0, w, h);
  const pad = 18;
  const innerW = w - pad * 2;
  const innerH = h - pad * 2;

  const next = 0.5 + Math.sin(t * 0.0027) * 0.23 + (Math.random() - 0.5) * 0.12;
  appState.line.push(Math.max(0.08, Math.min(0.95, next)));
  appState.line.shift();

  ctx.strokeStyle = "rgba(87, 242, 255, 0.22)";
  for (let i = 0; i < 5; i++) {
    const y = pad + (innerH / 4) * i;
    ctx.beginPath();
    ctx.moveTo(pad, y);
    ctx.lineTo(w - pad, y);
    ctx.stroke();
  }

  ctx.beginPath();
  appState.line.forEach((v, i) => {
    const x = pad + (innerW * i) / (appState.line.length - 1);
    const y = pad + innerH - innerH * v;
    if (i === 0) ctx.moveTo(x, y);
    else ctx.lineTo(x, y);
  });
  ctx.strokeStyle = "rgba(39, 255, 197, 0.9)";
  ctx.lineWidth = 2;
  ctx.stroke();

  ctx.lineTo(w - pad, h - pad);
  ctx.lineTo(pad, h - pad);
  ctx.closePath();
  ctx.fillStyle = "rgba(39, 255, 197, 0.1)";
  ctx.fill();
}

function drawMap(ctx, w, h, t) {
  ctx.clearRect(0, 0, w, h);
  ctx.fillStyle = "rgba(87, 242, 255, 0.08)";
  const blobs = [
    [0.12, 0.3, 0.2, 0.16],
    [0.4, 0.22, 0.2, 0.18],
    [0.7, 0.3, 0.22, 0.2],
    [0.2, 0.62, 0.2, 0.16],
    [0.55, 0.6, 0.32, 0.2],
  ];

  blobs.forEach(([x, y, bw, bh]) => {
    ctx.beginPath();
    ctx.ellipse(x * w, y * h, bw * w * 0.5, bh * h * 0.5, 0, 0, Math.PI * 2);
    ctx.fill();
  });

  for (let i = 0; i < 7; i++) {
    const x = (i * w) / 6;
    ctx.strokeStyle = "rgba(87, 242, 255, 0.12)";
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, h);
    ctx.stroke();
  }

  for (let i = 0; i < 5; i++) {
    const y = (i * h) / 4;
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(w, y);
    ctx.stroke();
  }

  if (appState.pings.length < 4 && Math.random() > 0.96) {
    appState.pings.push({ x: Math.random() * w, y: Math.random() * h, life: 0 });
  }

  appState.pings = appState.pings.filter((p) => p.life < 1);
  appState.pings.forEach((p) => {
    p.life += 0.015;
    const r = p.life * 85;
    ctx.strokeStyle = `rgba(255, 198, 89, ${1 - p.life})`;
    ctx.beginPath();
    ctx.arc(p.x, p.y, r, 0, Math.PI * 2);
    ctx.stroke();
  });
}

function drawAll(t) {
  if (!appState.started) return;

  const [radarCtx, graphCtx, barsCtx, lineCtx, mapCtx] = contexts;

  drawRadar(radarCtx, radarCanvas.clientWidth, radarCanvas.clientHeight, t);
  drawGraph(graphCtx, graphCanvas.clientWidth, graphCanvas.clientHeight, t);
  drawBars(barsCtx, barsCanvas.clientWidth, barsCanvas.clientHeight, t);
  drawLine(lineCtx, lineCanvas.clientWidth, lineCanvas.clientHeight, t);
  drawMap(mapCtx, mapCanvas.clientWidth, mapCanvas.clientHeight, t);

  requestAnimationFrame(drawAll);
}

function updateClock() {
  const now = new Date();
  const hh = String(now.getUTCHours()).padStart(2, "0");
  const mm = String(now.getUTCMinutes()).padStart(2, "0");
  const ss = String(now.getUTCSeconds()).padStart(2, "0");
  clockEl.textContent = `${hh}:${mm}:${ss} UTC`;
}

function attemptFullscreen() {
  const el = document.documentElement;
  if (!document.fullscreenElement && el.requestFullscreen) {
    el.requestFullscreen().catch(() => {});
  }
}

function startSimulation() {
  if (appState.started) return;

  appState.started = true;
  bootOverlay.classList.add("hidden");
  resizeAll();
  setupNodes();
  pushFeedLine();
  pushFeedLine();
  pushFeedLine();
  updateClock();
  setInterval(updateClock, 1000);
  setInterval(pushFeedLine, 950);
  startAudio();
  syncControlsFromState();
  drawAll(0);
  attemptFullscreen();
}

function resizeAll() {
  [radarCanvas, graphCanvas, barsCanvas, lineCanvas, mapCanvas].forEach(resizeCanvas);
  resizeHackMatrixCanvas();
}

window.addEventListener("resize", resizeAll);
engageBtn.addEventListener("click", startSimulation);
muteBtn.addEventListener("click", toggleMute);
setupSoundControlEvents();
syncControlsFromState();
setupDragAndDrop();
sceneSwitchBtn.addEventListener("click", startHackMode);
backToHudBtn.addEventListener("click", stopHackMode);
cycleSceneBtn.addEventListener("click", () => cycleHackScene(1));
applyHackScene();

document.addEventListener("keydown", (event) => {
  if (event.key.toLowerCase() === "f") {
    if (!document.fullscreenElement) {
      attemptFullscreen();
    } else {
      document.exitFullscreen?.();
    }
  }

  if (event.key.toLowerCase() === "m") {
    toggleMute();
  }

  if (event.key.toLowerCase() === "h") {
    if (hackMode) {
      stopHackMode();
    } else {
      startHackMode();
    }
  }

  if (event.key.toLowerCase() === "j") {
    cycleHackScene(1);
  }

  if (hackMode && event.key.length === 1) {
    queueFakeCommandBurst(1 + Math.floor(Math.random() * 3));
  }
});
