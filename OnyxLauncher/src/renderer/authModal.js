(function () {
  const overlay = document.getElementById('auth-modal');
  const vignette = document.getElementById('vignette');
  const sessionNotice = document.getElementById('session-notice');
  const btnMicrosoft = document.getElementById('btn-microsoft');
  const btnGuest = document.getElementById('btn-guest');
  const guestForm = document.getElementById('guest-form');
  const guestInput = document.getElementById('guest-username');
  const termsLink = document.getElementById('terms-link');

  let busy = false;

  function show(options) {
    options = options || {};
    overlay.classList.remove('hidden');
    vignette.classList.remove('hidden');
    // retrigger entry animation
    const card = overlay.querySelector('.auth-card');
    card.style.animation = 'none';
    // force reflow
    void card.offsetWidth;
    card.style.animation = '';

    if (options.sessionExpired) {
      sessionNotice.classList.remove('hidden');
    } else {
      sessionNotice.classList.add('hidden');
    }
    guestForm.classList.add('hidden');
    guestInput.value = '';
  }

  function hide() {
    overlay.classList.add('hidden');
    vignette.classList.add('hidden');
    guestForm.classList.add('hidden');
  }

  function setBusy(state) {
    busy = state;
    btnMicrosoft.disabled = state;
    btnGuest.disabled = state;
    guestInput.disabled = state;
  }

  btnMicrosoft.addEventListener('click', async () => {
    if (busy) return;
    setBusy(true);
    btnMicrosoft.querySelector('span:last-child').textContent = 'Signing in…';
    try {
      const result = await window.onyx.startMicrosoft();
      if (result.ok) {
        hide();
        window.dispatchEvent(new CustomEvent('onyx:authenticated', { detail: result.session }));
      } else {
        sessionNotice.textContent = result.error || 'Sign-in failed';
        sessionNotice.classList.remove('hidden');
      }
    } catch (err) {
      sessionNotice.textContent = err.message || 'Sign-in failed';
      sessionNotice.classList.remove('hidden');
    } finally {
      setBusy(false);
      btnMicrosoft.querySelector('span:last-child').textContent = 'Continue with Microsoft';
    }
  });

  btnGuest.addEventListener('click', () => {
    if (busy) return;
    if (guestForm.classList.contains('hidden')) {
      guestForm.classList.remove('hidden');
      guestInput.focus();
      return;
    }
    submitGuest();
  });

  guestInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      submitGuest();
    }
  });

  async function submitGuest() {
    if (busy) return;
    const name = guestInput.value.trim();
    if (!name) {
      guestInput.focus();
      return;
    }
    setBusy(true);
    try {
      const result = await window.onyx.playAsGuest(name);
      if (result.ok) {
        hide();
        window.dispatchEvent(new CustomEvent('onyx:authenticated', { detail: result.session }));
      } else {
        sessionNotice.textContent = result.error || 'Guest login failed';
        sessionNotice.classList.remove('hidden');
      }
    } finally {
      setBusy(false);
    }
  }

  termsLink.addEventListener('click', (e) => {
    e.preventDefault();
    window.onyx.getSettings().then((s) => {
      window.onyx.openExternal(s.termsUrl || 'https://onyxrbw.com/terms');
    });
  });

  window.OnyxAuthModal = { show, hide, setBusy };
})();
