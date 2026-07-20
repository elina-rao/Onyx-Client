(function () {
  const canvas = document.getElementById('particles');
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  let particles = [];
  let raf = 0;
  let w = 0;
  let h = 0;

  function resize() {
    w = canvas.width = window.innerWidth;
    h = canvas.height = window.innerHeight;
  }

  function spawn(count) {
    particles = [];
    for (let i = 0; i < count; i++) {
      particles.push({
        x: Math.random() * w,
        y: Math.random() * h,
        r: 1 + Math.random() * 2.4,
        vx: (Math.random() - 0.5) * 0.25,
        vy: -0.1 - Math.random() * 0.35,
        a: 0.15 + Math.random() * 0.45,
        pulse: Math.random() * Math.PI * 2
      });
    }
  }

  function drawBackground(t) {
    const g = ctx.createLinearGradient(0, 0, w, h);
    const shift = (Math.sin(t * 0.0003) + 1) * 0.5;
    g.addColorStop(0, '#0D0D0D');
    g.addColorStop(0.5, shift > 0.5 ? '#1A0A2E' : '#120818');
    g.addColorStop(1, '#0D0D0D');
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, w, h);

    // faint pulsing grid
    ctx.save();
    ctx.strokeStyle = `rgba(123, 47, 190, ${0.04 + shift * 0.04})`;
    ctx.lineWidth = 1;
    const step = 48;
    for (let x = 0; x < w; x += step) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, h);
      ctx.stroke();
    }
    for (let y = 0; y < h; y += step) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(w, y);
      ctx.stroke();
    }
    ctx.restore();
  }

  function frame(t) {
    drawBackground(t);
    for (const p of particles) {
      p.x += p.vx;
      p.y += p.vy;
      p.pulse += 0.02;
      if (p.y < -10) {
        p.y = h + 10;
        p.x = Math.random() * w;
      }
      if (p.x < -10) p.x = w + 10;
      if (p.x > w + 10) p.x = -10;

      const alpha = p.a * (0.65 + 0.35 * Math.sin(p.pulse));
      ctx.beginPath();
      ctx.fillStyle = `rgba(176, 96, 255, ${alpha})`;
      ctx.shadowColor = 'rgba(123, 47, 190, 0.8)';
      ctx.shadowBlur = 12;
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.fill();
      ctx.shadowBlur = 0;
    }
    raf = requestAnimationFrame(frame);
  }

  window.addEventListener('resize', () => {
    resize();
    spawn(Math.min(70, Math.floor((w * h) / 18000)));
  });

  resize();
  spawn(Math.min(70, Math.floor((w * h) / 18000)));
  raf = requestAnimationFrame(frame);

  window.OnyxParticles = {
    stop() {
      cancelAnimationFrame(raf);
    }
  };
})();
